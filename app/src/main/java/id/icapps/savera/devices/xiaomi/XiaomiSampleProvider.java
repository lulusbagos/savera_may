/*  Copyright (C) 2023-2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package id.icapps.savera.devices.xiaomi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import id.icapps.savera.devices.AbstractSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.XiaomiActivitySample;
import id.icapps.savera.entities.XiaomiActivitySampleDao;
import id.icapps.savera.entities.XiaomiSleepStageSample;
import id.icapps.savera.entities.XiaomiSleepTimeSample;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivityKind;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.util.RangeMap;

public class XiaomiSampleProvider extends AbstractSampleProvider<XiaomiActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSampleProvider.class);

    public XiaomiSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<XiaomiActivitySample, ?> getSampleDao() {
        return getSession().getXiaomiActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return XiaomiActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return XiaomiActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return XiaomiActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(final int rawType) {
        // TODO
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(final ActivityKind activityKind) {
        // TODO
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        return rawIntensity / 100f;
    }

    @Override
    public XiaomiActivitySample createActivitySample() {
        return new XiaomiActivitySample();
    }

    @Override
    public void addGBActivitySamples(final XiaomiActivitySample[] activitySamples) {
        preserveExistingBiometrics(activitySamples);
        super.addGBActivitySamples(activitySamples);
    }

    @Override
    public void addGBActivitySample(final XiaomiActivitySample activitySample) {
        preserveExistingBiometrics(new XiaomiActivitySample[]{activitySample});
        super.addGBActivitySample(activitySample);
    }

    private void preserveExistingBiometrics(final XiaomiActivitySample[] activitySamples) {
        if (activitySamples == null || activitySamples.length == 0) {
            return;
        }

        int minTimestamp = Integer.MAX_VALUE;
        int maxTimestamp = Integer.MIN_VALUE;
        long deviceId = activitySamples[0].getDeviceId();
        for (final XiaomiActivitySample sample : activitySamples) {
            minTimestamp = Math.min(minTimestamp, sample.getTimestamp());
            maxTimestamp = Math.max(maxTimestamp, sample.getTimestamp());
        }

        final QueryBuilder<XiaomiActivitySample> qb = getSampleDao().queryBuilder();
        qb.where(
                XiaomiActivitySampleDao.Properties.DeviceId.eq(deviceId),
                XiaomiActivitySampleDao.Properties.Timestamp.ge(minTimestamp),
                XiaomiActivitySampleDao.Properties.Timestamp.le(maxTimestamp)
        );

        final Map<Integer, XiaomiActivitySample> existingByTimestamp = new HashMap<>();
        for (final XiaomiActivitySample existing : qb.build().list()) {
            existingByTimestamp.put(existing.getTimestamp(), existing);
        }

        for (final XiaomiActivitySample sample : activitySamples) {
            final XiaomiActivitySample existing = existingByTimestamp.get(sample.getTimestamp());
            if (existing == null) {
                continue;
            }

            if (sample.getSpo2() == null && existing.getSpo2() != null) {
                sample.setSpo2(existing.getSpo2());
            }
            if (sample.getStress() == null && existing.getStress() != null) {
                sample.setStress(existing.getStress());
            }
            if ((sample.getHeartRate() <= 0 || sample.getHeartRate() >= 255) && existing.getHeartRate() > 0 && existing.getHeartRate() < 255) {
                sample.setHeartRate(existing.getHeartRate());
            }
            if (sample.getSteps() == 0 && existing.getSteps() > 0) {
                sample.setSteps(existing.getSteps());
            }
        }
    }

    @Override
    protected List<XiaomiActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        final List<XiaomiActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to);

        overlaySleep(samples, timestamp_from, timestamp_to);

        return samples;
    }

    /**
     * See {@link id.icapps.savera.service.devices.xiaomi.activity.impl.SleepDetailsParser}
     */
    private static ActivityKind getActivityKindForSample(final XiaomiSleepStageSample sample) {
        switch (sample.getStage()) {
            case 2:
                return ActivityKind.DEEP_SLEEP;
            case 3:
                return ActivityKind.LIGHT_SLEEP;
            case 4:
                return ActivityKind.REM_SLEEP;
            default: // default to awake
                return ActivityKind.UNKNOWN;
        }
    }

    /**
     * Overlay sleep states on activity samples, since they are stored on a separate table.
     *
     * @implNote In order to determine whether a sleep session was ongoing at the start of the
     * given range and what the detected sleep stage was at that time, the last sleep stage and
     * sleep time sample before the given range will be queried and included in the results if
     * found.
     */
    public void overlaySleep(final List<XiaomiActivitySample> samples, final int timestamp_from, final int timestamp_to) {
        final RangeMap<Long, ActivityKind> stagesMap = new RangeMap<>(RangeMap.Mode.LOWER_BOUND);

        final XiaomiSleepTimeSampleProvider sleepTimeSampleProvider = new XiaomiSleepTimeSampleProvider(getDevice(), getSession());
        final XiaomiSleepStageSampleProvider sleepStagesSampleProvider = new XiaomiSleepStageSampleProvider(getDevice(), getSession());

        // First populate all samples within this range
        final List<XiaomiSleepTimeSample> sleepTimesWithinRange = sleepTimeSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);
        LOG.trace("Found {} sleep samples between {} and {}", sleepTimesWithinRange.size(), timestamp_from, timestamp_to);

        for (final XiaomiSleepTimeSample sleepTimeSample : sleepTimesWithinRange) {
            stagesMap.put(sleepTimeSample.getWakeupTime(), ActivityKind.UNKNOWN);
            stagesMap.put(sleepTimeSample.getTimestamp(), ActivityKind.LIGHT_SLEEP);
        }

        // Retrieve the last stage before this time range, as the user could have been asleep during
        // the range transition
        final XiaomiSleepStageSample lastSleepStageBeforeRange = sleepStagesSampleProvider.getLastSampleBefore(timestamp_from * 1000L);

        if (lastSleepStageBeforeRange != null) {
            LOG.debug("Last sleep stage before range: ts={}, stage={}", lastSleepStageBeforeRange.getTimestamp(), lastSleepStageBeforeRange.getStage());
            stagesMap.put(lastSleepStageBeforeRange.getTimestamp(), getActivityKindForSample(lastSleepStageBeforeRange));
        }

        // Retrieve all sleep stage samples during the range
        final List<XiaomiSleepStageSample> sleepStagesInRange = sleepStagesSampleProvider.getAllSamples(
                timestamp_from * 1000L,
                timestamp_to * 1000L
        );

        if (!sleepStagesInRange.isEmpty()) {
            // We got actual sleep stages
            LOG.debug("Found {} sleep stage samples between {} and {}", sleepStagesInRange.size(), timestamp_from, timestamp_to);

            for (final XiaomiSleepStageSample stageSample : sleepStagesInRange) {
                stagesMap.put(stageSample.getTimestamp(), getActivityKindForSample(stageSample));
            }
        }


        // Find last sleep sample before the requested range, as the recorded wake up time may be
        // in the current range
        final XiaomiSleepTimeSample lastSleepTimesBeforeRange = sleepTimeSampleProvider.getLastSampleBefore(timestamp_from * 1000L);

        if (lastSleepTimesBeforeRange != null) {
            LOG.debug("Last sleep time before range: ts={}, stage={}", lastSleepTimesBeforeRange.getTimestamp(), lastSleepTimesBeforeRange);

            stagesMap.put(lastSleepTimesBeforeRange.getWakeupTime(), ActivityKind.UNKNOWN);
            stagesMap.put(lastSleepTimesBeforeRange.getTimestamp(), ActivityKind.LIGHT_SLEEP);
        }

        // Find all wake up and sleep samples in the current time range
        final List<XiaomiSleepTimeSample> sleepTimesInRange = sleepTimeSampleProvider.getAllSamples(
                timestamp_from * 1000L,
                timestamp_to * 1000L
        );

        if (!sleepTimesInRange.isEmpty()) {
            LOG.debug("Found {} sleep samples between {} and {}", sleepTimesInRange.size(), timestamp_from, timestamp_to);
            for (final XiaomiSleepTimeSample stageSample : sleepTimesInRange) {
                if (sleepStagesInRange.isEmpty()) {
                    // Only overlay them as light sleep if we don't have actual sleep stages
                    stagesMap.put(stageSample.getTimestamp(), ActivityKind.LIGHT_SLEEP);
                }

                // We need to set the wakeup times, because some bands don't report them in the stage samples (see #3502)
                stagesMap.put(stageSample.getWakeupTime(), ActivityKind.UNKNOWN);
            }
        }

        if (!stagesMap.isEmpty()) {
            LOG.debug("Found {} sleep stage samples between {} and {}", stagesMap.size(), timestamp_from, timestamp_to);

            // If no activity samples exist for a given minute in this range but there IS sleep data,
            // create synthetic 1-minute samples so the sleep overlay can work even when
            // ACTIVITY_DAILY detail files don't cover the sleep window (e.g. overnight sleep).
            final Set<Long> existingTs = new HashSet<>();
            for (final XiaomiActivitySample s : samples) {
                existingTs.add((long) s.getTimestamp());
            }
            final List<XiaomiActivitySample> syntheticSamples = new ArrayList<>();
            for (long ts = (long) timestamp_from; ts <= (long) timestamp_to; ts += 60) {
                if (!existingTs.contains(ts)) {
                    final ActivityKind kind = stagesMap.get(ts * 1000L);
                    if (kind != null && kind != ActivityKind.UNKNOWN) {
                        final XiaomiActivitySample s = createActivitySample();
                        s.setProvider(this);
                        s.setTimestamp((int) ts);
                        s.setRawKind(kind.getCode());
                        s.setRawIntensity(ActivitySample.NOT_MEASURED);
                        syntheticSamples.add(s);
                    }
                }
            }
            samples.addAll(syntheticSamples);

            for (final XiaomiActivitySample sample : samples) {
                final long ts = sample.getTimestamp() * 1000L;
                final ActivityKind sleepType = stagesMap.get(ts);
                if (sleepType != null && !sleepType.equals(ActivityKind.UNKNOWN)) {
                    sample.setRawKind(sleepType.getCode());
                    sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                }
            }
        }
    }
}

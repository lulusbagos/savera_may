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
package id.icapps.savera.service.devices.xiaomi.activity.impl;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import id.icapps.savera.GBApplication;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.database.DBHelper;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.SampleProvider;
import id.icapps.savera.devices.xiaomi.XiaomiSleepStageSampleProvider;
import id.icapps.savera.devices.xiaomi.XiaomiSleepTimeSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.entities.User;
import id.icapps.savera.entities.XiaomiActivitySample;
import id.icapps.savera.entities.XiaomiSleepStageSample;
import id.icapps.savera.entities.XiaomiSleepTimeSample;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.service.devices.xiaomi.XiaomiSupport;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityFileId;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityParser;
import id.icapps.savera.util.GB;

public class SleepDetailsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(SleepDetailsParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        // Seems to come both as DetailType.DETAILS (version 2) and DetailType.SUMMARY (version 4, 5)
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 1:
            case 2:
            case 3:
            case 4:
                headerSize = 1;
                break;
            case 5:
            default:
                if (version > 5) {
                    LOG.warn("Unknown sleep details version {}, attempting to parse as v5", version);
                }
                headerSize = 2;
                break;
        }

        // Current offset in the header, which only advances if we process a field available in the version
        int headerIdx = 0;

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

        final byte[] header = new byte[headerSize];
        buf.get(header);

        final int isAwake = buf.get() & 0xff; // 0/1 - more correctly this would be !isSleepFinish
        headerIdx++;
        final int bedTime = buf.getInt();
        headerIdx++;
        final int wakeupTime = buf.getInt();
        headerIdx++;
        int sleepQuality = -1;
        if (fileId.getVersion() >= 4) {
            if (XiaomiActivityParser.validData(header, headerIdx)) {
                sleepQuality = buf.get() & 0xff;
            }
            headerIdx++;
        }

        if (fileId.getVersion() >= 5) {
            buf.get(new byte[9]); // unknown fields
            final int bedTime2 = buf.getInt();
            final int wakeupTime2 = buf.getInt();
            headerIdx += 5;
        }

        LOG.debug("Sleep sample: bedTime: {}, wakeupTime: {}, isAwake: {}", bedTime, wakeupTime, isAwake);

        final List<XiaomiSleepTimeSample> summaries = new ArrayList<>();

        XiaomiSleepTimeSample sample = new XiaomiSleepTimeSample();
        sample.setTimestamp(bedTime * 1000L);
        sample.setWakeupTime(wakeupTime * 1000L);
        sample.setIsAwake(isAwake == 1);

        // Heart rate samples - parse and store as XiaomiActivitySample
        final Map<Integer, XiaomiActivitySample> hrSpoSamples = new LinkedHashMap<>();
        if (XiaomiActivityParser.validData(header, headerIdx)) {
            LOG.debug("Heart rate samples from offset {}", Integer.toHexString(buf.position()));
            final int unit = buf.getShort() & 0xFFFF; // Time unit in seconds
            final int count = buf.getShort() & 0xFFFF;

            if (count > 0) {
                final int firstRecordTime = (fileId.getVersion() >= 2) ? buf.getInt() : bedTime;

                for (int i = 0; i < count; i++) {
                    final int hr = buf.get() & 0xFF;
                    if (hr > 0 && hr < 255) {
                        final int ts = firstRecordTime + unit * i;
                        final XiaomiActivitySample s = new XiaomiActivitySample();
                        s.setTimestamp(ts);
                        s.setHeartRate(hr);
                        hrSpoSamples.put(ts, s);
                    }
                }
                LOG.debug("Parsed {} HR samples from sleep file", hrSpoSamples.size());
            }
        }
        headerIdx++;

        // SpO2 samples - merge into hrSpoSamples map
        if (XiaomiActivityParser.validData(header, headerIdx)) {
            LOG.debug("SpO\u2082 samples from offset {}", Integer.toHexString(buf.position()));
            final int unit = buf.getShort() & 0xFFFF; // Time unit in seconds
            final int count = buf.getShort() & 0xFFFF;

            if (count > 0) {
                final int firstRecordTime = (fileId.getVersion() >= 2) ? buf.getInt() : bedTime;

                for (int i = 0; i < count; i++) {
                    final int spo2 = buf.get() & 0xFF;
                    if (spo2 > 0 && spo2 <= 100) {
                        final int ts = firstRecordTime + unit * i;
                        XiaomiActivitySample s = hrSpoSamples.get(ts);
                        if (s == null) {
                            s = new XiaomiActivitySample();
                            s.setTimestamp(ts);
                            hrSpoSamples.put(ts, s);
                        }
                        s.setSpo2(spo2);
                    }
                }
                LOG.debug("Parsed SpO2 samples from sleep file, total HR/SpO2 samples: {}", hrSpoSamples.size());
            }
        }
        headerIdx++;

        // snore samples
        if (fileId.getVersion() >= 3) {
            if (XiaomiActivityParser.validData(header, headerIdx)) {
                LOG.debug("Snore level samples from offset {}", Integer.toHexString(buf.position()));
                final int unit = buf.getShort(); // Time unit (i.e sample rate)
                final int count = buf.getShort();

                if (count > 0) {
                    // If version is less than 2 firstRecordTime is bedTime
                    if (fileId.getVersion() >= 2) {
                        final int firstRecordTime = buf.getInt();
                    }

                    // Skip count samples - each sample is a float
                    //   timestamp of each sample is firstRecordTime + (unit * index)
                    buf.position(buf.position() + count * 4);
                }
            }
            headerIdx++;
        }

        final List<XiaomiSleepStageSample> stages = new ArrayList<>();
        LOG.debug("Sleep stage packets from offset {}", Integer.toHexString(buf.position()));

        // Do not crash if we face a buffer underflow, as the next parsing is not 100% fool-proof,
        // and we still want to persist whatever we got so far
        boolean stagesParseFailed = false;
        try {
            while (buf.remaining() >= 17) {
                if (!readStagePacketHeader(buf)) {
                    break;
                }

                final int headerLen = buf.get() & 0xFF; // this seems to always be 17

                // This timestamp is kind of weird, is seems to sometimes be in seconds
                // and other times in nanoseconds. Message types 16 and 17 are in seconds
                final long ts = buf.getLong();
                final int parity = buf.get() & 0xFF; // sum of stage bit count should be uneven
                final int type = buf.get() & 0xFF;
                final int dataLen = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);

                // Known types:
                //  - acc_unk = 0,
                //  - ppg_unk = 1,
                //  - fall_asleep = 2,
                //  - wake_up = 3,
                //  - switch_ts_unk1 = 12,
                //  - switch_ts_unk2 = 13,
                //  - Summary = 16,
                //  - Stages = 17

                if (type == 0x2 || type == 0x3 || type == 0x9 || type == 0xc || type == 0xd || type == 0xe || type == 0xf) {
                    // the bytes reserved for the data length are believed to be flags, as they
                    // do not actually have any data following the headers
                    continue;
                }

                final byte[] data = new byte[dataLen];
                buf.get(data);

                final ByteBuffer dataBuf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

                if (type == 16) {
                    final int data_0 = dataBuf.get() & 0xFF;
                    final int sleep_index = data_0 >> 4;
                    final int wake_count = data_0 & 0x0F;

                    final int sleep_duration = dataBuf.getShort() & 0xFFFF;
                    final int wake_duration = dataBuf.getShort() & 0xFFFF;
                    final int light_duration = dataBuf.getShort() & 0xFFFF;
                    final int rem_duration = dataBuf.getShort() & 0xFFFF;
                    final int deep_duration = dataBuf.getShort() & 0xFFFF;

                    final int data_1 = dataBuf.get() & 0xFF;
                    final boolean has_rem = (data_1 >> 4) == 1;
                    final boolean has_stage = (data_1 >> 2) == 1;

                    // Could probably be an "awake" duration after sleep
                    final int unk_duration_minutes = dataBuf.get() & 0xFF;

                    if (sample == null) {
                        sample = new XiaomiSleepTimeSample();
                    }

                    sample.setTimestamp(bedTime * 1000L);
                    sample.setWakeupTime(wakeupTime * 1000L);
                    sample.setTotalDuration(sleep_duration);
                    sample.setDeepSleepDuration(deep_duration);
                    sample.setLightSleepDuration(light_duration);
                    sample.setRemSleepDuration(rem_duration);
                    sample.setAwakeDuration(wake_duration);

                    // FIXME: This is an array, but we end up persisting only the last sample, since
                    //        the timestamp is the primary key
                    summaries.add(sample);
                    sample = null;
                } else if (type == 17) { // Stages
                    long currentTime = ts * 1000;
                    for (int i = 0; i < dataLen / 2; i++) {
                        // when the change to the phase occurs
                        final int val = dataBuf.getShort() & 0xFFFF;

                        final int stage = val >> 12;
                        final int offsetMinutes = val & 0xFFF;

                        final XiaomiSleepStageSample stageSample = new XiaomiSleepStageSample();
                        stageSample.setTimestamp(currentTime);
                        stageSample.setStage(decodeStage(stage));
                        stages.add(stageSample);

                        currentTime += offsetMinutes * 60000;
                    }
                }
            }
        } catch (final BufferUnderflowException e) {
            LOG.warn("Buffer underflow while parsing sleep stages...", e);
            stagesParseFailed = true;
        }

        if (summaries.isEmpty()) {
            // We did not manage to find sleep stage samples - ensure we at least persist the base one
            summaries.add(sample);
        }

        // Persist HR and SpO2 samples from sleep file as XiaomiActivitySample
        if (!hrSpoSamples.isEmpty()) {
            LOG.debug("Persisting {} sleep HR/SpO2 activity samples", hrSpoSamples.size());
            try (DBHandler handler = GBApplication.acquireDB()) {
                final DaoSession session = handler.getDaoSession();
                final GBDevice gbDevice = support.getDevice();
                final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
                final SampleProvider<XiaomiActivitySample> sampleProvider =
                        (SampleProvider<XiaomiActivitySample>) coordinator.getSampleProvider(gbDevice, session);
                final Device device = DBHelper.getDevice(gbDevice, session);
                final User user = DBHelper.getUser(session);

                for (final XiaomiActivitySample s : hrSpoSamples.values()) {
                    s.setDevice(device);
                    s.setUser(user);
                    s.setProvider(sampleProvider);
                }
                sampleProvider.addGBActivitySamples(hrSpoSamples.values().toArray(new XiaomiActivitySample[0]));
            } catch (final Exception e) {
                LOG.error("Error saving sleep HR/SpO2 samples", e);
            }
        }

        // save all the samples that we got
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = support.getDevice();

            final XiaomiSleepTimeSampleProvider sampleProvider = new XiaomiSleepTimeSampleProvider(gbDevice, session);

            for (final XiaomiSleepTimeSample summary : summaries) {
                summary.setDevice(DBHelper.getDevice(gbDevice, session));
                summary.setUser(DBHelper.getUser(session));

                // Check if there is already a later sleep sample - if so, ignore this one
                // Samples for the same sleep will always have the same bedtime (timestamp), but we might get
                // multiple bedtimes until the user wakes up
                final List<XiaomiSleepTimeSample> existingSamples = sampleProvider.getAllSamples(summary.getTimestamp(), summary.getTimestamp());
                if (!existingSamples.isEmpty()) {
                    final XiaomiSleepTimeSample existingSample = existingSamples.get(0);
                    if (existingSample.getWakeupTime() > summary.getWakeupTime()) {
                        LOG.warn("Ignoring sleep sample - existing sample is more recent ({})", existingSample.getWakeupTime());
                        continue;
                    }
                }

                sampleProvider.addSample(summary);
            }
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving sleep sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving sleep sample", e);
            return false;
        }

        if (!stagesParseFailed && !stages.isEmpty()) {
            LOG.debug("Persisting {} sleep stage samples", stages.size());

            // Save the sleep stage samples
            try (DBHandler handler = GBApplication.acquireDB()) {
                final DaoSession session = handler.getDaoSession();
                final GBDevice gbDevice = support.getDevice();
                final Device device = DBHelper.getDevice(gbDevice, session);
                final User user = DBHelper.getUser(session);

                final XiaomiSleepStageSampleProvider sampleProvider = new XiaomiSleepStageSampleProvider(gbDevice, session);

                for (final XiaomiSleepStageSample stageSample : stages) {
                    stageSample.setDevice(device);
                    stageSample.setUser(user);
                }

                sampleProvider.addSamples(stages);
            } catch (final Exception e) {
                GB.toast(support.getContext(), "Error saving sleep stage samples", Toast.LENGTH_LONG, GB.ERROR);
                LOG.error("Error saving sleep stage samples", e);
                return false;
            }
        }

        return !stagesParseFailed;
    }

    private static boolean readStagePacketHeader(final ByteBuffer buffer) {
        while (buffer.remaining() >= 17) {
            if (buffer.getInt() != 0xfffcfafb) {
                // rollback to second byte of header
                buffer.position(buffer.position() - 3);
                continue;
            }

            return true;
        }
        return false;
    }

    private static int decodeStage(int rawStage) {
        switch (rawStage) {
            case 0:
                return 5; // AWAKE
            case 1:
                return 3; // LIGHT_SLEEP
            case 2:
                return 2; // DEEP_SLEEP
            case 3:
                return 4; // REM_SLEEP
            case 4:
                return 0; // NOT_SLEEP
            default:
                return 1; // N/A
        }
    }
}

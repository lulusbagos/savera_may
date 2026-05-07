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
package id.icapps.savera.service.devices.huami.operations.fetch;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.GregorianCalendar;
import java.util.List;

import id.icapps.savera.GBApplication;
import id.icapps.savera.R;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.database.DBHelper;
import id.icapps.savera.devices.huami.HuamiCoordinator;
import id.icapps.savera.devices.huami.HuamiSpo2SampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.entities.HuamiSpo2Sample;
import id.icapps.savera.entities.User;
import id.icapps.savera.model.Spo2Sample;
import id.icapps.savera.service.devices.huami.HuamiSupport;
import id.icapps.savera.util.GB;

/**
 * An operation that fetches SPO2 data for sleep measurements (this requires sleep breathing quality enabled).
 */
public class FetchSpo2SleepOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSpo2SleepOperation.class);

    public FetchSpo2SleepOperation(final HuamiSupport support) {
        super(support, HuamiFetchDataType.SPO2_SLEEP);
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_spo2_data);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        if ((bytes.length - 1) % 30 != 0) {
            LOG.error("Unexpected length for sleep spo2 data {}, not divisible by 30", bytes.length);
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final int version = buf.get() & 0xff;
        if (version != 2) {
            LOG.error("Unknown sleep spo2 data version {}", version);
            return false;
        }

        final List<HuamiSpo2Sample> samples = new ArrayList<>();

        while (buf.position() < bytes.length) {
            final long timestampSeconds = buf.getInt();
            // this doesn't match the spo2 value returned by FetchSpo2NormalOperation.. it's often 100 when the other is 99, but not always
            final int spo2 = buf.get() & 0xff;

            final int duration = buf.get() & 0xff;
            final byte[] spo2High = new byte[6];
            final byte[] spo2Low = new byte[6];
            final byte[] signalQuality = new byte[8];
            final byte[] extend = new byte[4];
            buf.get(spo2High);
            buf.get(spo2Low);
            buf.get(signalQuality);
            buf.get(extend);

            timestamp.setTimeInMillis(timestampSeconds * 1000L);

            LOG.debug(
                    "SPO2 (sleep) at {}: {} duration={} high={} low={} signalQuality={}, extend={}",
                    timestamp.getTime(),
                    spo2,
                    duration,
                    spo2High,
                    spo2Low,
                    signalQuality,
                    extend
            );

            if (spo2 > 0) {
                final HuamiSpo2Sample sample = new HuamiSpo2Sample();
                sample.setTimestamp(timestamp.getTimeInMillis());
                sample.setTypeNum(Spo2Sample.Type.AUTOMATIC.getNum());
                sample.setSpo2(spo2);
                samples.add(sample);
            }
        }

        return persistSamples(samples);
    }

    protected boolean persistSamples(final List<HuamiSpo2Sample> samples) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final HuamiCoordinator coordinator = (HuamiCoordinator) getDevice().getDeviceCoordinator();
            final HuamiSpo2SampleProvider sampleProvider = coordinator.getSpo2SampleProvider(getDevice(), session);

            for (final HuamiSpo2Sample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} sleep spo2 samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving sleep spo2 samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSpo2sleepTimeMillis";
    }
}

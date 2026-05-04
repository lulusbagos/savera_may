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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import id.icapps.savera.GBApplication;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.database.DBHelper;
import id.icapps.savera.devices.xiaomi.XiaomiDailySummarySampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.XiaomiDailySummarySample;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.service.devices.xiaomi.XiaomiSupport;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityFileId;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityParser;
import id.icapps.savera.util.GB;

public class DailySummaryParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(DailySummaryParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 5:
            default:
                if (version != 5) {
                    LOG.warn("Unknown daily summary version {}, attempting to parse as v5", version);
                }
                headerSize = 4;
                break;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Header: {}", GB.hexdump(header));

        final XiaomiDailySummarySample sample = new XiaomiDailySummarySample();
        sample.setTimestamp(fileId.getTimestamp().getTime());
        sample.setTimezone(fileId.getTimezone());

        sample.setSteps(buf.getInt());
        final int unk1 = buf.get() & 0xff; // 0
        final int unk2 = buf.get() & 0xff; // 0
        final int unk3 = buf.get() & 0xff; // 0
        sample.setHrResting(buf.get() & 0xff);
        sample.setHrMax(buf.get() & 0xff);
        sample.setHrMaxTs(buf.getInt());
        sample.setHrMin(buf.get() & 0xff);
        sample.setHrMinTs(buf.getInt());
        sample.setHrAvg(buf.get() & 0xff);
        sample.setStressAvg(buf.get() & 0xff);
        sample.setStressMax(buf.get() & 0xff);
        sample.setStressMin(buf.get() & 0xff);
        final byte[] standingArr = new byte[3];
        buf.get(standingArr);
        // each bit represents one hour where the user was standing up for that day,
        // starting at 00:00-01:00. Let's convert it to an int
        int standing = (standingArr[0] | (standingArr[1] << 8) | (standingArr[2] << 16)) & 0x00FFFFFF;
        sample.setStanding(standing);
        sample.setCalories((int) buf.getShort());
        final int unk7 = buf.get() & 0xff; // 0
        final int unk8 = buf.get() & 0xff; // 0
        final int unk9 = buf.get() & 0xff; // 0
        sample.setSpo2Max(buf.get() & 0xff);
        sample.setSpo2MaxTs(buf.getInt());
        sample.setSpo2Min(buf.get() & 0xff);
        sample.setSpo2MinTs(buf.getInt());
        sample.setSpo2Avg(buf.get() & 0xff);
        sample.setTrainingLoadDay((int) buf.getShort());
        sample.setTrainingLoadWeek((int) buf.getShort());
        sample.setTrainingLoadLevel(buf.get() & 0xff); // TODO confirm - 1 for low training load level?
        sample.setVitalityIncreaseLight(buf.get() & 0xff);
        sample.setVitalityIncreaseModerate(buf.get() & 0xff);
        sample.setVitalityIncreaseHigh(buf.get() & 0xff);
        sample.setVitalityCurrent((int) buf.getShort());

        LOG.debug("Persisting 1 daily summary sample");

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice device = support.getDevice();

            sample.setDevice(DBHelper.getDevice(device, session));
            sample.setUser(DBHelper.getUser(session));

            final XiaomiDailySummarySampleProvider sampleProvider = new XiaomiDailySummarySampleProvider(device, session);
            sampleProvider.addSample(sample);
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving daily summary", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving daily summary", e);
            return false;
        }

        return true;
    }
}

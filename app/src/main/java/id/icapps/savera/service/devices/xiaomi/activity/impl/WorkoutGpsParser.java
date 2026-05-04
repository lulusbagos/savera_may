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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import id.icapps.savera.GBApplication;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.database.DBHelper;
import id.icapps.savera.entities.BaseActivitySummary;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.entities.User;
import id.icapps.savera.export.ActivityTrackExporter;
import id.icapps.savera.export.GPXExporter;
import id.icapps.savera.model.ActivityKind;
import id.icapps.savera.model.ActivityPoint;
import id.icapps.savera.model.ActivityTrack;
import id.icapps.savera.model.GPSCoordinate;
import id.icapps.savera.service.devices.xiaomi.XiaomiSupport;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityFileId;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityParser;
import id.icapps.savera.util.DateTimeUtils;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.GB;

public class WorkoutGpsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutGpsParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        final int sampleSize;
        switch (version) {
            case 1:
            case 2:
                headerSize = 1;
                sampleSize = 18;
                break;
            default:
                LOG.warn("Unable to parse workout gps version {}", fileId.getVersion());
                return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.limit(buf.limit() - 4); // discard crc at the end
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }
        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Workout gps Header: {}", GB.hexdump(header));

        if ((buf.limit() - buf.position()) % sampleSize != 0) {
            LOG.warn("Remaining data in the buffer is not a multiple of {}", sampleSize);
        }

        final ActivityTrack activityTrack = new ActivityTrack();

        while (buf.position() < buf.limit()) {
            final int ts = buf.getInt();
            final float longitude = buf.getFloat();
            final float latitude = buf.getFloat();
            final int unk1 = buf.getInt(); // 0
            final float speed = (buf.getShort() >> 2) / 10.0f;

            final ActivityPoint ap = new ActivityPoint(new Date(ts * 1000L));
            ap.setLocation(new GPSCoordinate(longitude, latitude, 0));
            activityTrack.addTrackPoint(ap);

            LOG.trace("ActivityPoint: ts={} lon={} lat={} unk1={} speed={}", ts, longitude, latitude, unk1, speed);
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(support.getDevice(), session);
            final User user = DBHelper.getUser(session);

            // Find the matching summary
            final BaseActivitySummary summary = findOrCreateBaseActivitySummary(session, device, user, fileId);

            // Set the info on the activity track
            activityTrack.setUser(user);
            activityTrack.setDevice(device);
            activityTrack.setName(ActivityKind.fromCode(summary.getActivityKind()).getLabel(support.getContext()));

            // Save the raw bytes
            final String rawBytesPath = saveRawBytes(fileId, bytes);

            // Save the gpx file
            final GPXExporter exporter = new GPXExporter();

            final String gpxFileName = FileUtils.makeValidFileName("saverawatch-" + DateTimeUtils.formatIso8601(fileId.getTimestamp()) + ".gpx");
            final File gpxTargetFile = new File(FileUtils.getExternalFilesDir(), gpxFileName);

            boolean exportGpxSuccess = true;
            try {
                exporter.performExport(activityTrack, gpxTargetFile);
            } catch (final ActivityTrackExporter.GPXTrackEmptyException ex) {
                exportGpxSuccess = false;
                GB.toast(support.getContext(), "This activity does not contain GPX tracks.", Toast.LENGTH_LONG, GB.ERROR, ex);
            }

            if (exportGpxSuccess) {
                summary.setGpxTrack(gpxTargetFile.getAbsolutePath());
            }
            if (rawBytesPath != null) {
                summary.setRawDetailsPath(rawBytesPath);
            }
            session.getBaseActivitySummaryDao().insertOrReplace(summary);
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving workout gps", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    private String saveRawBytes(final XiaomiActivityFileId fileId, final byte[] bytes) {
        try {
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), "rawDetails");
            //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
            final File targetFile = new File(targetFolder, fileId.getFilename());
            FileOutputStream outputStream = new FileOutputStream(targetFile);
            outputStream.write(fileId.toBytes());
            outputStream.write(bytes);
            outputStream.close();
            return targetFile.getAbsolutePath();
        } catch (final IOException e) {
            LOG.error("Failed to save raw bytes", e);
        }

        return null;
    }
}

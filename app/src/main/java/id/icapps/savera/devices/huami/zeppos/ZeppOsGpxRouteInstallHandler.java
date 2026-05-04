/*  Copyright (C) 2023-2024 Daniel Dakhno, José Rebelo

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
package id.icapps.savera.devices.huami.zeppos;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import id.icapps.savera.R;
import id.icapps.savera.activities.InstallActivity;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.InstallHandler;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.GenericItem;
import id.icapps.savera.service.devices.huami.zeppos.operations.ZeppOsGpxRouteFile;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.UriHelper;

public class ZeppOsGpxRouteInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsGpxRouteInstallHandler.class);

    protected final Context mContext;
    private ZeppOsGpxRouteFile file;

    public ZeppOsGpxRouteInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }
        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            final byte[] rawBytes = FileUtils.readAll(in, 1024 * 1024); // 1MB
            final ZeppOsGpxRouteFile gpxFile = new ZeppOsGpxRouteFile(rawBytes);
            if (gpxFile.isValid()) {
                this.file = gpxFile;
            }
        } catch (final Exception e) {
            LOG.error("Failed to read file", e);
        }
    }

    @Override
    public boolean isValid() {
        return file != null;
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof ZeppOsCoordinator)) {
            LOG.warn("Coordinator is not a ZeppOsCoordinator: {}", coordinator.getClass());
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }
        final ZeppOsCoordinator zeppOsCoordinator = (ZeppOsCoordinator) coordinator;
        if (!zeppOsCoordinator.supportsGpxUploads()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        final GenericItem fwItem = createInstallItem(device);
        fwItem.setIcon(coordinator.getDefaultIconResource());

        if (file == null) {
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_incompatible_version));
            installActivity.setInfoText(mContext.getString(R.string.fwinstaller_firmware_not_compatible_to_device));
            installActivity.setInstallEnabled(false);
            return;
        }

        final StringBuilder builder = new StringBuilder();
        final String gpxRoute = mContext.getString(R.string.kind_gpx_route);
        builder.append(mContext.getString(R.string.fw_upgrade_notice, gpxRoute));
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
    }

    public ZeppOsGpxRouteFile getFile() {
        return file;
    }

    private GenericItem createInstallItem(final GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final String firmwareName = mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                mContext.getString(R.string.kind_gpx_route),
                file.getName()
        );
        return new GenericItem(firmwareName);
    }
}

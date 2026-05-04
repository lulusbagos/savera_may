/*  Copyright (C) 2020-2024 Andreas Shimokawa, José Rebelo

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
package id.icapps.savera.service.devices.huami.amazfitgtr;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import id.icapps.savera.devices.huami.HuamiCoordinator;
import id.icapps.savera.devices.huami.HuamiFWHelper;
import id.icapps.savera.devices.huami.amazfitgtr.AmazfitGTRLiteFWHelper;
import id.icapps.savera.service.btle.TransactionBuilder;
import id.icapps.savera.service.devices.huami.amazfitgts.AmazfitGTSSupport;
import id.icapps.savera.service.devices.huami.operations.update.UpdateFirmwareOperation;
import id.icapps.savera.service.devices.huami.operations.update.UpdateFirmwareOperationNew;
import id.icapps.savera.util.Version;

public class AmazfitGTRLiteSupport extends AmazfitGTSSupport {

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitGTRLiteFWHelper(uri, context);
    }

    // override to skip requesting GPS version
    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);

        if (HuamiCoordinator.getOverwriteSettingsOnConnection(getDevice().getAddress())) {
            setLanguage(builder);
        }
    }

    @Override
    public UpdateFirmwareOperation createUpdateFirmwareOperation(Uri uri) {
        return new UpdateFirmwareOperationNew(uri, this);
    }

    @Override
    protected void handleDeviceInfo(id.icapps.savera.service.btle.profiles.deviceinfo.DeviceInfo info) {
        super.handleDeviceInfo(info);
        if (gbDevice.getFirmwareVersion() != null) {
            Version version = new Version(gbDevice.getFirmwareVersion());
            if (version.compareTo(new Version("1.0.0.33")) >= 0) {
                mActivitySampleSize = 8;
            }
        }
    }
}

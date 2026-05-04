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
package id.icapps.savera.devices.huami;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import id.icapps.savera.devices.AbstractTimeSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.HuamiHeartRateRestingSample;
import id.icapps.savera.entities.HuamiHeartRateRestingSampleDao;
import id.icapps.savera.impl.GBDevice;

public class HuamiHeartRateRestingSampleProvider extends AbstractTimeSampleProvider<HuamiHeartRateRestingSample> {
    public HuamiHeartRateRestingSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuamiHeartRateRestingSample, ?> getSampleDao() {
        return getSession().getHuamiHeartRateRestingSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuamiHeartRateRestingSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuamiHeartRateRestingSampleDao.Properties.DeviceId;
    }

    @Override
    public HuamiHeartRateRestingSample createSample() {
        return new HuamiHeartRateRestingSample();
    }
}

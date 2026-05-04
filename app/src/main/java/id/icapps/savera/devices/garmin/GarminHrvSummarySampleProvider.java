/*  Copyright (C) 2024 José Rebelo

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
package id.icapps.savera.devices.garmin;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import id.icapps.savera.devices.AbstractTimeSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.GarminHrvSummarySample;
import id.icapps.savera.entities.GarminHrvSummarySampleDao;
import id.icapps.savera.impl.GBDevice;

public class GarminHrvSummarySampleProvider extends AbstractTimeSampleProvider<GarminHrvSummarySample> {
    public GarminHrvSummarySampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<GarminHrvSummarySample, ?> getSampleDao() {
        return getSession().getGarminHrvSummarySampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GarminHrvSummarySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GarminHrvSummarySampleDao.Properties.DeviceId;
    }

    @Override
    public GarminHrvSummarySample createSample() {
        return new GarminHrvSummarySample();
    }
}

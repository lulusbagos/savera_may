/*  Copyright (C) 2024 Arjan Schrijver

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
package id.icapps.savera.devices.colmi.samples;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import id.icapps.savera.devices.AbstractTimeSampleProvider;
import id.icapps.savera.entities.ColmiHrvSummarySample;
import id.icapps.savera.entities.ColmiHrvSummarySampleDao;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.impl.GBDevice;

public class ColmiHrvSummarySampleProvider extends AbstractTimeSampleProvider<ColmiHrvSummarySample> {
    public ColmiHrvSummarySampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<ColmiHrvSummarySample, ?> getSampleDao() {
        return getSession().getColmiHrvSummarySampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return ColmiHrvSummarySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return ColmiHrvSummarySampleDao.Properties.DeviceId;
    }

    @Override
    public ColmiHrvSummarySample createSample() {
        return new ColmiHrvSummarySample();
    }
}

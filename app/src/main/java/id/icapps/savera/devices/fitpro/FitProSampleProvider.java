/*  Copyright (C) 2021-2024 Petr Vaněk

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

package id.icapps.savera.devices.fitpro;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import id.icapps.savera.devices.AbstractSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.FitProActivitySample;
import id.icapps.savera.entities.FitProActivitySampleDao;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivityKind;

public class FitProSampleProvider extends AbstractSampleProvider<FitProActivitySample> {
    public FitProSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<FitProActivitySample, ?> getSampleDao() {
        return getSession().getFitProActivitySampleDao();
    }


    // as per FitProDeviceSupport.rawActivityKindToUniqueKind

    @Override
    public ActivityKind normalizeType(int rawType) {
        switch (rawType) {
            case 1:
                return ActivityKind.ACTIVITY;
            case 11:
                return ActivityKind.DEEP_SLEEP;
            case 12:
                return ActivityKind.LIGHT_SLEEP;
            default:
                return ActivityKind.UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        switch (activityKind) {
            case DEEP_SLEEP:
                return 11;
            case LIGHT_SLEEP:
                return 12;
            case ACTIVITY:
            default:
                return 1;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 2000f; //samples are per 5 minutes, so this should be sufficient
    }

    @Override
    public FitProActivitySample createActivitySample() {
        return new FitProActivitySample();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return FitProActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return FitProActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return FitProActivitySampleDao.Properties.DeviceId;
    }
}
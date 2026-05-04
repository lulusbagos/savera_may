/*  Copyright (C) 2021-2024 ITCactus, Patric Gruber

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
package id.icapps.savera.devices.pinetime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Optional;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import id.icapps.savera.devices.AbstractSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.PineTimeActivitySample;
import id.icapps.savera.entities.PineTimeActivitySampleDao;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivityKind;

public class PineTimeActivitySampleProvider extends AbstractSampleProvider<PineTimeActivitySample> {
    public PineTimeActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<PineTimeActivitySample, ?> getSampleDao() {
        return getSession().getPineTimeActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return PineTimeActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return PineTimeActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PineTimeActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    /**
     * Factory method to creates an empty sample of the correct type for this sample provider
     *
     * @return the newly created "empty" sample
     */
    @Override
    public PineTimeActivitySample createActivitySample() {
        return new PineTimeActivitySample();
    }

    public Optional<PineTimeActivitySample> getSampleForTimestamp(int timestamp) {
        List<PineTimeActivitySample> foundSamples = this.getGBActivitySamples(timestamp, timestamp);
        if (foundSamples.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(foundSamples.get(0));
    }
}

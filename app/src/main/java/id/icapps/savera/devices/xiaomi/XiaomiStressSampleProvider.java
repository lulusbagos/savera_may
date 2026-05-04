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
package id.icapps.savera.devices.xiaomi;

import id.icapps.savera.devices.AbstractSampleToTimeSampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.XiaomiActivitySample;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.StressSample;

public class XiaomiStressSampleProvider extends AbstractSampleToTimeSampleProvider<StressSample, XiaomiActivitySample> {
    public XiaomiStressSampleProvider(final GBDevice device, final DaoSession session) {
        super(new XiaomiSampleProvider(device, session), device, session);
    }

    @Override
    protected StressSample convertSample(final XiaomiActivitySample sample) {
        if (sample.getStress() == null || sample.getStress() == 0) {
            return null;
        }

        return new XiaomiStressSample(
                sample.getTimestamp() * 1000L,
                sample.getStress()
        );
    }

    protected static class XiaomiStressSample implements StressSample {
        private final long timestamp;
        private final int stress;

        public XiaomiStressSample(final long timestamp, final int stress) {
            this.timestamp = timestamp;
            this.stress = stress;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Type getType() {
            return Type.UNKNOWN;
        }

        @Override
        public int getStress() {
            return stress;
        }
    }
}

/*  Copyright (C) 2024 Damien Gaignon

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
package id.icapps.savera.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import id.icapps.savera.GBApplication;
import id.icapps.savera.devices.huawei.HuaweiPacket;
import id.icapps.savera.devices.huawei.packets.FitnessData;
import id.icapps.savera.devices.huawei.packets.FitnessData.MotionGoal;
import id.icapps.savera.model.ActivityUser;
import id.icapps.savera.service.devices.huawei.HuaweiSupportProvider;

public class SendFitnessGoalRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendFitnessGoalRequest.class);

    public SendFitnessGoalRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = MotionGoal.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsMotionGoal();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // Hardcoded values till interface for goal
            int stepGoal = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
            int calorieGoal = 0;
            short durationGoal = 0;
            return new MotionGoal.Request(paramsProvider,
                    (byte) 0x01,
                    (byte) 0x00,
                    stepGoal,
                    calorieGoal,
                    durationGoal
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Send Fitness Goal Request");
    }
}

package id.icapps.savera.service.devices.huawei.requests;

import java.util.List;

import id.icapps.savera.devices.huawei.HuaweiPacket;
import id.icapps.savera.devices.huawei.packets.Ephemeris;
import id.icapps.savera.service.devices.huawei.HuaweiSupportProvider;

public class SendEphemerisOperatorResponse extends Request {
    final int responseCode;

    public SendEphemerisOperatorResponse(HuaweiSupportProvider support, int responseCode) {
        super(support);
        this.serviceId = Ephemeris.id;
        this.commandId = Ephemeris.OperatorData.id;
        this.responseCode = responseCode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Ephemeris.OperatorData.OperatorResponse(this.paramsProvider, this.responseCode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

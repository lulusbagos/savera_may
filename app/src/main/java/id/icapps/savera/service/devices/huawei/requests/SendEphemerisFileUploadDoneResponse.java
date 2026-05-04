package id.icapps.savera.service.devices.huawei.requests;

import java.util.List;

import id.icapps.savera.devices.huawei.HuaweiPacket;
import id.icapps.savera.devices.huawei.packets.EphemerisFileUpload;
import id.icapps.savera.service.devices.huawei.HuaweiSupportProvider;

public class SendEphemerisFileUploadDoneResponse extends Request {
    final int responseCode;

    public SendEphemerisFileUploadDoneResponse(HuaweiSupportProvider support, int responseCode) {
        super(support);
        this.serviceId = EphemerisFileUpload.id;
        this.commandId = EphemerisFileUpload.UploadDone.id;
        this.responseCode = responseCode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new EphemerisFileUpload.UploadDone.UploadDoneResponse(this.paramsProvider, this.responseCode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}

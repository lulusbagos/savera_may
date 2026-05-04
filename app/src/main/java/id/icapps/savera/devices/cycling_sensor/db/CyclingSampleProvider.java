package id.icapps.savera.devices.cycling_sensor.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import id.icapps.savera.devices.AbstractSampleProvider;
import id.icapps.savera.devices.AbstractTimeSampleProvider;
import id.icapps.savera.entities.CyclingSample;
import id.icapps.savera.entities.CyclingSampleDao;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.impl.GBDevice;

public class CyclingSampleProvider extends AbstractTimeSampleProvider<CyclingSample> {
    public CyclingSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<CyclingSample, ?> getSampleDao() {
        return getSession().getCyclingSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return CyclingSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return CyclingSampleDao.Properties.DeviceId;
    }

    @Override
    public CyclingSample createSample() {
        return new CyclingSample();
    }
}

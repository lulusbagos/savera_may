package id.icapps.savera.devices;

import androidx.annotation.Nullable;

import id.icapps.savera.model.Vo2MaxSample;

public interface Vo2MaxSampleProvider<T extends Vo2MaxSample> extends TimeSampleProvider<T> {
    @Nullable
    T getLatestSample(Vo2MaxSample.Type type, long until);
}

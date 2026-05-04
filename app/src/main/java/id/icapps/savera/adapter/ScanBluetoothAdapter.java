package id.icapps.savera.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.impl.GBDeviceCandidate;
import id.icapps.savera.model.DeviceType;
import id.icapps.savera.util.DeviceHelper;
import id.icapps.savera.util.GB;

/**
 * Adapter for displaying GBDeviceCandate instances.
 */
public class ScanBluetoothAdapter extends ArrayAdapter<GBDeviceCandidate> {

    private final Context context;

    public ScanBluetoothAdapter(Context context, List<GBDeviceCandidate> deviceCandidates) {
        super(context, 0, deviceCandidates);

        this.context = context;
    }

    private LocalStorage localStorage;

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GBDeviceCandidate device = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.scan_bluetooth_item, parent, false);
        }
        ImageView deviceImageView = view.findViewById(R.id.item_image);
        TextView deviceNameLabel = view.findViewById(R.id.item_name);
        TextView deviceAddressLabel = view.findViewById(R.id.item_details);
        TextView deviceStatus = view.findViewById(R.id.item_status);
        LinearLayout deviceLayout = view.findViewById(R.id.item_layout);

        DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(device);

        DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();

        String name = formatDeviceCandidate(device);
        deviceNameLabel.setText(name);
        deviceAddressLabel.setText(device.getMacAddress());
        deviceImageView.setImageResource(coordinator.getDefaultIconResource());

        final List<String> statusLines = new ArrayList<>();
        if (device.isBonded()) {
            statusLines.add(getContext().getString(R.string.device_is_currently_bonded));
        }

        if (!deviceType.isSupported()) {
            statusLines.add(getContext().getString(R.string.device_unsupported));
        }

        if (coordinator.isExperimental()) {
            statusLines.add(getContext().getString(R.string.device_experimental));
        }
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            statusLines.add(getContext().getString(R.string.device_requires_key));
        }

        deviceStatus.setText(TextUtils.join("\n", statusLines));
        deviceLayout.setBackgroundColor(Color.parseColor("#00000000"));

        localStorage = new LocalStorage(this.context);
        if (Objects.equals(device.getMacAddress(), localStorage.getDevice())) {
            deviceLayout.setBackgroundColor(Color.parseColor("#7Fffff00"));
        }

        return view;
    }

    private String formatDeviceCandidate(GBDeviceCandidate device) {
        if (device.getRssi() > GBDevice.RSSI_UNKNOWN) {
            return context.getString(R.string.device_with_rssi, device.getName(), GB.formatRssi(device.getRssi()));
        }
        return device.getName();
    }
}

package id.icapps.savera.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import id.icapps.savera.GBApplication;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.activities.devicesettings.DeviceSettingsPreferenceConst;
import id.icapps.savera.adapter.MyDeviceAdapter;
import id.icapps.savera.database.DBAccess;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.DeviceManager;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.model.DailyTotals;
import id.icapps.savera.model.DeviceService;
import id.icapps.savera.util.GB;

public class MyDevices extends Fragment {
    private DeviceManager deviceManager;
    private MyDeviceAdapter myDeviceAdapter;
    private RecyclerView deviceListView;
    private Button btnAdd;
    private ImageButton btnScan;
    private TextView textSearch;
    private LinearLayout deviceSearchPanel;
    private LinearLayout deviceNotConnect;
    List<GBDevice> deviceList;
    private HashMap<String, DailyTotals> deviceActivityHashMap = new HashMap();
    private LocalStorage localStorage;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            switch (Objects.requireNonNull(action)) {
                case DeviceManager.ACTION_DEVICES_CHANGED:
                case GBApplication.ACTION_NEW_DATA:
                    if (action.equals(GBApplication.ACTION_NEW_DATA)) {
                        createRefreshTask("get activity data", requireContext(), device).execute();
                    }
                    if (device != null) {
                        // Refresh only this device
                        refreshSingleDevice(device);
                        assert getActivity() != null;
                        ((HomeActivity) getActivity()).changeViewPagerPostition(0);
                    } else {
                        refreshPairedDevices();
                    }

                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    handleRealtimeSample(device, intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
            }
        }
    };

    private void handleRealtimeSample(GBDevice device, Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
                if (device != null) {
                    refreshSingleDevice(device);
                } else {
                    refreshPairedDevices();
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_devices, container, false);

        localStorage = new LocalStorage(view.getContext());

        assert getActivity() != null;
        deviceManager = ((GBApplication) getActivity().getApplication()).getDeviceManager();

        deviceListView = view.findViewById(R.id.deviceListView);
        deviceListView.setHasFixedSize(true);
        deviceListView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        deviceList = deviceManager.getDevices();
        myDeviceAdapter = new MyDeviceAdapter(view.getContext(), deviceList, deviceActivityHashMap, canManageDevices());
        myDeviceAdapter.setHasStableIds(true);

        deviceListView.setAdapter(this.myDeviceAdapter);

        // get activity data asynchronously, this fills the deviceActivityHashMap
        // and calls refreshPairedDevices() → notifyDataSetChanged
        deviceListView.post(new Runnable() {
            @Override
            public void run() {
                if (getContext() != null) {
                    createRefreshTask("get activity data", getContext(), null).execute();
                }
            }
        });

        deviceNotConnect = view.findViewById(R.id.deviceNotConnect);
        deviceSearchPanel = view.findViewById(R.id.deviceSearchPanel);

        btnAdd = view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDiscoveryActivity();
            }
        });

        btnScan = view.findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation anim = new AlphaAnimation(1, 0.5f);
                anim.setDuration(50);
                anim.setRepeatMode(Animation.REVERSE);
                btnScan.startAnimation(anim);

                launchDiscoveryActivity();
            }
        });

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(0);
            }
        });

        textSearch = view.findViewById(R.id.textSearch);
        textSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                myDeviceAdapter = new MyDeviceAdapter(view.getContext(), deviceList.stream()
                        .filter(x -> x.getAliasOrName().toLowerCase().contains(s.toString().toLowerCase().trim()) ||
                                x.getAddress().toLowerCase().replace(":", "").contains(s.toString().toLowerCase().trim()))
                        .collect(Collectors.toList()), deviceActivityHashMap, canManageDevices());
                myDeviceAdapter.setHasStableIds(true);
                deviceListView.setAdapter(myDeviceAdapter);
            }
        });

        registerForContextMenu(deviceListView);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        refreshPairedDevices();

        if (GB.isBluetoothEnabled() && deviceList.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startActivity(new Intent(getActivity(), ScanBluetooth.class));
        }

        return view;
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(getActivity(), ScanBluetooth.class));
    }

    @Override
    public void onDestroy() {
        if (deviceListView != null) unregisterForContextMenu(deviceListView);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private DailyTotals getSteps(GBDevice device, DBHandler db) {
        Calendar day = GregorianCalendar.getInstance();

        return DailyTotals.getDailyTotalsForDevice(device, day, db);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshPairedDevices() {
        if (myDeviceAdapter != null) {
            myDeviceAdapter.rebuildFolders();
            myDeviceAdapter.notifyDataSetChanged();

            infoNotConnect();
        }
    }

    public void refreshSingleDevice(final GBDevice device) {
        if (myDeviceAdapter != null) {
            myDeviceAdapter.refreshSingleDevice(device);

            infoNotConnect();
        }
    }

    public void infoNotConnect() {
        if (deviceList.isEmpty()) {
            deviceNotConnect.setVisibility(View.VISIBLE);
            deviceSearchPanel.setVisibility(View.GONE);
            btnAdd.setVisibility(View.GONE);
            textSearch.setVisibility(View.GONE);
        } else {
            deviceNotConnect.setVisibility(View.GONE);
            deviceSearchPanel.setVisibility(View.GONE);
            btnAdd.setVisibility(View.GONE);
            textSearch.setVisibility(View.GONE);
            if (canManageDevices()) {
                deviceSearchPanel.setVisibility(View.VISIBLE);
                btnAdd.setVisibility(View.VISIBLE);
                textSearch.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean canManageDevices() {
        return localStorage != null && (localStorage.getAdmin() || localStorage.getSleepUploader());
    }

    public RefreshTask createRefreshTask(String task, Context context, GBDevice device) {
        return new RefreshTask(task, context, device);
    }

    public class RefreshTask extends DBAccess {
        private final GBDevice device;

        public RefreshTask(final String task, final Context context, final GBDevice device) {
            super(task, context);
            this.device = device;
        }

        @Override
        protected void doInBackground(final DBHandler db) {
            if (device != null) {
                updateDevice(db, device);
            } else {
                for (GBDevice gbDevice : deviceList) {
                    updateDevice(db, gbDevice);
                }
            }
        }

        private void updateDevice(final DBHandler db, final GBDevice gbDevice) {
            final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            final boolean showActivityCard = GBApplication.getDevicePrefs(gbDevice).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD, true);
            if (coordinator.supportsActivityTracking() && showActivityCard) {
                final DailyTotals stepsAndSleepData = getSteps(gbDevice, db);
                deviceActivityHashMap.put(gbDevice.getAddress(), stepsAndSleepData);
            }
        }

        @Override
        protected void onPostExecute(final Object o) {
            if (device != null) {
                refreshSingleDevice(device);
            } else {
                refreshPairedDevices();
            }
        }
    }
}

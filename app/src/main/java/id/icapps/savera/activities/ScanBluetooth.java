package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import id.icapps.savera.GBApplication;
import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.activities.discovery.GBScanEvent;
import id.icapps.savera.activities.discovery.GBScanEventProcessor;
import id.icapps.savera.adapter.ScanBluetoothAdapter;
import id.icapps.savera.adapter.SpinnerWithIconAdapter;
import id.icapps.savera.adapter.SpinnerWithIconItem;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.impl.GBDeviceCandidate;
import id.icapps.savera.model.DeviceType;
import id.icapps.savera.util.AndroidUtils;
import id.icapps.savera.util.BondingInterface;
import id.icapps.savera.util.BondingUtil;
import id.icapps.savera.util.DeviceHelper;
import id.icapps.savera.util.GB;
import id.icapps.savera.util.Prefs;

public class ScanBluetooth extends AbstractGBActivity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        BondingInterface,
        GBScanEventProcessor.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(ScanBluetooth.class);

    private final Handler handler = new Handler();

    private static final long SCAN_DURATION = 30000; // 30s
    private static final long LIST_REFRESH_THRESHOLD_MS = 1000L;
    private long lastListRefresh = System.currentTimeMillis();

    private final ScanCallback bleScanCallback = new BleScanCallback();

    private ProgressBar bluetoothProgress;

    private ScanBluetoothAdapter deviceCandidateAdapter;
    private GBDeviceCandidate deviceTarget;
    private BluetoothAdapter adapter;

    private Button startButton;
    private boolean scanning;

    private long selectedUnsupportedDeviceKey = DebugActivity.SELECT_DEVICE;

    private final Runnable stopRunnable = () -> {
        stopDiscovery();
        LOG.info("Discovery stopped by thread timeout.");
    };
    private static final String PREF_AUTH_KEY = "authkey";
    private static final String PREF_AUTH_KEY_SYNCED_AT = "authkey_synced_at";

    private final BroadcastReceiver bluetoothReceiver = new BluetoothReceiver();

    private final GBScanEventProcessor deviceFoundProcessor = new GBScanEventProcessor(this);

    // Array to back the adapter for the UI
    private final ArrayList<GBDeviceCandidate> deviceCandidates = new ArrayList<>();

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
    }

    @Nullable
    private GBDeviceCandidate getCandidateFromMAC(final BluetoothDevice device) {
        for (final GBDeviceCandidate candidate : deviceCandidates) {
            if (candidate.getMacAddress().equals(device.getAddress())) {
                return candidate;
            }
        }
        LOG.warn("This shouldn't happen unless the list somehow emptied itself, device MAC: {}", device.getAddress());
        return null;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.scan_bluetooth);
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        loadSettings();

        startButton = findViewById(R.id.discovery_start);
        startButton.setOnClickListener(v -> toggleDiscovery());

        bluetoothProgress = findViewById(R.id.discovery_progressbar);
        bluetoothProgress.setProgress(0);
        bluetoothProgress.setIndeterminate(true);
        bluetoothProgress.setVisibility(View.GONE);

        deviceCandidateAdapter = new ScanBluetoothAdapter(this, deviceCandidates);

        final ListView deviceCandidatesView = findViewById(R.id.discovery_device_candidates_list);
        deviceCandidatesView.setAdapter(deviceCandidateAdapter);
        deviceCandidatesView.setOnItemClickListener(this);
        deviceCandidatesView.setOnItemLongClickListener(this);

        registerBroadcastReceivers();

        checkAndRequestLocationPermission();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        if (!startDiscovery()) {
            /* if we couldn't start scanning, go back to the main page.
            A toast will have been shown explaining what's wrong */
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("deviceCandidates", deviceCandidates);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final List<Parcelable> restoredCandidates = savedInstanceState.getParcelableArrayList("deviceCandidates");
        if (restoredCandidates != null) {
            deviceCandidates.clear();
            for (final Parcelable p : restoredCandidates) {
                final GBDeviceCandidate candidate = (GBDeviceCandidate) p;
                deviceCandidates.add(candidate);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onStop();
    }

    @Override
    protected void onPause() {
        unregisterBroadcastReceivers();
        stopDiscovery();
        super.onPause();
    }

    @Override
    protected void onResume() {
        loadSettings();
        registerBroadcastReceivers();
        super.onResume();
    }

    private void refreshDeviceList(final boolean throttle) {
        handler.post(() -> {
            if (throttle && System.currentTimeMillis() - lastListRefresh < LIST_REFRESH_THRESHOLD_MS) {
                return;
            }

            LOG.debug("Refreshing device list");

            // Clear and re-populate the list. deviceFoundProcessor keeps insertion order, so newer devices
            // will still be at the end
            deviceCandidates.clear();
            deviceCandidates.addAll(deviceFoundProcessor.getDevices());

            deviceCandidateAdapter.notifyDataSetChanged();

            lastListRefresh = System.currentTimeMillis();
        });
    }

    private void toggleDiscovery() {
        if (scanning) {
            stopDiscovery();
        } else {
            startDiscovery();
        }
    }

    private boolean startDiscovery() {
        if (scanning) {
            LOG.warn("Not starting discovery, because already scanning.");
            return false;
        }

        LOG.info("Starting discovery");
        startButton.setText(getString(R.string.discovery_stop_scanning));

        deviceFoundProcessor.clear();
        deviceFoundProcessor.start();

        refreshDeviceList(false);

        // Pre-add currently connected devices, as those will not trigger discovery events
        // Paired devices that are not connected do not need to be added, as those will be discovered
        try {
            final Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (final BluetoothDevice device : pairedDevices) {
                try {
                    final Method isConnectedMethod = device.getClass().getMethod("isConnected");
                    final Boolean isConnected = (Boolean) isConnectedMethod.invoke(device);
                    if (isConnected != null && isConnected) {
                        LOG.debug("Pre-adding already bonded device {}", device.getAddress());
                        deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, (short) -1, null));
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to check whether {} is connected", device.getAddress());
                }
            }
        } catch (final SecurityException e) {
            LOG.error("Failed to pre-add paired devices", e);
        }

        try {
            if (!ensureBluetoothReady()) {
                toast(ScanBluetooth.this, getString(R.string.discovery_enable_bluetooth), Toast.LENGTH_SHORT, GB.ERROR);
                return false;
            }

            if (GB.supportsBluetoothLE()) {
                startBTLEDiscovery();
            }
            startBTDiscovery();

            bluetoothProgress.setVisibility(View.VISIBLE);
        } catch (final SecurityException e) {
            LOG.error("SecurityException on startDiscovery");
            deviceFoundProcessor.stop();
            return false;
        }

        setScanning(true);

        return true;
    }

    private void stopDiscovery() {
        LOG.info("Stopping discovery");
        try {
            stopBTDiscovery();
            stopBLEDiscovery();
        } catch (final SecurityException e) {
            LOG.error("SecurityException on stopDiscovery");
        }
        setScanning(false);
        deviceFoundProcessor.stop();
        handler.removeMessages(0, stopRunnable);

        // Refresh the device list one last time when finishing
        refreshDeviceList(false);
    }

    public void setScanning(final boolean scanning) {
        this.scanning = scanning;
        if (scanning) {
            startButton.setText(getString(R.string.discovery_stop_scanning));
        } else {
            startButton.setText(getString(R.string.discovery_start_scanning));
            bluetoothProgress.setVisibility(View.GONE);
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void startBTLEDiscovery() {
        LOG.info("Starting BLE discovery");

        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);

        // Filters being non-null would be a very good idea with background scan, but in this case,
        // not really required.
        // TODO getScanFilters maybe
        adapter.getBluetoothLeScanner().startScan(null, getScanSettings(), bleScanCallback);

        LOG.debug("Bluetooth LE discovery started successfully");
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void stopBLEDiscovery() {
        if (adapter == null) {
            return;
        }

        final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            LOG.warn("Could not get BluetoothLeScanner()!");
            return;
        }

        if (bleScanCallback == null) {
            LOG.warn("newLeScanCallback == null!");
            return;
        }

        try {
            bluetoothLeScanner.stopScan(bleScanCallback);
        } catch (final NullPointerException e) {
            LOG.warn("Internal NullPointerException when stopping the scan!");
            return;
        }

        LOG.debug("Stopped BLE discovery");
    }

    /**
     * Starts a regular Bluetooth scan
     */
    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void startBTDiscovery() {
        LOG.info("Starting BT discovery");
        try {
            // LineageOS quirk, can't stop scan properly,
            // if scan has been started by something else
            stopBTDiscovery();
        } catch (final Exception ignored) {
        }
        handler.removeMessages(0, stopRunnable);
        handler.sendMessageDelayed(getPostMessage(stopRunnable), SCAN_DURATION);

        if (adapter.startDiscovery()) {
            LOG.debug("Discovery started successfully");
        } else {
            LOG.error("Discovery starting failed");
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private void stopBTDiscovery() {
        if (adapter == null) return;
        adapter.cancelDiscovery();
        LOG.info("Stopped BT discovery");
    }

    private void bluetoothStateChanged(final int newState) {
        if (newState == BluetoothAdapter.STATE_ON) {
            this.adapter = BluetoothAdapter.getDefaultAdapter();
            startButton.setEnabled(true);
        } else {
            this.adapter = null;
            startButton.setEnabled(false);
            bluetoothProgress.setVisibility(View.GONE);
        }
    }

    private boolean checkBluetoothAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                LOG.warn("No BLUETOOTH_SCAN permission");
                this.adapter = null;
                return false;
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                LOG.warn("No BLUETOOTH_CONNECT permission");
                this.adapter = null;
                return false;
            }
        }

        final BluetoothManager bluetoothService = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothService == null) {
            LOG.warn("No bluetooth service available");
            this.adapter = null;
            return false;
        }

        final BluetoothAdapter adapter = bluetoothService.getAdapter();
        if (adapter == null) {
            LOG.warn("No bluetooth adapter available");
            this.adapter = null;
            return false;
        }

        if (!adapter.isEnabled()) {
            LOG.warn("Bluetooth not enabled");
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            this.adapter = null;
            return false;
        }

        this.adapter = adapter;
        return true;
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    private boolean ensureBluetoothReady() {
        final boolean available = checkBluetoothAvailable();
        startButton.setEnabled(available);

        if (available) {
            adapter.cancelDiscovery();
            // must not return the result of cancelDiscovery()
            // appears to return false when currently not scanning
            return true;
        }
        return false;
    }

    private static ScanSettings getScanSettings() {
        final ScanSettings.Builder builder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
            builder.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
        }

        return builder.build();
    }

    private Message getPostMessage(final Runnable runnable) {
        final Message message = Message.obtain(handler, runnable);
        message.obj = runnable;
        return message;
    }

    private void showWarnDialog(@StringRes final int message) {
        new MaterialAlertDialogBuilder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                })
                .show();
    }

    private void checkAndRequestLocationPermission() {
        /* This is more or less a copy of what's in ControlCenterv2, but
        we do this in case the permissions weren't requested since there
        is no way we can scan without this stuff */
        List<String> wantedPermissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access coarse location!");
            wantedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access fine location!");
            wantedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // if we need location permissions, request both together to avoid a bunch of dialogs
        if (wantedPermissions.size() > 0) {
            toast(ScanBluetooth.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
            ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[0]), 0);
            wantedPermissions.clear();
        }
        // Now we have to request background location separately!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access background location!");
                toast(ScanBluetooth.this, getString(R.string.error_no_location_access), Toast.LENGTH_SHORT, GB.ERROR);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
            }
        }
        // Now, we can request Bluetooth permissions....
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access Bluetooth scanning!");
                toast(ScanBluetooth.this, getString(R.string.error_no_bluetooth_scan), Toast.LENGTH_SHORT, GB.ERROR);
                wantedPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                LOG.error("No permission to access Bluetooth connection!");
                toast(ScanBluetooth.this, getString(R.string.error_no_bluetooth_connect), Toast.LENGTH_SHORT, GB.ERROR);
                wantedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (wantedPermissions.size() > 0) {
            GB.toast(this, getString(R.string.permission_granting_mandatory), Toast.LENGTH_LONG, GB.ERROR);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, wantedPermissions.toArray(new String[0]), 0);
            } else {
                ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher =
                        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                            if (!isGranted.containsValue(false)) {
                                // Permission is granted. Continue the action or workflow in your app.
                                // should we do startDiscovery here??
                            } else {
                                // Explain to the user that the feature is unavailable because the feature requires a permission that the user has denied.
                                GB.toast(this, getString(R.string.permission_granting_mandatory), Toast.LENGTH_LONG, GB.ERROR);
                            }
                        });
                requestMultiplePermissionsLauncher.launch(wantedPermissions.toArray(new String[0]));
            }
        }

        LocationManager locationManager = (LocationManager) ScanBluetooth.this.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Do nothing
                LOG.debug("Some location provider is enabled, assuming location is enabled");
            } else {
                toast(ScanBluetooth.this, getString(R.string.require_location_provider), Toast.LENGTH_LONG, GB.ERROR);
                ScanBluetooth.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                // We can't be sure location was enabled, cancel scan start and wait for new user action
                toast(ScanBluetooth.this, getString(R.string.error_location_enabled_mandatory), Toast.LENGTH_SHORT, GB.ERROR);
                return;
            }
        } catch (final Exception ex) {
            LOG.error("Exception when checking location status", ex);
        }
        LOG.info("Permissions seems to be fine for scanning");
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate clicked, but item not found");
            return;
        }

        DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(deviceCandidate);

        if (!deviceType.isSupported()) {
            LOG.warn("Unsupported device candidate {}", deviceCandidate);
            copyDetailsToClipboard(deviceCandidate);
            return;
        }

        stopDiscovery();

        final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
        LOG.info("Using device candidate {} with coordinator {}", deviceCandidate, coordinator.getClass());

        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceCandidate.getMacAddress());
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            if (!hasValidCachedAuthKey(sharedPrefs, coordinator)) {
                fetchAuthKeyFromServer(deviceCandidate, sharedPrefs, () -> onItemClick(parent, view, position, id));
                return;
            }
        }

        if (coordinator.suggestUnbindBeforePair() && deviceCandidate.isBonded()) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.unbind_before_pair_title)
                    .setMessage(R.string.unbind_before_pair_message)
                    .setIcon(R.drawable.ic_warning_gray)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                        startPair(deviceCandidate, coordinator);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            startPair(deviceCandidate, coordinator);
        }
    }

    private void startPair(final GBDeviceCandidate deviceCandidate, final DeviceCoordinator coordinator) {
        final Class<? extends Activity> pairingActivity = coordinator.getPairingActivity();
        if (pairingActivity != null) {
            final Intent intent = new Intent(this, pairingActivity);
            intent.putExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE, deviceCandidate);
            startActivity(intent);
        } else {
            if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_NONE ||
                    coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_LAZY) {
                LOG.info("No bonding needed, according to coordinator, so connecting right away");
                BondingUtil.connectThenComplete(this, deviceCandidate);
                return;
            }

            try {
                this.deviceTarget = deviceCandidate;
                BondingUtil.initiateCorrectBonding(this, deviceCandidate, coordinator);
            } catch (final Exception e) {
                LOG.error("Error pairing device {}", deviceCandidate.getMacAddress(), e);
            }
        }
    }

    private void copyDetailsToClipboard(final GBDeviceCandidate deviceCandidate) {
        final List<String> deviceDetails = new ArrayList<>();
        deviceDetails.add(deviceCandidate.getName());
        deviceDetails.add(deviceCandidate.getMacAddress());
        try {
            for (final ParcelUuid uuid : deviceCandidate.getServiceUuids()) {
                deviceDetails.add(uuid.getUuid().toString());
            }
        } catch (final Exception e) {
            LOG.error("Error collecting device uuids", e);
        }
        final String clipboardData = TextUtils.join(", ", deviceDetails);
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(deviceCandidate.getName(), clipboardData);
        clipboard.setPrimaryClip(clip);
        toast(this, "Device details copied to clipboard", Toast.LENGTH_SHORT, GB.INFO);
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        stopDiscovery();

        final GBDeviceCandidate deviceCandidate = deviceCandidates.get(position);
        if (deviceCandidate == null) {
            LOG.error("Device candidate clicked, but item not found");
            return true;
        }

        DeviceType deviceType = DeviceHelper.getInstance().resolveDeviceType(deviceCandidate);

        if (!deviceType.isSupported()) {
            showUnsupportedDeviceDialog(deviceCandidate);
            return true;
        }

        final DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
        final GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        if (coordinator.getSupportedDeviceSpecificSettings(device) == null) {
            return true;
        }

        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceCandidate.getMacAddress());
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY
                && hasValidCachedAuthKey(sharedPrefs, coordinator)) {
            Long syncedAt = sharedPrefs.getLong(PREF_AUTH_KEY_SYNCED_AT, 0L);
            LOG.info("Using cached auth key for mac={}, syncedAt={}", deviceCandidate.getMacAddress(), syncedAt);
            showAuthKeyReadyToast(true);
            onItemClick(adapterView, view, position, id);
            return true;
        }

        fetchAuthKeyFromServer(deviceCandidate, sharedPrefs, () -> onItemClick(adapterView, view, position, id));

        return true;
    }

    private boolean hasValidCachedAuthKey(SharedPreferences sharedPrefs, DeviceCoordinator coordinator) {
        String authKey = sharedPrefs.getString(PREF_AUTH_KEY, null);
        if (authKey == null || authKey.isEmpty()) {
            return false;
        }

        if (!coordinator.validateAuthKey(authKey)) {
            sharedPrefs.edit().remove(PREF_AUTH_KEY).remove(PREF_AUTH_KEY_SYNCED_AT).apply();
            return false;
        }

        return true;
    }

    private void fetchAuthKeyFromServer(final GBDeviceCandidate deviceCandidate,
                                        final SharedPreferences sharedPrefs,
                                        final Runnable onSuccess) {
        final String normalizedMacAddress = deviceCandidate.getMacAddress() == null
                ? ""
                : deviceCandidate.getMacAddress().trim().toUpperCase(Locale.ROOT);
        final String url = getString(R.string.base_url) + "/device/" + normalizedMacAddress;
        final LocalStorage localStorage = new LocalStorage(ScanBluetooth.this);
        final String companyHeader = localStorage.getCompanyCode();
        final ProgressDialog progressDialog = new ProgressDialog(ScanBluetooth.this);
        progressDialog.setMessage("Mengambil auth key perangkat...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        LOG.info("Pairing lookup request mac={} company={} url={}", normalizedMacAddress, companyHeader, url);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Http http = new Http(ScanBluetooth.this, url);
                http.setMethod("get");
                http.setToken(true);
                http.setBypassCache(true);
                http.setConnectTimeoutMs(8000);
                http.setReadTimeoutMs(12000);
                http.setMaxAttempts(1);
                http.send();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgress(progressDialog);
                        Integer code = http.getStatusCode();
                        LOG.info("Pairing lookup response mac={} company={} status={} body={}", normalizedMacAddress, companyHeader, code, http.getResponse());

                        if (code == 200) {
                            try {
                                JSONObject response = new JSONObject(http.getResponse());
                                String fetchedAuthKey = response.optString("auth_key", "");
                                if (fetchedAuthKey == null || fetchedAuthKey.trim().isEmpty() || "null".equalsIgnoreCase(fetchedAuthKey.trim())) {
                                    showAuthKeyErrorDialog(
                                            "Auth key tidak valid",
                                            "Server berhasil dihubungi, tetapi field auth_key kosong. Pastikan endpoint /device/" + normalizedMacAddress + " mengirim auth_key yang benar."
                                    );
                                    return;
                                }

                                sharedPrefs.edit()
                                        .putString(PREF_AUTH_KEY, fetchedAuthKey.trim())
                                        .putLong(PREF_AUTH_KEY_SYNCED_AT, System.currentTimeMillis())
                                        .apply();
                                showAuthKeyReadyToast(false);

                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                            } catch (JSONException e) {
                                LOG.warn("Failed parsing device auth key response", e);
                                showAuthKeyErrorDialog(
                                        "Respons auth key tidak sesuai",
                                        "Server merespons, tetapi format datanya belum sesuai. Pastikan response berupa JSON dengan field auth_key."
                                );
                            }
                        } else if (code == 422 || code == 401 || code == 404) {
                            String msg = extractAuthKeyApiMessage(http.getResponse());
                            showAuthKeyErrorDialog("Auth key belum siap", buildAuthKeyErrorMessage(code, normalizedMacAddress, msg));
                        } else {
                            showAuthKeyErrorDialog("Gagal mengambil auth key", buildAuthKeyErrorMessage(code, normalizedMacAddress, null));
                        }
                    }
                });
            }
        }).start();
    }

    private void dismissProgress(final ProgressDialog progressDialog) {
        if (progressDialog == null) {
            return;
        }

        try {
            if (progressDialog.isShowing() && !isFinishing()) {
                progressDialog.dismiss();
            }
        } catch (final Exception e) {
            LOG.warn("Failed dismissing auth key progress dialog", e);
        }
    }

    private void showAuthKeyReadyToast(final boolean fromCache) {
        final String source = fromCache ? "Auth key lokal siap." : "Auth key berhasil diambil.";
        toast(ScanBluetooth.this, source + " Menghubungkan ke wearable...", Toast.LENGTH_LONG, GB.INFO);
    }

    private String extractAuthKeyApiMessage(final String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject response = new JSONObject(responseBody);
            String message = response.optString("message", "").trim();
            return message.isEmpty() ? null : message;
        } catch (JSONException e) {
            LOG.warn("Failed parsing device auth key error response", e);
            return null;
        }
    }

    private String buildAuthKeyErrorMessage(final Integer code, final String macAddress, final String apiMessage) {
        final String serverMessage = apiMessage == null || apiMessage.trim().isEmpty()
                ? ""
                : "\n\nPesan server: " + apiMessage.trim();

        if (code == null || code == 0) {
            return "Aplikasi belum bisa menghubungi server untuk mengambil auth key.\n\nPeriksa koneksi internet, alamat API, lalu coba sambungkan ulang." + serverMessage;
        }

        if (code == 401) {
            return "Sesi login tidak valid atau sudah kedaluwarsa. Silakan login ulang, lalu coba sambungkan perangkat lagi." + serverMessage;
        }

        if (code == 404) {
            return "MAC " + macAddress + " belum terdaftar di server.\n\nPastikan perangkat sudah didaftarkan di backend dengan MAC yang sama, lalu coba lagi." + serverMessage;
        }

        if (code == 422) {
            return "Data perangkat belum bisa diproses server.\n\nPastikan MAC " + macAddress + " dan company user sudah sesuai di backend." + serverMessage;
        }

        return "Server belum mengirim auth key untuk perangkat ini. Status HTTP: " + code + ".\n\nCoba lagi setelah koneksi/API dipastikan normal." + serverMessage;
    }

    private void showAuthKeyErrorDialog(final String title, final String message) {
        if (isFinishing()) {
            toast(ScanBluetooth.this, message, Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        new MaterialAlertDialogBuilder(ScanBluetooth.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showUnsupportedDeviceDialog(final GBDeviceCandidate deviceCandidate) {
        LOG.info("Unsupported device candidate selected: {}", deviceCandidate);

        final Map<String, Pair<Long, Integer>> allDevices = DebugActivity.getAllSupportedDevices(getApplicationContext());

        final LinearLayout linearLayout = new LinearLayout(ScanBluetooth.this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final ArrayList<SpinnerWithIconItem> deviceListArray = new ArrayList<>();
        for (Map.Entry<String, Pair<Long, Integer>> item : allDevices.entrySet()) {
            deviceListArray.add(new SpinnerWithIconItem(item.getKey(), item.getValue().first, item.getValue().second));
        }
        final SpinnerWithIconAdapter deviceListAdapter = new SpinnerWithIconAdapter(
                ScanBluetooth.this,
                R.layout.spinner_with_image_layout,
                R.id.spinner_item_text,
                deviceListArray
        );

        final Spinner deviceListSpinner = new Spinner(ScanBluetooth.this);
        deviceListSpinner.setAdapter(deviceListAdapter);
        deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {
                final SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
                selectedUnsupportedDeviceKey = selectedItem.getId();
            }

            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
            }
        });
        linearLayout.addView(deviceListSpinner);

        final LinearLayout macLayout = new LinearLayout(ScanBluetooth.this);
        macLayout.setOrientation(LinearLayout.HORIZONTAL);
        macLayout.setPadding(20, 0, 20, 0);
        linearLayout.addView(macLayout);

        new MaterialAlertDialogBuilder(ScanBluetooth.this)
                .setCancelable(true)
                .setTitle(R.string.add_test_device)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (selectedUnsupportedDeviceKey != DebugActivity.SELECT_DEVICE) {
                        DebugActivity.createTestDevice(ScanBluetooth.this, selectedUnsupportedDeviceKey, deviceCandidate.getMacAddress(), deviceCandidate.getName());
                        finish();
                    }
                })
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                })
                .show();
    }

    @Override
    public void onBondingComplete(final boolean success) {
        finish();
    }

    @Override
    public GBDeviceCandidate getCurrentTarget() {
        return this.deviceTarget;
    }

    @Override
    public String getMacAddress() {
        return deviceTarget.getDevice().getAddress();
    }

    @Override
    public boolean getAttemptToConnect() {
        return true;
    }

    @Override
    public void registerBroadcastReceivers() {
        final IntentFilter bluetoothIntents = new IntentFilter();
        bluetoothIntents.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_UUID);
        bluetoothIntents.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothIntents, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bluetoothReceiver);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void loadSettings() {
        final Prefs prefs = GBApplication.getPrefs();
        deviceFoundProcessor.setDiscoverUnsupported(prefs.getBoolean("discover_unsupported_devices", false));
    }

    @Override
    public void onDeviceChanged() {
        refreshDeviceList(true);
    }

    private final class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    LOG.debug("ACTION_DISCOVERY_STARTED");
                    break;
                }
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    LOG.debug("ACTION_STATE_CHANGED ");
                    bluetoothStateChanged(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF));
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) {
                        LOG.warn("ACTION_FOUND with null device");
                        return;
                    }
                    LOG.debug("ACTION_FOUND {}", device.getAddress());
                    final short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN);
                    deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, rssi, null));
                    break;
                }
                case BluetoothDevice.ACTION_UUID: {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) {
                        LOG.warn("ACTION_UUID with null device");
                        return;
                    }
                    LOG.debug("ACTION_UUID {}", device.getAddress());
                    final short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, GBDevice.RSSI_UNKNOWN);
                    final Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    final ParcelUuid[] uuids2 = AndroidUtils.toParcelUuids(uuids);
                    deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, rssi, uuids2));
                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    LOG.debug("ACTION_BOND_STATE_CHANGED");
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        LOG.debug("Bond state: {}", bondState);

                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            BondingUtil.handleDeviceBonded((BondingInterface) context, getCandidateFromMAC(device));
                        }
                    }
                    break;
                }
            }
        }
    }

    private final class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            try {
                final ScanRecord scanRecord = result.getScanRecord();
                ParcelUuid[] uuids = null;
                if (scanRecord != null) {
                    final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
                    if (serviceUuids != null) {
                        uuids = serviceUuids.toArray(new ParcelUuid[0]);
                    }
                }
                final BluetoothDevice device = result.getDevice();
                final short rssi = (short) result.getRssi();
                LOG.debug("BLE result: {}, {}, {}", device.getAddress(), ((scanRecord != null) ? scanRecord.getBytes().length : -1), rssi);
                deviceFoundProcessor.scheduleProcessing(new GBScanEvent(device, rssi, uuids));
            } catch (final Exception e) {
                LOG.warn("Error handling BLE scan result", e);
            }
        }
    }
}

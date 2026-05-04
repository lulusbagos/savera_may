package id.icapps.savera.adapter;

import static id.icapps.savera.model.DeviceService.ACTION_CONNECT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import id.icapps.savera.GBApplication;
import id.icapps.savera.R;
import id.icapps.savera.activities.ControlCenterv2;
import id.icapps.savera.activities.HomeActivity;
import id.icapps.savera.activities.OpenFwAppInstallerActivity;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.DeviceManager;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.impl.GBDeviceFolder;
import id.icapps.savera.model.DailyTotals;
import id.icapps.savera.model.RecordedDataTypes;
import id.icapps.savera.util.DeviceHelper;
import id.icapps.savera.util.GB;
import id.icapps.savera.util.GBPrefs;
import id.icapps.savera.util.StringUtils;

/**
 * Adapter for displaying GBDevice instances.
 */
public class MyDeviceAdapter extends ListAdapter<GBDevice, MyDeviceAdapter.ViewHolder> {
    private static final Logger LOG = LoggerFactory.getLogger(MyDeviceAdapter.class);

    private final Context context;
    private List<GBDevice> deviceList;
    private List<GBDevice> devicesListWithFolders;
    private String expandedDeviceAddress = "";
    private String expandedFolderName = "";
    private ViewGroup parent;
    private HashMap<String, DailyTotals> deviceActivityMap = new HashMap<>();
    private final StableIdGenerator idGenerator = new StableIdGenerator();
    private boolean isAdmin;

    public MyDeviceAdapter(Context context, List<GBDevice> deviceList, HashMap<String, DailyTotals> deviceMap, boolean isAdmin) {
        super(new GBDeviceDiffUtil());
        this.context = context;
        this.deviceList = deviceList;
        rebuildFolders();
        this.deviceActivityMap = deviceMap;
        this.isAdmin = isAdmin;
    }

    public void rebuildFolders() {
        this.devicesListWithFolders = enrichDeviceListWithFolder(deviceList);
    }

    @SuppressLint("NotifyDataSetChanged")
    public final void refreshSingleDevice(final GBDevice device) {
        final int i = devicesListWithFolders.indexOf(device);
        if (i > 0) {
            notifyItemChanged(i);
        } else {
            // Somehow the device was not on the list - rebuild everything
            rebuildFolders();
            notifyDataSetChanged();
        }
    }

    private List<GBDevice> enrichDeviceListWithFolder(List<GBDevice> deviceList) {
        final Map<String, List<GBDevice>> devicesPerFolder = new LinkedHashMap<>();
        final List<GBDevice> enrichedList = new ArrayList<>();

        for (GBDevice device : deviceList) {
            String folder = device.getParentFolder();
            if (StringUtils.isNullOrEmpty(folder)) {
                enrichedList.add(device);
                continue;
            }
            if (!devicesPerFolder.containsKey(folder)) {
                devicesPerFolder.put(folder, new ArrayList<>());
            }
            devicesPerFolder.get(folder).add(device);
        }

        for (final Map.Entry<String, List<GBDevice>> folder : devicesPerFolder.entrySet()) {
            enrichedList.add(new GBDeviceFolder(folder.getKey()));
            if (folder.getKey().equals(expandedFolderName)) {
                enrichedList.addAll(folder.getValue());
            }
        }

        return enrichedList;
    }

    @NonNull
    @Override
    public MyDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_device_item, parent, false);
        return new ViewHolder(view);
    }

    private void setItemMargin(ViewHolder holder, GBDevice device) {
        Resources r = context.getResources();
        int widthDp = 8;
        if (!StringUtils.isNullOrEmpty(device.getParentFolder())) {
            widthDp = 16;
        }
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widthDp,
                r.getDisplayMetrics()
        );
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) holder.container.getLayoutParams();
        layoutParams.setMarginStart((int) px);
        holder.container.setLayoutParams(layoutParams);

        int alpha = 0;
        if (device instanceof GBDeviceFolder && device.getName().equals(expandedFolderName)) {
            alpha = 50;
        } else if (!StringUtils.isNullOrEmpty(device.getParentFolder()) && expandedFolderName.equals(device.getParentFolder())) {
            alpha = 50;
        }

        holder.root.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
    }

    void handleDeviceConnect(GBDevice device) {
        if (!device.getDeviceCoordinator().isConnectable()) {
            device.setState(GBDevice.State.WAITING_FOR_SCAN);
            device.sendDeviceUpdateIntent(GBApplication.getContext(), GBDevice.DeviceUpdateSubject.CONNECTION_STATE);
            return;
        }

        GBApplication.deviceService(device).connect();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final GBDevice device = devicesListWithFolders.get(position);

        // setItemMargin(holder, device);

        String parentFolder = device.getParentFolder();
        if (!StringUtils.isNullOrEmpty(parentFolder)) {
            if (parentFolder.equals(expandedFolderName)) {
                holder.container.setVisibility(View.VISIBLE);
            } else {
                holder.container.setVisibility(View.GONE);
            }
        } else {
            holder.container.setVisibility(View.VISIBLE);
        }

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device.isInitialized() || device.isConnected()) {
                    showTransientSnackbar(R.string.controlcenter_snackbar_need_longpress);
                } else {
                    showTransientSnackbar(R.string.controlcenter_snackbar_connecting);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        createDynamicShortcut(device);
                    }
                    handleDeviceConnect(device);
                }
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (device.getState() != GBDevice.State.NOT_CONNECTED) {
                    showTransientSnackbar(R.string.controlcenter_snackbar_disconnecting);
                    GBApplication.deviceService(device).disconnect();
                }
                removeFromLastDeviceAddressesPref(device);
                return true;
            }
        });

        if (device.isInitialized()) {
            holder.deviceImageView.setImageResource(R.drawable.bg_device_connect);
        } else {
            holder.deviceImageView.setImageResource(R.drawable.bg_device_disconnect);
        }

        holder.deviceNameLabel.setText(getUniqueDeviceName(device));

        if (device.isBusy()) {
            holder.deviceStatusLabel.setText(device.getBusyTask());
        } else {
            holder.deviceStatusLabel.setText(device.getStateString(context));
        }

        holder.deviceInfoView.setVisibility(View.VISIBLE);
        holder.deviceInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeviceSubmenu(v, device);
            }
        });

        final boolean detailsShown = expandedDeviceAddress.equals(device.getAddress());
        holder.deviceInfoBox.setActivated(detailsShown);
        holder.deviceInfoBox.setVisibility(View.VISIBLE);

        ItemWithDetailsAdapter infoAdapter = new ItemWithDetailsAdapter(context, device.getDeviceInfos());
        infoAdapter.setHorizontalAlignment(true);
        holder.deviceInfoList.setAdapter(infoAdapter);
        holder.deviceInfoList.setEnabled(false);
        holder.deviceInfoList.setFocusable(false);
        justifyListViewHeightBasedOnChildren(holder.deviceInfoList);

        holder.btnReload.setVisibility(device.isConnected() ? View.VISIBLE : View.GONE);
        holder.btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation anim = new AlphaAnimation(1, 0.5f);
                anim.setDuration(50);
                anim.setRepeatMode(Animation.REVERSE);
                holder.btnReload.startAnimation(anim);

                showTransientSnackbar(R.string.busy_task_fetch_activity_data);
                GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);

                final String previouslyExpandedDeviceAddress = expandedDeviceAddress;
                if (!previouslyExpandedDeviceAddress.isEmpty()) {
                    // Notify the previously expanded device for a change (collapsing it)
                    for (int i = 0; i < devicesListWithFolders.size(); i++) {
                        final GBDevice gbDevice = devicesListWithFolders.get(i);
                        if (gbDevice.getAddress().equals(previouslyExpandedDeviceAddress)) {
                            notifyItemChanged(devicesListWithFolders.indexOf(gbDevice));
                            break;
                        }
                    }
                }

                // Update the current one
                notifyItemChanged(devicesListWithFolders.indexOf(device));
            }
        });
    }

    private void showDeviceSubmenu(final View v, final GBDevice device) {
        boolean deviceConnected = device.getState() != GBDevice.State.NOT_CONNECTED;
        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.inflate(R.menu.device_item_submenu);

        menu.getMenu().findItem(R.id.controlcenter_device_submenu_connect).setVisible(!deviceConnected);
        menu.getMenu().findItem(R.id.controlcenter_device_submenu_disconnect).setVisible(deviceConnected);
        menu.getMenu().findItem(R.id.controlcenter_device_submenu_installer).setEnabled(deviceConnected);
        if (!isAdmin) {
            // menu.getMenu().findItem(R.id.controlcenter_device_submenu_remove).setVisible(false);
        }

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                final int itemId = item.getItemId();
                if (itemId == R.id.controlcenter_device_submenu_connect) {
                    if (device.getState() != GBDevice.State.CONNECTED) {
                        showTransientSnackbar(R.string.controlcenter_snackbar_connecting);
                        handleDeviceConnect(device);
                    }
                    return true;
                } else if (itemId == R.id.controlcenter_device_submenu_disconnect) {
                    if (device.getState() != GBDevice.State.NOT_CONNECTED) {
                        showTransientSnackbar(R.string.controlcenter_snackbar_disconnecting);
                        GBApplication.deviceService(device).disconnect();
                    }
                    removeFromLastDeviceAddressesPref(device);
                    return true;
                } else if (itemId == R.id.controlcenter_device_submenu_installer) {
                    Intent openFwIntent = new Intent(context, OpenFwAppInstallerActivity.class);
                    openFwIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    context.startActivity(openFwIntent);
                    return false;
                } else if (itemId == R.id.controlcenter_device_submenu_remove) {
                    showRemoveDeviceDialog(device);
                    return true;
                }

                return false;
            }
        });
        menu.show();
    }

    private void showRemoveDeviceDialog(final GBDevice device) {
        new MaterialAlertDialogBuilder(context)
                .setCancelable(true)
                .setTitle(context.getString(R.string.controlcenter_delete_device_name, device.getName()))
                .setMessage(R.string.controlcenter_delete_device_dialogmessage)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            DeviceCoordinator coordinator = device.getDeviceCoordinator();
                            coordinator.deleteDevice(device);
                            DeviceHelper.getInstance().removeBond(device);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                removeDynamicShortcut(device);
                            }
                        } catch (Exception ex) {
                            GB.toast(context, context.getString(R.string.error_deleting_device, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        } finally {
                            Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);

                            Intent homeIntent = new Intent(context, HomeActivity.class);
                            homeIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                            context.startActivity(homeIntent);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }

    private void removeFromLastDeviceAddressesPref(GBDevice device) {
        Set<String> lastDeviceAddresses = GBApplication.getPrefs().getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, Collections.emptySet());
        if (lastDeviceAddresses.contains(device.getAddress())) {
            lastDeviceAddresses = new HashSet<String>(lastDeviceAddresses);
            lastDeviceAddresses.remove(device.getAddress());
            GBApplication.getPrefs().getPreferences().edit().putStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, lastDeviceAddresses).apply();
        }
    }

    @Override
    public int getItemCount() {
        return devicesListWithFolders.size();
    }

    @Override
    public long getItemId(int position) {
        return idGenerator.getId(devicesListWithFolders.get(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View root;
        MaterialCardView container;

        ImageView deviceImageView;
        TextView deviceNameLabel;
        TextView deviceStatusLabel;

        RelativeLayout deviceInfoBox;
        ListView deviceInfoList;
        ImageView deviceInfoView;

        ImageButton btnReload;

        ViewHolder(View view) {
            super(view);
            root = view;
            container = view.findViewById(R.id.card_view);
            deviceImageView = view.findViewById(R.id.device_image);
            deviceNameLabel = view.findViewById(R.id.device_name);
            deviceStatusLabel = view.findViewById(R.id.device_status);
            deviceInfoView = view.findViewById(R.id.device_info_image);
            deviceInfoBox = view.findViewById(R.id.device_item_infos_box);
            deviceInfoList = view.findViewById(R.id.device_item_infos);
            btnReload = view.findViewById(R.id.btnReload);
        }
    }

    private void justifyListViewHeightBasedOnChildren(ListView listView) {
        ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();

        if (adapter == null) {
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams par = listView.getLayoutParams();
        par.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(par);
        listView.requestLayout();
    }

    private String getUniqueDeviceName(GBDevice device) {
        String deviceName = device.getAliasOrName();

        if (!isUniqueDeviceName(device, deviceName)) {
            if (device.getModel() != null) {
                deviceName = deviceName + " " + device.getModel();
                if (!isUniqueDeviceName(device, deviceName)) {
                    deviceName = deviceName + " " + device.getShortAddress();
                }
            } else {
                deviceName = deviceName + " " + device.getShortAddress();
            }
        }
        return deviceName;
    }

    private boolean isUniqueDeviceName(GBDevice device, String deviceName) {
        for (int i = 0; i < deviceList.size(); i++) {
            GBDevice item = deviceList.get(i);
            if (item == device) {
                continue;
            }
            if (deviceName.equals(item.getName())) {
                return false;
            }
        }
        return true;
    }

    private void showTransientSnackbar(int resource) {
        Snackbar snackbar = Snackbar.make(parent, resource, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void createDynamicShortcut(GBDevice device) {
        Intent intent = new Intent(context, ControlCenterv2.class)
                .setAction(ACTION_CONNECT)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("device", device.getAddress());

        ShortcutManager shortcutManager = (ShortcutManager) context.getApplicationContext().getSystemService(Context.SHORTCUT_SERVICE);

        DeviceCoordinator coordinator = device.getDeviceCoordinator();

        shortcutManager.pushDynamicShortcut(new ShortcutInfo.Builder(context, device.getAddress())
                .setLongLived(false)
                .setShortLabel(device.getAliasOrName())
                .setIntent(intent)
                .setIcon(Icon.createWithResource(context, coordinator.getDefaultIconResource()))
                .build()
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void removeDynamicShortcut(GBDevice device) {
        final ShortcutManager shortcutManager = (ShortcutManager) context.getApplicationContext().getSystemService(Context.SHORTCUT_SERVICE);

        shortcutManager.removeDynamicShortcuts(Collections.singletonList(device.getAddress()));
    }

    private static class GBDeviceDiffUtil extends DiffUtil.ItemCallback<GBDevice> {
        @Override
        public boolean areItemsTheSame(@NonNull GBDevice oldItem, @NonNull GBDevice newItem) {
            return new EqualsBuilder()
                    .append(oldItem.getAddress(), newItem.getAddress())
                    .append(oldItem.getName(), newItem.getName())
                    .isEquals();
        }

        @Override
        public boolean areContentsTheSame(@NonNull GBDevice oldItem, @NonNull GBDevice newItem) {
            return EqualsBuilder.reflectionEquals(oldItem, newItem);
        }
    }

    /**
     * A generator of stable IDs, given a string, since hashCode can easily have collisions.
     */
    private static class StableIdGenerator {
        private final Map<String, Long> idMapping = new HashMap<String, Long>();

        private long nextId = 0;

        public long getId(final GBDevice device) {
            final String str = String.format("%s_%s", device.getAddress(), device.getName());

            if (!idMapping.containsKey(str)) {
                idMapping.put(str, nextId++);
            }

            return idMapping.get(str);
        }
    }
}

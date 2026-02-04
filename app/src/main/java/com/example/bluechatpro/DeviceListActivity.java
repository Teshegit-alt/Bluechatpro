package com.example.bluechatpro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluechatpro.adapters.DeviceAdapter;
import com.example.bluechatpro.models.BluetoothDeviceItem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private DeviceAdapter deviceAdapter;
    private List<BluetoothDeviceItem> deviceList = new ArrayList<>();
    private List<BluetoothDeviceItem> pairedDevicesList = new ArrayList<>();
    private List<BluetoothDeviceItem> discoveredDevicesList = new ArrayList<>();

    private ProgressBar progressBar;
    private TextView textStatus;
    private Button btnDiscover;
    private Button btnPairedDevices;
    private Button btnNewDevices;
    private Button btnAllDevices;
    private ImageButton btnBack;
    private RecyclerView recyclerViewDevices;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isDiscovering = false;
    private Handler discoveryHandler = new Handler();
    private static final int DISCOVERY_TIMEOUT = 12000; // 12 seconds

    // BroadcastReceiver for device discovery
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
                BluetoothDevice device = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }

                if (device != null) {
                    try {
                        // Create BluetoothDeviceItem with device name and address
                        String deviceName = getDeviceNameSafe(device);
                        String deviceAddress = device.getAddress();
                        BluetoothDeviceItem deviceItem = new BluetoothDeviceItem(deviceName, deviceAddress);

                        // Check if device is already in the list
                        boolean exists = false;
                        for (BluetoothDeviceItem item : discoveredDevicesList) {
                            if (item.getDeviceAddress().equals(deviceItem.getDeviceAddress())) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            discoveredDevicesList.add(deviceItem);
                            updateDeviceList();

                            // Update status
                            runOnUiThread(() -> {
                                textStatus.setText("Found: " + deviceItem.getDeviceName());
                            });
                        }
                    } catch (SecurityException e) {
                        Log.e("DeviceListActivity", "SecurityException handling found device", e);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // Discovery started
                isDiscovering = true;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.VISIBLE);
                    textStatus.setText("Discovering devices...");
                    btnDiscover.setText("Stop Discovery");
                });
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery finished
                isDiscovering = false;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textStatus.setText("Discovery finished. Found " + discoveredDevicesList.size() + " devices");
                    btnDiscover.setText("Discover Devices");
                });

                // Remove discovery timeout
                discoveryHandler.removeCallbacksAndMessages(null);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // Device pairing state changed
                BluetoothDevice device = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }

                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (device != null) {
                    if (state == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(DeviceListActivity.this,
                                "Paired with " + getDeviceNameSafe(device), Toast.LENGTH_SHORT).show();
                        loadPairedDevices();
                    } else if (state == BluetoothDevice.BOND_BONDING) {
                        Toast.makeText(DeviceListActivity.this,
                                "Pairing with " + getDeviceNameSafe(device) + "...", Toast.LENGTH_SHORT).show();
                    } else if (state == BluetoothDevice.BOND_NONE) {
                        Toast.makeText(DeviceListActivity.this,
                                "Unpaired from " + getDeviceNameSafe(device), Toast.LENGTH_SHORT).show();
                        loadPairedDevices();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the correct layout name
        setContentView(R.layout.activity_device_list);

        // Initialize views
        initializeViews();

        // Get Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Register BroadcastReceiver
        registerReceiver();

        // Setup click listeners
        setupClickListeners();

        // Load paired devices initially
        loadPairedDevices();

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshDeviceList);
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        textStatus = findViewById(R.id.textStatus);
        btnDiscover = findViewById(R.id.btnDiscover);
        btnPairedDevices = findViewById(R.id.btnPairedDevices);
        btnNewDevices = findViewById(R.id.btnNewDevices);
        btnAllDevices = findViewById(R.id.btnAllDevices);
        btnBack = findViewById(R.id.btnBack);
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(BluetoothDeviceItem deviceItem) {
                connectToDevice(deviceItem);
            }

            @Override
            public void onPairClick(BluetoothDeviceItem deviceItem) {
                pairDevice(deviceItem);
            }
        });

        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    private void setupClickListeners() {
        btnDiscover.setOnClickListener(v -> toggleDiscovery());
        btnPairedDevices.setOnClickListener(v -> showPairedDevices());
        btnNewDevices.setOnClickListener(v -> showNewDevices());
        btnAllDevices.setOnClickListener(v -> showAllDevices());
        btnBack.setOnClickListener(v -> finish());
    }

    private void toggleDiscovery() {
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isDiscovering) {
                // Stop discovery
                stopDiscovery();
            } else {
                // Start discovery
                startDiscovery();
            }
        } catch (SecurityException e) {
            Log.e("DeviceListActivity", "SecurityException in toggleDiscovery", e);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            requestBluetoothPermissions();
        }
    }

    private void startDiscovery() {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth scan permission required", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions();
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission required for Bluetooth scanning", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions();
                return;
            }
        }

        clearDiscoveredDevices();

        try {
            if (bluetoothAdapter.startDiscovery()) {
                // Set discovery timeout
                discoveryHandler.postDelayed(() -> {
                    if (isDiscovering) {
                        runOnUiThread(() -> {
                            stopDiscovery();
                            Toast.makeText(DeviceListActivity.this,
                                    "Discovery timeout after 12 seconds", Toast.LENGTH_SHORT).show();
                        });
                    }
                }, DISCOVERY_TIMEOUT);
            } else {
                Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e("DeviceListActivity", "SecurityException starting discovery", e);
            Toast.makeText(this, "Cannot start discovery: Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDiscovery() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.e("DeviceListActivity", "SecurityException stopping discovery", e);
        }

        isDiscovering = false;
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnDiscover.setText("Discover Devices");
            textStatus.setText("Discovery stopped");
        });

        // Remove discovery timeout
        discoveryHandler.removeCallbacksAndMessages(null);
    }

    private void loadPairedDevices() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            pairedDevicesList.clear();

            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesList.add(new BluetoothDeviceItem(getDeviceNameSafe(device), device.getAddress()));
                }
            }

            showPairedDevices();
        } catch (SecurityException e) {
            Log.e("DeviceListActivity", "SecurityException loading paired devices", e);
            Toast.makeText(this, "Cannot load paired devices: Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPairedDevices() {
        deviceList.clear();
        deviceList.addAll(pairedDevicesList);
        deviceAdapter.notifyDataSetChanged();
        textStatus.setText("Paired Devices (" + pairedDevicesList.size() + ")");

        // Update button states
        updateButtonStates(0);
    }

    private void showNewDevices() {
        deviceList.clear();
        deviceList.addAll(discoveredDevicesList);
        deviceAdapter.notifyDataSetChanged();
        textStatus.setText("New Devices (" + discoveredDevicesList.size() + ")");

        // Update button states
        updateButtonStates(1);
    }

    private void showAllDevices() {
        deviceList.clear();
        deviceList.addAll(pairedDevicesList);

        // Add discovered devices that aren't already paired
        for (BluetoothDeviceItem discoveredDevice : discoveredDevicesList) {
            boolean isAlreadyPaired = false;
            for (BluetoothDeviceItem pairedDevice : pairedDevicesList) {
                if (discoveredDevice.getDeviceAddress().equals(pairedDevice.getDeviceAddress())) {
                    isAlreadyPaired = true;
                    break;
                }
            }
            if (!isAlreadyPaired) {
                deviceList.add(discoveredDevice);
            }
        }

        deviceAdapter.notifyDataSetChanged();
        textStatus.setText("All Devices (" + deviceList.size() + ")");

        // Update button states
        updateButtonStates(2);
    }

    private void updateButtonStates(int selectedTab) {
        // Reset all buttons
        btnPairedDevices.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        btnNewDevices.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        btnAllDevices.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        // Highlight selected tab - use a default color
        int highlightColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);

        switch (selectedTab) {
            case 0: // Paired
                btnPairedDevices.setBackgroundColor(highlightColor);
                break;
            case 1: // New
                btnNewDevices.setBackgroundColor(highlightColor);
                break;
            case 2: // All
                btnAllDevices.setBackgroundColor(highlightColor);
                break;
        }
    }

    private void updateDeviceList() {
        runOnUiThread(() -> {
            // Check which tab is active and update accordingly
            int highlightColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);

            // Check if the button has the highlight color
            if (isViewHighlighted(btnNewDevices, highlightColor) ||
                    isViewHighlighted(btnAllDevices, highlightColor)) {
                showAllDevices();
            }
        });
    }

    private boolean isViewHighlighted(View view, int highlightColor) {
        if (view.getBackground() instanceof ColorDrawable) {
            return ((ColorDrawable) view.getBackground()).getColor() == highlightColor;
        }
        return false;
    }

    private void clearDiscoveredDevices() {
        discoveredDevicesList.clear();
        updateDeviceList();
    }

    private void connectToDevice(BluetoothDeviceItem deviceItem) {
        // Cancel discovery if in progress
        if (isDiscovering) {
            stopDiscovery();
        }

        Toast.makeText(this, "Connecting to " + deviceItem.getDeviceName(), Toast.LENGTH_SHORT).show();

        // Start ChatActivity
        Intent intent = new Intent(DeviceListActivity.this, ChatActivity.class);
        intent.putExtra("device_name", deviceItem.getDeviceName());
        intent.putExtra("device_address", deviceItem.getDeviceAddress());
        startActivity(intent);
    }

    private void pairDevice(BluetoothDeviceItem deviceItem) {
        try {
            // Get BluetoothDevice from the list or create it from address
            String deviceAddress = deviceItem.getDeviceAddress();
            BluetoothDevice device = null;

            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                }
            } catch (SecurityException e) {
                Log.e("DeviceListActivity", "SecurityException getting remote device", e);
                Toast.makeText(this, "Permission denied to access device", Toast.LENGTH_SHORT).show();
                return;
            }

            if (device == null) {
                Toast.makeText(this, "Cannot access device", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth connect permission required", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions();
                return;
            }

            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                // Already paired, unpair
                try {
                    Method removeBondMethod = device.getClass().getMethod("removeBond");
                    removeBondMethod.invoke(device);
                    Toast.makeText(this, "Unpairing from " + getDeviceNameSafe(device), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("DeviceListActivity", "Failed to unpair device", e);
                    Toast.makeText(this, "Failed to unpair device", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Not paired, initiate pairing
                try {
                    device.createBond();
                    Toast.makeText(this, "Pairing with " + getDeviceNameSafe(device) + "...", Toast.LENGTH_SHORT).show();
                } catch (SecurityException e) {
                    Log.e("DeviceListActivity", "SecurityException creating bond", e);
                    Toast.makeText(this, "Failed to pair device: Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e("DeviceListActivity", "Error in pairDevice", e);
            Toast.makeText(this, "Failed to pair device", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    200);
        }
    }

    private String getDeviceNameSafe(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                String name = device.getName();
                return name != null ? name : "Unknown Device";
            }
        } catch (SecurityException e) {
            Log.e("DeviceListActivity", "SecurityException getting device name", e);
        }
        return "Unknown Device";
    }

    private void refreshDeviceList() {
        loadPairedDevices();

        // If discovery was running, restart it
        if (isDiscovering) {
            stopDiscovery();
            startDiscovery();
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                loadPairedDevices();
            } else {
                Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cancel discovery if in progress
        if (isDiscovering) {
            stopDiscovery();
        }

        // Unregister receiver
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }

        // Remove any pending handlers
        discoveryHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Cancel discovery when activity is paused to save battery
        if (isDiscovering) {
            stopDiscovery();
        }
    }
}
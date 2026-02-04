package com.example.bluechatpro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE_BT = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CONNECT_PERMISSION_REQUEST_CODE = 101;

    // UI elements
    private TextView textBluetoothStatus;
    private Button btnEnableBluetooth;
    private Button btnDiscoverDevices;
    private Button btnMakeDiscoverable;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        checkBluetoothSupport();
        requestAllPermissions();
    }

    private void initializeViews() {
        // Find views by ID
        textBluetoothStatus = findViewById(R.id.textBluetoothStatus);
        btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        btnDiscoverDevices = findViewById(R.id.btnDiscoverDevices);
        btnMakeDiscoverable = findViewById(R.id.btnMakeDiscoverable);
        btnSettings = findViewById(R.id.btnSettings);

        // Set click listeners
        if (btnEnableBluetooth != null) {
            btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());
        }

        if (btnDiscoverDevices != null) {
            btnDiscoverDevices.setOnClickListener(v -> openDeviceList());
        }

        if (btnMakeDiscoverable != null) {
            btnMakeDiscoverable.setOnClickListener(v -> makeDiscoverable());
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openBluetoothSettings());
        }
    }

    private void checkBluetoothSupport() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Your device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        } else {
            updateBluetoothStatus();
        }
    }

    private void updateBluetoothStatus() {
        runOnUiThread(() -> {
            if (bluetoothAdapter == null) {
                return;
            }

            try {
                // Check if we can read Bluetooth state
                if (hasBluetoothPermission()) {
                    if (bluetoothAdapter.isEnabled()) {
                        if (textBluetoothStatus != null) {
                            textBluetoothStatus.setText("Bluetooth: ON");
                            textBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                        }

                        if (btnEnableBluetooth != null) {
                            btnEnableBluetooth.setText("Disable Bluetooth");
                        }

                        // Enable other buttons
                        if (btnDiscoverDevices != null) {
                            btnDiscoverDevices.setEnabled(true);
                        }
                        if (btnMakeDiscoverable != null) {
                            btnMakeDiscoverable.setEnabled(true);
                        }
                    } else {
                        if (textBluetoothStatus != null) {
                            textBluetoothStatus.setText("Bluetooth: OFF");
                            textBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                        }

                        if (btnEnableBluetooth != null) {
                            btnEnableBluetooth.setText("Enable Bluetooth");
                        }

                        // Disable other buttons
                        if (btnDiscoverDevices != null) {
                            btnDiscoverDevices.setEnabled(false);
                        }
                        if (btnMakeDiscoverable != null) {
                            btnMakeDiscoverable.setEnabled(false);
                        }
                    }
                } else {
                    // Don't have permission to check Bluetooth status
                    if (textBluetoothStatus != null) {
                        textBluetoothStatus.setText("Bluetooth: Permission needed");
                        textBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                    }
                }
            } catch (SecurityException e) {
                Log.e("MainActivity", "SecurityException checking Bluetooth status", e);
                if (textBluetoothStatus != null) {
                    textBluetoothStatus.setText("Bluetooth: Permission denied");
                }
            }
        });
    }

    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            return;
        }

        try {
            if (bluetoothAdapter.isEnabled()) {
                // Disable Bluetooth
                disableBluetooth();
            } else {
                // Enable Bluetooth
                enableBluetoothWithIntent();
            }
        } catch (SecurityException e) {
            Log.e("MainActivity", "SecurityException checking Bluetooth state", e);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            requestBluetoothPermissions();
        }
    }

    private void disableBluetooth() {
        // For Android 12+, we need BLUETOOTH_CONNECT permission to disable Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth connect permission required to disable", Toast.LENGTH_SHORT).show();
                requestConnectPermission();
                return;
            }
        }

        try {
            bluetoothAdapter.disable();
            Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> runOnUiThread(this::updateBluetoothStatus), 500);
        } catch (SecurityException e) {
            Log.e("MainActivity", "SecurityException disabling Bluetooth", e);
            Toast.makeText(this, "Cannot disable Bluetooth: Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableBluetoothWithIntent() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        try {
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } catch (SecurityException e) {
            Log.e("MainActivity", "SecurityException enabling Bluetooth", e);
            Toast.makeText(this, "Cannot enable Bluetooth: Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Always need these
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Android 12+ specific permissions
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
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void requestBluetoothPermissions() {
        requestAllPermissions();
    }

    private void requestConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    CONNECT_PERMISSION_REQUEST_CODE);
        }
    }

    private void openDeviceList() {
        if (bluetoothAdapter == null) {
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivity(intent);
        } catch (SecurityException e) {
            Log.e("MainActivity", "SecurityException checking Bluetooth state", e);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeDiscoverable() {
        if (bluetoothAdapter == null) {
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // 5 minutes
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);
        } catch (SecurityException e) {
            Log.e("MainActivity", "SecurityException making discoverable", e);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void openBluetoothSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open Bluetooth settings", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE || requestCode == CONNECT_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                updateBluetoothStatus();
            } else {
                Toast.makeText(this, "Some permissions denied. Some features may not work.",
                        Toast.LENGTH_LONG).show();
                updateBluetoothStatus();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth was not enabled", Toast.LENGTH_SHORT).show();
            }
            updateBluetoothStatus();
        } else if (requestCode == REQUEST_DISCOVERABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Device is now discoverable for 5 minutes", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Device is not discoverable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();
    }
}
package com.example.bluechatpro.models;

import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.app.ActivityCompat;
import android.content.Context;

public class BluetoothDeviceItem implements Parcelable {
    private String deviceName;
    private String deviceAddress;
    private boolean isPaired;
    private int signalStrength;
    private BluetoothDevice bluetoothDevice;
    private Context context;

    // Constructor with context for permission checks
    public BluetoothDeviceItem(String deviceName, String deviceAddress, BluetoothDevice device, Context context) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.bluetoothDevice = device;
        this.context = context;

        // Check pairing state safely
        this.isPaired = false;
        if (device != null && context != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        this.isPaired = device.getBondState() == BluetoothDevice.BOND_BONDED;
                    }
                } else {
                    this.isPaired = device.getBondState() == BluetoothDevice.BOND_BONDED;
                }
            } catch (SecurityException e) {
                // Permission denied, assume not paired
                this.isPaired = false;
            }
        }
        this.signalStrength = -1; // Default value
    }

    // Constructor without context (for simple cases)
    public BluetoothDeviceItem(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.isPaired = false;
        this.signalStrength = -1;
        this.bluetoothDevice = null;
        this.context = null;
    }

    // Constructor with device only (legacy support)
    public BluetoothDeviceItem(BluetoothDevice device, Context context) {
        this(getDeviceNameSafe(device, context),
                device != null ? device.getAddress() : "Unknown",
                device,
                context);
    }

    // Static helper method to safely get device name
    private static String getDeviceNameSafe(BluetoothDevice device, Context context) {
        if (device == null) {
            return "Unknown Device";
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (context != null &&
                        ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String name = device.getName();
                    return name != null ? name : "Unknown Device";
                }
            } else {
                String name = device.getName();
                return name != null ? name : "Unknown Device";
            }
        } catch (SecurityException e) {
            // Permission denied
        }
        return "Unknown Device";
    }

    // Getters and setters
    public String getDeviceName() { return deviceName; }
    public String getDeviceAddress() { return deviceAddress; }
    public boolean isPaired() { return isPaired; }
    public int getSignalStrength() { return signalStrength; }
    public BluetoothDevice getBluetoothDevice() { return bluetoothDevice; }

    public void setPaired(boolean paired) { isPaired = paired; }
    public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }

    // Update pairing status with permission check
    public void updatePairingStatus() {
        if (bluetoothDevice != null && context != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        this.isPaired = bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED;
                    }
                } else {
                    this.isPaired = bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED;
                }
            } catch (SecurityException e) {
                this.isPaired = false;
            }
        }
    }

    // Parcelable implementation
    protected BluetoothDeviceItem(Parcel in) {
        deviceName = in.readString();
        deviceAddress = in.readString();
        isPaired = in.readByte() != 0;
        signalStrength = in.readInt();
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        context = null; // Context is not parcelable
    }

    public static final Creator<BluetoothDeviceItem> CREATOR = new Creator<BluetoothDeviceItem>() {
        @Override
        public BluetoothDeviceItem createFromParcel(Parcel in) {
            return new BluetoothDeviceItem(in);
        }

        @Override
        public BluetoothDeviceItem[] newArray(int size) {
            return new BluetoothDeviceItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceName);
        dest.writeString(deviceAddress);
        dest.writeByte((byte) (isPaired ? 1 : 0));
        dest.writeInt(signalStrength);
        dest.writeParcelable(bluetoothDevice, flags);
        // Note: context is not written to parcel
    }

    @Override
    public String toString() {
        return deviceName + " (" + deviceAddress + ") - " + (isPaired ? "Paired" : "Not Paired");
    }
}
package com.example.bluechatpro.adapters;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluechatpro.R;
import com.example.bluechatpro.models.BluetoothDeviceItem;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<BluetoothDeviceItem> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDeviceItem deviceItem);
        void onPairClick(BluetoothDeviceItem deviceItem);
    }

    public DeviceAdapter(List<BluetoothDeviceItem> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDeviceItem deviceItem = deviceList.get(position);

        // Set device name
        holder.textDeviceName.setText(deviceItem.getDeviceName());

        // Set device address
        holder.textDeviceAddress.setText(deviceItem.getDeviceAddress());

        // Set device type icon
        int iconRes = getDeviceIcon(deviceItem);
        holder.imageDeviceIcon.setImageResource(iconRes);

        // Set pairing status
        if (deviceItem.isPaired()) {
            holder.textPairStatus.setText("Paired");
            holder.textPairStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_green_dark));
            holder.btnPair.setText("Unpair");
        } else {
            holder.textPairStatus.setText("Not paired");
            holder.textPairStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_red_dark));
            holder.btnPair.setText("Pair");
        }

        // Set click listeners
        holder.cardDevice.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(deviceItem);
            }
        });

        holder.btnPair.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPairClick(deviceItem);
            }
        });

        // Show/hide signal strength (if available)
        if (deviceItem.getSignalStrength() > 0) {
            holder.textSignalStrength.setText("Signal: " + deviceItem.getSignalStrength() + " dBm");
            holder.textSignalStrength.setVisibility(View.VISIBLE);
        } else {
            holder.textSignalStrength.setVisibility(View.GONE);
        }
    }

    private int getDeviceIcon(BluetoothDeviceItem deviceItem) {
        // In a real app, you would check BluetoothClass to get device type
        // For now, we'll use a simple logic based on device name
        String name = deviceItem.getDeviceName().toLowerCase();

        if (name.contains("phone") || name.contains("mobile") || name.contains("android")) {
            return R.drawable.ic_phone;
        } else if (name.contains("tablet") || name.contains("pad")) {
            return R.drawable.ic_tablet;
        } else if (name.contains("laptop") || name.contains("pc") || name.contains("computer")) {
            return R.drawable.ic_computer;
        } else if (name.contains("headset") || name.contains("earphone") || name.contains("headphone")) {
            return R.drawable.ic_headset;
        } else if (name.contains("watch") || name.contains("wear")) {
            return R.drawable.ic_watch;
        } else {
            return R.drawable.ic_device_unknown;
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        CardView cardDevice;
        ImageView imageDeviceIcon;
        TextView textDeviceName;
        TextView textDeviceAddress;
        TextView textPairStatus;
        TextView textSignalStrength;
        TextView btnPair;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDevice = itemView.findViewById(R.id.cardDevice);
            imageDeviceIcon = itemView.findViewById(R.id.imageDeviceIcon);
            textDeviceName = itemView.findViewById(R.id.textDeviceName);
            textDeviceAddress = itemView.findViewById(R.id.textDeviceAddress);
            textPairStatus = itemView.findViewById(R.id.textPairStatus);
            textSignalStrength = itemView.findViewById(R.id.textSignalStrength);
            btnPair = itemView.findViewById(R.id.btnPair);
        }
    }
}
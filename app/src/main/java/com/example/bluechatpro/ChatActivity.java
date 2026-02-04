package com.example.bluechatpro;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluechatpro.adapters.MessageAdapter;
import com.example.bluechatpro.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private String connectedDeviceName;
    private String connectedDeviceAddress;
    private BluetoothDevice connectedDevice;

    // UI elements
    private ProgressBar progressBar;
    private TextView textStatus;
    private TextView textDeviceName;
    private TextView textDeviceAddress;
    private EditText editMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private ImageButton btnBack;
    private ImageButton btnInfo;
    private RecyclerView recyclerViewMessages;

    // Handler for BluetoothService callbacks
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    handleStateChange(msg);
                    break;

                case BluetoothService.MESSAGE_READ:
                    handleMessageRead(msg);
                    break;

                case BluetoothService.MESSAGE_WRITE:
                    handleMessageWrite(msg);
                    break;

                case BluetoothService.MESSAGE_DEVICE_NAME:
                    if (msg.obj != null) {
                        connectedDeviceName = msg.obj.toString();
                    }
                    updateTitle();
                    break;

                case BluetoothService.MESSAGE_TOAST:
                    if (msg.obj != null) {
                        Toast.makeText(ChatActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    }
                    break;

                case BluetoothService.MESSAGE_DEVICE_CONNECTED:
                    handleDeviceConnected(msg);
                    break;

                case BluetoothService.MESSAGE_CONNECTION_FAILED:
                    handleConnectionFailed();
                    break;

                case BluetoothService.MESSAGE_CONNECTION_LOST:
                    handleConnectionLost();
                    break;
            }
        }
    };

    // BroadcastReceiver for Bluetooth state changes
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(ChatActivity.this, "Bluetooth turned off", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        updateStatus("Bluetooth turning off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(ChatActivity.this, "Bluetooth turned on", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        updateStatus("Bluetooth turning on...");
                        break;
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Device disconnected
                BluetoothDevice device = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }

                if (device != null && device.getAddress().equals(connectedDeviceAddress)) {
                    runOnUiThread(() -> {
                        updateStatus("Device disconnected");
                        Toast.makeText(ChatActivity.this, "Disconnected from " + connectedDeviceName,
                                Toast.LENGTH_SHORT).show();
                        enableInput(false);
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get device info from intent
        Intent intent = getIntent();
        connectedDeviceName = intent.getStringExtra("device_name");
        connectedDeviceAddress = intent.getStringExtra("device_address");

        if (connectedDeviceName == null || connectedDeviceAddress == null) {
            Toast.makeText(this, "Invalid device information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Get Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup UI
        setupUI();

        // Check Bluetooth permissions
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            initializeBluetooth();
        }
    }

    private boolean checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private void requestBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth permissions required for chat", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        textStatus = findViewById(R.id.textStatus);
        textDeviceName = findViewById(R.id.textDeviceName);
        textDeviceAddress = findViewById(R.id.textDeviceAddress);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        btnBack = findViewById(R.id.btnBack);
        btnInfo = findViewById(R.id.btnInfo);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
    }

    private void setupUI() {
        // Setup RecyclerView
        setupRecyclerView();

        // Setup message input
        setupMessageInput();

        // Setup click listeners
        setupClickListeners();

        // Update title
        updateTitle();
        enableInput(false); // Disable until connected
        updateStatus("Connecting...");
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void initializeBluetooth() {
        try {
            // Get the Bluetooth device with permission check
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                connectedDevice = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
            } else {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Setup Bluetooth service
            setupBluetoothService();

            // Register broadcast receiver
            registerBluetoothReceiver();

        } catch (SecurityException e) {
            Log.e("ChatActivity", "SecurityException: " + e.getMessage());
            Toast.makeText(this, "Security exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupBluetoothService() {
        bluetoothService = new BluetoothService(this, handler);

        // Connect to the device
        if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
            bluetoothService.start();
        }

        // Start connection
        new Handler().postDelayed(() -> {
            if (bluetoothService != null && connectedDevice != null) {
                bluetoothService.connect(connectedDevice);
            }
        }, 500);
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnAttach.setOnClickListener(v -> showAttachmentOptions());
        btnInfo.setOnClickListener(v -> showConnectionInfo());

        btnInfo.setOnLongClickListener(v -> {
            clearChat();
            return true;
        });
    }

    private void setupMessageInput() {
        if (editMessage != null) {
            // Send on Enter key
            editMessage.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    sendMessage();
                    return true;
                }
                return false;
            });

            // Enable/disable send button based on text
            editMessage.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    btnSend.setEnabled(s.toString().trim().length() > 0);
                }
            });
        }
    }

    private void updateTitle() {
        runOnUiThread(() -> {
            if (textDeviceName != null) {
                textDeviceName.setText(connectedDeviceName);
            }
            if (textDeviceAddress != null) {
                textDeviceAddress.setText(connectedDeviceAddress);
            }
            setTitle("Chat with " + connectedDeviceName);
        });
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            if (textStatus != null) {
                textStatus.setText(status);
            }
            if (progressBar != null) {
                if (status.contains("Connecting")) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void enableInput(boolean enabled) {
        runOnUiThread(() -> {
            if (editMessage != null) {
                editMessage.setEnabled(enabled);
                if (enabled) {
                    editMessage.setHint("Type a message...");
                    editMessage.requestFocus();
                } else {
                    editMessage.setHint("Connecting...");
                }
            }
            if (btnSend != null) {
                if (editMessage != null) {
                    btnSend.setEnabled(enabled && editMessage.getText().toString().trim().length() > 0);
                } else {
                    btnSend.setEnabled(enabled);
                }
            }
            if (btnAttach != null) {
                btnAttach.setEnabled(enabled);
            }
        });
    }

    private void sendMessage() {
        if (editMessage == null) {
            return;
        }

        String message = editMessage.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }

        if (bluetoothService == null || bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Not connected to device", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create and add message using the correct constructor
            ChatMessage chatMessage = new ChatMessage(
                    message,
                    "You",
                    "your_address", // You might want to add your device address here
                    true
            );
            chatMessage.setTimestamp(System.currentTimeMillis());

            messageList.add(chatMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();

            // Clear input
            editMessage.setText("");

            // Send via Bluetooth
            byte[] send = message.getBytes();
            bluetoothService.write(send);

        } catch (Exception e) {
            Log.e("ChatActivity", "Error sending message: " + e.getMessage());
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        runOnUiThread(() -> {
            if (messageList.size() > 0 && recyclerViewMessages != null) {
                recyclerViewMessages.smoothScrollToPosition(messageList.size() - 1);
            }
        });
    }

    // Handler methods
    private void handleStateChange(Message msg) {
        switch (msg.arg1) {
            case BluetoothService.STATE_CONNECTED:
                updateStatus("Connected");
                enableInput(true);

                // Add welcome message
                ChatMessage welcomeMsg = new ChatMessage(
                        "Connected to " + connectedDeviceName,
                        "System",
                        connectedDeviceAddress,
                        false
                );
                welcomeMsg.setTimestamp(System.currentTimeMillis());

                messageList.add(welcomeMsg);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                scrollToBottom();
                break;

            case BluetoothService.STATE_CONNECTING:
                updateStatus("Connecting...");
                enableInput(false);
                break;

            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
                updateStatus("Not connected");
                enableInput(false);
                break;
        }
    }

    private void handleMessageRead(Message msg) {
        if (msg.obj instanceof byte[]) {
            byte[] readBuf = (byte[]) msg.obj;
            int bytes = msg.arg1;

            if (bytes > 0) {
                String readMessage = new String(readBuf, 0, bytes);

                // Add received message to UI
                ChatMessage chatMessage = new ChatMessage(
                        readMessage,
                        connectedDeviceName,
                        connectedDeviceAddress,
                        false
                );
                chatMessage.setTimestamp(System.currentTimeMillis());

                messageList.add(chatMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                scrollToBottom();

                // Show notification
                showNotification(readMessage);
            }
        }
    }

    private void handleMessageWrite(Message msg) {
        if (msg.obj instanceof byte[]) {
            byte[] writeBuf = (byte[]) msg.obj;
            String writeMessage = new String(writeBuf);

            // Update message status in the list
            if (messageList.size() > 0) {
                for (int i = messageList.size() - 1; i >= 0; i--) {
                    ChatMessage message = messageList.get(i);
                    if (message.getContent().equals(writeMessage) && message.isSent()) {
                        // Update status if needed
                        messageAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
        }
    }

    private void handleDeviceConnected(Message msg) {
        if (msg.obj instanceof BluetoothDevice) {
            BluetoothDevice device = (BluetoothDevice) msg.obj;
            if (device != null) {
                connectedDevice = device;
                String deviceName = "Unknown Device";
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        deviceName = device.getName();
                        if (deviceName == null) {
                            deviceName = "Unknown Device";
                        }
                    }
                } catch (SecurityException e) {
                    Log.e("ChatActivity", "SecurityException getting device name: " + e.getMessage());
                }
                connectedDeviceName = deviceName;
                updateTitle();
                Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleConnectionFailed() {
        updateStatus("Connection failed");
        enableInput(false);

        Toast.makeText(this, "Unable to connect to device", Toast.LENGTH_LONG).show();

        // Try to reconnect after 3 seconds
        new Handler().postDelayed(() -> {
            if (bluetoothService != null && connectedDevice != null) {
                bluetoothService.connect(connectedDevice);
            }
        }, 3000);
    }

    private void handleConnectionLost() {
        updateStatus("Connection lost");
        enableInput(false);

        Toast.makeText(this, "Connection lost. Attempting to reconnect...", Toast.LENGTH_LONG).show();

        // Add disconnect message
        ChatMessage disconnectMsg = new ChatMessage(
                "Connection lost",
                "System",
                connectedDeviceAddress,
                false
        );
        disconnectMsg.setTimestamp(System.currentTimeMillis());

        messageList.add(disconnectMsg);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();

        // Try to reconnect after 5 seconds
        new Handler().postDelayed(() -> {
            if (bluetoothService != null && connectedDevice != null) {
                bluetoothService.connect(connectedDevice);
            }
        }, 5000);
    }

    private void showAttachmentOptions() {
        Toast.makeText(this, "Attachment feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showConnectionInfo() {
        String state = "Unknown";
        if (bluetoothService != null) {
            int serviceState = bluetoothService.getState();
            switch (serviceState) {
                case BluetoothService.STATE_NONE: state = "None"; break;
                case BluetoothService.STATE_LISTEN: state = "Listening"; break;
                case BluetoothService.STATE_CONNECTING: state = "Connecting"; break;
                case BluetoothService.STATE_CONNECTED: state = "Connected"; break;
            }
        }

        String info = "Device: " + connectedDeviceName + "\n" +
                "Address: " + connectedDeviceAddress + "\n" +
                "Status: " + getStatusText() + "\n" +
                "Messages: " + messageList.size() + "\n" +
                "Connection State: " + state;

        new AlertDialog.Builder(this)
                .setTitle("Connection Info")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getStatusText() {
        if (textStatus != null) {
            return textStatus.getText().toString();
        }
        return "Unknown";
    }

    private void clearChat() {
        if (messageList.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear all messages?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    messageList.clear();
                    messageAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotification(String message) {
        Log.d("ChatActivity", "New message received: " + message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop Bluetooth service
        if (bluetoothService != null) {
            bluetoothService.stop();
        }

        // Unregister receiver
        try {
            unregisterReceiver(bluetoothStateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }

    @Override
    public void onBackPressed() {
        // Confirm before exiting if connected
        if (bluetoothService != null && bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            new AlertDialog.Builder(this)
                    .setTitle("Disconnect")
                    .setMessage("Do you want to disconnect from " + connectedDeviceName + "?")
                    .setPositiveButton("Disconnect", (dialog, which) -> finish())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
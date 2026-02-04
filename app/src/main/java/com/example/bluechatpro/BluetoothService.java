package com.example.bluechatpro;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final String APP_NAME = "BlueChatPro";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Message types sent from BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECTION_FAILED = 6;
    public static final int MESSAGE_CONNECTION_LOST = 7;
    public static final int MESSAGE_DEVICE_CONNECTED = 8;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private final Context context;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;

    public BluetoothService(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            sendToastMessage("Bluetooth not supported on this device");
        }
    }

    // Set the current state of the connection
    private synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    // Get current connection state
    public synchronized int getState() {
        return state;
    }

    // Start the chat service
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    // Start ConnectThread to initiate a connection to a remote device
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    // Start ConnectedThread to manage a Bluetooth connection
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(MESSAGE_DEVICE_NAME);
        String deviceName = (device != null) ? getDeviceNameSafe(device) : "Unknown Device";
        msg.obj = deviceName;
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);

        // Send connection success message
        Message connectedMsg = handler.obtainMessage(MESSAGE_DEVICE_CONNECTED);
        connectedMsg.obj = device;
        handler.sendMessage(connectedMsg);
    }

    // Safe method to get device name with permission check
    private String getDeviceNameSafe(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                return device.getName();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when getting device name", e);
        }
        return "Unknown Device";
    }

    // Stop all threads
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        setState(STATE_NONE);
    }

    // Write to the ConnectedThread
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                sendToastMessage("Not connected to any device");
                return;
            }
            r = connectedThread;
        }

        if (r != null) {
            r.write(out);
        }
    }

    // Helper method to send toast messages
    private void sendToastMessage(String message) {
        if (handler != null) {
            Message msg = handler.obtainMessage(MESSAGE_TOAST);
            msg.obj = message;
            handler.sendMessage(msg);
        }
    }

    // Indicate that the connection attempt failed and notify the UI Activity
    private void connectionFailed() {
        Message msg = handler.obtainMessage(MESSAGE_CONNECTION_FAILED);
        handler.sendMessage(msg);

        sendToastMessage("Unable to connect to device");

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    // Indicate that the connection was lost and notify the UI Activity
    private void connectionLost() {
        Message msg = handler.obtainMessage(MESSAGE_CONNECTION_LOST);
        handler.sendMessage(msg);

        sendToastMessage("Device connection was lost");

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    // This thread runs while listening for incoming connections
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket with permission check
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    sendToastMessage("Bluetooth permission required");
                    return;
                }

                // Wrap in try-catch to handle SecurityException
                try {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException when creating server socket", e);
                    sendToastMessage("Security exception: Bluetooth permission denied");
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "IOException when creating server socket", e);
                    sendToastMessage("Failed to create server socket");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception when creating server socket", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN AcceptThread");
            setName("AcceptThread");

            if (serverSocket == null) {
                Log.e(TAG, "Server socket is null");
                return;
            }

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                BluetoothDevice device = null;
                                try {
                                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                        device = socket.getRemoteDevice();
                                    }
                                } catch (SecurityException e) {
                                    Log.e(TAG, "SecurityException when getting remote device", e);
                                }
                                connected(socket, device);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }

            Log.i(TAG, "END AcceptThread");

            // Clean up server socket
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close server socket", e);
            }
        }

        public void cancel() {
            Log.d(TAG, "cancel AcceptThread");
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed", e);
            }
        }
    }

    // This thread runs while attempting to make an outgoing connection with a device
    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            if (device == null) {
                Log.e(TAG, "Device is null");
                return;
            }

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    sendToastMessage("Bluetooth permission required");
                    return;
                }

                try {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException when creating socket", e);
                    sendToastMessage("Security exception: Cannot create connection");
                } catch (IOException e) {
                    Log.e(TAG, "Socket create() failed", e);
                    sendToastMessage("Failed to create connection socket");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in ConnectThread constructor", e);
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnectThread");
            setName("ConnectThread");

            if (socket == null) {
                Log.e(TAG, "Socket is null, cannot connect");
                connectionFailed();
                return;
            }

            // Always cancel discovery because it will slow down a connection
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when cancelling discovery", e);
            }

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                socket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Unable to connect: " + e.getMessage());
                // Close the socket
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close socket during connection failure", e2);
                }
                connectionFailed();
                return;
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when connecting", e);
                sendToastMessage("Security exception: Cannot connect to device");
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(socket, device);
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    // This thread runs during a connection with a remote device
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);

                    if (bytes > 0) {
                        // Send the obtained bytes to the UI activity
                        byte[] messageBytes = new byte[bytes];
                        System.arraycopy(buffer, 0, messageBytes, 0, bytes);
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, messageBytes)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        // Write to the connected OutStream
        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);

                // Share the sent message back to the UI Activity
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                sendToastMessage("Failed to send message");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException during write", e);
                sendToastMessage("Security exception: Cannot send message");
            }
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
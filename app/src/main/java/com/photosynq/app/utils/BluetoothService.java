package com.photosynq.app.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.photosynq.app.PhotoSyncApplication;
import com.photosynq.app.model.BluetoothMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.photosynq.app.utils.NxDebugEngine.log;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    // Unique UUID for this application
    private static final UUID PHOTOSYNQ_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    


    // Member fields
    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private BluetoothMessage mBluetoothMessage;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private String readMessage;
    private StringBuffer measurement;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_FIRST_RESP = 4;  // now connected to a remote device
    
    public static final String DEVICE_ADDRESS = "BLUETOOTH_ADDRESS";

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    private BluetoothService(BluetoothMessage bluetoothMessage, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mBluetoothMessage = bluetoothMessage;
    }

    private static BluetoothService bluetoothService;
    public static BluetoothService getInstance(BluetoothMessage bluetoothMessage, Handler handler){

        if (bluetoothService == null){

            bluetoothService = new BluetoothService(bluetoothMessage, handler);
        }
        bluetoothService.mBluetoothMessage = bluetoothMessage;
        bluetoothService.mHandler = handler;

        return bluetoothService;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1, mBluetoothMessage).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME, mBluetoothMessage);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        measurement=new StringBuffer();

        r.write(out);

    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST, mBluetoothMessage);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect to device.\n" +
                "\n Make sure device is powered on (device auto-shuts off after 2 min of inactivity).\n" +
                "\n Check batteries if connection issues persist");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        mHandler.obtainMessage (Constants.MESSAGE_STOP, 0, -1, "STOP").sendToTarget();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST, mBluetoothMessage);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(PHOTOSYNQ_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN ## mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.i(TAG, "create Connected");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
			Log.i(TAG, "BEGIN $$$$$$$ mConnectedThread");
			byte[] buffer = new byte[10485];
            //measurement=new StringBuffer();
            //StringBuffer tempMeasurement=new StringBuffer();
            //int totalbytes =0;
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    PhotoSyncApplication.sApplication.log("read from bt stream", bytes + " bytes read", "bt-transfers");
                    log("read buffer: %s", new String(buffer));

                    // Send the obtained bytes to the UI Activity
//					mHandler.obtainMessage(ResultActivity.MESSAGE_READ, bytes,-1, buffer).sendToTarget();
                    readMessage = new String(buffer, 0, bytes);
                    long time = System.currentTimeMillis();
//                    0xFF ... 0xFE
                    PhotoSyncApplication.sApplication.log("incoming BT string", readMessage, "bt-transfers");

                    measurement.append(readMessage.replaceAll("\\{", "{\"time\":" + time + ","));

                    //Log.d("DeviceOutput", readMessage);
                    //Log.d("DeviceOutput-Measure=", measurement.toString());
                    //tempMeasurement.append(readMessage.replaceAll("\\{", "{\"time\":\""+time+"\","));
                    mBluetoothMessage.message = measurement.toString();

                    PhotoSyncApplication.sApplication.log("checking if message contains ############", mBluetoothMessage.message.replaceAll("\\r\\n", "######"), "bt-transfers");
                    if (mBluetoothMessage.message.replaceAll("\\r\\n", "######").contains("############")) {
                        PhotoSyncApplication.sApplication.log("processed string", mBluetoothMessage.message, "bt-transfers");
						mHandler.obtainMessage (Constants.MESSAGE_READ, measurement.length(), -1, mBluetoothMessage).sendToTarget();
                        Log.d("DeviceOutput", "MEASUREMENT Complete");
                        measurement = null;
						measurement=new StringBuffer();
                        buffer = null;
                        buffer = new byte[10485];


                        try {
                            Thread.sleep(1000); //1000 milliseconds is one second.
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

					}else{
                        //Log.d("DeviceOutput", "STREAMING");
                        mHandler.obtainMessage (Constants.MESSAGE_STREAM, readMessage.length(), -1, readMessage).sendToTarget();
                        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, BluetoothService.STATE_FIRST_RESP, 0, mBluetoothMessage).sendToTarget();

                    }
				} catch (IOException e) {
                    mHandler.obtainMessage (Constants.MESSAGE_STOP, 0, -1, "STOP").sendToTarget();
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                log("sending data to multispec %s", new String(buffer));
                PhotoSyncApplication.sApplication.log("sending data to multispec", new String(buffer), "bt-outgoing");
                mmOutStream.write(buffer);

//                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, mBluetoothMessage)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
package com.example.myproject;

import android.Manifest;
import android.app.AlertDialog;
//import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "MYAPP";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    AlertDialog mProgressDialog;
    private ConnectedThread mConnectedThread;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothConnectionService(Context context){
        mContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        start();

    }
    // this thread runs while listening for incoming connections.
    //It behaves like a server-side client.
    //It runs until a connection is accepted or o until cancelled.

    private class AcceptThread extends Thread{

        //local server socket
        private final BluetoothServerSocket mmServerSocket;

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            //create a new listening server socket
            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using:" + MY_UUID_INSECURE);
            }catch(IOException e){
                Log.e(TAG, "AcceptThread: IOException:" + e.getMessage());

            }
            mmServerSocket = tmp;

        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;
            try{
                //this is a blocking call and will only return on a successful connection or an exception
                Log.d(TAG, "run: RFCOMM server socket start.......");

                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCOMM server socket accepted connection.");

            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException:" + e.getMessage());
            }

            //for later
            if(socket != null){
                Log.e(TAG, "AcceptThread: Connection accepted, closing server socket.");

                connected(socket, mmDevice);   // Start connection

                //closing the server socket so this thread ends
                try{
                    mmServerSocket.close();
                    Log.e(TAG,"AcceptThread: Server socket closed after successful connection.");
                } catch (IOException e){
                    Log.e(TAG, "AcceptThread: Error closing server socket - " + e.getMessage());
                }

                //ending the thread
                return;
            }

            Log.i(TAG, "cancel: Canceling AcceptThread.");
        }
        public void cancel(){
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try{
                mmServerSocket.close();
            } catch (IOException e){
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed." + e.getMessage());
            }
        }

    }
    //This thread runs while attempting to make an outgoing connection with a device.
    //It runs straight through the connection, either succeeds or fails.
    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;
        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID= uuid;
        }
        //RUN method
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN: mConnectThread");
            //Get a BluetoothSocket for a connection with the given BluetoothDevice
            try{
                Log.d(TAG, "ConnectThread: Trying to create InsecureRF commSocket usingUUID:" +
                        MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            }catch (IOException e){
                Log.e(TAG,"ConnectThread: Could not create InsecureRfcommSocket" + e.getMessage());
            }
            mmSocket = tmp;

            //note: always cancel discovery because it will slow down the connection.
            mBluetoothAdapter.cancelDiscovery();

            //making a connection to the BluetoothSocket and retrying the connection if connection fails
            int maxAttempts = 3;
            int attempt = 0;
            boolean isConnected = false;

            while (attempt <= maxAttempts && !isConnected){
                try{
                    Log.d(TAG, "Attempt " + attempt + ": Trying to connect...");
                    mmSocket.connect();
                    Log.e(TAG, "run: Connected on attempt " + attempt);
                    isConnected = true;
                    break;
                } catch (IOException e){
                    Log.e(TAG, "Attempt " + attempt + " failed: " + e.getMessage());

                    if(attempt == maxAttempts){
                        try {
                            mmSocket.close();
                            Log.d(TAG, "run: Closed socket after 3 failed attempts");
                        } catch (IOException e1){
                            Log.e(TAG, "run: Failed to close socket: " + e1.getMessage());
                        }
                        ((android.app.Activity) mContext).runOnUiThread(() ->
                                android.widget.Toast.makeText(mContext,
                                        "Failed to connect to Bluetooth device after 3 attempts",
                                        android.widget.Toast.LENGTH_LONG).show());

                        return;
                    }
                    try{
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored){
                        Log.e(TAG, "Sleep interrupted while retrying Bluetooth connection", ignored);
                    }
                }
            }
            if (isConnected){
                connected(mmSocket, mmDevice);
            }

        }
        public void cancel(){
            try{
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            }catch (IOException e){
                Log.e(TAG, "cancel: close() of Connect thread failed." + e.getMessage());
            }
        }
    }
    //starting the chat service
    //Specifically start AcceptThread to begin a session in listening (server) mode. Called by thE Activity onResume().

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized void start(){
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();

        }
    }
    //Accept thread starts and sits waiting for a connection.
    //Then connect thread starts and attempts to make a connection with the other devices AcceptThread.
    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");

        //init progress dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Connecting Bluetooth");
        builder.setMessage("Please wait...");
        builder.setCancelable(false);

        ProgressBar progressBar = new ProgressBar(mContext);
        builder.setView(progressBar);
        mProgressDialog = builder.create();
        mProgressDialog.show();

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    // creating the ConnectThread



    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: Starting");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            //dismiss the progress dialog when connection is established
            try {
                if (mProgressDialog != null && mProgressDialog.isShowing()){
                    mProgressDialog.dismiss();
                }
            }catch (Exception e){
                Log.e(TAG,"Connected Thread:Exception while dismissing dialog",e);

            }

            try{
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            }catch (IOException e){
                Log.e(TAG,"ConnectedThread: IOException while getting socket streams", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run(){
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes;  // bytes returned from read()

            //Keep listening to the InputStream until an exception occurs
            while(true){
                //Read from the InputStream
                try {
                    //Read from the input stream
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "ConnectedThread: Received - " + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);


                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading the inputStream." + e.getMessage());
                    break;
                }



            }
        }

        //call this from MainActivity to send data to the remote device
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: writing the output stream:" + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing the output stream." + e.getMessage());
            }

        }

        //call this from main activity to shutdown the connection
        public void cancel(){
            try {
                mmSocket.close();
            }catch (IOException e) {
                Log.e(TAG,"Error closing Bluetooth socket:"+ e.getMessage());
            }

        }
    }
    public void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
        Toast.makeText(mContext, "Bluetooth connected", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "connected: ConnectedThread started");
        Log.d(TAG, "connected : Starting");

        //cancel currently running thread
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //Start the thread to manage the connection and perform transmission
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
        //feedback to user
        Toast.makeText(mContext, "Bluetooth Connected", Toast.LENGTH_SHORT).show();
    }

    public void write(byte[] out){
        //create temporary object

        if (mConnectedThread!= null){
            mConnectedThread.write(out);
        }else{
            Log.e(TAG, "write: No connected thread. Message not sent.");
            Toast.makeText(mContext, "Not connected to any device", Toast.LENGTH_SHORT).show();
        }
    }
    public void stop() {
        Log.d(TAG, "stop: Stopping all Bluetooth threads.");

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

}




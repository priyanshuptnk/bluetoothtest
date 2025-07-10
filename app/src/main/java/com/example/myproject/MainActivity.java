package com.example.myproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String TAG ="Main Activity";
    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;

    BluetoothConnectionService mBluetoothConnection;

    Button btnStartConnection;

    Button btnSend;
    EditText etSend;
    BluetoothDevice mBTDevice;

    TextView incomingMessages;
    StringBuilder messages;
    public static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;




    public
    ListView lvNewDevices;



    //creating a broadcaster for action found
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // when discovery finds a device
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");    // AFTER WRITING THIS CODE 13 ERRORS WERE GONE
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1 : STATE TURNING ON");
                        break;

                }
            }
        }
    };

    // Broadcast receiver for Discoverability on and off
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    // device is in discover mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enable");
                        break;
                    // device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enable. Able to receive connections");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2 : Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2 : Connected.");
                        break;
                }
            }
        }
    };
    //BroadcastReceiver for listing devices that are not yet paired.
    //executed by btnDiscover() method.
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND");

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device != null){
                    mBTDevices.add(device);
                    Log.e(TAG, "onReceiver:" + device.getName()+ ":"+ device.getAddress());
                    mDeviceListAdapter = new DeviceListAdapter(context,R.layout.device_adapter_view, mBTDevices);
                    lvNewDevices.setAdapter(mDeviceListAdapter);
                }else{
                    Log.e(TAG, "onReceive: BluetoothDevice is null");
                }
            }

        }
    };

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                BluetoothDevice mDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice != null) {
                    //3 cases
                    //case1: bonded already
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.d(TAG, "BroadcastReceiver  :BOND_BONDED.");
                        //insert our bluetooth device
                        mBTDevice = mDevice;
                    }
                    // case2: creating a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Log.d(TAG, "BroadcastReceiver : BOND_BONDING.");
                    }
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                    }
                }else{
                    Log.e(TAG, "BroadcastReceiver: mDevice is null");
                }

            }
        }
    };

    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        //mBluetoothAdapter.cancelDiscovery
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnONOFF = findViewById(R.id.btnONOFF); // initializing the button
        btnEnableDisable_Discoverable = findViewById(R.id.btnDiscoverable_on_off);
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter(); // bluetooth adapter obj is initialized
        // OnClickLister : Lets us define what should happen when a button is clicked
        lvNewDevices = findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        //declare the button variables
        btnStartConnection = findViewById(R.id.btnStartConnection);
        btnSend = findViewById(R.id.btnSend);
        etSend = findViewById(R.id.editText);

        incomingMessages = findViewById(R.id.incomingMessage);
        messages = new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        //Broadcast when bond state changes (i.e. pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);
        lvNewDevices.setOnItemClickListener(MainActivity.this);


        

        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth");
                enableDisableBT();

            }


        });


        btnSend.setOnClickListener(v -> startConnection());

        btnSend.setOnClickListener(v -> {
            byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
            mBluetoothConnection.write(bytes);
            etSend.setText("");
        });


    }


    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            messages.append(text).append("\n");
            incomingMessages.setText(messages);
        }
    };

    //creating method for starting connections
    public void startConnection(){
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    //starting the chat service method
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection : Initializing RFCOMM BluetoothConnection.");
        mBluetoothConnection.startClient(device, uuid);
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public void enableDisableBT(){
            if (mBluetoothAdapter == null){
                Log.d(TAG, "enableDisableBT: Does not have bluetooth capabilities");
                return;
            }

            if(!mBluetoothAdapter.isEnabled()){
                Log.d(TAG, "enableDisableBT: enabling BT.");
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBTIntent);

                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(mBroadcastReceiver1, BTIntent);
            }
            if(mBluetoothAdapter.isEnabled()){
                Log.d(TAG, "enableDisableBT: disabling BT.");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){    // for android 13 and above guides to bluetooth settings
                    Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(this, "PLease turn off Bluetooth manually",
                            Toast.LENGTH_SHORT).show();
                }else {
                    mBluetoothAdapter.disable();   // for android 12 and below
                }


                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(mBroadcastReceiver1, BTIntent);

            }
        }

        //Creating our button onClick method for discoverability
        public void btnEnableDisable_Discoverable(View view){
            Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);

            IntentFilter intentFiler = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(mBroadcastReceiver2, intentFiler);

        }
        // creating a btn discover method

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        public void btnDiscover(View view){
            Log.d(TAG,"btnDiscover: Looking for unpaired devices");
            if (mBluetoothAdapter.isDiscovering()) {

                mBluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "btnDiscover: Cancelling discovery.");
                //check bt permission in manifest
                checkBTPermissions();

                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

                if (!mBluetoothAdapter.isDiscovering()){
                    //check BT permissions in manifest
                    checkBTPermissions();
                    mBluetoothAdapter.startDiscovery();
                    IntentFilter discoverDeviceIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mBroadcastReceiver3, discoverDeviceIntent);

                }

            }


            }


//Android must programmatically check the permissions for bluetooth. Putting the permissions in the manifest is not enough.
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_CORES_LOCATION");
            if(permissionCheck!=0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1000
                        );
            }
        }else {
            Log.d(TAG, "checkBTPermission: No need to check permissions. SDK version < LOLLIPOP");
        }

    }





    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first we cancel discovery because it is very memory intensive
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick  :You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName =" + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress =" + deviceAddress);

        //creating the bond
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with" + deviceName);
            mBTDevices.get(i).createBond();
            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }




    }


}


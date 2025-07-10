package com.example.myproject;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;

import androidx.annotation.RequiresPermission;

//import com.example.myproject.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private final LayoutInflater mLayoutInflater;
    private final ArrayList<BluetoothDevice> mDevices;
    private final int mViewResourceId;
    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {

        super(context, tvResourceId, devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public @NotNull View getView(int position, View convertView, @NotNull ViewGroup parent){
        if(convertView==null){
            convertView = mLayoutInflater.inflate(mViewResourceId, null);
        }


        BluetoothDevice device = mDevices.get(position);

        if(device !=null){
            TextView deviceName = convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = convertView.findViewById(R.id.tvDeviceAddress);

            if (deviceName!=null){
                deviceName.setText(device.getName());
            }
            if (deviceAddress!=null){
                deviceAddress.setText(device.getAddress());
            }



        }
        return convertView;
    }
}

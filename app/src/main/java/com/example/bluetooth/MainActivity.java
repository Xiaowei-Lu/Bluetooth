package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";
//    private static final java.util.UUID UUID = null;

    Button onoffButton;
    ListView listView;
    TextView status_textView;
    Button searchButton;
    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    ArrayList<String> bluetoothlist = new ArrayList<>();
    Set<String> addresses = new HashSet<>();
    ArrayAdapter arrayAdapter;

    BluetoothAdapter bluetoothAdapter;

//    String uuid = UUID.randomUUID().toString();

    protected void enableDisableBT(){
        // if no BT capabilities
        if(bluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities");
        }
        // if bluetooth is not enabled
        if(!bluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            registerReceiver(broadcastReceiver1, BTIntent);
        }
        // if bluetooth is enabled
        else if(bluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            bluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(broadcastReceiver1, BTIntent);
        }
    }

    public void searchClicked(View view){
        if(bluetoothAdapter.isEnabled()){
            status_textView.setText("Searching... ");
            searchButton.setEnabled(false); // restrict button from user for a moment
            bluetoothDevices.clear();
            addresses.clear();
            bluetoothAdapter.startDiscovery();
        }else{
            status_textView.setText("pls enable bluetooth first");
        }
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //first cancel discovery because its very memory intensive.
        bluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = bluetoothDevices.get(position).getName();
        String deviceAddress = bluetoothDevices.get(position).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            bluetoothDevices.get(position).createBond();
        }
    }

    protected final BroadcastReceiver broadcastReceiver1 =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch(state){
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "onReceive: STATE OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                    break;
            }
        }
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(broadcastReceiver1);
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(broadcastReceiver4);
    }

    protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("Action", action);

        if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
            status_textView.setText("Finished ");
            searchButton.setEnabled(true);
        }else if(BluetoothDevice.ACTION_FOUND.equals(action)){
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // any bluetooth device have this method
            String name = device.getName();
            String address = device.getAddress();
            String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)); // distance
            Log.i("Device Found","Name: " + name + "Address: " + address + "RSSI: " + rssi);
            // avoid duplicates
            if(!addresses.contains(address)){
                addresses.add(address);
                String deviceString = "";
                if(name == null || name.equals("")){
                    deviceString = address + " - RSSI " + rssi + "dBm";
                }else{
                    deviceString = name + " - RSSI " +  rssi + "dBm";
                }
                // add device to list bluetoothDevices
                bluetoothDevices.add(device);
                bluetoothlist.add(deviceString);
                arrayAdapter.notifyDataSetChanged();
            }
        }
        }
    };

    private final BroadcastReceiver broadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
            BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //3 cases:
            //case1: bonded already
            if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
            }
            //case2: creating a bond
            if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
            }
            //case3: breaking a bond
            if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
            }
        }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 1. on/off: initialize on/off button
        onoffButton = findViewById(R.id.onoffButton);

        // 1. on/off: set a listener
        onoffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        // 2.initiate basic buttons and views to discovery devices
        listView = findViewById(R.id.listView);
        status_textView = findViewById(R.id.status_textView);
        searchButton = findViewById(R.id.searchButton);

        arrayAdapter = new DeviceListAdapter(this, R.layout.deviceview, bluetoothDevices);

        listView.setAdapter(arrayAdapter);

        // filter for discovery
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver,intentFilter);

        // 3.Broadcasts when bond state changes (ie:pairing)

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver4, filter);

        listView.setOnItemClickListener(MainActivity.this);
    }
}
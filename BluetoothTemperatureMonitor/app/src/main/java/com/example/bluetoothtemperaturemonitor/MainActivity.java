package com.example.bluetoothtemperaturemonitor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_NOT_CONNECTED = 3;
    public static final int MessageReceived = 4;
    public static final int CONNECTION_LOST = 5;
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button Temperature,fetch;
    ProgressBar progressBar;
    Toolbar toolbar;
    TextView label;
    int REQUEST_CODE = 1;
    Handler handler;
    String name, address;
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devices;
    BluetoothSocket socket;
    List<String> bondedDevice = new ArrayList<>();
    List<String> bondedDeviceAddress = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    int sItems = com.google.android.material.R.layout.support_simple_spinner_dropdown_item;
    Spinner spinner;
    output obj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Temperature = findViewById(R.id.tmp);
        progressBar = findViewById(R.id.progressBar);
        Temperature.setText(R.string.connect);
        label = findViewById(R.id.label);
        toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        progressBar.setVisibility(View.INVISIBLE);
        label.setVisibility(View.INVISIBLE);
        fetch = findViewById(R.id.fetch);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        spinner = findViewById(R.id.spinner);

        handler = new Handler(msg -> {
            switch (msg.what) {
                case STATE_CONNECTING:
                    progressBar.setVisibility(View.VISIBLE);
                    Temperature.setText(R.string.connecting);
                    progressBar.setIndeterminate(true);
                    break;
                case STATE_CONNECTED:
                    Temperature.setText(R.string.connected);
                    progressBar.setIndeterminate(false);
                    break;
                case STATE_NOT_CONNECTED:
                    Toast.makeText(this, "Unable to connect with  device \n" +
                            "  Make sure device is on and discoverable \n" +
                            " Try to Disconnect Device and connect again from settings \n", Toast.LENGTH_LONG).show();
                    Temperature.setText(R.string.fail);
                    progressBar.setIndeterminate(false);
                    progressBar.setProgressDrawable(ContextCompat.getDrawable(this,R.drawable.fail_progress));
                    socket=null;
                    SystemClock.sleep(1000);
                    Temperature.setText(R.string.connect);
                    break;
                case MessageReceived:
                    //TODO
                    byte[] buffer = (byte[]) msg.obj;
                    String message = new String(buffer, 0, msg.arg1 - 1);
                    Temperature.setText(message);
                    label.setVisibility(View.VISIBLE);
                    String[] result = message.split("\\n");
                    if (!result[0].isEmpty() && result[0]!="")
                    {
                            try {
                                    int val = Integer.parseInt(result[0].replaceAll("[^0-9]", ""));
                                    if (val >= 35) {
                                        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.fail_progress));
                                    } else {
                                        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.green_progress));
                                    }
                                    progressBar.setProgress(val, true);
                                }

                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
            }
                    break;
                case CONNECTION_LOST:
                    Toast.makeText(this, "Connection Lost......", Toast.LENGTH_SHORT).show();
                    Temperature.setText(R.string.disconnect);
                    label.setVisibility(View.INVISIBLE);
                    socket=null;
                    SystemClock.sleep(500);
                    Temperature.setText(R.string.connect);
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    public void clicked(View view) {

        progressBar.setProgressDrawable(ContextCompat.getDrawable(this,R.drawable.progressbar));
        if (btAdapter != null) {
            if (socket!=null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                        socket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                if (btAdapter.isEnabled()) {
                    if (bondedDevice.size()>0) {
                        int index = 0;
                        for (String i : bondedDevice) {
                            index++;
                            if (!spinner.getSelectedItem().equals("Select Device.....")) {
                                if (spinner.getSelectedItem().equals(i)) {
                                    BluetoothDevice remoteDevice = btAdapter.getRemoteDevice(bondedDeviceAddress.get(index - 2));
                                    handler.obtainMessage(STATE_CONNECTING).sendToTarget();
                                    getSocket(remoteDevice);
                                }
                            }
                            else
                            {
                                Toast.makeText(this, "Please Select Device", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(this, "Please load Devices", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        Intent bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        checkPermission(Manifest.permission.BLUETOOTH, REQUEST_CODE);
                        startActivity(bt);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    getDevice();
                }
            }
        } else {
            Toast.makeText(this, " Your Device didn't have Bluetooth ", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadDevices(View view)
    {
        if (btAdapter!=null) {
            if (btAdapter.isEnabled()) {
                getDevice();
            }
            else {
                Toast.makeText(this, " Please turn on Bluetooth ", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, " Your Device didn't have Bluetooth ", Toast.LENGTH_SHORT).show();
        }
    }
    private void getDevice() {

        checkPermission(Manifest.permission.BLUETOOTH, REQUEST_CODE);
        devices = btAdapter.getBondedDevices();
        if (devices.size() > 0) {
            if (bondedDevice.isEmpty()) {
                bondedDevice.add("Select Device.....");
                for (BluetoothDevice device : devices) {
                    name = device.getName();
                    address = device.getAddress();
                    bondedDevice.add(name);
                    bondedDeviceAddress.add(address);
                }
                arrayAdapter = new ArrayAdapter<>(this, sItems, bondedDevice);
                spinner.setAdapter(arrayAdapter);
            }
        }
    }


    private void getSocket(@NonNull BluetoothDevice remoteDevice) {
        new Thread(() -> {
            try {
                checkPermission(Manifest.permission.BLUETOOTH, REQUEST_CODE);
                socket = remoteDevice.createRfcommSocketToServiceRecord(mUUID);
                socket.connect();
                handler.obtainMessage(STATE_CONNECTED).sendToTarget();
                SystemClock.sleep(1000);
                if (socket!=null) {
                    obj = new output(socket);
                    obj.start();
                }
                else
                {
                    handler.obtainMessage(STATE_NOT_CONNECTED).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(STATE_NOT_CONNECTED).sendToTarget();
            }
        }).start();
    }


    // Function to check and request permission.
    public void checkPermission(String bluetooth, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, bluetooth) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{bluetooth}, requestCode);
        }
    }

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(MainActivity.this, "Bluetooth Permission Needed to run app", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private class output extends Thread {
        private final InputStream stream;
        private InputStream tmp;

        private output(BluetoothSocket socket) {
            try {
                tmp = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stream = tmp;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            while (true) {
                try {
                    bytes = stream.read(buffer);
                    handler.obtainMessage(MessageReceived, bytes, -1, buffer).sendToTarget();
                  SystemClock.sleep(1000);

                } catch (IOException e) {
                    e.printStackTrace();
                    handler.obtainMessage(CONNECTION_LOST).sendToTarget();
                    return;
                }
            }
        }

    }

}

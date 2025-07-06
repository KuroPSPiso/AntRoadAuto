package com.example.antroadauto;


import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

public class BTHandling {
    public static MainActivity mainActivity;

    public final String TAG = "BLHandling";
    private BluetoothManager bluetoothManager;
    public AcceptThread acceptThread;
    public final BluetoothAdapter bluetoothAdapter;
    public BluetoothDevice bluetoothDevice;
    private final static int REQUEST_ENABLE_BT = 1;

    public enum BTHANDLING_TYPE {
        RECEIVER_SERVER,
        SENDER_CLIENT
    }

    public BTHandling(MainActivity argMainActivity, BTHANDLING_TYPE btType) {
        mainActivity = argMainActivity;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mainActivity, "No Bluetooth permissions", Toast.LENGTH_LONG).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT}, 1);
                } else {
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH}, 1);
                }
                return;
            }

            ActivityResultLauncher<Intent> activityLauncher = mainActivity.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult o) {
                            if (o.getResultCode() == RESULT_OK) {
                                Toast.makeText(mainActivity, "success bl", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(mainActivity, String.format("%s", o.getResultCode()), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );
            //Intent intent = new Intent(mainActivity, MainActivity.class);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityLauncher.launch(enableBtIntent);
        }

        if (btType == BTHANDLING_TYPE.RECEIVER_SERVER) {
            this.acceptThread = new AcceptThread(this);
            acceptThread.start();
        }
    }
}

package com.example.antroadauto.bt;

import static androidx.core.app.ActivityCompat.startActivityForResult;
import static com.example.antroadauto.MainActivity.mainActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.antroadauto.CONNECTION_STATUS;
import com.example.antroadauto.Common;
import com.example.antroadauto.MainActivity;
import com.example.antroadauto.R;

@SuppressLint("MissingPermission")
public class Client extends Thread {

    private BluetoothSocket socket;
    private final BluetoothDevice serverDevice;

    public Client( BluetoothDevice selectedServer) {
        this.serverDevice = selectedServer;
        try {
            MainActivity.setConnectionStatus(CONNECTION_STATUS.CONNECTING);
            this.socket = this.serverDevice.createRfcommSocketToServiceRecord(Common.MY_UUID);
        } catch (Exception ex) {
            //
            MainActivity.setConnectionStatus(CONNECTION_STATUS.FAILED);
        }
    }

    @Override
    public void run() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                }
            }

            if (!Common.BA.isEnabled()) {
                Toast.makeText(mainActivity, "Bluetooth is not enabled", Toast.LENGTH_LONG).show();
                return;
            }

            mainActivity.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   final Button btnReceive = mainActivity.findViewById(R.id.btnReceive);
                   btnReceive.setEnabled(false);
                   final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                   btnShare.setText("Pending");
                   final Spinner spDevices = mainActivity.findViewById(R.id.spDevices);
                   spDevices.setEnabled(false);
               }
            });

            this.socket.connect();
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.setConnectionStatus(CONNECTION_STATUS.CONNECTED);
                    final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                    btnShare.setText("Disconnect from " + socket.getRemoteDevice().getName());
                }
            });
        } catch (Exception ex) {
            //
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.setConnectionStatus(CONNECTION_STATUS.FAILED);
                    final Button btnReceive = mainActivity.findViewById(R.id.btnReceive);
                    btnReceive.setEnabled(true);
                    final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                    btnShare.setText("Share");
                    final Spinner spDevices = mainActivity.findViewById(R.id.spDevices);
                    spDevices.setEnabled(true);
                }
            });
            Toast.makeText(mainActivity, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void interrupt() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.setConnectionStatus(CONNECTION_STATUS.STOPPING);
            }
        });

        super.interrupt();

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.setConnectionStatus(CONNECTION_STATUS.DISCONNECTED);
                final Button btnReceive = mainActivity.findViewById(R.id.btnReceive);
                btnReceive.setEnabled(true);
                final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                btnShare.setText("Share");
                final Spinner spDevices = mainActivity.findViewById(R.id.spDevices);
                spDevices.setEnabled(true);
            }
        });
    }

    public void sendLocationMessage(Location loc){

    }
}

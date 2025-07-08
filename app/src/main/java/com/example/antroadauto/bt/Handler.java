package com.example.antroadauto.bt;

import static com.example.antroadauto.MainActivity.mainActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

import com.example.antroadauto.CONNECTION_STATUS;
import com.example.antroadauto.Common;
import com.example.antroadauto.MainActivity;
import com.example.antroadauto.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Handler {

    private BluetoothDevice[] pairedDevices;

    private boolean isServer;
    private boolean isInitialised;

    private Server server;

    private Client client;

    public Handler(MainActivity mainActivity) {
        if(!Common.BA.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.activityResultLauncher.launch(enableBtIntent);
        } else {
            initBT();
        }
    }

    public void startServer() {
        if(server != null) {
            if(!server.isInterrupted()){
                //stop server
                server.interrupt();
                return;
            }

        }
        server = new Server();
        server.start();
    }

    public void startClient(int deviceIndex) {
        if(client != null) {
            if(!client.isInterrupted()){
                //stop server
                client.interrupt();
                return;
            }
        }

        client = new Client(pairedDevices[deviceIndex]);
        client.start();
    }

    @SuppressLint("MissingPermission")
    public void initBT() {
        if(isInitialised) return;

        Thread checkAndExecute = new Thread(new Runnable() {
            @Override
            public void run() {
                Set<BluetoothDevice> bondedDevicesSet = Common.BA.getBondedDevices();
                pairedDevices = new BluetoothDevice[bondedDevicesSet.size()];
                pairedDevices = bondedDevicesSet.toArray(pairedDevices);

                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> sArrBT = new ArrayList<String>();
                        sArrBT.add("NONE");
                        for(BluetoothDevice bt : pairedDevices) {
                            try {
                                sArrBT.add(bt.getName() != null ? bt.getName() : bt.getAddress());
                            } catch (Exception ex) {
                                sArrBT.add(bt.getAddress());
                            }

                            try {
                                ArrayAdapter<String> aaDevices = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_item, sArrBT);
                                aaDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                final Spinner spDevices = mainActivity.findViewById(R.id.spDevices);
                                spDevices.setAdapter(aaDevices);
                            }
                            catch (Exception ex){
                                final TextView tvConStatus = mainActivity.findViewById(R.id.tvConStatus);
                                tvConStatus.setText(ex.getMessage());
                            }
                        }

                        isInitialised = true;
                    }
                });
            }
        });

        checkAndExecute.start();
    }
}

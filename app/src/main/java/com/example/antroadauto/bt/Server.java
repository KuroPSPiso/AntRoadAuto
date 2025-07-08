package com.example.antroadauto.bt;

import static com.example.antroadauto.MainActivity.mainActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Message;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.antroadauto.CONNECTION_STATUS;
import com.example.antroadauto.Common;
import com.example.antroadauto.MainActivity;
import com.example.antroadauto.R;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;

@SuppressLint("MissingPermission")
public class Server extends Thread {

    private BluetoothServerSocket serverSocket;
    private static Button btnReference;

    public Server() {
        try {
            serverSocket = Common.BA.listenUsingInsecureRfcommWithServiceRecord(Common.ServiceRecord, Common.MY_UUID);
        } catch (IOException e) {
            //
        }
    }

    @Override
    public void run() {
        BluetoothSocket socket = null;

        while (socket == null) {
            try {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.setConnectionStatus(CONNECTION_STATUS.AWAITING_CONNECTION);
                        final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                        btnShare.setEnabled(false);
                        final Button btnReceive = mainActivity.findViewById(R.id.btnReceive);
                        //btnReference = new Button(mainActivity);
                        //btnReference.setBackground(btnReceive.getBackground());
                        btnReceive.setText("STOP");

                        //btnReceive.setBackgroundColor(Color.RED);
                    }
                });
                socket = serverSocket.accept();
            } catch (Exception ex) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.setConnectionStatus(CONNECTION_STATUS.FAILED);
                        final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                        btnShare.setEnabled(true);
                        final Button btnReceive = mainActivity.findViewById(R.id.btnReceive);
                        //btnReceive.setBackground(btnReference.getBackground());
                        btnReceive.setText("Receive");
                    }
                });
                break;
            }

            if(socket != null) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.setConnectionStatus(CONNECTION_STATUS.HOSTING);
                    }
                });
            }
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
                final Button btnShare = mainActivity.findViewById(R.id.btnShare);
                btnShare.setEnabled(true);
                final Button btnReceive = mainActivity.findViewById(R.id.btnReceive);
                //btnReceive.setBackground(btnReference.getBackground());
                btnReceive.setText("Receive");
            }
        });
    }
}

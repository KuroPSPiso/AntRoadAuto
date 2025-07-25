package com.example.antroadauto;

import android.Manifest;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Binder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.util.UUID;

public class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private final BTHandling btHandler;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public AcceptThread(BTHandling argBTHandler) {
        btHandler = argBTHandler;
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            //btHandler.bluetoothDevice.getName()
            tmp = btHandler.bluetoothAdapter.listenUsingRfcommWithServiceRecord("LocShare", MY_UUID);
            Toast.makeText(BTHandling.mainActivity, "Ready to Receive", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(BTHandling.mainActivity, "Socket's listen() method failed:" + e.getMessage(), Toast.LENGTH_LONG).show();
            //Log.e(btHandler.TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (Exception e) {
                Toast.makeText(BTHandling.mainActivity,"Socket's accept() method failed", Toast.LENGTH_SHORT).show();
                //Toast.makeText(BTHandling.mainActivity,e.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                //Toast.makeText(BTHandling.mainActivity,"Connection accepted:" + socket.getRemoteDevice().getName(), Toast.LENGTH_SHORT).show();
                //manageMyConnectedSocket(socket);
                //mmServerSocket.close();
                break;
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            if(mmServerSocket != null) {
                mmServerSocket.close();
            }
        } catch (Exception e) {
            Toast.makeText(BTHandling.mainActivity,"Could not close the connect socket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

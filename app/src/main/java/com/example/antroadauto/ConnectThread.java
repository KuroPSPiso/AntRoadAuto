package com.example.antroadauto;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import java.io.IOException;

class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice btDevice;

    private final BTHandling btHandler;

    public ConnectThread(BTHandling argBTHandler, BluetoothDevice argBTDevice) {
        this.btHandler = argBTHandler;
        this.btDevice = argBTDevice;
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;

        if(this.btDevice == null) {
            this.btHandler.mainActivity.LogMessage("missing bt device");
            mmSocket = null;
            return;
        }

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = this.btDevice.createRfcommSocketToServiceRecord(Common.MY_UUID);
        } catch (Exception e) {
            this.btHandler.mainActivity.LogMessage(e.getMessage());
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        //Common.BA.cancelDiscovery();
        this.btHandler.mainActivity.LogMessage("attempting connection");
        if(mmSocket == null) {
            this.btHandler.mainActivity.LogMessage("connection socket is not set");
            return;
        }

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Toast.makeText(this.btHandler.mainActivity,"Could not close the client socket: " + connectException.getMessage(), Toast.LENGTH_LONG).show();
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                this.btHandler.mainActivity.LogMessage("Could not close the client socket: " + closeException.getMessage());
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        try {
            this.btHandler.mainActivity.LogMessage("(try) connected");
            //this.btHandler.mainActivity.LogMessage("connected to:" + mmSocket.getRemoteDevice().getName());
        } catch (Exception ex) {
            this.btHandler.mainActivity.LogMessage("(try) connected");
        }
        //manageMyConnectedSocket(mmSocket);
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Toast.makeText(this.btHandler.mainActivity, "Could not close the client socket:" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

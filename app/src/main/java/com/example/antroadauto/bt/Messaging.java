package com.example.antroadauto.bt;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Messaging extends Thread {

    private final BluetoothSocket socketCon;
    private InputStream is;
    private OutputStream os;

    public Messaging(BluetoothSocket argSocketCon){
        super();

        socketCon = argSocketCon;
        try {
            is = socketCon.getInputStream();
            os = socketCon.getOutputStream();
        } catch (IOException e) {
            //
        }
    }

    @Override
    public void run() {
        while (this.isAlive()) {
            //CRC for protocol and message len
            //[in/out 2 ][len 4][data x][cksm? 2]
            //00
        }
    }
}

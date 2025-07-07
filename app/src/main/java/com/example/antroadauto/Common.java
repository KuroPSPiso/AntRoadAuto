package com.example.antroadauto;

import android.bluetooth.BluetoothAdapter;

import java.util.UUID;

public class Common {

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String ServiceRecord = "AntRoadAuto";
    public static final BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();
}

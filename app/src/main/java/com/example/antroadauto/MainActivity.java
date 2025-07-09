package com.example.antroadauto;

import static android.location.LocationManager.GPS_PROVIDER;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.antroadauto.bt.Handler;
import com.example.antroadauto.loc.LocationHandler;
import com.example.antroadauto.monitor.Camera2Activity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "com.LocShare.Notifications";
    private static final String TAG = "MyBackgroundService";

    private static CONNECTION_STATUS connectionStatus;
    public static Activity mainActivity;

    private static Handler btHandler;
    private static LocationHandler locHandler;
    private static Camera2Activity cameraStream;


    public ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Bluetooth has been enabled
                    Toast.makeText(mainActivity, "Bluetooth booting", Toast.LENGTH_LONG).show();
                    btHandler.initBT();
                } else {
                    // Bluetooth was not enabled
                    Toast.makeText(mainActivity, "Bluetooth not allowed", Toast.LENGTH_LONG).show();
                }
            }
        }
    );

    private static String generateConStatus(){
        return connectionStatus.toString();
    }

    /// Set connection status based on @CONNECTION_STATUS
    public static void conStatusMessage() {
        conStatusMessage(generateConStatus());
    }

    /// Set connection status based on CUSTOM_MESSAGE
    public static void conStatusMessage(String warn){
        final TextView tvConStatus = mainActivity.findViewById(R.id.tvConStatus);
        tvConStatus.setText(warn);
    }

    public static void setConnectionStatus(CONNECTION_STATUS conStatus) {
        connectionStatus = conStatus;
        conStatusMessage();
    }

    private void RequestPermissions() {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
            }
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, // Add BLUETOOTH_SCAN if you also plan to discover
                        1);
            }
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.CAMERA}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.FOREGROUND_SERVICE_CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.FOREGROUND_SERVICE_CAMERA}, 1);
            }
        }

        //Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(turnOn, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainActivity = this;
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();
        RequestPermissions();
        btHandler = new Handler(this);
    }

    protected void initUI() {
        final Button btnShare = findViewById(R.id.btnShare);
        final Button btnReceive = findViewById(R.id.btnReceive);
        final TextView tvConStatus = findViewById(R.id.tvConStatus);
        connectionStatus = CONNECTION_STATUS.DISCONNECTED;
        conStatusMessage();

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartClient();
                initLocation(true);
            }
        });

        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartServer();
            }
        });
    }

    protected static void StartServer() {
        btHandler.startServer();
        cameraStream = new Camera2Activity();
        cameraStream.mTextureView = mainActivity.findViewById(R.id.textureView);

        // Initialize CameraManager
        cameraStream.mCameraManager = (CameraManager) mainActivity.getSystemService(Context.CAMERA_SERVICE);
        if (cameraStream.mCameraManager == null) {
            Toast.makeText(mainActivity, "Camera services not available.", Toast.LENGTH_SHORT).show();
            cameraStream.finish();
            return;
        }
        cameraStream.openCamera();
    }

    protected static void StartClient() {
        final Spinner spDevices = mainActivity.findViewById(R.id.spDevices);
        if(spDevices.getSelectedItemPosition() == 0) {
            Toast.makeText(mainActivity,"Please first select a device below", Toast.LENGTH_LONG).show();

            GradientDrawable borderDrawable = new GradientDrawable();
            borderDrawable.setShape(GradientDrawable.RECTANGLE);
            borderDrawable.setColor(Color.TRANSPARENT);
            borderDrawable.setStroke(3, Color.YELLOW);
            borderDrawable.setCornerRadius(8f * mainActivity.getResources().getDisplayMetrics().density);
            spDevices.setBackground(borderDrawable);
            return;
        } else {
            final Spinner emptySpinner = new Spinner(mainActivity);
            spDevices.setBackground(emptySpinner.getBackground());
        }

        btHandler.startClient(spDevices.getSelectedItemPosition() - 1);
    }

    protected void initLocation(boolean isGPSOwner) {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //no rights
            return;
        }

        if(locHandler == null) locHandler = new LocationHandler();

    }

    public static void writeLocText(double lat, double lon)
    {
        final TextView tvLat = mainActivity.findViewById(R.id.tvLat);
        final TextView tvLon = mainActivity.findViewById(R.id.tvLon);

        tvLat.setText(String.format("%s", lat));
        tvLon.setText(String.format("%s", lon));
    }




}
package com.example.antroadauto;

import static android.location.LocationManager.GPS_PROVIDER;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "com.LocShare.Notifications";
    private static final String TAG = "MyBackgroundService";

    private enum CONNECTION_STATUS {
        DISCONNECTED,
        CONNECTED,
        CONNECTING,
        HOSTING,
        FAILED,
        AWAITING_CONNECTION,
        STOPPING
    };

    private static CONNECTION_STATUS connectionStatus;
    private static LocationManager locationManager;
    private static Location currLoc;
    protected static Activity mainActivity;
    private static BluetoothDevice[] pairedDevices;
    private static BTHandling btHandler;

    private static final double home_lat = 52.10144435;
    private static final double home_lon = 5.04532337;
//    private static final TimerTask timer = new TimerTask() {
//        @Override
//        public void run() {
//            receiveLocationData();
//        }
//    };

    public void LogMessage(String warn){
        final TextView tvConStatus = findViewById(R.id.tvConStatus);
        tvConStatus.setText(warn);
    }

    private void RequestPermissions()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
            }
        }
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainActivity = this;

        RequestPermissions();

        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnOn, 0);
        Set<BluetoothDevice> bondedDevicesSet = Common.BA.getBondedDevices();
        pairedDevices = new BluetoothDevice[bondedDevicesSet.size()];
        pairedDevices = bondedDevicesSet.toArray(pairedDevices);

        List<String> sArrBT = new ArrayList<String>();
        sArrBT.add("NONE");
        for(BluetoothDevice bt : pairedDevices) {
            sArrBT.add(bt.getName());
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        createNotificationChannel();

        final Button btnShare = findViewById(R.id.btnShare);
        final Button btnReceive = findViewById(R.id.btnReceive);
        final TextView tvConStatus = findViewById(R.id.tvConStatus);
        connectionStatus = CONNECTION_STATUS.DISCONNECTED;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        final Spinner spDevices = findViewById(R.id.spDevices);

        try {
            ArrayAdapter<String> aaDevices = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sArrBT);
            aaDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spDevices.setAdapter(aaDevices);
        }
        catch (Exception ex){
            tvConStatus.setText(ex.getMessage());
        }

        sendNotification("LocSharer",
                String.format("Location active %s",locationManager.isLocationEnabled()));

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spDevices.getSelectedItemPosition() == 0) {
                    Toast warnNoDevice = Toast.makeText(mainActivity, "No Device Selected", Toast.LENGTH_LONG);
                    try {
                        View view = warnNoDevice.getView();
                        view.getBackground().setColorFilter(0xfff2d985, PorterDuff.Mode.SRC_IN);
                        TextView text = (TextView) view.findViewById(android.R.id.message);
                        text.setTextColor(Color.parseColor("#e09d31"));
                        warnNoDevice.show();
                    } catch (Exception ex) {
                        //dunno
                        Toast.makeText(mainActivity, "No Device Selected", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                connectionStatus = CONNECTION_STATUS.CONNECTING;
                tvConStatus.setText(generateConStatus());

                if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                        //&& ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        ) {
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                    }
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH}, 1);
                    connectionStatus = CONNECTION_STATUS.FAILED;
                    tvConStatus.setText(generateConStatus());
                    return;
                }
                btHandler = new BTHandling((MainActivity) mainActivity, BTHandling.BTHANDLING_TYPE.SENDER_CLIENT, pairedDevices[spDevices.getSelectedItemPosition()]);

                getCurrLocationData();

                btnShare.setBackgroundColor(0xFFFF0000);
                btnReceive.setEnabled(false);
            }
        });

        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btHandler != null) {
                    if(btHandler.acceptThread != null){
                        connectionStatus = CONNECTION_STATUS.STOPPING;
                        tvConStatus.setText(generateConStatus());
                        btHandler.acceptThread.interrupt();
                        btHandler.acceptThread.cancel();
                        try {
                            Thread.sleep(300);
                            connectionStatus = CONNECTION_STATUS.DISCONNECTED;
                            tvConStatus.setText(generateConStatus());
                        } catch (InterruptedException e) {
                            //throw new RuntimeException(e);
                            connectionStatus = CONNECTION_STATUS.FAILED;
                            tvConStatus.setText(generateConStatus());
                        }
                    }
                }

                connectionStatus = CONNECTION_STATUS.CONNECTING;
                tvConStatus.setText(generateConStatus());
                if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                ) {ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                    }
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.BLUETOOTH}, 1);
                    connectionStatus = CONNECTION_STATUS.FAILED;
                    tvConStatus.setText(generateConStatus());
                    return;
                }

                connectionStatus = CONNECTION_STATUS.AWAITING_CONNECTION;
                tvConStatus.setText(generateConStatus());

                btHandler = new BTHandling((MainActivity) mainActivity, BTHandling.BTHANDLING_TYPE.RECEIVER_SERVER);
                receiveLocationData();
                btnShare.setEnabled(false);
                btnReceive.setBackgroundColor(0xFFFF0000);
                //TODO: DISABLE RECEIVE
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(String title, String message)
    {
        //RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.class);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //.setSmallIcon(R.drawable.) // Notification icon
                //.setContent(contentView) // Custom notification content
                //.setContentTitle("Hello") // Title displayed in the notification
                //.setContentText("Welcome to GeeksforGeeks!!") // Text displayed in the notification
                //.setContentIntent(pendingIntent) // Pending intent triggered when tapped
                .setAutoCancel(true); // Dismiss notification when tapped
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        //notificationManager.notify(1234, builder.build());
    }

    private void writeLocText(double lat, double lon)
    {
        final TextView tvLat = findViewById(R.id.tvLat);
        final TextView tvLon = findViewById(R.id.tvLon);

        tvLat.setText(String.format("%s", lat));
        tvLon.setText(String.format("%s", lon));
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION})//, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void getCurrLocationData(){
        //LocationListener (notification on device loc change)
        try {
            currLoc = locationManager.getLastKnownLocation(GPS_PROVIDER);

            if(currLoc != null) {
                writeLocText(currLoc.getLatitude(), currLoc.getLongitude());
            } else {
                final TextView tvLat = findViewById(R.id.tvLat);
                tvLat.setText("NO GPS DETECTED!");
            }
        } catch (Exception ex)
        {
            final TextView tvLat = findViewById(R.id.tvLat);
            tvLat.setText(ex.getMessage());
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void receiveLocationData()
    {
        Location loc = new Location(GPS_PROVIDER);
        loc.setLatitude(home_lat);
        loc.setLongitude(home_lon);
        loc.setSpeed(60);
        loc.setAccuracy(Criteria.ACCURACY_HIGH);
        loc.setBearing(0);
        loc.setAltitude(0);
        loc.setTime(System.currentTimeMillis());
        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        writeLocText(loc.getLatitude(), loc.getLongitude());

        String providers = "";
        try {
            if(locationManager.getProvider(GPS_PROVIDER) == null) {
                locationManager.addTestProvider(GPS_PROVIDER, false, false, false, false, false, false, false, 0, 1);
            }

            //locationManager.setTestProviderStatus(GPS_PROVIDER, 1, null, 1);
            locationManager.setTestProviderEnabled(GPS_PROVIDER, true);
            //locationManager.setTestProviderLocation(GPS_PROVIDER, loc);
            locationManager.setTestProviderStatus(GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
//            locationManager.requestLocationUpdates(GPS_PROVIDER, 50, 0, new LocationListener() {
//
//                @Override
//                public void onLocationChanged(@NonNull Location location) {
//                    Location loc = new Location(GPS_PROVIDER);
//                    loc.setLatitude(home_lat);
//                    loc.setLongitude(home_lon + new Random(10000).nextDouble()/100000);
//                    loc.setSpeed(60);
//                    loc.setAccuracy(Criteria.ACCURACY_HIGH);
//                    loc.setBearing(0);
//                    loc.setAltitude(0);
//                    loc.setTime(System.currentTimeMillis());
//                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
//                    locationManager.setTestProviderLocation(GPS_PROVIDER, loc);
//                    writeLocText(loc.getLatitude(), loc.getLongitude());
//                }
//            });
            locationManager.setTestProviderLocation(GPS_PROVIDER, loc);
            Timer timer = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - scheduledExecutionTime() >=
                            2000) {
                        return;
                    }
                    Location loc = new Location(GPS_PROVIDER);
                    loc.setLatitude(home_lat);
                    loc.setLongitude(home_lon + new Random(10000).nextDouble()/100000);
                    loc.setSpeed(60);
                    loc.setAccuracy(Criteria.ACCURACY_HIGH);
                    loc.setBearing(0);
                    loc.setAltitude(0);
                    loc.setTime(System.currentTimeMillis());
                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    locationManager.setTestProviderLocation(GPS_PROVIDER, loc);

                }
            };
            timer.schedule(tt, 1000, 1000);
        } catch (Exception ex)
        {
            final TextView tvConStatus = findViewById(R.id.tvConStatus);
            tvConStatus.setText(ex.getMessage());
        }
//        try {
//            locationManager.addTestProvider(GPS_PROVIDER, false, false, false, false, false, true, true, 0, 5);
//        } catch (IllegalArgumentException ignored){}
        //locationManager.setTestProviderEnabled(GPS_PROVIDER, true);
        //locationManager.setTestProviderLocation(GPS_PROVIDER, loc);
    }

    private String generateConStatus(){
        return connectionStatus.toString();
    }
}
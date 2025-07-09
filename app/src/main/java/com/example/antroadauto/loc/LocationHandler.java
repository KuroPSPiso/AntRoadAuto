package com.example.antroadauto.loc;

import static android.location.LocationManager.GPS_PROVIDER;

import static com.example.antroadauto.MainActivity.mainActivity;
import static com.example.antroadauto.MainActivity.writeLocText;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.example.antroadauto.MainActivity;
import com.example.antroadauto.R;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class LocationHandler {

    private static LocationManager locationManager;
    private static Location currLoc;

    LocationListener locGPSListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currLoc = location;
            mainActivity.runOnUiThread(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    getCurrLocationData();
                    Toast.makeText(mainActivity, "ping gps", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    LocationListener locNETListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currLoc = location;
            mainActivity.runOnUiThread(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    getCurrLocationData();
                    Toast.makeText(mainActivity, "ping net", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private static final double test_home_lat = 52.10144435;
    private static final double test_home_lon = 5.04532337;
    @SuppressLint("MissingPermission")
    public LocationHandler (){
        locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        currLoc = null;

        if(locationManager.isLocationEnabled()){
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0f,
                    locNETListener);
            locationManager.requestLocationUpdates(
                GPS_PROVIDER,
                2000,
                10f,
                locGPSListener
            );
        } else {
            Toast.makeText(mainActivity, "GPS not active", Toast.LENGTH_LONG).show();
        }

        //mainActivity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION})//, Manifest.permission.ACCESS_COARSE_LOCATION})
    private static void getCurrLocationData(){
        //LocationListener (notification on device loc change)
        try {
            //currLoc = locationManager.getLastKnownLocation(GPS_PROVIDER);


            if(currLoc != null) {
                MainActivity.writeLocText(currLoc.getLatitude(), currLoc.getLongitude());
            } else {
                final TextView tvLat = mainActivity.findViewById(R.id.tvLat);
                tvLat.setText("NO GPS DETECTED!");
            }
        } catch (Exception ex)
        {
            final TextView tvLat = mainActivity.findViewById(R.id.tvLat);
            tvLat.setText(ex.getMessage());
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void receiveLocationData()
    {
        Location loc = new Location(GPS_PROVIDER);
        loc.setLatitude(test_home_lat);
        loc.setLongitude(test_home_lon);
        loc.setSpeed(60);
        loc.setAccuracy(Criteria.ACCURACY_HIGH);
        loc.setBearing(0);
        loc.setAltitude(0);
        loc.setTime(System.currentTimeMillis());
        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        MainActivity.writeLocText(loc.getLatitude(), loc.getLongitude());

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
                    loc.setLatitude(test_home_lat);
                    loc.setLongitude(test_home_lon + new Random(10000).nextDouble()/100000);
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
            final TextView tvConStatus = mainActivity.findViewById(R.id.tvConStatus);
            tvConStatus.setText(ex.getMessage());
        }
//        try {
//            locationManager.addTestProvider(GPS_PROVIDER, false, false, false, false, false, true, true, 0, 5);
//        } catch (IllegalArgumentException ignored){}
        //locationManager.setTestProviderEnabled(GPS_PROVIDER, true);
        //locationManager.setTestProviderLocation(GPS_PROVIDER, loc);
    }
}

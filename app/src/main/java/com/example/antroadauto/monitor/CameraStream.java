package com.example.antroadauto.monitor;

import static com.example.antroadauto.MainActivity.mainActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.antroadauto.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class CameraStream {

    final MediaRecorder recorder;
    public Camera cam;

    public boolean isReady;

    /** Check if this device has a camera */
    private boolean checkCameraHardware() {
        if (mainActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public CameraStream()
    {
        recorder = new MediaRecorder();
        isReady = false;
        cam = null;


        if(!checkCameraHardware()) return;

        cam = Camera.open();
        cam.unlock();

        try {
//            recorder.setCamera(cam);
//            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            Date currentTime = Calendar.getInstance().getTime(); // current time
//            String curTimeStr = currentTime.toString().replace(" ", "_");
//            recorder.setOutputFile(String.format(mainActivity.getExternalMediaDirs()[0].getAbsolutePath() + "/%s.mp4", curTimeStr));
            try {
                //recorder.prepare();
                isReady = true;
            } catch (Exception e) {
                Toast.makeText(mainActivity, e.getMessage(), Toast.LENGTH_LONG).show();
                isReady = false;
            }
        } catch (Exception ex) {
            //no cam
            cam = null;
            isReady = false;
            Toast.makeText(mainActivity, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void Start(){
        if(!isReady) return;
        recorder.start();
    }

}

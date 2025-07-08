package com.example.antroadauto.monitor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.antroadauto.R;

import java.util.Arrays;

public class Camera2Activity extends AppCompatActivity {

    private static final String TAG = "Camera2Activity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    public TextureView mTextureView;
    public CameraManager mCameraManager;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Size mPreviewSize;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    // 1. TextureView Listener to get a SurfaceTexture
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                    Log.d(TAG, "onSurfaceTextureAvailable");
                    openCamera(); // Open camera when TextureView is ready
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                    // Handle size changes if necessary, e.g., reconfigure preview
                    Log.d(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                    Log.d(TAG, "onSurfaceTextureDestroyed");
                    return true; // Return true to destroy the SurfaceTexture
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                    // Called every time the SurfaceTexture is updated
                }
            };

    // 2. CameraDevice.StateCallback to get the CameraDevice
    private final CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "CameraDevice onOpened");
                    mCameraDevice = cameraDevice;
                    createCameraPreviewSession(); // Proceed to create preview session
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "CameraDevice onDisconnected");
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    Log.e(TAG, "CameraDevice onError: " + error);
                    cameraDevice.close();
                    mCameraDevice = null;
                    finish(); // Close activity on critical error
                }

                @Override
                public void onClosed(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "CameraDevice onClosed");
                    // Can perform cleanup specific to camera closure here
                }
            };

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera2); // Assuming you have a TextureView with id @+id/textureView
//
//        mTextureView = findViewById(R.id.textureView);
//
//        // Initialize CameraManager
//        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        if (mCameraManager == null) {
//            Toast.makeText(this, "Camera services not available.", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        // Check and request permissions
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA},
//                    REQUEST_CAMERA_PERMISSION);
//        }
//        // else: Permissions already granted, no need to do anything here, will open in onResume or onSurfaceTextureAvailable
//    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread(); // Start background thread for camera ops

        // When the screen is turned off and turned back on, the SurfaceTexture is already available,
        // and "onSurfaceTextureAvailable" will not be called. In that case, we can open a camera here.
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("MissingPermission") // Permissions are handled in onCreate/onRequestPermissionsResult
    public void openCamera() {
        try {
            // Find a suitable camera (e.g., back camera)
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    break;
                }
            }

            if (mCameraId == null) {
                Toast.makeText(this, "No back camera found.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Get supported preview sizes and choose one
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                Toast.makeText(this, "Cannot get StreamConfigurationMap.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    mTextureView.getWidth(), mTextureView.getHeight());

            // Set the TextureView's buffer size to the chosen preview size
            mTextureView.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // THIS IS THE CALL TO OPEN THE CAMERA
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            // This usually means permissions were not granted
            e.printStackTrace();
            Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeCamera() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        // Don't close MediaRecorder here, as this example doesn't include it.
        // If you had MediaRecorder, you'd stop and release it here.
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // This is the output Surface for the preview.
            Surface previewSurface = new Surface(texture);

            // Create a CaptureSession
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "CameraCaptureSession onConfigured");
                            // The camera is already closed
                            if (mCameraDevice == null) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                mPreviewRequestBuilder.addTarget(previewSurface);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Finally, we start displaying the camera preview.
                                mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                                        null, mBackgroundHandler); // No capture callback needed for simple preview

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Failed to start camera preview: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "CameraCaptureSession onConfigureFailed");
                            Toast.makeText(Camera2Activity.this, "Failed to configure camera.", Toast.LENGTH_SHORT).show();
                        }
                    }, mBackgroundHandler); // Use background handler for session callbacks

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Helper to choose an optimal preview size (simplified)
    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        // and have the same aspect ratio as our TextureView.
        // For simplicity, this example just picks the first available size.
        // In a real app, you'd want more robust logic to pick the best fit.
        for (Size option : choices) {
            if (option.getWidth() == textureViewWidth && option.getHeight() == textureViewHeight) {
                return option;
            }
        }
        // If no perfect match, return the first one or implement more sophisticated logic
        return choices[0];
    }


    // Make sure your layout has a TextureView with this ID:
    // res/layout/activity_camera2.xml
    /*
    <?xml version="1.0" encoding="utf-8"?>
    <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".Camera2Activity">

        <TextureView
            android:id="@+id/textureView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </FrameLayout>
    */
}
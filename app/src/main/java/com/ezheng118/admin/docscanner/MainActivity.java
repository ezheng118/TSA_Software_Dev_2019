package com.ezheng118.admin.docscanner;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;

import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(checkCameraHardware(this)){
            mCamera = getCameraInstance();
            Log.d("CAMERA", "Camera opened");
        }
        else{
            Log.d("CAMERA", "Failed to get camera");
        }

        if(mCamera == null){
            Log.d("CAMERA", "Camera doesn't work");
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera, this);

        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        Button captureButton = findViewById(R.id.take_picture_button);
        captureButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mCamera.takePicture(null, null, mPicture);
            }
        });

        Button importButton = findViewById(R.id.button_import);
        importButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                importImage(v);
            }
        });

        Log.d("LAYOUT DEBUGGING", "Successfully added buttons and camera preview");
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            Log.d("DEBUGGING", "internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
        else
        {
            Log.d("DEBUGGING", "OpenCV library found inside package");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status){
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OPENCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private Camera.PictureCallback mPicture = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera){
            Log.d("CAMERA DEBUGGING", "onPictureTaken: Start taking picture");

        }
    };

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    public void importImage(View view){
        Intent importIntent = new Intent(this, ImportImageActivity.class);
        startActivity(importIntent);
    }

    public void setCameraDisplayOrientation(){

        Camera.CameraInfo info = new Camera.CameraInfo();

        //cameraId of 1 indicates the forward facing camera
        Camera.getCameraInfo(1, info);

        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch(rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }

        int result;
        result = (info.orientation + degrees) % 360;
        //compensate for mirror
        result = (360 - result) % 360;


        mCamera.setDisplayOrientation(result);
    }
}

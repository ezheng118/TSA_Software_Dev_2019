package com.ezheng118.admin.docscanner;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;

import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
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

        Camera.Parameters mParameters = mCamera.getParameters();
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(mParameters);

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
    public void onPause() {
        super.onPause();
        try {
            // release the camera immediately on pause event
            // releaseCamera();
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        mCamera = Camera.open();

        Camera.Parameters mParameters = mCamera.getParameters();
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(mParameters);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera, this);

        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
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
            Bitmap pic = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix rotmat = new Matrix();
            rotmat.postRotate(-90);
            Bitmap scaledBmp = Bitmap.createScaledBitmap(pic, pic.getWidth(), pic.getHeight(), true);
            pic = Bitmap.createBitmap(scaledBmp, 0, 0, scaledBmp.getWidth(), scaledBmp.getHeight(), rotmat, true);
            Uri imUri = null;

            //save the taken picture
            FileOutputStream out = null;

            File sd = new File(Environment.getExternalStorageDirectory() + "/documents");
            boolean success = true;
            if(!sd.exists()){
                success = sd.mkdir();
                Log.d("DEBUGGING", sd.getAbsolutePath());
            }

            if(success){
                File dest = new File(sd, "im.png");

                try {
                    out = new FileOutputStream(dest);
                    pic.compress(Bitmap.CompressFormat.PNG, 100, out);

                    imUri = Uri.fromFile(dest);
                    Log.d("FILE SAVE: ", "got uri from file path");
                }
                catch(Exception e){
                    Log.d("FILE SAVE ERROR: ", e.getMessage());
                }
                finally {
                    try {
                        if (out != null) {
                            out.close();
                            Log.d("FILE SAVE:", "save complete, FileOutputStream closed");
                        }
                    }
                    catch (IOException e){
                        Log.d("FILE SAVE ERROR: ", e.getMessage());
                    }
                }

            }

            if(imUri != null){
                picTaken(imUri);
            }
        }
    };

    private void picTaken(Uri imUri){
        //will start a new activity that takes the selected image and extracts the document from it
        Intent runCV = new Intent(this, extractDocActivity.class);

        //add the uri of the image to the intent to pass to the other activity
        runCV.putExtra("img_uri", imUri.toString());

        Log.d("DEBUGGING", "starting cv activity from picTaken");
        startActivity(runCV);

        //when this is called, the activity is done and can be closed
        Log.d("DEBUGGING", "made it to the end of import activity");
    }

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

    
}

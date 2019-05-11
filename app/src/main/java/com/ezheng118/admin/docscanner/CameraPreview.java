package com.ezheng118.admin.docscanner;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Activity cameraActivity;

    public CameraPreview(Context context, Camera camera, Activity act){
        super(context);
        mCamera = camera;
        cameraActivity = act;

        mHolder = getHolder();
        mHolder.addCallback(this);

        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder){
        try{
            mCamera.setPreviewDisplay(holder);
            setCameraDisplayOrientation();
            mCamera.startPreview();
        }
        catch(IOException e){
            Log.d("CAMERA DEBUGGING", "Error setting camera preview" + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder){

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){

        if(mHolder.getSurface() == null){
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try{
            mCamera.setPreviewDisplay(mHolder);
            setCameraDisplayOrientation();
            mCamera.startPreview();
        }
        catch(Exception e){
            Log.d("CAMERA DEBUGGING", "Error starting camera preview" + e.getMessage());
        }
    }

    public void setCameraDisplayOrientation(){
        Camera.CameraInfo info = new Camera.CameraInfo();

        //cameraId of 1 indicates the forward facing camera
        Camera.getCameraInfo(1, info);
        Log.d("CAMERA DEBUGGING", "camera info gotten");

        int rotation = cameraActivity.getWindowManager().getDefaultDisplay().getRotation();
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

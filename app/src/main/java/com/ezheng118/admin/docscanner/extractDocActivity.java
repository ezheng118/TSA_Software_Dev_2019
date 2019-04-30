package com.ezheng118.admin.docscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class extractDocActivity extends AppCompatActivity {

    ImageView imPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract_doc);

        Log.d("DEBUGGING", "made it to cv portion");
        Bitmap image;
        Uri imgUri = Uri.parse(getIntent().getStringExtra("img_uri"));
        Log.d("DEBUGGING", "On create: got bitmap uri from previous activity");

        imPreview = findViewById(R.id.image_processing_preview);

        try {
            image = convertToBmp(imgUri);
            showIMG(image);
            findDocContours(image);
        }
        catch(IOException e){
            Log.d("DEBUGGING", e.getMessage());
        }
    }

    public void onResume(){
        super.onResume();

        //code to get OpenCV in the program, will not work unless called from MainActivity
        /*if(!OpenCVLoader.initDebug()){
            Log.d("DEBUGGING", "internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
        else
        {
            Log.d("DEBUGGING", "OpenCV library found inside package");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }*/
    }

    protected void onDestroy(){
        super.onDestroy();
    }

    /*private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
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
    };*/

    private Bitmap convertToBmp(Uri uri) throws IOException{
        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fDescriptor = pfd.getFileDescriptor();
        Bitmap img = BitmapFactory.decodeFileDescriptor(fDescriptor);
        pfd.close();

        return img;
    }

    private void showIMG(Bitmap img){
        imPreview.setImageBitmap(img);
    }

    private void showIMG(Mat img_prev){
        Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(img_prev.cols(), img_prev.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_prev, bmp);

            imPreview.setImageBitmap(bmp);
        }
        catch(CvException e){
            Log.d("DEBUGGING", "Error converting mat to bmp (preview image): " + e.getMessage());
        }
    }

    private Bitmap findDocContours(Bitmap bmp_img){
        Log.d("CV", "start processing image");
        int width = bmp_img.getWidth();
        int height = bmp_img.getHeight();

        //declaring each frame needed in each step of processing
        Mat imgFrame = new Mat(width, height, CvType.CV_8UC4);
        Log.d("DEBUGGING", "Able to import and use cv libs");
        Mat hsvFrame = new Mat(width, height, CvType.CV_8UC4);
        Mat thresh1 = new Mat(width, height, CvType.CV_8UC4);
        Mat thresh2 = new Mat(width, height, CvType.CV_8UC4);
        Mat combinedThresh = new Mat(width, height, CvType.CV_8UC4);
        Mat mask = new Mat(width, height, CvType.CV_8UC4);
        Mat edges = new Mat(width, height, CvType.CV_8UC4);

        Utils.bitmapToMat(bmp_img, imgFrame);
        Log.d("DEBUGGING", "successfully converted from bmp to mat");

        //change from RGB to HSV
        Imgproc.cvtColor(imgFrame, hsvFrame, Imgproc.COLOR_RGB2HSV);

        showIMG(hsvFrame);

        //define the color thresholds for what is white
        Scalar lower_white1 = new Scalar(10, 0, 110);
        Scalar upper_white1 = new Scalar(50, 60, 255);
        Scalar lower_white2 = new Scalar(0, 0, 0);
        Scalar upper_white2 = new Scalar(0, 0, 0);

        //filter out the the document based on color
        Core.inRange(hsvFrame, lower_white1, upper_white1, thresh1);
        Core.inRange(hsvFrame, lower_white2, upper_white2, thresh2);
        //combine the filters
        Core.bitwise_or(thresh1, thresh2, combinedThresh);

        showIMG(combinedThresh);

        //get rid of noise in the image
        Mat kernel = new Mat(20, 20, CvType.CV_8UC1);
        Imgproc.morphologyEx(combinedThresh, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        Log.d("CV", "mid CV, finished morphological transformations");

        showIMG(mask);

        //find the edges of the document
        Imgproc.Canny(mask, edges, 100, 200);

        showIMG(edges);

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);


        int id = 0;
        int i = 0;

        return bmp_img;
    }

}

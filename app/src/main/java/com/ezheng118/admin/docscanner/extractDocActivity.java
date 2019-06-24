package com.ezheng118.admin.docscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class extractDocActivity extends AppCompatActivity {

    ImageView imPreview;
    TextView progressMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract_doc);

        Log.d("DEBUGGING", "made it to cv portion");
        Bitmap image;
        Uri imgUri = Uri.parse(getIntent().getStringExtra("img_uri"));
        Log.d("DEBUGGING", "On create: got bitmap uri from previous activity");

        imPreview = findViewById(R.id.image_processing_preview);
        imPreview.setRotation(90);
        progressMessage = findViewById(R.id.progress_description);

        try {
            image = convertToBmp(imgUri);
            showImg(image);
            getDoc(image);
            //progressMessage.setText("Finished! :)");
        }
        catch(IOException e){
            Log.d("DEBUGGING", e.getMessage());
        }

    }

    public void onResume(){
        super.onResume();
    }

    protected void onDestroy(){
        super.onDestroy();
    }

    private Bitmap convertToBmp(Uri uri) throws IOException{
        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fDescriptor = pfd.getFileDescriptor();
        Bitmap img = BitmapFactory.decodeFileDescriptor(fDescriptor);
        pfd.close();

        return img;
    }

    private Bitmap convertToBmp(Mat image){
        Bitmap newBmp = null;

        try{
            newBmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image, newBmp);
        }
        catch(CvException e){
            Log.d("DEBUGGING : CV", "error creating bitmap" + e.getMessage());
        }

        return newBmp;
    }

    private void showImg(Bitmap img){
        imPreview.setImageBitmap(img);
    }

    private void showImg(Mat img_prev){
        /*Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(img_prev.cols(), img_prev.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_prev, bmp);

            imPreview.setImageBitmap(bmp);
        }
        catch(CvException e){
            Log.d("DEBUGGING", "Error converting mat to bmp (preview image): " + e.getMessage());
        }*/

        showImg(convertToBmp(img_prev));
    }

    private void getDoc(Bitmap bmp_img){
        Log.d("CV", "start processing image");

        Mat imgFrame = new Mat(bmp_img.getWidth(), bmp_img.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bmp_img, imgFrame);
        MatOfPoint docContour = findDocContours(imgFrame);

        topDownTransform(imgFrame, docContour);
    }

    private MatOfPoint findDocContours(Mat imgFrame){
        Log.d("CV", "begin to find contours");

        Mat hsvFrame = new Mat(imgFrame.size(), CvType.CV_8UC4);
        Mat thresh1 = new Mat(imgFrame.size(), CvType.CV_8UC4);
        Mat thresh2 = new Mat(imgFrame.size(), CvType.CV_8UC4);
        Mat combinedThresh = new Mat(imgFrame.size(), CvType.CV_8UC4);
        Mat mask = new Mat(imgFrame.size(), CvType.CV_8UC4);
        Mat edges = new Mat(imgFrame.size(), CvType.CV_8UC4);
        Log.d("DEBUGGING", "Able to import and use cv libs");

        Log.d("DEBUGGING", "successfully converted from bmp to mat");

        //change from RGB to HSV
        Imgproc.cvtColor(imgFrame, hsvFrame, Imgproc.COLOR_RGB2HSV);

        Log.d("DEBUGGING", "converted to hsv");

        //showImg(hsvFrame);

        //define the color thresholds for what is white
        Scalar lower_white1 = new Scalar(0, 0, 120);
        Scalar upper_white1 = new Scalar(60, 50, 255);
        /*supposed to work but actually didnt work at all
        Scalar lower_white2 = new Scalar(130, 0, 120);
        Scalar upper_white2 = new Scalar(255, 70, 255);

        *I just guessed randomly here
        Scalar lower_white1 = new Scalar(220, 0, 110);
        Scalar upper_white1 = new Scalar(280, 70, 255);
        */
        Scalar lower_white2 = new Scalar(110, 0, 120);
        Scalar upper_white2 = new Scalar(180, 50, 255);

        //filter out the the document based on color
        Core.inRange(hsvFrame, lower_white1, upper_white1, thresh1);
        Core.inRange(hsvFrame, lower_white2, upper_white2, thresh2);
        //combine the filters
        Core.bitwise_or(thresh1, thresh2, combinedThresh);

        Log.d("DEBUGGING", "finished thresholding");

        //showImg(combinedThresh);

        //get rid of noise in the image
        Mat kernel = new Mat(10, 10, CvType.CV_8UC1);
        Imgproc.morphologyEx(combinedThresh, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        Log.d("DEBUGGING", "mid CV, finished morphological transformations");

        //showImg(mask);

        /* for some reason, using canny to find the edges of a document
        works really badly, so I'm just going to use the mask instead

        //find the edges of the document
        Imgproc.Canny(mask, edges, 30, 30);
        Mat k2 = new Mat(3, 3, CvType.CV_8UC1);
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, k2);

        //showImg(edges);
        */


        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        int id = 0;
        int i = 0;
        double largestSize = -1;
        double size;
        for(MatOfPoint contour: contours){
            size = Imgproc.contourArea(contour);
            if(size > largestSize){
                id = i;
                largestSize = size;
            }
            i++;
        }

        MatOfPoint docContour = contours.get(id);

        Mat contourDrawing = new Mat(imgFrame.size(), CvType.CV_8UC4);
        contourDrawing = imgFrame.clone();
        Imgproc.drawContours(contourDrawing, contours, id, new Scalar(0, 255, 0), 10);
        Log.d("DEBUGGING", "finished cv, found contours");

        showImg(contourDrawing);

        saveMat(imgFrame, "frame.png");
        saveMat(mask, "mask.png");
        saveMat(edges, "edge.png");

        Log.d("File CV", "Finished saving cv results");

        return docContour;
    }

    public void saveMat(Mat image, String filename){
        Bitmap bmp = convertToBmp(image);

        FileOutputStream out = null;

        File sd = new File(Environment.getExternalStorageDirectory() + "/documents");
        boolean success = true;
        if(!sd.exists()){
            success = sd.mkdir();
            Log.d("DEBUGGING", sd.getAbsolutePath());
        }

        if(success){
            File dest = new File(sd, filename);

            try{
                out = new FileOutputStream(dest);

                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                //PNG is loss-less format, compression factor is ignored
                Log.d("File CV", "Bitmap write to external storage success");
            }
            catch(Exception e){
                Log.d("File CV", e.getMessage());
            }
            finally {
                try {
                    if(out != null){
                        out.close();
                        Log.d("File CV", "outputstream closed");
                    }
                }
                catch(IOException e){
                    Log.d("File CV", e.getMessage() + "ERROR");
                }
            }
        }
    }

    private Mat topDownTransform(Mat im, MatOfPoint contour){
        Mat topDown = im;
        double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
        MatOfPoint2f approxContour = new MatOfPoint2f();
        MatOfPoint approx1f = new MatOfPoint();
        List<MatOfPoint> temp_contour = new ArrayList<>();

        //approxPolyDp should give a rectangular approximation of the document contour that is found
        //from this contour you can find the four corners of the document
        Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approxContour, 0.03*perimeter, true);


        //just stuff used to draw the approximate contour
        approxContour.convertTo(approx1f, CvType.CV_32S);
        temp_contour.add(approx1f);
        Imgproc.drawContours(topDown, temp_contour, 0, new Scalar(255, 0, 255), 10);


        //MatOfPoint2f page = new MatOfPoint2f();
        double[][] page = new double[approxContour.height()][2];
        //use height to get the first point of each row, corresponds to the four corners

        for(int i = 0; i < approxContour.height(); i++) {
            //page.put(i, 0, approxContour.get(i, 0));

            //this *should* result in a double[2] array being returned from get() and being put in page
            page[i] = approxContour.get(i, 0);
        }

        double[] dif = diff(page);

        double[] sum = summ(page);

        //points = corners of the page ordered as = [top_left, top_right, bottom_left, bottom_right]
        double[][] points = orderCorners(page, dif, sum);

        double width1 = Math.sqrt(((points[3][0] - points[2][0])*(points[3][0]-points[2][0])
                + (points[3][1]-points[2][1])*(points[3][1]-points[2][1])));
        double width2 = Math.sqrt(((points[1][0] - points[0][0])*(points[1][0]-points[0][0])
                + (points[1][1]-points[0][1])*(points[1][1]-points[0][1])));

        double height1 = Math.sqrt(((points[1][0] - points[3][0])*(points[1][0]-points[3][0])
                + (points[1][1]-points[3][1])*(points[1][1]-points[3][1])));
        double height2 = Math.sqrt(((points[0][0] - points[2][0])*(points[0][0]-points[2][0])
                + (points[0][1]-points[2][1])*(points[0][1]-points[2][1])));

        double maxWidth;
        double maxHeight;

        if(width1 > width2){
            maxWidth = width1;
        }
        else{
            maxWidth = width2;
        }
        if(height1 > height2){
            maxHeight = height1;
        }
        else{
            maxHeight = height2;
        }

        maxWidth = new BigDecimal(maxWidth).setScale(0, RoundingMode.HALF_UP).doubleValue();
        maxHeight = new BigDecimal(maxHeight).setScale(0, RoundingMode.HALF_UP).doubleValue();

        //dst[0] = top left = [0, 0]
        //dst[1] = top right = [maxWidth - 1, 0]
        //dst[2] = bottom left = [0, maxHeight - 1]
        //dst[3] = bottom right = [maxWidth - 1, maxHeight - 1]
        double[][] destination = new double[4][2];
        destination[1][0] = maxWidth - 1;
        destination[2][1] = maxHeight - 1;
        destination[3][0] = maxWidth - 1;
        destination[3][1] = maxHeight - 1;

        Mat src = new Mat(4, 2, CvType.CV_32FC1);
        Mat dst = new Mat(4, 2, CvType.CV_32FC1);

        for(int i = 0; i < 4; i++){
            src.put(i, 0, points[i]);
            dst.put(i, 0, destination[i]);
        }

        Mat transformationMat = Imgproc.getPerspectiveTransform(src, dst);
        Imgproc.warpPerspective(im, topDown, transformationMat, new Size(maxWidth, maxHeight));

        topDown = resizeToLetter(topDown);

        showImg(topDown);

        return topDown;
    }

    private double[] diff(double[][] inputArray){
        double[] diffedArray = new double[inputArray.length];

        int i = 0;
        for(double[] coordinate: inputArray){
            diffedArray[i] = coordinate[1] - coordinate[0];
            i++;
        }

        return diffedArray;
    }

    private double[] summ(double[][] inputArray){
        double[] summedArray = new double[inputArray.length];

        int i = 0;
        for(double[] coordinate: inputArray){
            summedArray[i] = coordinate[1] + coordinate[0];
            i++;
        }

        return summedArray;
    }

    private double[][] orderCorners(double[][] unordered, double[] dif, double[] sum){
        int diffMinIndex = 0;
        int diffMaxIndex = 0;
        int sumMinIndex = 0;
        int sumMaxIndex = 0;

        for(int i = 0; i < dif.length; i++){
            if(dif[diffMinIndex] > dif[i]){
                diffMinIndex = i;
            }

            if(dif[diffMaxIndex] < dif[i]){
                diffMaxIndex = i;
            }
        }

        for(int i = 0; i < sum.length; i++){
            if(sum[sumMinIndex] > sum[i]){
                sumMinIndex = i;
            }

            if(sum[sumMaxIndex] < sum[i]){
                sumMaxIndex = i;
            }
        }

        //[top_left, top_right, bottom_left, bottom_right]
        double[][] ordered = {unordered[sumMinIndex], unordered[diffMinIndex], unordered[diffMaxIndex], unordered[sumMaxIndex]};

        return ordered;
    }

    private Mat resizeToLetter(Mat m){
        Mat resized = new Mat();
        Size letter;

        if(m.size().height * .773 != m.size().width){
            letter = new Size(m.size().width, m.size().width*1.294);
        }
        else{
            letter = m.size();
        }

        Imgproc.resize(m, resized, letter);

        return resized;
    }
}

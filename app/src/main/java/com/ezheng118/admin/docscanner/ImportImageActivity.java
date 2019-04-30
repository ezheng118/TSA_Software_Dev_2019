package com.ezheng118.admin.docscanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;

public class ImportImageActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    private Uri imgURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_image);

        Intent intent = getIntent();

        performFileSearch();
    }

    public void onResume(){
        super.onResume();
    }

    protected void onDestroy(){
        super.onDestroy();
    }


    public void performFileSearch(){
        Intent searchIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        //will only look at files that are openable
        searchIntent.addCategory(Intent.CATEGORY_OPENABLE);

        //will only look for images
        searchIntent.setType("image/*");

        startActivityForResult(searchIntent, READ_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData){
        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK);

        if(resultData != null){
            imgURI = resultData.getData();
            Log.i("importIMG", "Uri: " + imgURI.toString());
            try {
                displayPreviewImage();
            }
            catch(IOException e){
                e.printStackTrace();
                return;
            }

        }

        return;
    }

    private void displayPreviewImage() throws IOException{
        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(imgURI, "r");
        FileDescriptor fDescriptor = pfd.getFileDescriptor();
        Bitmap img = BitmapFactory.decodeFileDescriptor(fDescriptor);
        pfd.close();

        ImageView imgPreview = findViewById(R.id.imported_img_preview);
        imgPreview.setImageBitmap(img);
    }

    //called on click of the change button
    public void changeSelection(View view){
        performFileSearch();
    }

    //called on click of the confirm button
    public void confirmSelection(View view){
        //will start a new activity that takes the selected image and extracts the document from it
        Intent runCV = new Intent(this, extractDocActivity.class);

        //add the uri of the image to the intent to pass to the other activity
        runCV.putExtra("img_uri", imgURI.toString());
        Log.d("DEBUGGING", "starting cv activity");
        startActivity(runCV);

        //when this is called, the activity is done and can be closed
        Log.d("DEBUGGING", "made it to the end of import activity");
        finish();
    }
}

package com.example.imageeffectsopencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.example.imageeffectsopencv.R;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();
    }

    public void stitchVectical(View view){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap im1 = BitmapFactory.decodeResource(getResources(), R.drawable.part1, options);
        Bitmap im2 = BitmapFactory.decodeResource(getResources(), R.drawable.part2, options);
        Bitmap im3 = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat img1 = new Mat();
        Mat img2 = new Mat();
        Mat img3 = new Mat();
        Utils.bitmapToMat(im1, img1);
        Utils.bitmapToMat(im2, img2);
        Utils.bitmapToMat(im3, img3);

        Bitmap imgBitmap = stitchImagesVectical(Arrays.asList(img1, img2, img3));
        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "stitch_vectical");
    }

    Bitmap stitchImagesVectical(List<Mat> src) {
        Mat dst = new Mat();
        Core.vconcat(src, dst); //Core.hconcat(src, dst);
        Bitmap imgBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, imgBitmap);

        return imgBitmap;
    }

    public void stitchHorizontal(View view){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap im1 = BitmapFactory.decodeResource(getResources(), R.drawable.part1, options);
        Bitmap im2 = BitmapFactory.decodeResource(getResources(), R.drawable.part2, options);
        Bitmap im3 = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat img1 = new Mat();
        Mat img2 = new Mat();
        Mat img3 = new Mat();
        Utils.bitmapToMat(im1, img1);
        Utils.bitmapToMat(im2, img2);
        Utils.bitmapToMat(im3, img3);

        Bitmap imgBitmap = stitchImagesHorizontal(Arrays.asList(img1, img2, img3));
        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "stitch_horizontal");
    }

    Bitmap stitchImagesHorizontal(List<Mat> src) {
        Mat dst = new Mat();
        Core.hconcat(src, dst); //Core.vconcat(src, dst);
        Bitmap imgBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, imgBitmap);

        return imgBitmap;
    }

    public void saveBitmap(Bitmap imgBitmap, String fileNameOpening){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = fileNameOpening + "_" + formatter.format(now) + ".jpg";

        FileOutputStream outStream;
        try{
            // Get a public path on the device storage for saving the file. Note that the word external does not mean the file is saved in the SD card. It is still saved in the internal storage.
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            // Creates directory for saving the image.
            File saveDir =  new File(path + "/HeartBeat/");

            // If the directory is not created, create it.
            if(!saveDir.exists())
                saveDir.mkdirs();

            // Create the image file within the directory.
            File fileDir =  new File(saveDir, fileName); // Creates the file.

            // Write into the image file by the BitMap content.
            outStream = new FileOutputStream(fileDir);
            imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

            MediaScannerConnection.scanFile(this.getApplicationContext(),
                    new String[] { fileDir.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });

            // Close the output stream.
            outStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
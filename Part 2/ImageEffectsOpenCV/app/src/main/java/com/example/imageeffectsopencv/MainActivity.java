package com.example.imageeffectsopencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.imageeffectsopencv.R;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.Core.LUT;
import static org.opencv.core.CvType.CV_8UC1;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();
    }

    public void cartoonImage(View view) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGRA2BGR);

        Mat result = cartoon(img1, 80, 15, 10);

        Bitmap imgBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, imgBitmap);

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "cartoon");
    }

    public void reduceImageColors(View view){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);

        Mat result = reduceColors(img1, 80, 15, 10);

        Bitmap imgBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, imgBitmap);

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "reduce_colors");
    }

    public void reduceImageColorsGray(View view){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);

        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2GRAY);
        Mat result = reduceColorsGray(img1, 5);

        Bitmap imgBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, imgBitmap);

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "reduce_colors_gray");
    }

    public void medianFilter(View view) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);
        Mat medianFilter = new Mat();
        Imgproc.cvtColor(img1, medianFilter, Imgproc.COLOR_BGR2GRAY);

        Imgproc.medianBlur(medianFilter, medianFilter, 15);

        Bitmap imgBitmap = Bitmap.createBitmap(medianFilter.cols(), medianFilter.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(medianFilter, imgBitmap);

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "median_filter");
    }

    public void adaptiveThreshold(View view) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        Mat adaptiveTh = new Mat();
        Utils.bitmapToMat(original, adaptiveTh);
        Imgproc.cvtColor(adaptiveTh, adaptiveTh, Imgproc.COLOR_BGR2GRAY);

        Imgproc.medianBlur(adaptiveTh, adaptiveTh, 15);

        Imgproc.adaptiveThreshold(adaptiveTh, adaptiveTh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 2);

        Bitmap imgBitmap = Bitmap.createBitmap(adaptiveTh.cols(), adaptiveTh.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(adaptiveTh, imgBitmap);

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(imgBitmap);
        saveBitmap(imgBitmap, "adaptive_threshold");
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

    Mat cartoon(Mat img, int numRed, int numGreen, int numBlue) {
        Mat reducedColorImage = reduceColors(img, numRed, numGreen, numBlue);

        Mat result = new Mat();
        Imgproc.cvtColor(img, result, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(result, result, 15);

        Imgproc.adaptiveThreshold(result, result, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 2);

        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2BGR);

        Log.d("PPP", result.height() + " " + result.width() + " " + reducedColorImage.type() + " " + result.channels());
        Log.d("PPP", reducedColorImage.height() + " " + reducedColorImage.width() + " " + reducedColorImage.type() + " " + reducedColorImage.channels());

        Core.bitwise_and(reducedColorImage, result, result);

        return result;
    }

    Mat reduceColors(Mat img, int numRed, int numGreen, int numBlue) {
        Mat redLUT = createLUT(numRed);
        Mat greenLUT = createLUT(numGreen);
        Mat blueLUT = createLUT(numBlue);

        List<Mat> BGR = new ArrayList<>(3);
        Core.split(img, BGR); // splits the image into its channels in the List of Mat arrays.

        LUT(BGR.get(0), blueLUT, BGR.get(0));
        LUT(BGR.get(1), greenLUT, BGR.get(1));
        LUT(BGR.get(2), redLUT, BGR.get(2));

        Core.merge(BGR, img);

        return img;
    }

    Mat reduceColorsGray(Mat img, int numColors) {
        Mat LUT = createLUT(numColors);

        LUT(img, LUT, img);

        return img;
    }

    Mat createLUT(int numColors) {
        // When numColors=1 the LUT will only have 1 color which is black.
        if (numColors < 0 || numColors > 256) {
            System.out.println("Invalid Number of Colors. It must be between 0 and 256 inclusive.");
            return null;
        }

        Mat lookupTable = Mat.zeros(new Size(1, 256), CV_8UC1);

        int startIdx = 0;
        for (int x = 0; x < 256; x += 256.0 / numColors) {
            lookupTable.put(x, 0, x);

            for (int y = startIdx; y < x; y++) {
                if (lookupTable.get(y, 0)[0] == 0) {
                    lookupTable.put(y, 0, lookupTable.get(x, 0));
                }
            }
            startIdx = x;
        }
        return lookupTable;
    }

    Bitmap stitchImagesVectical(List<Mat> src) {
        Mat dst = new Mat();
        Core.vconcat(src, dst); //Core.hconcat(src, dst);
        Bitmap imgBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, imgBitmap);

        return imgBitmap;
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

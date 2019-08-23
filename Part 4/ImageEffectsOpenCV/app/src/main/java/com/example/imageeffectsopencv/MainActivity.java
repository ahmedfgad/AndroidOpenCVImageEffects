package com.example.imageeffectsopencv;

import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.imageeffectsopencv.R;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.Core.LUT;
import static org.opencv.core.CvType.CV_8UC1;

public class MainActivity extends AppCompatActivity {

    final int SELECT_MULTIPLE_IMAGES = 1;
    ArrayList<String> selectedImagesPaths; // Paths of the image(s) selected by the user.
    boolean imagesSelected = false; // Whether the user selected at least an image or not.

    Bitmap resultBitmap; // Result of the last operation.
    String resultName = null; // File name to save the result of the last operation.

    boolean GIFLastEffect = false;
    ByteArrayOutputStream GIFImageByteArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();
    }

    public void createAnimatedGIF(View view) {
        List<Mat> imagesMatList = returnMultipleSelectedImages(selectedImagesPaths, 2, false);
        if (imagesMatList == null) {
            return;
        }

        GIFImageByteArray = createGIF(imagesMatList, 150);
        resultName = "animated_GIF";
        GIFLastEffect = true;
    }

    ByteArrayOutputStream createGIF(List<Mat> imagesMatList, int delay) {
        ByteArrayOutputStream imageByteArray = new ByteArrayOutputStream();
        // Implementation of the AnimatedGifEncoder.java file: https://gist.githubusercontent.com/wasabeef/8785346/raw/53a15d99062a382690275ef5666174139b32edb5/AnimatedGifEncoder.java
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(imageByteArray);

        AnimationDrawable animatedGIF = new AnimationDrawable();

        for (Mat img : imagesMatList) {
            Bitmap imgBitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, imgBitmap);
            encoder.setDelay(delay);
            encoder.addFrame(imgBitmap);

            animatedGIF.addFrame(new BitmapDrawable(getResources(), imgBitmap), delay);
        }

        encoder.finish();

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setBackground(animatedGIF); // attach animation to a view
        animatedGIF.run();

        return imageByteArray;
    }

    void saveGif(ByteArrayOutputStream imageByteArray, String fileNameOpening) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = fileNameOpening + "_" + formatter.format(now) + ".gif";

        FileOutputStream outStream;
        try {
            // Get a public path on the device storage for saving the file. Note that the word external does not mean the file is saved in the SD card. It is still saved in the internal storage.
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            // Creates a directory for saving the image.
            File saveDir = new File(path + "/HeartBeat/");

            // If the directory is not created, create it.
            if (!saveDir.exists())
                saveDir.mkdirs();

            // Create the image file within the directory.
            File fileDir = new File(saveDir, fileName); // Creates the file.

            // Write into the image file by the BitMap content.
            outStream = new FileOutputStream(fileDir);
            outStream.write(imageByteArray.toByteArray());

            MediaScannerConnection.scanFile(this.getApplicationContext(),
                    new String[]{fileDir.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });

            // Close the output stream.
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveImage(View v) {
        if (resultName == null) {
            Toast.makeText(getApplicationContext(), "Please Apply an Operation to Save its Result.", Toast.LENGTH_LONG).show();
            return;
        }

        if (GIFLastEffect == true) {
            saveGif(GIFImageByteArray, "animated_GIF");
        } else {
            saveBitmap(resultBitmap, resultName);
        }
        Toast.makeText(getApplicationContext(), "Image Saved Successfully.", Toast.LENGTH_LONG).show();
    }

    public void selectImage(View v) {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_MULTIPLE_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == SELECT_MULTIPLE_IMAGES && resultCode == RESULT_OK && null != data) {
                // When a single image is selected.
                String currentImagePath;
                selectedImagesPaths = new ArrayList<>();
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    currentImagePath = getPath(getApplicationContext(), uri);
                    Log.d("ImageDetails", "Single Image URI : " + uri);
                    Log.d("ImageDetails", "Single Image Path : " + currentImagePath);
                    selectedImagesPaths.add(currentImagePath);
                    imagesSelected = true;
                } else {
                    // When multiple images are selected.
                    // Thanks tp Laith Mihyar for this Stackoverflow answer : https://stackoverflow.com/a/34047251/5426539
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {

                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();

                            currentImagePath = getPath(getApplicationContext(), uri);
                            selectedImagesPaths.add(currentImagePath);
                            Log.d("ImageDetails", "Image URI " + i + " = " + uri);
                            Log.d("ImageDetails", "Image Path " + i + " = " + currentImagePath);
                            imagesSelected = true;
                        }
                    }
                }
            } else {
                Toast.makeText(this, "You haven't Picked any Image.", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(getApplicationContext(), selectedImagesPaths.size() + " Image(s) Selected.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void blendRegions(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap img1Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.im1, options);
//        Bitmap img2Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.im2, options);
//        Bitmap img1MaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mask_im1, options);
//
//        Mat img1 = new Mat();
//        Mat img2 = new Mat();
//        Mat img1Mask = new Mat();
//
//        Utils.bitmapToMat(img1Bitmap, img1);
//        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGRA2BGR);
//
//        Utils.bitmapToMat(img2Bitmap, img2);
//        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGRA2BGR);
//
//        Utils.bitmapToMat(img1MaskBitmap, img1Mask);

        GIFLastEffect = false;

        List<Mat> imagesMatList = returnMultipleSelectedImages(selectedImagesPaths, 3, false);
        if (imagesMatList == null) {
            return;
        }

        Mat img1Mask = imagesMatList.get(imagesMatList.size() - 1);
        Imgproc.cvtColor(img1Mask, img1Mask, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(img1Mask, img1Mask, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(img1Mask, img1Mask, 200, 255.0, Imgproc.THRESH_BINARY);

        Mat result = regionBlending(imagesMatList.get(0), imagesMatList.get(1), img1Mask);

        resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, resultBitmap);
        resultName = "region_blending";
        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    public void blendImages(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap img1Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.im1, options);
//        Bitmap img2Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.im2, options);
//
//        Mat img1 = new Mat();
//        Mat img2 = new Mat();
//        Utils.bitmapToMat(img1Bitmap, img1);
//        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGRA2BGR);
//        Utils.bitmapToMat(img2Bitmap, img2);
//        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGRA2BGR);

        GIFLastEffect = false;

        List<Mat> imagesMatList = returnMultipleSelectedImages(selectedImagesPaths, 2, false);
        if (imagesMatList == null) {
            return;
        }

        Mat result = imageBlending(imagesMatList.get(0), imagesMatList.get(1), 128.0);

        resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, resultBitmap);
        resultName = "image_blending";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    public void cartoonImage(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        GIFLastEffect = false;

        Bitmap original = returnSingleImageSelected(selectedImagesPaths);
        if (original == null) {
            return;
        }

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGRA2BGR);

        Mat result = cartoon(img1, 80, 15, 10);

        resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, resultBitmap);
        resultName = "cartoon";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    public void reduceImageColors(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        GIFLastEffect = false;

        Bitmap original = returnSingleImageSelected(selectedImagesPaths);
        if (original == null) {
            return;
        }

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);

        Mat result = reduceColors(img1, 80, 15, 10);

        resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, resultBitmap);
        resultName = "reduce_colors";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    Bitmap returnSingleImageSelected(ArrayList<String> selectedImages) {
        if (imagesSelected == true) {
            return BitmapFactory.decodeFile(selectedImagesPaths.get(0));
        } else {
            Toast.makeText(getApplicationContext(), "No Image Selected. You have to Select an Image.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public void reduceImageColorsGray(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        GIFLastEffect = false;

        Bitmap original = returnSingleImageSelected(selectedImagesPaths);
        if (original == null) {
            return;
        }

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);

        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2GRAY);
        Mat result = reduceColorsGray(img1, 5);

        resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, resultBitmap);
        resultName = "reduce_colors_gray";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    public void medianFilter(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        GIFLastEffect = false;

        Bitmap original = returnSingleImageSelected(selectedImagesPaths);
        if (original == null) {
            return;
        }

        Mat img1 = new Mat();
        Utils.bitmapToMat(original, img1);
        Mat medianFilter = new Mat();
        Imgproc.cvtColor(img1, medianFilter, Imgproc.COLOR_BGR2GRAY);

        Imgproc.medianBlur(medianFilter, medianFilter, 15);

        resultBitmap = Bitmap.createBitmap(medianFilter.cols(), medianFilter.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(medianFilter, resultBitmap);
        resultName = "median_filter";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    public void adaptiveThreshold(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);

        GIFLastEffect = false;

        Bitmap original = returnSingleImageSelected(selectedImagesPaths);
        if (original == null) {
            return;
        }

        Mat adaptiveTh = new Mat();
        Utils.bitmapToMat(original, adaptiveTh);
        Imgproc.cvtColor(adaptiveTh, adaptiveTh, Imgproc.COLOR_BGR2GRAY);

        Imgproc.medianBlur(adaptiveTh, adaptiveTh, 15);

        Imgproc.adaptiveThreshold(adaptiveTh, adaptiveTh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 2);

        resultBitmap = Bitmap.createBitmap(adaptiveTh.cols(), adaptiveTh.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(adaptiveTh, resultBitmap);
        resultName = "adaptive_threshold";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    List<Mat> returnMultipleSelectedImages(ArrayList<String> selectedImages, int numImagesRequired, boolean moreAccepted) {
        if (selectedImages == null) {
            Toast.makeText(getApplicationContext(), "No Images Selected. You have to Select More than 1 Image.", Toast.LENGTH_LONG).show();
            return null;
        } else if (selectedImages.size() == 0 && moreAccepted == true) {
            Toast.makeText(getApplicationContext(), "No Images Selected. You have to Select at Least " + numImagesRequired + " Images.", Toast.LENGTH_LONG).show();
            return null;
        } else if (selectedImages.size() == 0 && moreAccepted == false) {
            Toast.makeText(getApplicationContext(), "No Images Selected. You have to Select Exactly " + numImagesRequired + " Images.", Toast.LENGTH_LONG).show();
            return null;
        } else if (selectedImages.size() < numImagesRequired && moreAccepted == true) {
            Toast.makeText(getApplicationContext(), "Sorry. You have to Select at Least " + numImagesRequired + " Images.", Toast.LENGTH_LONG).show();
            return null;
        } else if (selectedImages.size() < numImagesRequired && moreAccepted == false) {
            Toast.makeText(getApplicationContext(), "Sorry. You have to Select Exactly " + numImagesRequired + " Images.", Toast.LENGTH_LONG).show();
            return null;
        }

        List<Mat> imagesMatList = new ArrayList<>();
        Mat mat = Imgcodecs.imread(selectedImages.get(0));
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
        imagesMatList.add(mat);

        for (int i = 1; i < selectedImages.size(); i++) {
            mat = Imgcodecs.imread(selectedImages.get(i));
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
            if (imagesMatList.get(0).size().equals(mat)) {
                imagesMatList.add(mat);
            } else {
                Imgproc.resize(mat, mat, imagesMatList.get(0).size());
                imagesMatList.add(mat);
            }
        }
        return imagesMatList;
    }

    public void stitchVectical(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap im1 = BitmapFactory.decodeResource(getResources(), R.drawable.part1, options);
//        Bitmap im2 = BitmapFactory.decodeResource(getResources(), R.drawable.part2, options);
//        Bitmap im3 = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);
//
//        Mat img1 = new Mat();
//        Mat img2 = new Mat();
//        Mat img3 = new Mat();
//        Utils.bitmapToMat(im1, img1);
//        Utils.bitmapToMat(im2, img2);
//        Utils.bitmapToMat(im3, img3);

        GIFLastEffect = false;

        List<Mat> imagesMatList = returnMultipleSelectedImages(selectedImagesPaths, 2, true);
        if (imagesMatList == null) {
            return;
        }

        resultBitmap = stitchImagesVectical(imagesMatList);
        resultName = "stitch_vectical";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    public void stitchHorizontal(View view) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false; // Leaving it to true enlarges the decoded image size.
//        Bitmap im1 = BitmapFactory.decodeResource(getResources(), R.drawable.part1, options);
//        Bitmap im2 = BitmapFactory.decodeResource(getResources(), R.drawable.part2, options);
//        Bitmap im3 = BitmapFactory.decodeResource(getResources(), R.drawable.part3, options);
//
//        Mat img1 = new Mat();
//        Mat img2 = new Mat();
//        Mat img3 = new Mat();
//        Utils.bitmapToMat(im1, img1);
//        Utils.bitmapToMat(im2, img2);
//        Utils.bitmapToMat(im3, img3);

        GIFLastEffect = false;

        List<Mat> imagesMatList = returnMultipleSelectedImages(selectedImagesPaths, 2, true);
        if (imagesMatList == null) {
            return;
        }

        resultBitmap = stitchImagesHorizontal(imagesMatList);
        resultName = "stitch_horizontal";

        ImageView imageView = findViewById(R.id.opencvImg);
        imageView.setImageBitmap(resultBitmap);
    }

    Mat regionBlending(Mat img, Mat img2, Mat mask) {
        Mat result = img2;

        for (int row = 0; row < img.rows(); row++) {
            for (int col = 0; col < img.cols(); col++) {
                double[] img1Pixel = img.get(row, col);
                double[] binaryPixel = mask.get(row, col);
                if (binaryPixel[0] == 255.0) {
                    result.put(row, col, img1Pixel);
                }
            }
        }
        return result;
    }

    Mat imageBlending(Mat img, Mat img2, double alpha) {
        Mat result = img;

        if (alpha == 0.0) {
            return img2;
        } else if (alpha == 255.0) {
            return img;
        }

        for (int row = 0; row < img.rows(); row++) {
            for (int col = 0; col < img.cols(); col++) {
                double[] pixel1 = img.get(row, col);

                double[] pixel2 = img2.get(row, col);

                double fraction = alpha / 255.0;

                pixel1[0] = pixel1[0] * fraction + pixel2[0] * (1.0 - fraction);
                pixel1[1] = pixel1[1] * fraction + pixel2[1] * (1.0 - fraction);
                pixel1[2] = pixel1[2] * fraction + pixel2[2] * (1.0 - fraction);

                result.put(row, col, pixel1);
            }
        }
        return result;
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

    void saveBitmap(Bitmap imgBitmap, String fileNameOpening) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = fileNameOpening + "_" + formatter.format(now) + ".jpg";

        FileOutputStream outStream;
        try {
            // Get a public path on the device storage for saving the file. Note that the word external does not mean the file is saved in the SD card. It is still saved in the internal storage.
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            // Creates directory for saving the image.
            File saveDir = new File(path + "/HeartBeat/");

            // If the directory is not created, create it.
            if (!saveDir.exists())
                saveDir.mkdirs();

            // Create the image file within the directory.
            File fileDir = new File(saveDir, fileName); // Creates the file.

            // Write into the image file by the BitMap content.
            outStream = new FileOutputStream(fileDir);
            imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

            MediaScannerConnection.scanFile(this.getApplicationContext(),
                    new String[]{fileDir.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });

            // Close the output stream.
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of the getPath() method and all its requirements is taken from the StackOverflow Paul Burke's answer: https://stackoverflow.com/a/20559175/5426539
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
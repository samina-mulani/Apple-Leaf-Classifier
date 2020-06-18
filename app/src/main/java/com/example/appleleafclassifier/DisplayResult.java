package com.example.appleleafclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.label.TensorLabel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DisplayResult extends AppCompatActivity {
    private int inputImageHeight = 60;
    private int inputImageWidth = 60;
    private int noOfChannels = 3;
    TextView resultHeader;
    TextView content;
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hides default title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_display_result);

        DisplayResult.context = getApplicationContext(); //Set Context

        resultHeader = (TextView) findViewById(R.id.resultHeader2);
        content = (TextView) findViewById(R.id.content);

        Intent intent = getIntent();
        Uri imageUri = intent.getData(); //get the image passed on to this Activity

        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream); //converts obtained image to Bitmap
        int result = predict(selectedImage);
        displayResult(result);
    }

    private void displayResult(int result) {
        switch (result) {
            case 0:
                resultHeader.setText("Healthy Apple");
                content.setText(getString(R.string.healthy));
                break;
            case 1:
                resultHeader.setText("Apple Scab");
                content.setText(getString(R.string.scab));
                break;
            case 2:
                resultHeader.setText("Black Rot");
                content.setText(getString(R.string.blackrot));
                break;
            case 3:
                resultHeader.setText("Cedar Apple Rust");
                content.setText(getString(R.string.rust));
                break;
        }
    }

    private int predict(Bitmap bp) {
        int res = -1;
        String result = ""; //used in earlier version to debug app
        List<String> associatedAxisLabels = Arrays.asList("Healthy Apple", "Apple Scab", "Black Rot", "Cedar Apple Rust"); //List of Labels
        TensorBuffer probabilityBuffer =
                TensorBuffer.createFixedSize(new int[]{1, 4}, DataType.FLOAT32); //holds the output which is of shape 1x4

        //Initialise model
        Interpreter tflite = null;
        AssetManager assets = context.getAssets();
        try {
            MappedByteBuffer tfliteModel = loadModelFile(assets, "converted_model.tflite");
            tflite = new Interpreter(tfliteModel);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error reading model", e);
        }

        // Running inference
        if (null != tflite) {
            tflite.run(convertBitmapToByteBuffer(bp), probabilityBuffer.getBuffer()); //runs the model with first and second parameters as input and output respectively
            TensorLabel labels = new TensorLabel(associatedAxisLabels, probabilityBuffer); //Maps each label with corresponding probability
            Map<String, Float> resultmap = labels.getMapWithFloatValue();

            float max = 0;
            int iterator = 0;
            for (Map.Entry<String, Float> entry : resultmap.entrySet()) {
                if (max < entry.getValue()) {
                    max = entry.getValue();
                    res = iterator;
                }
                result += "Key = " + entry.getKey() + "\nValue = " + entry.getValue() + "\n"; //used in earlier version to debug
                iterator++;
            }
        }
        return res;
    }

    public void home(View view) {
        finish(); //Current Activity ends on clicking Home Button
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        //gets the tflite model stored under Assets folder and converts it to a MappedByteBuffer
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bp) {
        //Converts Bitmap image to ByteBuffer. Image resizing and normalisation take place
        ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES * inputImageHeight * inputImageWidth * 3);
        imgData.order(ByteOrder.nativeOrder());
        //the following step has third parameter set to true implying that
        //bilinear filtering will be used when scaling which has better image quality at the cost of worse performance.
        Bitmap bitmap = Bitmap.createScaledBitmap(bp, inputImageWidth, inputImageHeight, true);
        int[] intValues = new int[inputImageWidth * inputImageHeight];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convert the image to floating point.
        int pixel = 0;

        for (int i = 0; i < 60; ++i) {
            for (int j = 0; j < 60; ++j) {
                final int val = intValues[pixel++];
                //Alpha channel of bitmap is ignored
                imgData.putFloat(((val >> 16) & 0xFF) / 255.f); //red pixel value is picked and divided by 255
                imgData.putFloat(((val >> 8) & 0xFF) / 255.f); //green pixel value is picked and divided by 255
                imgData.putFloat((val & 0xFF) / 255.f); //blue pixel value is picked and divided by 255
            }
        }
        return imgData;
    }
}

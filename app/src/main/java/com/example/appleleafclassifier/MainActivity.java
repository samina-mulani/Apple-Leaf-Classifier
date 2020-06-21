package com.example.appleleafclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private static final int CAMERA = 200;
    Uri mUri;
    Uri imageUri;
    private static final String LOG_TAG =
            MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hides default title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);
    }

    public void goToGallery(View view) {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        if (gallery.resolveActivity((getPackageManager())) != null) {
            startActivityForResult(gallery, PICK_IMAGE);
        } else {
            Log.d("Gallery implicit intent", "Can't handle this intent");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            imageUri = data.getData();
            Intent intent = new Intent(this, DisplayResult.class);
            intent.setData(imageUri);
            startActivity(intent);
        }
        else if(resultCode == RESULT_OK && requestCode == CAMERA)
        {
            Intent intent = new Intent(this, DisplayResult.class);
            intent.setData(mUri);
            startActivity(intent);
        }
    }

    public void goToCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "pic_"+ String.valueOf(System.currentTimeMillis()) + ".jpg"));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
        startActivityForResult(intent, CAMERA);

    }
}

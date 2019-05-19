package com.example.snappassport;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.snapchat.kit.sdk.SnapCreative;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitApi;
import com.snapchat.kit.sdk.creative.exceptions.SnapMediaSizeException;
import com.snapchat.kit.sdk.creative.exceptions.SnapStickerSizeException;
import com.snapchat.kit.sdk.creative.media.SnapMediaFactory;
import com.snapchat.kit.sdk.creative.media.SnapPhotoFile;
import com.snapchat.kit.sdk.creative.media.SnapSticker;
import com.snapchat.kit.sdk.creative.models.SnapPhotoContent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    //  Snapchat Creative API
    public SnapCreativeKitApi snapCreativeKitApi;

    //  Sticker Stuff
    private static final String STICKER_NAME = "mlhSticker";
    private static final String STICKER_NAME2 = "yosemiteSticker";
    private File mStickerFile;
    private File mStickerFile2;

    //  Location Coordinates
    private FusedLocationProviderClient fusedLocationClient;
    private double longit;
    private double latit;

    // Main Activity XML Stuff
    public TextView mText;
    public TextView mText2;
    public TextView mText3;

    //  Permission Stuff
    private static final int WRITE_STORAGE_PERM = 123;
    private static final int COURSE_LOCATION_PERM = 122;

    boolean photoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        EasyImage.configuration(this).setAllowMultiplePickInGallery(false);

        snapCreativeKitApi = SnapCreative.getApi(MainActivity.this);

        mStickerFile = new File(getCacheDir(), STICKER_NAME);
        mStickerFile2 = new File(getCacheDir(), STICKER_NAME2);

        photoPicker = false;

        mText = findViewById(R.id.textView);
        mText2 = findViewById(R.id.textView2);
        mText3 = findViewById(R.id.textView3);

        Button sendButton = findViewById(R.id.sndPhotoButton);
        Button scanButton = findViewById(R.id.scanbutton);

        if (!mStickerFile.exists()) {
            try (InputStream inputStream = getAssets().open("mlhSticker.png")) {
                copyFile(inputStream, mStickerFile);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to copy sticker asset", Toast.LENGTH_SHORT).show();
            }
        }

        if (!mStickerFile2.exists()) {
            try (InputStream inputStream = getAssets().open("yosemiteSticker.png")) {
                copyFile(inputStream, mStickerFile2);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to copy sticker asset", Toast.LENGTH_SHORT).show();
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            EasyPermissions.requestPermissions(this, getString(R.string.app_permission_text), COURSE_LOCATION_PERM, Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    EasyPermissions.requestPermissions(MainActivity.this, getString(R.string.app_permission_text), COURSE_LOCATION_PERM, Manifest.permission.ACCESS_COARSE_LOCATION);
                } else {
                    scanLoc();
                }
            }
        });

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            longit = location.getLongitude();
                            latit = location.getLatitude();

                            mText.setText(Double.toString(longit));
                            mText2.setText(Double.toString(latit));

                            onCheckLocation();
                            // Logic to handle location object
                        }
                    }
                });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPhotoSend();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (photoPicker) {
            EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
                @Override
                public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                    photoPicker = false;
                    //Some error handling
                }

                @Override
                public void onImagesPicked(List<File> imagesFiles, EasyImage.ImageSource source, int type) {
                    //Handle the images
                    photoPicker = false;
                    onPhotosReturned(imagesFiles);
                }
            });
        }
    }

    private void scanLoc() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            EasyPermissions.requestPermissions(this, getString(R.string.app_permission_text), COURSE_LOCATION_PERM, Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                longit = location.getLongitude();
                                latit = location.getLatitude();

                                mText.setText(Double.toString(longit));
                                mText2.setText(Double.toString(latit));

                                onCheckLocation();
                                // Logic to handle location object
                            }
                        }
                    });
        }
    }

    private void onPhotosReturned(List<File> imageFiles) {
        Log.d("photo Returned", "Photo returned");
        SnapMediaFactory snapMediaFactory = SnapCreative.getMediaFactory(this);

        if (imageFiles.size() == 1) {
            SnapPhotoFile photoFile;
            try {
                photoFile = snapMediaFactory.getSnapPhotoFromFile(imageFiles.get(0));
            } catch (SnapMediaSizeException e) {
                e.printStackTrace();
                return;
            }
            SnapPhotoContent snapPhotoContent = new SnapPhotoContent(photoFile);

            snapPhotoContent = checkLocationS(snapPhotoContent);

            snapCreativeKitApi.send(snapPhotoContent);
            Log.d("Snapchat", "Photo sent to snapchat");
        }
    }

    private SnapPhotoContent checkLocationS(SnapPhotoContent snapPhotoContent) {
        //if..... within certain range of coordinates
        if (longit >= -79.4f && longit <= -79.3f && latit >= 43.6f && latit <= 43.7f) { //   George Vari!

            SnapMediaFactory snapMediaFactory = SnapCreative.getMediaFactory(this);
            SnapSticker snapSticker = null;
            try {
                snapSticker = snapMediaFactory.getSnapStickerFromFile(mStickerFile);
            } catch (SnapStickerSizeException e) {
                e.printStackTrace();
                return null;
            }

            snapSticker.setHeight(300);
            snapSticker.setWidth(300);
            snapSticker.setPosX(0.75f);
            snapSticker.setPosY(0.25f);
            snapSticker.setRotationDegreesClockwise(45f);
            snapPhotoContent.setSnapSticker(snapSticker);
            snapPhotoContent.setCaptionText("Welcome to RUHacks 2019!");
            return snapPhotoContent;

        } else if (longit >= 36.8f && longit <= 39.8f && latit >= 118.5f && latit <= 120.5f) {
            SnapMediaFactory snapMediaFactory = SnapCreative.getMediaFactory(this);
            SnapSticker snapSticker = null;
            try {
                snapSticker = snapMediaFactory.getSnapStickerFromFile(mStickerFile2);
            } catch (SnapStickerSizeException e) {
                e.printStackTrace();
                return null;
            }

            snapSticker.setHeight(300);
            snapSticker.setWidth(300);
            snapSticker.setPosX(0.75f);
            snapSticker.setPosY(0.25f);
            snapSticker.setRotationDegreesClockwise(45f);
            snapPhotoContent.setSnapSticker(snapSticker);
            snapPhotoContent.setCaptionText("Welcome to Yosemite National Park!");
            return snapPhotoContent;
        } else {
            Toast.makeText(MainActivity.this, "Not at a special location", Toast.LENGTH_SHORT).show();
            return snapPhotoContent;
        }
    }

    private void onCheckLocation() {
        if (longit >= -79.4f && longit <= -79.3f && latit >= 43.6f && latit <= 43.7f) {
            mText3.setText("RUHacks!");
        } else if (longit >= 36.8f && longit <= 39.8f && latit >= 118.5f && latit <= 120.5f) {
            mText3.setText("Yosemite National Park!");
        } else {
            mText3.setText("No where special :(((");
        }
    }

    private void startPhotoSend() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            //has permissions
            EasyImage.openGallery(MainActivity.this, 0);
            photoPicker = true;
        } else {
            //does not have permissions
            EasyPermissions.requestPermissions(this, getString(R.string.app_permission_text), WRITE_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private static void copyFile(InputStream inputStream, File file) throws IOException {
        byte[] buffer = new byte[1024];
        int length;

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }
}

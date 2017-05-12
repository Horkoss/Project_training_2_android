package com.keycam.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.keycam.HomeManager;
import com.keycam.R;
import com.keycam.SocketManager;
import com.keycam.models.VideoSessionModel;
import com.keycam.network.ApiEndPointInterface;
import com.keycam.network.ApiError;
import com.keycam.network.RequestFactory;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.SubscriberKit;

import org.parceler.Parcels;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends HomeManager {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        ButterKnife.bind(this);
        userModel = Parcels.unwrap(getIntent().getParcelableExtra("user"));

        SharedPreferences sharedPreferences = getSharedPreferences("shared", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", userModel.getToken()).apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
            } else
                initAll();
        } else
            initAll();
    }

    private void initAll() {
        mode = 0;
        getPlaylist();
        initTextToSpeech();
        connectSocket();
        initBatteryListener();
    }

    private void connectSocket() {
        socketManager = new SocketManager(this, userModel.getToken());
        socketManager.initSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketManager.disconnectSocket();
        if (mediaPlayer != null)
            mediaPlayer.stop();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
        }
        unregisterReceiver(powerConnectionReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length == 3
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    initAll();
                } else
                    finish();
                break;
            }
        }
    }
}

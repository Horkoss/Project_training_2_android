package com.keycam.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.keycam.HomeManager;
import com.keycam.R;
import com.keycam.SocketManager;

import org.parceler.Parcels;

import butterknife.ButterKnife;

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
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else
                initAll();
        } else
            initAll();
    }

    private void initAll() {
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
        unregisterReceiver(powerConnectionReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length == 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    initAll();
                } else
                    finish();
                break;
            }
        }
    }
}

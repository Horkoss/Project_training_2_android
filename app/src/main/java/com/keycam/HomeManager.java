package com.keycam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.keycam.models.PlayerModel;
import com.keycam.models.UserModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;


public class HomeManager extends AppCompatActivity {
    protected List<String> songTitle;
    protected List<String> songPath;
    protected SocketManager socketManager;
    protected TextToSpeech textToSpeech;
    protected UserModel userModel;
    protected Intent mIntent;
    private Camera camera;
    private Camera.PictureCallback pictureCallback;
    private int currentCam;
    protected MediaPlayer mediaPlayer;
    protected PowerConnectionReceiver powerConnectionReceiver;

    @BindView(R.id.surface)
    SurfaceView surfaceView;

    protected void getPlaylist(){
        String[] ARG_STRING = {MediaStore.Audio.Media.TITLE, android.provider.MediaStore.MediaColumns.DATA};
        Cursor cursor = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ARG_STRING, null, null, null);
        songTitle = new ArrayList<>();
        songPath = new ArrayList<>();

        if (cursor != null) {
            while(cursor.moveToNext()) {
                songTitle.add(cursor.getString(0));
                songPath.add(cursor.getString(1));
            }
            cursor.close();
        }
    }

    protected void initTextToSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    textToSpeech.setLanguage(Locale.forLanguageTag(Locale.getDefault().getDisplayLanguage()));
                } else {
                    textToSpeech.setLanguage(new Locale(Locale.getDefault().getDisplayLanguage()));
                }
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String s) {
                    }

                    @Override
                    public void onDone(String s) {
                        socketManager.sendStringData("message talked", "text");
                    }

                    @Override
                    public void onError(String s) {

                    }
                });
            }
        });
    }

    private class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIntent = intent;
            sendBatteryStatus();
        }
    }


    protected void sendBatteryStatus() {
        int status = mIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_PLUGGED_AC || status == BatteryManager.BATTERY_PLUGGED_USB;

        String data = "off";
        if (isCharging)
            data = "on";
        socketManager.sendStringData(data, "battery");
    }

    protected void initBatteryListener() {
        powerConnectionReceiver = new PowerConnectionReceiver();
        mIntent = this.registerReceiver(powerConnectionReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    protected void dispatchTakePictureIntent(int camNum) throws IOException {
        currentCam = camNum;
        camera = Camera.open(camNum);

        camera.setDisplayOrientation(90);
        pictureCallback = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 15;  // downsample factor (16 pixels -> 1 pixel)

                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bmp , 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
                byte[] byteArray = stream.toByteArray();

                socketManager.sendPicture(byteArray);

                camera.stopPreview();
                camera.startPreview();
            }
        };
        SurfaceHolder mHolder = surfaceView.getHolder();
        camera.setPreviewDisplay(mHolder);
        camera.startPreview();
    }


    protected void switchCamera() {
        camera.stopPreview();
        camera.release();
        String camState = "back";
        if(currentCam == Camera.CameraInfo.CAMERA_FACING_BACK){
            currentCam = Camera.CameraInfo.CAMERA_FACING_FRONT;
            camState = "front";
        }
        else
            currentCam = Camera.CameraInfo.CAMERA_FACING_BACK;
        try {
            dispatchTakePictureIntent(currentCam);
            socketManager.sendStringData(camState, "switch");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        camera.takePicture(null, null, pictureCallback);
    }

    protected void sendPlaylist() {
        getPlaylist();
        socketManager.sendPlaylist(songTitle);
    }

    protected void managePlayer(Object command) {
        Gson gson = new Gson();
        PlayerModel playerModel = gson.fromJson(command.toString(), PlayerModel.class);
        String state;
        switch (playerModel.getAction()) {
            case "play":
                if (mediaPlayer != null && !mediaPlayer.isPlaying())
                    mediaPlayer.start();
                else
                    playSong(songPath.get(playerModel.getSong()));
                state = "play";
                break;
            case "stop":
                if (mediaPlayer != null)
                    mediaPlayer.stop();
                mediaPlayer = null;
                state = "stop";
                break;
            default:
                if (mediaPlayer != null)
                    mediaPlayer.pause();
                state = "pause";
                break;
        }
        socketManager.sendStringData(state, "player");
    }


    private void playSong(String dataStream){
        mediaPlayer = new MediaPlayer();
        if(dataStream == null)
            return;
        try {
            mediaPlayer.setDataSource(dataStream);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

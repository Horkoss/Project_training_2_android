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
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.keycam.models.PlayerModel;
import com.keycam.models.UserModel;
import com.keycam.models.VideoSessionModel;
import com.keycam.network.ApiEndPointInterface;
import com.keycam.network.ApiError;
import com.keycam.network.RequestFactory;
import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.GONE;


public class HomeManager extends AppCompatActivity  implements Session.SessionListener, PublisherKit.PublisherListener{
    protected List<String> songTitle;
    protected List<String> songPath;
    protected SocketManager socketManager;
    protected TextToSpeech textToSpeech;
    protected UserModel userModel;
    protected Intent mIntent;
    protected Camera camera;
    private Camera.PictureCallback pictureCallback;
    private int currentCam;
    protected MediaPlayer mediaPlayer;
    protected PowerConnectionReceiver powerConnectionReceiver;
    protected MediaRecorder mediaRecorder;
    private int previousSoundLevel;
    protected int mode;
    Session mSession;
    Publisher mPublisher;
    Timer timer;

    @BindView(R.id.publisher_container)
    FrameLayout mPublisherViewContainer;

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
                if (currentCam == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    matrix.postRotate(-90);
                else
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
        camera.setPreviewDisplay(surfaceView.getHolder());
        camera.startPreview();
        if (mediaRecorder == null)
            startRecord();
    }

    private void startRecord() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                checkSoundLevel();
            }
        },0,1000);
    }

    private void checkSoundLevel() {
        int soundLevel = mediaRecorder.getMaxAmplitude();
        if(soundLevel < 500)
            soundLevel = 0;
        else if(soundLevel < 5000)
            soundLevel = 1000;
        else
            soundLevel = 10000;

        if (previousSoundLevel != soundLevel)
            socketManager.sendStringData(String.valueOf(soundLevel), "mood");

        previousSoundLevel = soundLevel;
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

    protected void controlFlashLight() {
        if (camera != null && currentCam == Camera.CameraInfo.CAMERA_FACING_BACK) {
            if (camera.getParameters().getFlashMode().equals("torch")) {
                Camera.Parameters cameraParameters = camera.getParameters();
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(cameraParameters);
                socketManager.sendStringData("off", "light");
            } else {
                Camera.Parameters cameraParameters = camera.getParameters();
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(cameraParameters);
                socketManager.sendStringData("on", "light");
            }
        }
    }

    protected void switchMode() {
        if (mode == 0) {
            mode = 1;
            camera.stopPreview();
            camera.release();
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            timer.cancel();
            if (mSession == null)
                getSession();
            else
                publish();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    surfaceView.setVisibility(GONE);
                    mPublisherViewContainer.setVisibility(View.VISIBLE);
                }
            });
            socketManager.sendStringData("video", "mode");
        }
        else {
            mode = 0;
            BaseVideoCapturer baseVideoCapturer = mPublisher.getCapturer();
            mPublisher.setPublishVideo(false);
            if (baseVideoCapturer != null) {
                baseVideoCapturer.stopCapture();
                baseVideoCapturer.destroy();
            }
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPublisherViewContainer.setVisibility(View.GONE);
                    surfaceView.setVisibility(View.VISIBLE);
                    try {
                        dispatchTakePictureIntent(Camera.CameraInfo.CAMERA_FACING_BACK);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socketManager.sendStringData("picture", "mode");
                }
            });

        }
    }

    private void getSession() {
        ApiEndPointInterface apiRequest = RequestFactory.createApiCallRequest();
        Call<VideoSessionModel> call = apiRequest.getSession();
        call.enqueue(new Callback<VideoSessionModel>() {
            @Override
            public void onResponse(Call<VideoSessionModel> call, Response<VideoSessionModel> response) {
                parseResponse(response);
            }

            @Override
            public void onFailure(Call<VideoSessionModel> call, Throwable t) {
                //Toast.makeText(, "Get session failed, please check your network", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void parseResponse(Response<VideoSessionModel> response){
        int statusCode = response.code();

        Log.d("STATUS CODE", String.valueOf(statusCode));
        if (statusCode == 200) { // Success
            initializeSession(response.body());
        }
        else if (statusCode >= 300 && statusCode < 500){
            ApiError apiError = RequestFactory.parseError(response);
            Toast.makeText(this, apiError.getMessage(), Toast.LENGTH_SHORT).show();        }
        else
            Toast.makeText(this, "Get session failed, try again later", Toast.LENGTH_SHORT).show();
    }

    private void initializeSession(VideoSessionModel userModel) {
        if (mSession == null) {
            mSession = new Session.Builder(this, userModel.getKey(), userModel.getId()).build();
            mSession.setSessionListener(this);
            mSession.connect(userModel.getToken());
        } else
            publish();
    }
    private void publish() {
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.cycleCamera();
        mPublisher.setPublisherListener(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPublisherViewContainer.addView(mPublisher.getView());
            }
        });
        mSession.publish(mPublisher);
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onConnected(Session session) {
        publish();
    }

    @Override
    public void onDisconnected(Session session) {

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

    }
}

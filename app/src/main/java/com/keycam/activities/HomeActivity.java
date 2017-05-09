package com.keycam.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.keycam.R;
import com.keycam.models.UserModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class HomeActivity extends AppCompatActivity {
    private UserModel userModel;
    private Socket socket;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Camera camera;
    private Camera.PictureCallback pictureCallback;
    private TextToSpeech textToSpeech;
    private int currentCam;

    @BindView(R.id.surface)
    SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        ButterKnife.bind(this);
        userModel = Parcels.unwrap(getIntent().getParcelableExtra("user"));

        SharedPreferences sharedPreferences = getSharedPreferences("shared", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", userModel.getToken()).apply();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                1);

        initTextToSpeech();
        try {
            connectSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void connectSocket() throws URISyntaxException {
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.query = "token=" + userModel.getToken() + "&type=baby";
        socket = IO.socket("http://10.0.1.6:4444", opts);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                //socket.emit("chat message", "toto");
                try {
                    dispatchTakePictureIntent(Camera.CameraInfo.CAMERA_FACING_BACK);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).on("text", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    textToSpeech.speak((String) args[0], TextToSpeech.QUEUE_FLUSH, null, "messageID");
                } else {
                    textToSpeech.speak((String) args[0], TextToSpeech.QUEUE_FLUSH, map);
                }
            }

        }).on("picture", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                camera.takePicture(null, null, pictureCallback);
            }
        }).on("switch", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                camera.stopPreview();
                camera.release();
                if(currentCam == Camera.CameraInfo.CAMERA_FACING_BACK){
                    currentCam = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                else {
                    currentCam = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                Log.d("CAM BACK", String.valueOf(Camera.CameraInfo.CAMERA_FACING_BACK));
                Log.d("CAM FRONT", String.valueOf(Camera.CameraInfo.CAMERA_FACING_FRONT));
                Log.d("CAM NUMBER", String.valueOf(currentCam));
                try {
                    dispatchTakePictureIntent(currentCam);
                    socket.emit("switch", "camera switched");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.connect();
    }

    private void initTextToSpeech() {
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
                        socket.emit("text", "message talked");
                    }

                    @Override
                    public void onError(String s) {

                    }
                });
            }
        });

    }

    private void dispatchTakePictureIntent(int camNum) throws IOException {
        currentCam = camNum;
        camera = Camera.open(camNum);
    //    Camera.Parameters params = camera.getParameters();
    //    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    //    camera.setParameters(params);
        camera.setDisplayOrientation(90);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        pictureCallback = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                JSONObject obj = new JSONObject();
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 15;  // downsample factor (16 pixels -> 1 pixel)

                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 50, stream);
                    byte[] byteArray = stream.toByteArray();
                    obj.put("picture", byteArray);
                    socket.emit("picture", obj);
                    camera.stopPreview();
                    camera.startPreview();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        SurfaceHolder mHolder = surfaceView.getHolder();
        camera.setPreviewDisplay(mHolder);
        camera.startPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }
}

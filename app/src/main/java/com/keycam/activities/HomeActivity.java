package com.keycam.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.os.Bundle;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class HomeActivity extends AppCompatActivity{
    private UserModel userModel;
    private Socket socket;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Camera camera;
    private Camera.PictureCallback pictureCallback;

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
                    dispatchTakePictureIntent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).on("chat message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("CHAT MESSAGE", (String)args[0]);
            }

        }).on("picture", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                camera.takePicture(null, null, pictureCallback);
            }
        });
        socket.connect();
    }


    private void dispatchTakePictureIntent() throws IOException {
        camera = Camera.open(0);
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
        camera.setDisplayOrientation(90);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        pictureCallback = new Camera.PictureCallback() {
/*            @Override
            public void onShutter() {
                Log.d("LOLOOL", "LOLOOLOL");
            }*/

            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("picture", bytes);
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
        /*        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }*/
    }

/*    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            JSONObject obj = new JSONObject();
            try {
                obj.put("picture", byteArray);
                socket.emit("picture", obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }
}

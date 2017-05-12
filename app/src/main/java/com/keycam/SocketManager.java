package com.keycam;

import android.hardware.Camera;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import com.keycam.activities.HomeActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private HomeActivity mActivity;
    private String mToken;
    private Socket socket;

    public SocketManager(HomeActivity homeActivity, String token) {
        mActivity = homeActivity;
        mToken = token;
    }

    public void initSocket() {
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.query = "token=" + mToken + "&type=baby";
        try {
            socket = IO.socket("http://10.0.1.6:4444", opts);
            initSocketListener();
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void initSocketListener() {
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    mActivity.dispatchTakePictureIntent(Camera.CameraInfo.CAMERA_FACING_BACK);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).on("text", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args[0] != null)
                    textToSpeech((String) args[0]);
            }

        }).on("picture", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mActivity.takePicture();
            }
        }).on("switch", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mActivity.switchCamera();
            }
        }).on("playlist", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mActivity.sendPlaylist();
            }
        }).on("player", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mActivity.managePlayer(args[0]);
            }
        }).on("battery", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mActivity.sendBatteryStatus();
            }
        }).on("light", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        mActivity.controlFlashLight();
                    }
        }).on("mode", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mActivity.switchMode();
            }
        });
    }

    private void textToSpeech(String text){
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "messageID");
        } else {
            mActivity.textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    void sendPicture(byte[] byteArray) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("data", byteArray);
            jsonObject.put("type", "picture");
            socket.emit("picture", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void sendStringData(String data, String channel){
        try {
            socket.emit(channel, createJSONObjectFromString(data, channel));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void sendPlaylist(List<String> songTitle) {
        JSONObject jsonObject = new JSONObject();
        JSONArray array = new JSONArray(songTitle);
        try {
            jsonObject.put("data", array);
            jsonObject.put("type", "playlist");
            socket.emit("playlist", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createJSONObjectFromString(String data, String channel) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", data);
        jsonObject.put("type", channel);
        return jsonObject;
    }

    public void disconnectSocket() {
        socket.disconnect();
    }
}

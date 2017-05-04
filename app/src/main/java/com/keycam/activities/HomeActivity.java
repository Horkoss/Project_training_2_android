package com.keycam.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.keycam.R;
import com.keycam.models.UserModel;

import org.parceler.Parcels;

import java.net.URISyntaxException;

import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class HomeActivity extends AppCompatActivity {
    private UserModel userModel;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        ButterKnife.bind(this);
        userModel = Parcels.unwrap(getIntent().getParcelableExtra("user"));

        SharedPreferences sharedPreferences = getSharedPreferences("shared", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", userModel.getToken()).apply();
        try {
            connectSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void connectSocket() throws URISyntaxException {
        socket = IO.socket("http://10.0.1.6:1234");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                socket.emit("chat message", "toto");
            }

        }).on("chat message", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("CHAT MESSAGE", (String)args[0]);
/*                String message = (String)args[0];
                MessageModel messageModel = new MessageModel();
                messageModel.setUsername(mFriend);
                messageModel.setMessage(message);
                mMessageAdapter.getmMessageList().add(messageModel);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMessageAdapter.notifyItemInserted(mMessageAdapter.getItemCount() - 1);
                    }
                });*/
            }

        });
        socket.connect();
    }
}

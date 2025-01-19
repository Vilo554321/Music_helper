package sk.upjs.ics.android.musichelper;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button createRoomButton = findViewById(R.id.btnCreateRoom);
        Button joinRoomButton = findViewById(R.id.btnJoinRoom);

        createRoomButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateRoomActivity.class));
        });

        joinRoomButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, JoinRoomActivity.class));
        });
    }
}







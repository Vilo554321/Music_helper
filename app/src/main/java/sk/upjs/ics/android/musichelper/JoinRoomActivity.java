package sk.upjs.ics.android.musichelper;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class JoinRoomActivity extends AppCompatActivity {
    private EditText songEditText;
    private String serverIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        songEditText = findViewById(R.id.etSongName);
        Button sendButton = findViewById(R.id.btnSend);

        discoverServer();

        sendButton.setOnClickListener(v -> {
            if (serverIp != null) {
                String songName = songEditText.getText().toString();
                if (!songName.isEmpty()) {
                    sendSongName(songName);
                    songEditText.setText("");
                }
            } else {
                System.out.println("Server IP not found. Please wait...");
            }
        });
    }

    private void discoverServer() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(8081);
                socket.setBroadcast(true);
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                System.out.println("Waiting for server broadcast...");
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("SERVER_IP:")) {
                    serverIp = message.split(":")[1];
                    System.out.println("Discovered server IP: " + serverIp);
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendSongName(String songName) {
        new Thread(() -> {
            try {
                System.out.println("Attempting to connect to server...");
                Socket socket = new Socket(serverIp, 8080);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(songName.getBytes());
                outputStream.flush();
                System.out.println("Message sent: " + songName);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error sending message: " + e.getMessage());
            }
        }).start();
    }
}

package sk.upjs.ics.android.musichelper;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateRoomActivity extends AppCompatActivity {
    private RecyclerView songsRecyclerView;
    private SongsAdapter songsAdapter;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    private List<String> songsList = new ArrayList<>();

    private static final String SONGS_LIST_KEY = "songsList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        songsRecyclerView = findViewById(R.id.recyclerViewSongs);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            songsList = savedInstanceState.getStringArrayList(SONGS_LIST_KEY);
        }

        songsAdapter = new SongsAdapter(songsList, song -> {
            songsList.remove(song);
            songsAdapter.notifyDataSetChanged();
        });

        songsRecyclerView.setAdapter(songsAdapter);

        executorService = Executors.newFixedThreadPool(4);

        startBroadcast();
        startServer(songsList);
    }

    private void startBroadcast() {
        new Thread(() -> {
            try {
                DatagramSocket broadcastSocket = new DatagramSocket();
                broadcastSocket.setBroadcast(true);
                String message = "SERVER_IP:" + getLocalIpAddress();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName("255.255.255.255"), 8081);
                while (true) {
                    broadcastSocket.send(packet);
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Broadcast error: " + e.getMessage());
            }
        }).start();
    }

    private void startServer(List<String> songsList) {
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                System.out.println("Server is running on port 8080...");
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    handleClient(clientSocket, songsList);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Server error: " + e.getMessage());
            }
        });
    }

    private void handleClient(Socket clientSocket, List<String> songsList) {
        executorService.execute(() -> {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    String songName = new String(buffer, 0, bytesRead).trim();
                    System.out.println("Received song: " + songName);
                    runOnUiThread(() -> {
                        songsList.add(songName);
                        songsAdapter.notifyDataSetChanged();
                    });
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error handling client: " + e.getMessage());
            }
        });
    }

    private String getLocalIpAddress() {
        try {
            for (java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                java.net.NetworkInterface intf = en.nextElement();
                for (java.util.Enumeration<java.net.InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SONGS_LIST_KEY, new ArrayList<>(songsList));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

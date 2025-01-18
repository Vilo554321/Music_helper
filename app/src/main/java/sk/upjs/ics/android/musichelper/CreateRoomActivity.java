package sk.upjs.ics.android.musichelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private SongsAdapter songsAdapter;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRoomRunning = false;

    private List<String> songsList = new ArrayList<>();
    private static final String SONGS_LIST_KEY = "songsList";

    private static final int ROOM_CHECK_PORT = 5000;
    private static final String ROOM_CHECK_MESSAGE = "ROOM_CHECK";
    private static final String ROOM_EXISTS_RESPONSE = "ROOM_EXISTS";

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        RecyclerView songsRecyclerView = findViewById(R.id.recyclerViewSongs);
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

        executorService.execute(() -> {
            if (isRoomAlreadyCreated()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "Miestnosť je už vyvorená!", Toast.LENGTH_LONG).show();
                    finish();
                });
            } else {
                isRoomRunning = true;
                startBroadcast();
                startServer(songsList);
                startRoomCheckServer();
            }
        });
    }

    private boolean isRoomAlreadyCreated() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1000);

            InetAddress broadcastAddress = getBroadcastAddress();
            byte[] sendData = ROOM_CHECK_MESSAGE.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, ROOM_CHECK_PORT);
            socket.send(sendPacket);

            byte[] receiveData = new byte[16];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            return ROOM_EXISTS_RESPONSE.equals(response);
        } catch (IOException e) {
            return false;
        }
    }

    private void startRoomCheckServer() {
        executorService.execute(() -> {
            try (DatagramSocket serverSocket = new DatagramSocket(ROOM_CHECK_PORT)) {
                while (isRoomRunning) {
                    byte[] receiveData = new byte[16];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);

                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    if (ROOM_CHECK_MESSAGE.equals(message)) {
                        InetAddress clientAddress = receivePacket.getAddress();
                        int clientPort = receivePacket.getPort();

                        byte[] sendData = ROOM_EXISTS_RESPONSE.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                    }
                }
            } catch (IOException ignored) {
            }
        });
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            int ipAddress = wifi.getDhcpInfo().ipAddress;
            int broadcast = (ipAddress & 0x00FFFFFF) | 0xFF000000;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            return InetAddress.getByAddress(quads);
        }
        return InetAddress.getByName("255.255.255.255");
    }

    private void startBroadcast() {
        executorService.execute(() -> {
            try (DatagramSocket broadcastSocket = new DatagramSocket()) {
                broadcastSocket.setBroadcast(true);
                String message = "SERVER_IP:" + getLocalIpAddress();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName("255.255.255.255"), 8081);
                while (isRoomRunning) {
                    broadcastSocket.send(packet);
                    Thread.sleep(3000);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void startServer(List<String> songsList) {
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                while (isRoomRunning) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket, songsList);
                }
            } catch (IOException ignored) {
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleClient(Socket clientSocket, List<String> songsList) {
        executorService.execute(() -> {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    String songName = new String(buffer, 0, bytesRead).trim();
                    runOnUiThread(() -> {
                        songsList.add(songName);
                        songsAdapter.notifyDataSetChanged();
                    });
                }
                clientSocket.close();
            } catch (IOException ignored) {
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
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SONGS_LIST_KEY, new ArrayList<>(songsList));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRoomRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }
}

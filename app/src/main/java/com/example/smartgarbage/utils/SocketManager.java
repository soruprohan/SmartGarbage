package com.example.smartgarbage.utils;

import android.util.Log;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {

    private static final String TAG = "SocketManager";
    // Use the Railway backend URL — no trailing slash
    private static final String SERVER_URL = "https://smart-garbage-production.up.railway.app";

    private static SocketManager instance;
    private Socket socket;
    private boolean isConnected = false;

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Connect to socket server with JWT token for authentication.
     * Must be called once (e.g., in MessagesActivity.onStart).
     */
    public void connect(String token) {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Already connected");
            return;
        }
        try {
            IO.Options options = new IO.Options();
            options.auth = java.util.Collections.singletonMap("token", token);
            // forceNew ensures we always get a fresh connection
            options.forceNew = true;
            // transports: ["websocket"] avoids polling fallback issues on Railway
            options.transports = new String[]{"websocket"};

            socket = IO.socket(SERVER_URL, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                isConnected = true;
                Log.d(TAG, "Socket connected");
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                isConnected = false;
                Log.d(TAG, "Socket disconnected");
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                isConnected = false;
                if (args.length > 0) {
                    Log.e(TAG, "Socket connect error: " + args[0].toString());
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid server URI", e);
        }
    }

    /**
     * Join the room for a driver<->admin conversation.
     * Room name from backend: "driver_<driverId>-admin_<adminId>"
     */
    public void joinRoom(int driverId, int adminId) {
        if (socket == null) return;
        String room = "driver_" + driverId + "-admin_" + adminId;
        socket.emit("joinRoom", room);
        Log.d(TAG, "Joined room: " + room);
    }

    /**
     * Emit a sendMessage event through socket.
     * The backend saves it and broadcasts to both rooms.
     */
    public void sendMessage(int adminId, String content) {
        if (socket == null || !socket.connected()) {
            Log.w(TAG, "Socket not connected, cannot send via socket");
            return;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("receiver_role", "admin");
            data.put("receiver_id", adminId);
            data.put("content", content);
            socket.emit("sendMessage", data);
            Log.d(TAG, "Emitted sendMessage via socket");
        } catch (Exception e) {
            Log.e(TAG, "Error building sendMessage payload", e);
        }
    }

    /**
     * Register a listener for incoming messages.
     * Call this in MessagesActivity to receive real-time messages.
     */
    public void onNewMessage(Emitter.Listener listener) {
        if (socket == null) return;
        socket.off("newMessage"); // remove old listener first to avoid duplicates
        socket.on("newMessage", listener);
    }

    /**
     * Remove the newMessage listener (call in onStop/onDestroy).
     */
    public void offNewMessage() {
        if (socket == null) return;
        socket.off("newMessage");
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    /**
     * Disconnect from socket (call when user logs out).
     */
    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
            isConnected = false;
        }
    }
}
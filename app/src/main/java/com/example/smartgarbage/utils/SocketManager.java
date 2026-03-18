package com.example.smartgarbage.utils;

import android.util.Log;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static final String SERVER_URL = "https://smart-garbage-production.up.railway.app";

    private static SocketManager instance;
    private Socket socket;

    // Pending room join: if joinRoom() is called before socket connects,
    // we store it here and emit it the moment EVENT_CONNECT fires.
    private String pendingRoom = null;

    // The active newMessage listener — kept so we can re-register it after reconnect.
    private Emitter.Listener newMessageListener = null;

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Connect to the socket server with a JWT token for authentication.
     * Safe to call multiple times — skips if already connected.
     */
    public void connect(String token) {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Already connected");
            return;
        }

        // If a stale disconnected socket exists, tear it down cleanly first.
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }

        try {
            IO.Options options = new IO.Options();
            options.auth = java.util.Collections.singletonMap("token", token);
            options.forceNew = true;
            options.transports = new String[]{"websocket"};

            socket = IO.socket(SERVER_URL, options);

            // ── FIX 1: All post-connect work (joinRoom + listener) happens
            //    inside EVENT_CONNECT, guaranteeing the server has finished
            //    its JWT auth middleware before we emit anything.
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Socket connected");

                // Re-join the room if we already know it (covers reconnects too).
                if (pendingRoom != null) {
                    socket.emit("joinRoom", pendingRoom);
                    Log.d(TAG, "Joined room on connect: " + pendingRoom);
                }

                // Re-attach the newMessage listener after every (re)connect.
                if (newMessageListener != null) {
                    socket.off("newMessage");
                    socket.on("newMessage", newMessageListener);
                    Log.d(TAG, "Re-registered newMessage listener after connect");
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args ->
                    Log.d(TAG, "Socket disconnected"));

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
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
     * Join the conversation room for this driver ↔ admin pair.
     * Room name the backend emits to: "driver_<driverId>-admin_<adminId>"
     *
     * Safe to call before or after connect() — if the socket isn't ready yet
     * the room name is stored and emitted the moment EVENT_CONNECT fires.
     */
    public void joinRoom(int driverId, int adminId) {
        String room = "driver_" + driverId + "-admin_" + adminId;
        pendingRoom = room; // always store so reconnects work too

        if (socket != null && socket.connected()) {
            socket.emit("joinRoom", room);
            Log.d(TAG, "Joined room immediately: " + room);
        } else {
            Log.d(TAG, "Room stored as pending (will join on connect): " + room);
        }
    }

    /**
     * Register the listener for incoming real-time messages.
     * Stores the listener so it survives reconnects automatically.
     * Safe to call before connect() — it will be attached once the socket connects.
     */
    public void onNewMessage(Emitter.Listener listener) {
        newMessageListener = listener; // persist for reconnects

        if (socket == null) return;
        socket.off("newMessage"); // prevent duplicate listeners
        socket.on("newMessage", listener);
        Log.d(TAG, "newMessage listener registered");
    }

    /**
     * Remove the newMessage listener (call in onStop/onDestroy).
     * Clears the stored reference so reconnects don't re-attach it.
     */
    public void offNewMessage() {
        newMessageListener = null;
        if (socket == null) return;
        socket.off("newMessage");
    }

    /**
     * Emit a sendMessage event via socket.
     * The backend saves it and broadcasts to both the sender and receiver rooms.
     */
    public void sendMessage(int adminId, String content) {
        if (socket == null || !socket.connected()) {
            Log.w(TAG, "Socket not connected — cannot send via socket");
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

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    /**
     * Full teardown — call only on logout, not on screen navigation.
     */
    public void disconnect() {
        pendingRoom = null;
        newMessageListener = null;
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
    }
}
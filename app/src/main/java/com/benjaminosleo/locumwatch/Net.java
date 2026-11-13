package com.benjaminosleo.locumwatch;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small HTTP helpers: ntfy (free, for replies) and Pushover (optional, wakes the iPhone). */
public final class Net {
    private static final String TAG = "LocumWatch";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private Net() {}

    public static void async(Runnable r) { EXEC.execute(r); }

    /** Publish to an ntfy topic. priority: 1 (min) .. 5 (max). */
    public static boolean ntfy(String topic, String title, String message, int priority) {
        if (topic == null || topic.isEmpty()) return false;
        try {
            JSONObject o = new JSONObject();
            o.put("topic", topic);
            o.put("title", title);
            o.put("message", message);
            o.put("priority", priority);
            return post("https://ntfy.sh/", "application/json",
                o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(TAG, "ntfy failed", e);
            return false;
        }
    }

    /** Send via Pushover. priority 2 = emergency (critical alert, repeats until acknowledged). */
    public static boolean pushover(String token, String user, String title, String message,
                                   int priority, int expireSeconds) {
        if (token.isEmpty() || user.isEmpty()) return false;
        try {
            StringBuilder b = new StringBuilder();
            add(b, "token", token);
            add(b, "user", user);
            add(b, "title", title);
            add(b, "message", message.length() > 900 ? message.substring(0, 900) : message);
            add(b, "priority", String.valueOf(priority));
            if (priority == 2) {
                add(b, "retry", "30");
                add(b, "expire", String.valueOf(expireSeconds));
                add(b, "sound", "persistent");
            }
            return post("https://api.pushover.net/1/messages.json",
                "application/x-www-form-urlencoded", b.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(TAG, "pushover failed", e);
            return false;
        }
    }

    private static void add(StringBuilder b, String k, String v) throws Exception {
        if (b.length() > 0) b.append('&');
        b.append(k).append('=').append(URLEncoder.encode(v, "UTF-8"));
    }

    private static boolean post(String url, String contentType, byte[] body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(15_000);
            c.setReadTimeout(15_000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType);
            try (OutputStream os = c.getOutputStream()) { os.write(body); }
            int code = c.getResponseCode();
            return code >= 200 && code < 300;
        } finally {
            c.disconnect();
        }
    }
}

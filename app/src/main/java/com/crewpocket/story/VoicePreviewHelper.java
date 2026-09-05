package com.crewpocket.story;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class VoicePreviewHelper {
    private static final String TAG = "VoicePreviewHelper";
    private static final String LIVE_HOST = "generativelanguage.googleapis.com";
    private static final String LIVE_PATH = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent";

    private static WebSocket currentSocket;
    private static AudioTrack previewPlayer;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PreviewCallback {
        void onStart();
        void onDone();
        void onError(String msg);
    }

    public static synchronized void stopPreview() {
        if (currentSocket != null) {
            try { currentSocket.close(1000, "Preview stopped"); } catch (Exception ignored) {}
            currentSocket = null;
        }
        if (previewPlayer != null) {
            try {
                previewPlayer.pause();
                previewPlayer.flush();
                previewPlayer.stop();
                previewPlayer.release();
            } catch (Exception ignored) {}
            previewPlayer = null;
        }
    }

    public static synchronized void previewVoice(final Context context, final String voiceName, final PreviewCallback callback) {
        stopPreview();

        final String apiKey = AppConfig.getGeminiApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            if (callback != null) callback.onError(I18n.t(context, "請先設定 Gemini API Key", "Please set Gemini API Key first"));
            return;
        }

        int bufferSize = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4;
        previewPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, 24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        previewPlayer.play();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = "wss://" + LIVE_HOST + LIVE_PATH + "?key=" + apiKey.trim();
        Request request = new Request.Builder().url(url).build();

        if (callback != null) callback.onStart();

        currentSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                try {
                    JSONObject root = new JSONObject();
                    JSONObject setup = new JSONObject();
                    setup.put("model", "models/gemini-3.1-flash-live-preview");

                    JSONObject generation = new JSONObject();
                    generation.put("responseModalities", new JSONArray().put("AUDIO"));
                    String safeVoice = VoicePersonaDialog.isValidGeminiVoice(voiceName) ? voiceName : "Puck";
                    JSONObject voiceConfig = new JSONObject().put("voiceConfig", new JSONObject().put("prebuiltVoiceConfig", new JSONObject().put("voiceName", safeVoice)));
                    generation.put("speechConfig", voiceConfig);
                    setup.put("generationConfig", generation);

                    setup.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", "你是兒童繪本說書人。請用溫暖親切、生動歡快的語氣說一句簡短的自我介紹（限15字以內）。例如：『嗨！我是說書人，今天想聽什麼精彩故事呢？』"))));

                    root.put("setup", setup);
                    ws.send(root.toString());
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                    stopPreview();
                }
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JSONObject resp = new JSONObject(text);
                    if (resp.has("setupComplete") || resp.has("setup_complete")) {
                        // Send prompt to speak
                        JSONObject clientContent = new JSONObject();
                        JSONArray turns = new JSONArray();
                        JSONObject turn = new JSONObject().put("role", "user")
                                .put("parts", new JSONArray().put(new JSONObject().put("text", "請打個招呼！")));
                        turns.put(turn);
                        clientContent.put("turns", turns);
                        clientContent.put("turnComplete", true);
                        ws.send(new JSONObject().put("clientContent", clientContent).toString());
                        return;
                    }

                    JSONObject server = resp.optJSONObject("serverContent");
                    if (server == null) server = resp.optJSONObject("server_content");
                    if (server != null) {
                        JSONObject modelTurn = server.optJSONObject("modelTurn");
                        if (modelTurn == null) modelTurn = server.optJSONObject("model_turn");
                        if (modelTurn != null) {
                            JSONArray parts = modelTurn.optJSONArray("parts");
                            if (parts != null) {
                                for (int i = 0; i < parts.length(); i++) {
                                    JSONObject p = parts.getJSONObject(i);
                                    JSONObject inline = p.optJSONObject("inlineData");
                                    if (inline == null) inline = p.optJSONObject("inline_data");
                                    if (inline != null && "audio/pcm;rate=24000".equals(inline.optString("mimeType"))) {
                                        byte[] pcm = Base64.decode(inline.getString("data"), Base64.DEFAULT);
                                        if (previewPlayer != null && previewPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                                            previewPlayer.write(pcm, 0, pcm.length);
                                        }
                                    }
                                }
                            }
                        }

                        if (server.optBoolean("turnComplete", server.optBoolean("turn_complete", false))) {
                            mainHandler.postDelayed(new Runnable() {
                                @Override public void run() {
                                    stopPreview();
                                    if (callback != null) callback.onDone();
                                }
                            }, 1200);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "preview error", e);
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        stopPreview();
                        if (callback != null) callback.onError(t != null ? t.getMessage() : "連線失敗");
                    }
                });
            }
        });
    }
}

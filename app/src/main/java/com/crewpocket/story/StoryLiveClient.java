package com.crewpocket.story;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class StoryLiveClient {
    private static final String TAG = "StoryLiveClient";
    private static final String LIVE_HOST = "generativelanguage.googleapis.com";
    private static final String LIVE_PATH = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent";

    public interface Listener {
        void onConnected();
        void onDisconnected(String reason);
        void onError(String error);
        void onPageAdvanced(int newPageIndex, String chapterText);
        void onStoryFinished();
        void onAiSpeechStarted();
        void onAiSpeechEnded();
        void onUserInterrupted();
        void onStatusUpdate(String status);
    }

    private final Context context;
    private final StoryModel story;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int currentPageIndex = 0;
    private volatile boolean running = false;
    private volatile boolean setupReady = false;
    private volatile boolean isPaused = false;
    private volatile boolean isFinished = false;
    private volatile boolean aiSpeaking = false;

    private WebSocket webSocket;
    private OkHttpClient httpClient;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread micThread;
    private Thread playbackThread;

    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private boolean usingNativeOboe = false;
    private final ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();
    private final Object playerLock = new Object();

    // Echo suppression & timing
    private volatile long lastAiAudioPlayTime = 0;

    public StoryLiveClient(Context context, StoryModel story, int startPageIndex, Listener listener) {
        this.context = context;
        this.story = story;
        this.currentPageIndex = Math.max(0, Math.min(startPageIndex, story.pages.size() - 1));
        this.listener = listener;

        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(10, TimeUnit.SECONDS)
                .build();
    }

    public synchronized void start() {
        if (running) return;
        String apiKey = AppConfig.getGeminiApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            notifyError("Gemini API Key 未設定，請先至設定頁面填入！");
            return;
        }

        running = true;
        setupReady = false;
        isFinished = false;
        notifyStatus("正在連線至 Gemini Live 說書引擎…");

        initAudioOutput();

        String url = "wss://" + LIVE_HOST + LIVE_PATH + "?key=" + apiKey.trim();
        Request request = new Request.Builder().url(url).build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                try {
                    String setupJson = buildSetup();
                    if (!ws.send(setupJson)) {
                        notifyError("Setup 指令傳送失敗");
                        return;
                    }
                    notifyStatus("等待 AI 說書人連線就緒…");
                } catch (Exception e) {
                    notifyError("初始化建置失敗: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleIncomingJson(text);
            }

            @Override
            public void onMessage(WebSocket ws, ByteString bytes) {
                handleIncomingJson(bytes.utf8());
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                notifyStatus("連線關閉中: " + reason);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                if (running) {
                    notifyDisconnected("連線已結束：" + reason);
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                String detail = t != null ? t.getMessage() : "未知網路錯誤";
                if (response != null) {
                    detail = "HTTP " + response.code() + " " + response.message() + " (" + detail + ")";
                }
                notifyError("Gemini Live 連線失敗: " + detail);
            }
        });
    }

    public synchronized void pause() {
        isPaused = true;
        flushAudio();
        notifyStatus("故事已暫停");
    }

    public synchronized void resume() {
        if (!isPaused) return;
        isPaused = false;
        notifyStatus("繼續說故事...");
        sendNarrateCurrentPageDirective();
    }

    public synchronized void jumpToPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= story.pages.size()) return;
        this.currentPageIndex = pageIndex;
        flushAudio();
        sendNarrateCurrentPageDirective();
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public boolean isPaused() {
        return isPaused;
    }

    private static String mapToSupportedVoice(String name) {
        if (name == null || name.trim().isEmpty()) return "Puck";
        String v = name.trim();
        for (VoicePersonaDialog.VoiceInfo vi : VoicePersonaDialog.ALL_VOICES) {
            if (vi.name.equalsIgnoreCase(v)) return vi.name;
        }
        return "Puck";
    }

    private int currentTurnSequence = 0;
    private volatile long scheduledPlayEndTimeMs = 0;

    private String buildSetup() throws Exception {
        JSONObject root = new JSONObject();
        JSONObject setup = new JSONObject();
        setup.put("model", "models/gemini-3.1-flash-live-preview");

        JSONObject generation = new JSONObject();
        generation.put("responseModalities", new JSONArray().put("AUDIO"));
        String safeVoice = mapToSupportedVoice(AppConfig.getVoiceName(context));
        generation.put("speechConfig", new JSONObject().put("voiceConfig", new JSONObject().put("prebuiltVoiceConfig", new JSONObject().put("voiceName", safeVoice))));
        setup.put("generationConfig", generation);

        setup.put("contextWindowCompression", new JSONObject().put("slidingWindow", new JSONObject()));
        setup.put("sessionResumption", new JSONObject());
        setup.put("inputAudioTranscription", new JSONObject());
        setup.put("outputAudioTranscription", new JSONObject());

        String storyLang = AppConfig.getStoryLanguage(context);

        JSONObject sysInstruction = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        StringBuilder sb = new StringBuilder();
        sb.append("你是專業、富有情感的兒童故事繪本說書人「波波老師」。\n");
        sb.append("朗讀語言：").append(storyLang).append("。\n");
        sb.append("你正在為小聽眾生動朗讀一本繪本故事：《").append(story.title).append("》。\n");
        sb.append("請以親切生動、富有感情的童趣語氣專注朗讀指定頁面的繪本內容（包含旁白與角色對白演繹）。\n");
        sb.append("請直接生動朗讀該頁內容，不需要額外的問候語或自言自語。\n");

        part.put("text", sb.toString());
        parts.put(part);
        sysInstruction.put("parts", parts);
        setup.put("systemInstruction", sysInstruction);

        root.put("setup", setup);
        return root.toString();
    }

    private void handleIncomingJson(String text) {
        try {
            JSONObject response = new JSONObject(text);

            if (response.has("setupComplete") || response.has("setup_complete")) {
                setupReady = true;
                notifyStatus("🎙️ AI 說書人已就緒，開始說故事！");
                notifyConnected();
                startPlaybackEngine();
                sendNarrateCurrentPageDirective();
                return;
            }

            JSONObject server = response.optJSONObject("serverContent");
            if (server == null) server = response.optJSONObject("server_content");

            if (server != null) {
                JSONObject turn = server.optJSONObject("modelTurn");
                if (turn == null) turn = server.optJSONObject("model_turn");
                if (turn != null && !isPaused) {
                    JSONArray parts = turn.optJSONArray("parts");
                    if (parts != null) {
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject part = parts.getJSONObject(i);
                            JSONObject inline = part.optJSONObject("inlineData");
                            if (inline == null) inline = part.optJSONObject("inline_data");
                            if (inline != null && "audio/pcm;rate=24000".equals(inline.optString("mimeType"))) {
                                byte[] pcm = Base64.decode(inline.getString("data"), Base64.DEFAULT);
                                enqueueAudio(pcm);
                            }
                        }
                    }
                }

                if (server.optBoolean("turnComplete", server.optBoolean("turn_complete", false))) {
                    if (usingNativeOboe) NativeOboeOutput.finishTurn();
                    final int seq = currentTurnSequence;
                    long now = System.currentTimeMillis();
                    long remaining = Math.max(0, scheduledPlayEndTimeMs - now);

                    // 1. Notify AI speech ended when audio finishes physically playing
                    mainHandler.postDelayed(new Runnable() {
                        @Override public void run() {
                            if (seq == currentTurnSequence && listener != null) {
                                listener.onAiSpeechEnded();
                            }
                        }
                    }, remaining);

                    // 2. Schedule auto page turn after audio finishes + 900ms comfortable pause
                    mainHandler.postDelayed(new Runnable() {
                        @Override public void run() {
                            handleAutoAdvance(seq);
                        }
                    }, remaining + 900);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse incoming message", e);
        }
    }

    private synchronized void handleAutoAdvance(final int seq) {
        if (seq != currentTurnSequence || isPaused || !running) return;

        long now = System.currentTimeMillis();
        long remaining = scheduledPlayEndTimeMs - now;
        if (!audioQueue.isEmpty() || remaining > 0) {
            long delay = Math.max(80, Math.min(remaining + 80, 500));
            mainHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    handleAutoAdvance(seq);
                }
            }, delay);
            return;
        }

        if (currentPageIndex + 1 < story.pages.size()) {
            currentPageIndex++;
            notifyStatus("正在朗讀第 " + (currentPageIndex + 1) + " 頁…");
            sendNarrateCurrentPageDirective();
        } else {
            isFinished = true;
            notifyStatus("🎉 故事全篇朗讀完成！");
            mainHandler.post(new Runnable() {
                @Override public void run() {
                    if (listener != null) listener.onStoryFinished();
                }
            });
        }
    }

    private synchronized void sendNarrateCurrentPageDirective() {
        if (webSocket == null || !setupReady || currentPageIndex >= story.pages.size() || isPaused) return;
        currentTurnSequence++;
        final int seq = currentTurnSequence;
        try {
            StoryModel.Page page = story.pages.get(currentPageIndex);
            JSONObject clientContent = new JSONObject();
            JSONArray turns = new JSONArray();
            JSONObject turn = new JSONObject();
            turn.put("role", "user");
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            StringBuilder sb = new StringBuilder();
            sb.append("請以生動富有感情的童趣語氣，朗讀繪本故事《").append(story.title).append("》第 ").append(currentPageIndex + 1).append(" 頁：\n");
            sb.append("旁白：").append(page.text).append("\n");
            if (page.dialogue != null && !page.dialogue.trim().isEmpty()) {
                sb.append("對白 (").append(page.characterName != null && !page.characterName.isEmpty() ? page.characterName : "角色")
                  .append(" / ").append(page.emotion != null && !page.emotion.isEmpty() ? page.emotion : "生動").append("語氣): ")
                  .append(page.dialogue).append("\n");
            }

            part.put("text", sb.toString());
            parts.put(part);
            turn.put("parts", parts);
            turns.put(turn);
            clientContent.put("turns", turns);
            clientContent.put("turnComplete", true);

            JSONObject msg = new JSONObject();
            msg.put("clientContent", clientContent);
            webSocket.send(msg.toString());

            notifyPageAdvanced(currentPageIndex, page.text);

        } catch (Exception ignored) {}
    }

    private void initAudioOutput() {
        String outputMode = AppConfig.getAudioOutput(context);
        usingNativeOboe = NativeOboeOutput.start(outputMode);

        if (!usingNativeOboe) {
            synchronized (playerLock) {
                int streamType = "call".equals(outputMode) ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;
                int bufferSize = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4;
                audioTrack = new AudioTrack(streamType, 24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.play();
            }
        }
    }

    private synchronized void enqueueAudio(byte[] pcm) {
        if (isPaused || pcm == null || pcm.length == 0) return;

        long now = System.currentTimeMillis();
        // 24000 Hz, 16-bit PCM mono = 48000 bytes per second = 48 bytes per millisecond
        long pcmDurationMs = pcm.length / 48L;

        if (scheduledPlayEndTimeMs < now) {
            scheduledPlayEndTimeMs = now + pcmDurationMs;
        } else {
            scheduledPlayEndTimeMs += pcmDurationMs;
        }
        lastAiAudioPlayTime = scheduledPlayEndTimeMs;

        if (usingNativeOboe) {
            NativeOboeOutput.write(pcm);
        } else {
            audioQueue.add(pcm);
        }

        if (!aiSpeaking) {
            aiSpeaking = true;
            mainHandler.post(new Runnable() {
                @Override public void run() {
                    if (listener != null) listener.onAiSpeechStarted();
                }
            });
        }
    }

    private void startPlaybackEngine() {
        if (usingNativeOboe || playbackThread != null) return;
        playbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                while (running) {
                    byte[] chunk = audioQueue.poll();
                    if (chunk != null && chunk.length > 0 && !isPaused) {
                        synchronized (playerLock) {
                            if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                                audioTrack.write(chunk, 0, chunk.length);
                            }
                        }
                    } else {
                        try { Thread.sleep(10); } catch (InterruptedException ignored) { break; }
                    }
                }
            }
        });
        playbackThread.start();
    }

    private synchronized void flushAudio() {
        mainHandler.removeCallbacksAndMessages(null);
        currentTurnSequence++;
        audioQueue.clear();
        scheduledPlayEndTimeMs = 0;

        if (usingNativeOboe) {
            NativeOboeOutput.flush();
        } else {
            synchronized (playerLock) {
                if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        audioTrack.pause();
                        audioTrack.flush();
                        audioTrack.play();
                    } catch (Exception ignored) {}
                }
            }
        }
        aiSpeaking = false;
    }

    private void startMicRecording() {
        if (isRecording.getAndSet(true)) return;
        micThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                int bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2;
                try {
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                    audioRecord.startRecording();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start AudioRecord", e);
                    return;
                }

                byte[] buf = new byte[1280];
                while (isRecording.get() && running) {
                    int read = audioRecord.read(buf, 0, buf.length);
                    if (read > 0 && webSocket != null && setupReady) {
                        double rms = calculateRms(buf, read);
                        // Echo suppression: do not send mic audio if AI just spoke recently and sound is quiet
                        if (System.currentTimeMillis() - lastAiAudioPlayTime < 350 && rms < 500) {
                            continue;
                        }

                        byte[] chunk = (read == buf.length) ? buf : Arrays.copyOf(buf, read);
                        try {
                            JSONObject root = new JSONObject();
                            JSONObject audio = new JSONObject();
                            audio.put("mimeType", "audio/pcm;rate=16000");
                            audio.put("data", Base64.encodeToString(chunk, Base64.NO_WRAP));
                            root.put("realtimeInput", new JSONObject().put("audio", audio));
                            webSocket.send(root.toString());
                        } catch (Exception ignored) {}
                    }
                }
            }
        });
        micThread.start();
    }

    private double calculateRms(byte[] buffer, int length) {
        long sum = 0;
        int samples = length / 2;
        for (int i = 0; i < samples; i++) {
            short sample = (short) ((buffer[i * 2] & 0xFF) | (buffer[i * 2 + 1] << 8));
            sum += sample * sample;
        }
        return Math.sqrt((double) sum / Math.max(1, samples));
    }

    public synchronized void stop() {
        running = false;
        setupReady = false;
        isRecording.set(false);
        mainHandler.removeCallbacksAndMessages(null);
        flushAudio();

        if (micThread != null) {
            micThread.interrupt();
            micThread = null;
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        if (audioRecord != null) {
            try { audioRecord.stop(); audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }
        if (usingNativeOboe) {
            NativeOboeOutput.stop();
            usingNativeOboe = false;
        }
        synchronized (playerLock) {
            if (audioTrack != null) {
                try { audioTrack.stop(); audioTrack.release(); } catch (Exception ignored) {}
                audioTrack = null;
            }
        }
        if (webSocket != null) {
            try { webSocket.close(1000, "User stopped"); } catch (Exception ignored) {}
            webSocket = null;
        }
    }

    private void notifyConnected() {
        mainHandler.post(new Runnable() {
            @Override public void run() { if (listener != null) listener.onConnected(); }
        });
    }

    private void notifyDisconnected(final String reason) {
        mainHandler.post(new Runnable() {
            @Override public void run() { if (listener != null) listener.onDisconnected(reason); }
        });
    }

    private void notifyError(final String error) {
        mainHandler.post(new Runnable() {
            @Override public void run() { if (listener != null) listener.onError(error); }
        });
    }

    private void notifyPageAdvanced(final int pageIndex, final String chapterText) {
        mainHandler.post(new Runnable() {
            @Override public void run() { if (listener != null) listener.onPageAdvanced(pageIndex, chapterText); }
        });
    }

    private void notifyStatus(final String status) {
        mainHandler.post(new Runnable() {
            @Override public void run() { if (listener != null) listener.onStatusUpdate(status); }
        });
    }
}

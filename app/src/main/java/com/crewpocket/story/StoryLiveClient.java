package com.crewpocket.story;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
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

/**
 * Gemini Live storyteller client.
 *
 * 1002 interaction model:
 * - Microphone is OFF by default.
 * - The child explicitly presses "我要說話" to start a manual turn.
 * - Automatic server VAD is disabled, so room noise cannot barge in.
 * - beginUserTurn() sends activityStart and starts PCM streaming.
 * - endUserTurn() sends activityEnd and stops PCM streaming.
 * - After Gemini answers the child, narration resumes on the same page.
 */
public class StoryLiveClient {
    private static final String TAG = "StoryLiveClient";
    private static final String LIVE_HOST = "generativelanguage.googleapis.com";
    private static final String LIVE_PATH =
            "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent";

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

    // Manual child-turn state.
    private volatile boolean userTurnActive = false;
    private volatile boolean awaitingUserResponse = false;

    private WebSocket webSocket;
    private OkHttpClient httpClient;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread micThread;
    private Thread playbackThread;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private boolean usingNativeOboe = false;
    private final ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();
    private final Object playerLock = new Object();

    private int currentTurnSequence = 0;
    private volatile long scheduledPlayEndTimeMs = 0;

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
        userTurnActive = false;
        awaitingUserResponse = false;

        notifyStatus("正在連線至 Gemini Live 說書引擎…");
        acquireLocks();
        initAudioOutput();

        String url = "wss://" + LIVE_HOST + LIVE_PATH + "?key=" + apiKey.trim();
        Request request = new Request.Builder().url(url).build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                try {
                    if (!ws.send(buildSetup())) {
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
                if (running) notifyDisconnected("連線已結束：" + reason);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                String detail = t != null ? t.getMessage() : "未知網路錯誤";
                if (response != null) {
                    detail = "HTTP " + response.code() + " " + response.message()
                            + " (" + detail + ")";
                }
                notifyError("Gemini Live 連線失敗: " + detail);
            }
        });
    }

    public synchronized void pause() {
        if (userTurnActive) {
            endUserTurn();
        }
        awaitingUserResponse = false;
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

        stopMicRecordingInternal();
        userTurnActive = false;
        awaitingUserResponse = false;

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

    public boolean isUserSpeaking() {
        return userTurnActive;
    }

    public boolean isAwaitingUserResponse() {
        return awaitingUserResponse;
    }

    /**
     * Called by the UI when the child explicitly taps "我要說話".
     * Returns false when a manual turn cannot currently be started.
     */
    public synchronized boolean beginUserTurn() {
        if (!running || !setupReady || isPaused || isFinished) return false;
        if (userTurnActive || awaitingUserResponse) return false;

        // Stop narration locally first. activityStart also tells Gemini that
        // the user intentionally interrupts the current model response.
        flushAudio();
        userTurnActive = true;
        awaitingUserResponse = true;

        if (!sendRealtimeSignal("activityStart")) {
            userTurnActive = false;
            awaitingUserResponse = false;
            notifyError("無法開始語音互動，請再試一次");
            return false;
        }

        startMicRecording();
        notifyUserInterrupted();
        notifyStatus("正在聽你說話…");
        return true;
    }

    /**
     * Called by the UI when the child taps "說完了".
     * The microphone closes immediately and activityEnd finalizes the turn.
     */
    public synchronized boolean endUserTurn() {
        if (!userTurnActive) return false;

        stopMicRecordingInternal();
        userTurnActive = false;

        boolean sent = sendRealtimeSignal("activityEnd");
        if (sent) {
            notifyStatus("波波老師正在回答…");
        } else {
            awaitingUserResponse = false;
            notifyError("語音送出失敗，請再試一次");
        }
        return sent;
    }

    private boolean sendRealtimeSignal(String field) {
        if (webSocket == null || !setupReady) return false;
        try {
            JSONObject realtimeInput = new JSONObject();
            realtimeInput.put(field, new JSONObject());
            JSONObject root = new JSONObject();
            root.put("realtimeInput", realtimeInput);
            return webSocket.send(root.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send " + field, e);
            return false;
        }
    }

    private static String mapToSupportedVoice(String name) {
        if (name == null || name.trim().isEmpty()) return "Puck";
        String v = name.trim();
        for (VoicePersonaDialog.VoiceInfo vi : VoicePersonaDialog.ALL_VOICES) {
            if (vi.name.equalsIgnoreCase(v)) return vi.name;
        }
        return "Puck";
    }

    private String buildSetup() throws Exception {
        JSONObject root = new JSONObject();
        JSONObject setup = new JSONObject();
        setup.put("model", "models/gemini-3.1-flash-live-preview");

        JSONObject generation = new JSONObject();
        generation.put("responseModalities", new JSONArray().put("AUDIO"));
        String safeVoice = mapToSupportedVoice(AppConfig.getVoiceName(context));
        generation.put(
                "speechConfig",
                new JSONObject().put(
                        "voiceConfig",
                        new JSONObject().put(
                                "prebuiltVoiceConfig",
                                new JSONObject().put("voiceName", safeVoice)
                        )
                )
        );
        setup.put("generationConfig", generation);

        // 1002: true push-to-talk / manual activity boundaries.
        // Room noise cannot start a user turn because server-side automatic
        // activity detection is disabled.
        JSONObject realtimeInputConfig = new JSONObject();
        realtimeInputConfig.put(
                "automaticActivityDetection",
                new JSONObject().put("disabled", true)
        );
        realtimeInputConfig.put("activityHandling", "START_OF_ACTIVITY_INTERRUPTS");
        setup.put("realtimeInputConfig", realtimeInputConfig);

        setup.put("contextWindowCompression",
                new JSONObject().put("slidingWindow", new JSONObject()));
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
        sb.append("你正在為小聽眾生動朗讀一本繪本故事：《")
                .append(story.title).append("》。\n");
        sb.append("請以親切生動、富有感情的童趣語氣專注朗讀指定頁面的繪本內容")
                .append("（包含旁白與角色對白演繹）。\n");
        sb.append("當小朋友主動按下說話按鈕並提出問題時，先簡短、自然、適齡地回答，")
                .append("不要把回答變成長篇教學，也不要自行要求小朋友一直回答問題。\n");
        sb.append("回答完成後等待系統要求你回到故事，再自然接回被打斷的頁面。\n");
        sb.append("一般朗讀時請直接生動朗讀內容，不需要額外問候或自言自語。\n");

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
            if (server == null) return;

            boolean interrupted = server.optBoolean(
                    "interrupted",
                    server.optBoolean("is_interrupted", false)
            );

            if (interrupted) {
                // Gemini acknowledged an explicit activityStart interruption.
                // Do not treat the interrupted narration as a completed child response.
                flushAudio();
            }

            JSONObject turn = server.optJSONObject("modelTurn");
            if (turn == null) turn = server.optJSONObject("model_turn");

            if (turn != null && !isPaused && !userTurnActive) {
                JSONArray parts = turn.optJSONArray("parts");
                if (parts != null) {
                    for (int i = 0; i < parts.length(); i++) {
                        JSONObject part = parts.getJSONObject(i);
                        JSONObject inline = part.optJSONObject("inlineData");
                        if (inline == null) inline = part.optJSONObject("inline_data");

                        if (inline != null
                                && "audio/pcm;rate=24000".equals(inline.optString("mimeType"))) {
                            byte[] pcm = Base64.decode(
                                    inline.getString("data"),
                                    Base64.DEFAULT
                            );
                            enqueueAudio(pcm);
                        }
                    }
                }
            }

            boolean turnComplete = server.optBoolean(
                    "turnComplete",
                    server.optBoolean("turn_complete", false)
            );

            if (!turnComplete || interrupted || userTurnActive) return;

            if (usingNativeOboe) NativeOboeOutput.finishTurn();

            final int seq = currentTurnSequence;
            final boolean childAnswerTurn = awaitingUserResponse;
            long now = System.currentTimeMillis();
            long remaining = Math.max(0, scheduledPlayEndTimeMs - now);

            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (seq == currentTurnSequence && listener != null) {
                        listener.onAiSpeechEnded();
                    }
                }
            }, remaining);

            if (childAnswerTurn) {
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finishChildAnswerAndResume(seq);
                    }
                }, remaining + 650);
            } else {
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handleAutoAdvance(seq);
                    }
                }, remaining + 900);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse incoming message", e);
        }
    }

    private synchronized void finishChildAnswerAndResume(int seq) {
        if (seq != currentTurnSequence || !running || isPaused) return;
        if (!awaitingUserResponse) return;

        awaitingUserResponse = false;
        notifyStatus("回到故事…");
        sendResumeAfterConversationDirective();
    }

    private synchronized void handleAutoAdvance(final int seq) {
        if (seq != currentTurnSequence || isPaused || !running) return;
        if (userTurnActive || awaitingUserResponse) return;

        long now = System.currentTimeMillis();
        long remaining = scheduledPlayEndTimeMs - now;
        if (!audioQueue.isEmpty() || remaining > 0) {
            long delay = Math.max(80, Math.min(remaining + 80, 500));
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
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
                @Override
                public void run() {
                    if (listener != null) listener.onStoryFinished();
                }
            });
        }
    }

    private synchronized void sendNarrateCurrentPageDirective() {
        if (webSocket == null || !setupReady
                || currentPageIndex >= story.pages.size()
                || isPaused || userTurnActive || awaitingUserResponse) {
            return;
        }

        currentTurnSequence++;

        try {
            StoryModel.Page page = story.pages.get(currentPageIndex);
            StringBuilder sb = new StringBuilder();
            sb.append("請以生動富有感情的童趣語氣，朗讀繪本故事《")
                    .append(story.title).append("》第 ")
                    .append(currentPageIndex + 1).append(" 頁：\n");
            appendPageContent(sb, page);

            sendClientTextTurn(sb.toString());
            notifyPageAdvanced(currentPageIndex, page.text);
        } catch (Exception e) {
            Log.e(TAG, "Failed to narrate current page", e);
        }
    }

    private synchronized void sendResumeAfterConversationDirective() {
        if (webSocket == null || !setupReady
                || currentPageIndex >= story.pages.size()
                || isPaused || userTurnActive || awaitingUserResponse) {
            return;
        }

        currentTurnSequence++;

        try {
            StoryModel.Page page = story.pages.get(currentPageIndex);
            StringBuilder sb = new StringBuilder();
            sb.append("剛剛小朋友在第 ")
                    .append(currentPageIndex + 1)
                    .append(" 頁主動插話，你已經回答完了。")
                    .append("現在請自然回到故事，從剛才被打斷的位置附近接著說，")
                    .append("不要重新從整頁開頭朗讀，也不要再次回答剛才的問題。\n")
                    .append("目前頁面內容供你銜接：\n");
            appendPageContent(sb, page);

            sendClientTextTurn(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to resume narration after child answer", e);
        }
    }

    private void appendPageContent(StringBuilder sb, StoryModel.Page page) {
        sb.append("旁白：").append(page.text).append("\n");
        if (page.dialogue != null && !page.dialogue.trim().isEmpty()) {
            sb.append("對白 (")
                    .append(page.characterName != null && !page.characterName.isEmpty()
                            ? page.characterName : "角色")
                    .append(" / ")
                    .append(page.emotion != null && !page.emotion.isEmpty()
                            ? page.emotion : "生動")
                    .append("語氣): ")
                    .append(page.dialogue)
                    .append("\n");
        }
    }

    private void sendClientTextTurn(String text) throws Exception {
        JSONObject clientContent = new JSONObject();
        JSONArray turns = new JSONArray();
        JSONObject turn = new JSONObject();
        turn.put("role", "user");

        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", text));
        turn.put("parts", parts);
        turns.put(turn);

        clientContent.put("turns", turns);
        clientContent.put("turnComplete", true);

        JSONObject msg = new JSONObject();
        msg.put("clientContent", clientContent);
        webSocket.send(msg.toString());
    }

    private void initAudioOutput() {
        String outputMode = AppConfig.getAudioOutput(context);
        usingNativeOboe = NativeOboeOutput.start(outputMode);

        if (!usingNativeOboe) {
            synchronized (playerLock) {
                int streamType = "call".equals(outputMode)
                        ? AudioManager.STREAM_VOICE_CALL
                        : AudioManager.STREAM_MUSIC;

                int bufferSize = AudioTrack.getMinBufferSize(
                        24000,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                ) * 4;

                audioTrack = new AudioTrack(
                        streamType,
                        24000,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                );
                audioTrack.play();
            }
        }
    }

    private synchronized void enqueueAudio(byte[] pcm) {
        if (isPaused || userTurnActive || pcm == null || pcm.length == 0) return;

        long now = System.currentTimeMillis();
        // 24kHz, 16-bit PCM mono = 48 bytes/ms.
        long pcmDurationMs = pcm.length / 48L;

        if (scheduledPlayEndTimeMs < now) {
            scheduledPlayEndTimeMs = now + pcmDurationMs;
        } else {
            scheduledPlayEndTimeMs += pcmDurationMs;
        }

        if (usingNativeOboe) {
            NativeOboeOutput.write(pcm);
        } else {
            audioQueue.add(pcm);
        }

        if (!aiSpeaking) {
            aiSpeaking = true;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
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
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_AUDIO
                );

                while (running) {
                    byte[] chunk = audioQueue.poll();
                    if (chunk != null && chunk.length > 0
                            && !isPaused && !userTurnActive) {
                        synchronized (playerLock) {
                            if (audioTrack != null
                                    && audioTrack.getPlayState()
                                    == AudioTrack.PLAYSTATE_PLAYING) {
                                audioTrack.write(chunk, 0, chunk.length);
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                            break;
                        }
                    }
                }
            }
        }, "crew-story-playback");

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
                if (audioTrack != null
                        && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        audioTrack.pause();
                        audioTrack.flush();
                        audioTrack.play();
                    } catch (Exception ignored) {
                    }
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
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_AUDIO
                );

                int minBuffer = AudioRecord.getMinBufferSize(
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                );
                int bufferSize = Math.max(minBuffer * 2, 4096);

                try {
                    audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                            16000,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize
                    );
                    audioRecord.startRecording();

                    byte[] buf = new byte[1600]; // ~50ms of 16kHz PCM16 mono.

                    while (isRecording.get() && running && userTurnActive) {
                        int read = audioRecord.read(buf, 0, buf.length);
                        if (read <= 0 || webSocket == null || !setupReady) continue;

                        byte[] chunk = read == buf.length
                                ? buf
                                : Arrays.copyOf(buf, read);

                        JSONObject audio = new JSONObject();
                        audio.put("mimeType", "audio/pcm;rate=16000");
                        audio.put(
                                "data",
                                Base64.encodeToString(chunk, Base64.NO_WRAP)
                        );

                        JSONObject root = new JSONObject();
                        root.put(
                                "realtimeInput",
                                new JSONObject().put("audio", audio)
                        );
                        webSocket.send(root.toString());
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "RECORD_AUDIO permission missing", e);
                    notifyError("無法使用麥克風，請確認已允許錄音權限");
                    userTurnActive = false;
                    awaitingUserResponse = false;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start AudioRecord", e);
                    notifyError("麥克風啟動失敗，請再試一次");
                    userTurnActive = false;
                    awaitingUserResponse = false;
                } finally {
                    releaseAudioRecord();
                    isRecording.set(false);
                }
            }
        }, "crew-story-mic");

        micThread.start();
    }

    private void stopMicRecordingInternal() {
        isRecording.set(false);

        AudioRecord record = audioRecord;
        if (record != null) {
            try {
                record.stop();
            } catch (Exception ignored) {
            }
        }

        Thread thread = micThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void releaseAudioRecord() {
        AudioRecord record = audioRecord;
        audioRecord = null;

        if (record != null) {
            try {
                if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop();
                }
            } catch (Exception ignored) {
            }

            try {
                record.release();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void stop() {
        running = false;
        setupReady = false;
        userTurnActive = false;
        awaitingUserResponse = false;

        stopMicRecordingInternal();
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

        releaseAudioRecord();

        if (usingNativeOboe) {
            NativeOboeOutput.stop();
            usingNativeOboe = false;
        }

        synchronized (playerLock) {
            if (audioTrack != null) {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {
                }
                audioTrack = null;
            }
        }

        if (webSocket != null) {
            try {
                webSocket.close(1000, "User stopped");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }

        releaseLocks();
    }

    private void acquireLocks() {
        try {
            PowerManager pm =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && wakeLock == null) {
                wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "CrewStory:LiveClientWakeLock"
                );
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(4 * 60 * 60 * 1000L);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire WakeLock in LiveClient", e);
        }

        try {
            WifiManager wm =
                    (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);

            if (wm != null && wifiLock == null) {
                wifiLock = wm.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "CrewStory:LiveClientWifiLock"
                );
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire WifiLock in LiveClient", e);
        }
    }

    private void releaseLocks() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception ignored) {
        }

        try {
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
                wifiLock = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void notifyConnected() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onConnected();
            }
        });
    }

    private void notifyDisconnected(final String reason) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onDisconnected(reason);
            }
        });
    }

    private void notifyError(final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onError(error);
            }
        });
    }

    private void notifyPageAdvanced(final int pageIndex, final String chapterText) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onPageAdvanced(pageIndex, chapterText);
                }
            }
        });
    }

    private void notifyUserInterrupted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onUserInterrupted();
            }
        });
    }

    private void notifyStatus(final String status) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onStatusUpdate(status);
            }
        });
    }
}

package com.crewpocket.story;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class StoryPlaybackService extends Service {
    private static final String TAG = "StoryPlaybackService";
    private static final String CHANNEL_ID = "crew_story_playback_channel";
    private static final int NOTIFICATION_ID = 2001;

    public static final String ACTION_START = "ACTION_START_STORY_PLAYBACK";
    public static final String ACTION_UPDATE = "ACTION_UPDATE_STORY_PROGRESS";
    public static final String ACTION_STOP = "ACTION_STOP_STORY_PLAYBACK";

    public static final String EXTRA_STORY_ID = "EXTRA_STORY_ID";
    public static final String EXTRA_STORY_TITLE = "EXTRA_STORY_TITLE";
    public static final String EXTRA_STORY_EMOJI = "EXTRA_STORY_EMOJI";
    public static final String EXTRA_CURRENT_PAGE = "EXTRA_CURRENT_PAGE";
    public static final String EXTRA_TOTAL_PAGES = "EXTRA_TOTAL_PAGES";

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private String storyId = "";
    private String storyTitle = "";
    private String storyEmoji = "📖";
    private int currentPage = 0;
    private int totalPages = 1;

    public static void start(Context context, StoryModel story, int currentPage) {
        if (context == null || story == null) return;
        Intent intent = new Intent(context, StoryPlaybackService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_STORY_ID, story.id);
        intent.putExtra(EXTRA_STORY_TITLE, story.title);
        intent.putExtra(EXTRA_STORY_EMOJI, story.coverEmoji);
        intent.putExtra(EXTRA_CURRENT_PAGE, currentPage);
        intent.putExtra(EXTRA_TOTAL_PAGES, story.pages.size());

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    java.lang.reflect.Method m = context.getClass().getMethod("startForegroundService", Intent.class);
                    m.invoke(context, intent);
                    return;
                } catch (Exception ignored) {}
            }
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start StoryPlaybackService", e);
        }
    }

    public static void updateProgress(Context context, int currentPage, int totalPages) {
        if (context == null) return;
        Intent intent = new Intent(context, StoryPlaybackService.class);
        intent.setAction(ACTION_UPDATE);
        intent.putExtra(EXTRA_CURRENT_PAGE, currentPage);
        intent.putExtra(EXTRA_TOTAL_PAGES, totalPages);
        try {
            context.startService(intent);
        } catch (Exception ignored) {}
    }

    public static void stop(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, StoryPlaybackService.class);
        intent.setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireLocks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            storyId = intent.getStringExtra(EXTRA_STORY_ID);
            storyTitle = intent.getStringExtra(EXTRA_STORY_TITLE);
            storyEmoji = intent.getStringExtra(EXTRA_STORY_EMOJI);
            if (storyEmoji == null || storyEmoji.isEmpty()) storyEmoji = "📖";
            currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, 0);
            totalPages = intent.getIntExtra(EXTRA_TOTAL_PAGES, 1);

            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    java.lang.reflect.Method sf = Service.class.getMethod("startForeground", int.class, Notification.class, int.class);
                    sf.invoke(this, NOTIFICATION_ID, notification, 2 /* FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK */);
                    return START_STICKY;
                } catch (Exception ignored) {}
            }
            startForeground(NOTIFICATION_ID, notification);
        } else if (ACTION_UPDATE.equals(action)) {
            currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, currentPage);
            totalPages = intent.getIntExtra(EXTRA_TOTAL_PAGES, totalPages);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIFICATION_ID, buildNotification());
            }
        } else if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                Class<?> channelClass = Class.forName("android.app.NotificationChannel");
                java.lang.reflect.Constructor<?> constructor = channelClass.getConstructor(String.class, CharSequence.class, int.class);
                Object channel = constructor.newInstance(CHANNEL_ID, "Crew Story · 阿奇", 2 /* IMPORTANCE_LOW */);
                java.lang.reflect.Method setDesc = channelClass.getMethod("setDescription", String.class);
                setDesc.invoke(channel, "阿奇說故事的背景播放");

                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    java.lang.reflect.Method createChan = nm.getClass().getMethod("createNotificationChannel", channelClass);
                    createChan.invoke(nm, channel);
                }
            } catch (Exception ignored) {}
        }
    }

    private Notification buildNotification() {
        Intent contentIntent = new Intent(this, StoryPlayerActivity.class);
        if (storyId != null) {
            contentIntent.putExtra("EXTRA_STORY_ID", storyId);
        }
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= 0x04000000; // PendingIntent.FLAG_IMMUTABLE
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, flags);

        String title = (storyEmoji != null ? storyEmoji + " " : "") + (storyTitle != null ? storyTitle : "繪本說書中");
        String content = "第 " + (currentPage + 1) + " / " + totalPages + " 頁 · ✨ 阿奇說故事中";

        Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                java.lang.reflect.Method setChannelId = builder.getClass().getMethod("setChannelId", String.class);
                setChannelId.invoke(builder, CHANNEL_ID);
            } catch (Exception ignored) {}
        }

        builder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            builder.setCategory(Notification.CATEGORY_TRANSPORT);
        }

        return builder.build();
    }

    private void acquireLocks() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CrewStory:PlaybackWakeLock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(4 * 60 * 60 * 1000L); // Max 4 hours safe timeout
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire WakeLock", e);
        }

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wifiLock == null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "CrewStory:PlaybackWifiLock");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire WifiLock", e);
        }
    }

    private void releaseLocks() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception ignored) {}

        try {
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
                wifiLock = null;
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        releaseLocks();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

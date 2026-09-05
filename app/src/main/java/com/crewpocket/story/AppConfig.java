package com.crewpocket.story;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    public static final String PREFS_NAME = "crew_story_config";
    public static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    public static final String KEY_VOICE_NAME = "story_voice_name";
    public static final String KEY_STORY_LANGUAGE = "story_language";
    public static final String KEY_STORY_STYLE = "story_style";
    public static final String KEY_AUDIO_OUTPUT = "audio_output"; // media, call
    public static final String KEY_UI_LANGUAGE = "ui_language"; // zh, en

    public static final String DEFAULT_VOICE = "Puck"; // Puck (童趣歡快), Aoede, Kore, Fenrir, Charon
    public static final String DEFAULT_STORY_LANG = "zh-TW"; // zh-TW, en, ja, ko, fr, de, es
    public static final String DEFAULT_STYLE = "bedtime"; // bedtime, adventure, fable, scifi, emotional
    public static final String DEFAULT_UI_LANG = "zh";

    public static final String[] SUPPORTED_STORY_LANGS = {
            "zh-TW", "en", "ja", "ko", "fr", "de", "es"
    };

    public static final String[] SUPPORTED_VOICES = {
            // Female (15)
            "Kore", "Aoede", "Leda", "Callirrhoe", "Autonoe", "Despina", "Erinome", "Laomedeia", "Achernar", "Vindemiatrix", "Sadachbia", "Sulafat", "Algieba", "Pulcherrima", "Achird",
            // Male (15)
            "Puck", "Charon", "Fenrir", "Orus", "Zephyr", "Enceladus", "Iapetus", "Umbriel", "Algenib", "Rasalgethi", "Alnilam", "Schedar", "Gacrux", "Zubenelgenubi", "Sadaltager"
    };

    public static final String KEY_IMAGEN_UNAVAILABLE = "imagen_unavailable";

    public static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getGeminiApiKey(Context context) {
        if (context == null) return "";
        String key = getPrefs(context).getString(KEY_GEMINI_API_KEY, "");
        if (key.isEmpty()) {
            key = context.getSharedPreferences("crew_teacher_config", Context.MODE_PRIVATE).getString("gemini_api_key", "");
        }
        if (key.isEmpty()) {
            key = context.getSharedPreferences("crew_helper_config", Context.MODE_PRIVATE).getString("gemini_api_key", "");
        }
        if (key.isEmpty()) {
            key = context.getSharedPreferences("crew_native_live", Context.MODE_PRIVATE).getString("gemini_live_key", "");
        }
        return key.trim();
    }

    public static void setGeminiApiKey(Context context, String key) {
        if (context == null) return;
        getPrefs(context).edit()
                .putString(KEY_GEMINI_API_KEY, key != null ? key.trim() : "")
                .putBoolean(KEY_IMAGEN_UNAVAILABLE, false)
                .apply();
    }

    public static boolean isImagenUnavailable(Context context) {
        if (context == null) return false;
        return getPrefs(context).getBoolean(KEY_IMAGEN_UNAVAILABLE, false);
    }

    public static void setImagenUnavailable(Context context, boolean unavailable) {
        if (context == null) return;
        getPrefs(context).edit().putBoolean(KEY_IMAGEN_UNAVAILABLE, unavailable).apply();
    }

    public static String getUiLanguage(Context context) {
        if (context == null) return DEFAULT_UI_LANG;
        return getPrefs(context).getString(KEY_UI_LANGUAGE, DEFAULT_UI_LANG);
    }

    public static void setUiLanguage(Context context, String lang) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_UI_LANGUAGE, lang).apply();
    }

    public static String getVoiceName(Context context) {
        if (context == null) return DEFAULT_VOICE;
        return getPrefs(context).getString(KEY_VOICE_NAME, DEFAULT_VOICE);
    }

    public static void setVoiceName(Context context, String voice) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_VOICE_NAME, voice).apply();
    }

    public static String getStoryLanguage(Context context) {
        if (context == null) return DEFAULT_STORY_LANG;
        return getPrefs(context).getString(KEY_STORY_LANGUAGE, DEFAULT_STORY_LANG);
    }

    public static void setStoryLanguage(Context context, String lang) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_STORY_LANGUAGE, lang).apply();
    }

    public static String getStoryLanguageDisplayName(String langCode) {
        if ("en".equalsIgnoreCase(langCode)) return "🇺🇸 English (英文)";
        if ("ja".equalsIgnoreCase(langCode)) return "🇯🇵 日本語 (日文)";
        if ("ko".equalsIgnoreCase(langCode)) return "🇰🇷 한국어 (韓文)";
        if ("fr".equalsIgnoreCase(langCode)) return "🇫🇷 Français (法文)";
        if ("de".equalsIgnoreCase(langCode)) return "🇩🇪 Deutsch (德文)";
        if ("es".equalsIgnoreCase(langCode)) return "🇪🇸 Español (西文)";
        return "🇹🇼 繁體中文";
    }

    public static String getStoryStyle(Context context) {
        if (context == null) return DEFAULT_STYLE;
        return getPrefs(context).getString(KEY_STORY_STYLE, DEFAULT_STYLE);
    }

    public static void setStoryStyle(Context context, String style) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_STORY_STYLE, style).apply();
    }

    public static String getAudioOutput(Context context) {
        if (context == null) return "media";
        return getPrefs(context).getString(KEY_AUDIO_OUTPUT, "media");
    }

    public static void setAudioOutput(Context context, String output) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_AUDIO_OUTPUT, "call".equalsIgnoreCase(output) ? "call" : "media").apply();
    }
}

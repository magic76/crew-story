package com.crewpocket.story;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class VoicePersonaDialog {

    private static int currentVoiceFilterTab = 0; // 0: All, 1: Female, 2: Male
    private static TextToSpeech previewTts;

    public static class VoiceInfo {
        public final String name;
        public final boolean isFemale;
        public final String zhDesc;
        public final String enDesc;
        public final float pitch;

        public VoiceInfo(String name, boolean isFemale, String zhDesc, String enDesc, float pitch) {
            this.name = name;
            this.isFemale = isFemale;
            this.zhDesc = zhDesc;
            this.enDesc = enDesc;
            this.pitch = pitch;
        }
    }

    // Google Gemini Live 30 Official Supported Prebuilt Voices
    public static final VoiceInfo[] ALL_VOICES = new VoiceInfo[]{
            // Female (15)
            new VoiceInfo("Kore", true, "自然放鬆 · 溫柔沉穩", "Relaxed & Natural · Gentle", 1.15f),
            new VoiceInfo("Aoede", true, "清澈優雅 · 溫柔細膩", "Breathy & Gentle · Fairy Tale", 1.18f),
            new VoiceInfo("Leda", true, "年輕活潑 · 朝氣蓬勃", "Youthful & Bright", 1.25f),
            new VoiceInfo("Callirrhoe", true, "輕快悠閒 · 甜美清晰", "Easygoing & Sweet", 1.20f),
            new VoiceInfo("Autonoe", true, "明亮靈動 · 陽光開朗", "Bright & Lively", 1.22f),
            new VoiceInfo("Despina", true, "柔順舒適 · 抑揚頓挫", "Smooth & Fluent", 1.12f),
            new VoiceInfo("Erinome", true, "清新純淨 · 清楚動聽", "Clear & Melodic", 1.16f),
            new VoiceInfo("Laomedeia", true, "活潑俏皮 · 靈巧生動", "Cheerful & Playful", 1.26f),
            new VoiceInfo("Achernar", true, "柔和舒緩 · 靜謐溫暖", "Soft & Soothing", 1.05f),
            new VoiceInfo("Vindemiatrix", true, "溫柔親切 · 慈祥包容", "Gentle & Kind", 1.08f),
            new VoiceInfo("Sadachbia", true, "生動鮮明 · 富有情感", "Vivid & Expressive", 1.14f),
            new VoiceInfo("Sulafat", true, "溫暖安撫 · 睡前繪本", "Warm & Bedtime", 1.02f),
            new VoiceInfo("Algieba", true, "圓潤甜美 · 娓娓道來", "Rounded & Sweet", 1.10f),
            new VoiceInfo("Pulcherrima", true, "優雅前進 · 堅定自信", "Luminous & Elegant", 1.13f),
            new VoiceInfo("Achird", true, "友善鄰家 · 隨和親切", "Friendly & Approachable", 1.18f),

            // Male (15)
            new VoiceInfo("Puck", false, "童趣歡快 · 預設推薦", "Playful & Cheerful · Recommended", 0.95f),
            new VoiceInfo("Charon", false, "沉穩專業 · 磁性冷靜", "Deep & Confident", 0.80f),
            new VoiceInfo("Fenrir", false, "低沉冒險 · 威嚴有力", "Adventurous & Powerful", 0.75f),
            new VoiceInfo("Orus", false, "沉著清晰 · 條理分明", "Firm & Articulate", 0.88f),
            new VoiceInfo("Zephyr", false, "溫暖明亮 · 撫慰人心", "Warm & Bright", 0.92f),
            new VoiceInfo("Enceladus", false, "氣聲磁性 · 溫暖陪伴", "Breathy & Warm", 0.85f),
            new VoiceInfo("Iapetus", false, "踏實清晰 · 值得信賴", "Grounded & Clear", 0.82f),
            new VoiceInfo("Umbriel", false, "輕鬆休閒 · 幽默自在", "Easygoing & Calm", 0.88f),
            new VoiceInfo("Algenib", false, "沙啞磁性 · 歷練說書", "Husky & Storyteller", 0.78f),
            new VoiceInfo("Rasalgethi", false, "知識博學 · 沉穩說理", "Wise & Articulate", 0.86f),
            new VoiceInfo("Alnilam", false, "堅定沉著 · 宏亮有力", "Resonant & Firm", 0.76f),
            new VoiceInfo("Schedar", false, "平穩安定 · 故事說書", "Steady & Measured", 0.84f),
            new VoiceInfo("Gacrux", false, "成熟醇厚 · 威嚴可靠", "Mature & Rich", 0.72f),
            new VoiceInfo("Zubenelgenubi", false, "隨和親近 · 幽默自然", "Conversational & Warm", 0.90f),
            new VoiceInfo("Sadaltager", false, "博學智慧 · 娓娓道來", "Wise & Engaging", 0.86f)
    };

    public static boolean isValidGeminiVoice(String voiceName) {
        if (voiceName == null || voiceName.trim().isEmpty()) return false;
        String v = voiceName.trim();
        for (VoiceInfo vi : ALL_VOICES) {
            if (vi.name.equalsIgnoreCase(v)) return true;
        }
        return false;
    }

    public static String getVoiceDisplayName(Context context, String voiceName) {
        if (voiceName == null || voiceName.isEmpty()) voiceName = AppConfig.DEFAULT_VOICE;
        boolean en = I18n.isEnglish(context);
        for (VoiceInfo v : ALL_VOICES) {
            if (v.name.equalsIgnoreCase(voiceName)) {
                return (v.isFemale ? "👩 " : "👨 ") + v.name + " (" + (en ? v.enDesc : v.zhDesc) + ")";
            }
        }
        return "🎙️ " + voiceName;
    }

    private static void playAudition(final Context context, final VoiceInfo voice) {
        if (voice == null) return;
        if (previewTts == null) {
            previewTts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        speakVoiceSample(context, voice);
                    }
                }
            });
        } else {
            speakVoiceSample(context, voice);
        }
    }

    private static void speakVoiceSample(Context context, VoiceInfo voice) {
        if (previewTts == null || voice == null) return;
        try {
            previewTts.stop();
            boolean isFemale = voice.isFemale;
            boolean isEng = I18n.isEnglish(context);

            // 1. Try to match system TTS Voice by gender & language
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    java.util.Set<android.speech.tts.Voice> voices = previewTts.getVoices();
                    if (voices != null) {
                        String targetLang = isEng ? "en" : "zh";
                        for (android.speech.tts.Voice v : voices) {
                            if (v.getLocale() != null && v.getLocale().getLanguage().startsWith(targetLang)) {
                                String vName = v.getName().toLowerCase();
                                boolean isMaleVoice = vName.contains("male") || vName.contains("man") || vName.contains("boy") || vName.contains("ctd") || vName.contains("ccd") || vName.contains("tpm") || vName.contains("rjm");
                                boolean isFemaleVoice = vName.contains("female") || vName.contains("woman") || vName.contains("girl") || vName.contains("ctc") || vName.contains("ccc") || vName.contains("tpf") || vName.contains("rjs");

                                if (!isFemale && isMaleVoice) {
                                    previewTts.setVoice(v);
                                    break;
                                } else if (isFemale && isFemaleVoice) {
                                    previewTts.setVoice(v);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 2. Set clear resonant pitch curve for male vs female
            float basePitch = isFemale ? 1.25f : 0.60f;
            // Modulate slightly based on voice personality
            float customPitch = (voice.pitch - 1.0f) * 0.4f + basePitch;
            previewTts.setPitch(Math.max(0.50f, Math.min(1.50f, customPitch)));
            previewTts.setSpeechRate(isFemale ? 1.02f : 0.95f);

            if (isEng) {
                previewTts.setLanguage(Locale.US);
                previewTts.speak("Hello! I am " + voice.name + ", your picture book storyteller. Let's read wonderful stories together!", TextToSpeech.QUEUE_FLUSH, null, "sample_" + voice.name);
            } else {
                previewTts.setLanguage(Locale.TRADITIONAL_CHINESE);
                previewTts.speak("你好！我是 " + voice.name + "，你的繪本說書人，今天想聽什麼精彩的故事呢？", TextToSpeech.QUEUE_FLUSH, null, "sample_" + voice.name);
            }
        } catch (Exception ignored) {}
    }

    public static void show(final Context context, final Runnable onVoiceSelected) {
        final boolean en = I18n.isEnglish(context);
        final int dp16 = dp(context, 16);
        final int dp10 = dp(context, 10);
        final int dp12 = dp(context, 12);
        final int dp8 = dp(context, 8);
        final int dp4 = dp(context, 4);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp16, dp16, dp16, dp10);
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);

        TextView titleView = new TextView(context);
        titleView.setText(en ? "🎙️ Select Storyteller Voice (30 Voices)" : "🎙️ 說書人聲線選擇 (官方 30 款音色)");
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Color.WHITE);
        titleView.setPadding(0, 0, 0, dp4);
        root.addView(titleView);

        TextView subtitleView = new TextView(context);
        subtitleView.setText(en ? "Tap '▶️ Play' to audition. Tap card to select." : "點擊「▶️ 試聽」可播放聲音，點擊卡片直接選用。");
        subtitleView.setTextSize(11);
        subtitleView.setTextColor(CrewTheme.TEXT_SECONDARY);
        subtitleView.setPadding(0, 0, 0, dp12);
        root.addView(subtitleView);

        // Filter Tabs Row
        final LinearLayout tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setPadding(0, 0, 0, dp10);
        root.addView(tabsRow);

        final String currentVoice = AppConfig.getVoiceName(context);
        ScrollView scrollList = new ScrollView(context);
        scrollList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 340)));
        final LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollList.addView(listContainer);
        root.addView(scrollList);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        final Runnable refreshList = new Runnable() {
            @Override public void run() {
                listContainer.removeAllViews();
                for (int i = 0; i < ALL_VOICES.length; i++) {
                    final VoiceInfo voice = ALL_VOICES[i];
                    if (currentVoiceFilterTab == 1 && !voice.isFemale) continue;
                    if (currentVoiceFilterTab == 2 && voice.isFemale) continue;

                    final boolean isSelected = voice.name.equalsIgnoreCase(currentVoice);

                    LinearLayout itemCard = new LinearLayout(context);
                    itemCard.setOrientation(LinearLayout.HORIZONTAL);
                    itemCard.setGravity(Gravity.CENTER_VERTICAL);
                    itemCard.setPadding(dp12, dp10, dp10, dp10);
                    int cardBg = isSelected ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A");
                    int borderCol = isSelected ? CrewTheme.AMBER_400 : Color.parseColor("#334155");
                    GradientDrawable itemBg = new GradientDrawable();
                    itemBg.setColor(cardBg);
                    itemBg.setCornerRadius(dp12);
                    itemBg.setStroke(dp(context, isSelected ? 2 : 1), borderCol);
                    itemCard.setBackground(itemBg);

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    cardLp.setMargins(0, 0, 0, dp8);
                    itemCard.setLayoutParams(cardLp);

                    // Indicator
                    TextView indicator = new TextView(context);
                    indicator.setText(isSelected ? "●" : "○");
                    indicator.setTextSize(14);
                    indicator.setTextColor(isSelected ? CrewTheme.AMBER_400 : CrewTheme.TEXT_MUTED);
                    indicator.setPadding(0, 0, dp10, 0);
                    itemCard.addView(indicator);

                    // Text Info
                    LinearLayout infoCol = new LinearLayout(context);
                    infoCol.setOrientation(LinearLayout.VERTICAL);
                    itemCard.addView(infoCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                    TextView nameView = new TextView(context);
                    nameView.setText((voice.isFemale ? "👩 " : "👨 ") + voice.name);
                    nameView.setTextSize(13);
                    nameView.setTypeface(Typeface.DEFAULT_BOLD);
                    nameView.setTextColor(isSelected ? CrewTheme.AMBER_400 : Color.WHITE);
                    infoCol.addView(nameView);

                    TextView descView = new TextView(context);
                    descView.setText(en ? voice.enDesc : voice.zhDesc);
                    descView.setTextSize(10);
                    descView.setTextColor(CrewTheme.TEXT_SECONDARY);
                    descView.setPadding(0, dp(context, 2), 0, 0);
                    infoCol.addView(descView);

                    // Audition Button
                    final Button previewBtn = new Button(context);
                    previewBtn.setText("▶️ " + (en ? "Play" : "試聽"));
                    previewBtn.setTextSize(11);
                    previewBtn.setTextColor(Color.WHITE);
                    previewBtn.setTypeface(Typeface.DEFAULT_BOLD);
                    previewBtn.setAllCaps(false);
                    GradientDrawable pBg = new GradientDrawable();
                    pBg.setColor(Color.parseColor("#1E293B"));
                    pBg.setCornerRadius(dp8);
                    pBg.setStroke(dp(context, 1), Color.parseColor("#475569"));
                    previewBtn.setBackground(pBg);
                    previewBtn.setPadding(dp8, dp4, dp8, dp4);

                    previewBtn.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            playAudition(context, voice);
                        }
                    });
                    itemCard.addView(previewBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 34)));

                    // Click item to select
                    itemCard.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            if (previewTts != null) {
                                try { previewTts.stop(); } catch (Exception ignored) {}
                            }
                            AppConfig.setVoiceName(context, voice.name);
                            Toast.makeText(context, (en ? "✅ Switched voice to: " : "✅ 已選用聲線：") + voice.name, Toast.LENGTH_SHORT).show();
                            if (dialogRef[0] != null) dialogRef[0].dismiss();
                            if (onVoiceSelected != null) onVoiceSelected.run();
                        }
                    });

                    listContainer.addView(itemCard);
                }
            }
        };

        // Filter Tabs
        final String[] tabLabels = new String[]{
                en ? "🌟 All (30)" : "🌟 全部 (30)",
                en ? "👩 Female (15)" : "👩 女性 (15)",
                en ? "👨 Male (15)" : "👨 男性 (15)"
        };

        final Button[] tabButtons = new Button[3];
        for (int t = 0; t < 3; t++) {
            final int tabIndex = t;
            Button tabBtn = new Button(context);
            tabBtn.setText(tabLabels[t]);
            tabBtn.setTextSize(10);
            tabBtn.setAllCaps(false);
            tabButtons[t] = tabBtn;

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(context, 32), 1f);
            if (t > 0) tlp.setMargins(dp4, 0, 0, 0);
            tabBtn.setLayoutParams(tlp);

            tabBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    currentVoiceFilterTab = tabIndex;
                    for (int i = 0; i < 3; i++) {
                        boolean active = i == currentVoiceFilterTab;
                        GradientDrawable tabBg = new GradientDrawable();
                        tabBg.setColor(active ? CrewTheme.AMBER_400 : Color.parseColor("#1E293B"));
                        tabBg.setCornerRadius(dp8);
                        tabButtons[i].setBackground(tabBg);
                        tabButtons[i].setTextColor(active ? Color.BLACK : Color.WHITE);
                        tabButtons[i].setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    }
                    refreshList.run();
                }
            });
            tabsRow.addView(tabBtn);
        }

        // Initialize active tab styling
        for (int i = 0; i < 3; i++) {
            boolean active = i == currentVoiceFilterTab;
            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setColor(active ? CrewTheme.AMBER_400 : Color.parseColor("#1E293B"));
            tabBg.setCornerRadius(dp8);
            tabButtons[i].setBackground(tabBg);
            tabButtons[i].setTextColor(active ? Color.BLACK : Color.WHITE);
            tabButtons[i].setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }

        refreshList.run();

        builder.setView(root);
        builder.setNegativeButton(en ? "Cancel" : "取消", null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) {
                if (previewTts != null) {
                    try { previewTts.stop(); } catch (Exception ignored) {}
                }
            }
        });
        dialogRef[0] = builder.show();
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}

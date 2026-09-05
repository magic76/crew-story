package com.crewpocket.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK_IMAGES = 301;
    private static final int REQUEST_PERMISSIONS = 201;

    private int currentMainTab = 0; // 0: Story Shelf, 1: Settings
    private final Stack<Integer> tabHistory = new Stack<Integer>();

    private FrameLayout rootFrame;
    private LinearLayout pageContent;
    private LinearLayout bottomNav;
    private List<Uri> selectedImageUris = new ArrayList<Uri>();
    private LinearLayout imagePreviewRow;
    private TextView imageCountText;
    private AlertDialog createDialog;

    private int dp(float val) {
        return CrewTheme.dp(this, val);
    }

    private void switchTab(int targetTab, boolean addToHistory) {
        if (currentMainTab != targetTab) {
            if (addToHistory) {
                tabHistory.push(currentMainTab);
            }
            currentMainTab = targetTab;
            renderCurrentPage();
        }
    }

    @Override
    public void onBackPressed() {
        if (!tabHistory.isEmpty()) {
            int prevTab = tabHistory.pop();
            switchTab(prevTab, false);
        } else if (currentMainTab != 0) {
            switchTab(0, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(CrewTheme.BG_PRIMARY);
            getWindow().setNavigationBarColor(CrewTheme.BG_PRIMARY);
        }
        getWindow().getDecorView().setBackgroundColor(CrewTheme.BG_PRIMARY);

        checkAndRequestPermissions();
        setupMainLayout();
        renderCurrentPage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCurrentPage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoicePreviewHelper.stopPreview();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> perms = new ArrayList<String>();
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                perms.add(android.Manifest.permission.RECORD_AUDIO);
            }
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (!perms.isEmpty()) {
                requestPermissions(perms.toArray(new String[0]), REQUEST_PERMISSIONS);
            }
        }
    }

    private void setupMainLayout() {
        rootFrame = new FrameLayout(this);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Top App Bar
        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(dp(16), dp(14), dp(16), dp(12));
        appBar.setBackgroundColor(CrewTheme.BG_PRIMARY);

        TextView appTitle = new TextView(this);
        appTitle.setText(I18n.t(this, "📚 繪本說書人", "📚 Crew Story"));
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(17);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        appBar.addView(appTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Language toggle button in Top Bar
        Button langToggleBtn = new Button(this);
        boolean isEn = I18n.isEnglish(this);
        langToggleBtn.setText(isEn ? "🇹🇼 中文" : "🌐 EN");
        langToggleBtn.setTextSize(11);
        langToggleBtn.setTextColor(Color.WHITE);
        langToggleBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 10));
        langToggleBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        langToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String nextLang = I18n.isEnglish(MainActivity.this) ? "zh" : "en";
                AppConfig.setUiLanguage(MainActivity.this, nextLang);
                setupMainLayout();
                renderCurrentPage();
            }
        });
        LinearLayout.LayoutParams ltLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        ltLp.setMargins(0, 0, dp(8), 0);
        appBar.addView(langToggleBtn, ltLp);

        Button newStoryBtn = new Button(this);
        newStoryBtn.setText(I18n.t(this, "✨ 創作新故事", "✨ Create Story"));
        newStoryBtn.setTextSize(12);
        newStoryBtn.setTextColor(Color.BLACK);
        newStoryBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable nBg = new GradientDrawable();
        nBg.setColor(CrewTheme.AMBER_400);
        nBg.setCornerRadius(dp(12));
        newStoryBtn.setBackground(nBg);
        newStoryBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        newStoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showCreateStoryDialog();
            }
        });
        appBar.addView(newStoryBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
        contentContainer.addView(appBar);

        // Scrollable Page Content
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollLp);

        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(16), dp(10), dp(16), dp(70)); // bottom padding for nav bar
        scroll.addView(pageContent);
        contentContainer.addView(scroll);

        rootFrame.addView(contentContainer);

        // Bottom Navigation Bar
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(14), dp(8), dp(14), dp(10));

        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(CrewTheme.BG_SURFACE);
        navBg.setStroke(dp(1), Color.parseColor("#1E293B"));
        bottomNav.setBackground(navBg);

        FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        bLp.gravity = Gravity.BOTTOM;
        bottomNav.setLayoutParams(bLp);

        rootFrame.addView(bottomNav);
        setContentView(rootFrame);
    }

    private void renderCurrentPage() {
        pageContent.removeAllViews();
        renderBottomNav();

        if (currentMainTab == 0) {
            renderStoryShelfTab();
        } else {
            renderSettingsTab();
        }
    }

    private void renderBottomNav() {
        bottomNav.removeAllViews();
        bottomNav.addView(makeNavItem("📚", I18n.t(this, "故事書架", "Story Shelf"), 0, currentMainTab == 0));
        bottomNav.addView(makeNavItem("⚙️", I18n.t(this, "偏好設定", "Settings"), 1, currentMainTab == 1));
    }

    private View makeNavItem(String icon, String label, final int tabIndex, boolean active) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.HORIZONTAL);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(16), dp(10), dp(16), dp(10));

        GradientDrawable tBg = new GradientDrawable();
        tBg.setCornerRadius(dp(14));
        if (active) {
            tBg.setColor(Color.parseColor("#1E293B"));
            tBg.setStroke(dp(1), CrewTheme.AMBER_400);
        } else {
            tBg.setColor(Color.parseColor("#0F172A"));
            tBg.setStroke(dp(1), Color.parseColor("#1E293B"));
        }
        tab.setBackground(tBg);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(16);
        iconTv.setPadding(0, 0, dp(8), 0);
        tab.addView(iconTv);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(13);
        labelTv.setTextColor(active ? CrewTheme.AMBER_400 : Color.parseColor("#94A3B8"));
        labelTv.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tab.addView(labelTv);

        if (active) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(8);
            dot.setTextColor(CrewTheme.AMBER_400);
            dot.setPadding(dp(6), 0, 0, 0);
            tab.addView(dot);
        }

        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab(tabIndex, true);
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(6), 0, dp(6), 0);
        tab.setLayoutParams(lp);
        return tab;
    }

    // ── Tab 0: Story Shelf ──
    private void renderStoryShelfTab() {
        // Banner card
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(16), dp(14), dp(16), dp(14));
        banner.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, CrewTheme.BORDER_GOLD, 16));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(0, 0, 0, dp(12));
        banner.setLayoutParams(bLp);

        TextView bTitle = new TextView(this);
        bTitle.setText(I18n.t(this, "🌟 歡迎來到 Crew Story 說書房", "🌟 Welcome to Crew Story"));
        bTitle.setTextColor(CrewTheme.AMBER_400);
        bTitle.setTextSize(15);
        bTitle.setTypeface(Typeface.DEFAULT_BOLD);
        banner.addView(bTitle);

        TextView bDesc = new TextView(this);
        bDesc.setText(I18n.t(this,
                "挑選一本繪本，由 Gemini 雙向語音說書人生動朗讀！音訊完整朗讀完才會自動翻頁，隨時暫停或插圖更換。",
                "Pick a picture book for Gemini to read aloud! Pages flip synchronously with audio narration."));
        bDesc.setTextColor(CrewTheme.TEXT_SECONDARY);
        bDesc.setTextSize(12);
        bDesc.setPadding(0, dp(4), 0, 0);
        banner.addView(bDesc);
        pageContent.addView(banner);

        // ── Storyteller Language Selector Row ──
        LinearLayout langSection = new LinearLayout(this);
        langSection.setOrientation(LinearLayout.VERTICAL);
        langSection.setPadding(0, 0, 0, dp(12));

        TextView langHeader = new TextView(this);
        langHeader.setText(I18n.t(this, "🎙️ 說書人語言 (Storyteller Language)：", "🎙️ Storyteller Language:"));
        langHeader.setTextColor(CrewTheme.TEXT_SECONDARY);
        langHeader.setTextSize(12);
        langHeader.setTypeface(Typeface.DEFAULT_BOLD);
        langHeader.setPadding(0, 0, 0, dp(6));
        langSection.addView(langHeader);

        HorizontalScrollView langScroll = new HorizontalScrollView(this);
        langScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);

        String currentStoryLang = AppConfig.getStoryLanguage(this);
        for (final String lCode : AppConfig.SUPPORTED_STORY_LANGS) {
            final boolean isSelected = lCode.equalsIgnoreCase(currentStoryLang);
            Button chip = new Button(this);
            chip.setText(AppConfig.getStoryLanguageDisplayName(lCode));
            chip.setTextSize(11);
            chip.setTextColor(isSelected ? CrewTheme.AMBER_400 : Color.WHITE);
            chip.setTypeface(isSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            chip.setBackground(CrewTheme.createCard(this, Color.parseColor(isSelected ? "#1E293B" : "#0F172A"), isSelected ? CrewTheme.BORDER_GOLD : CrewTheme.BORDER_DEFAULT, 12));
            chip.setPadding(dp(12), dp(4), dp(12), dp(4));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AppConfig.setStoryLanguage(MainActivity.this, lCode);
                    Toast.makeText(MainActivity.this,
                            I18n.t(MainActivity.this, "說書語言已設為：" + AppConfig.getStoryLanguageDisplayName(lCode), "Language set to: " + AppConfig.getStoryLanguageDisplayName(lCode)),
                            Toast.LENGTH_SHORT).show();
                    renderCurrentPage();
                }
            });

            LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            cpLp.setMargins(0, 0, dp(8), 0);
            langRow.addView(chip, cpLp);
        }
        langScroll.addView(langRow);
        langSection.addView(langScroll);
        pageContent.addView(langSection);

        // Story List Header
        TextView listHeader = new TextView(this);
        listHeader.setText(I18n.t(this, "📖 我的故事繪本書庫", "📖 My Picture Books"));
        listHeader.setTextColor(Color.WHITE);
        listHeader.setTextSize(14);
        listHeader.setTypeface(Typeface.DEFAULT_BOLD);
        listHeader.setPadding(0, dp(4), 0, dp(8));
        pageContent.addView(listHeader);

        List<StoryModel> stories = StoryRepository.getStories(this);
        for (final StoryModel s : stories) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(12));
            card.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, CrewTheme.BORDER_DEFAULT, 14));
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cLp);

            // Top row: Emoji + Title + Summary
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.TOP);

            TextView emoji = new TextView(this);
            emoji.setText(s.coverEmoji);
            emoji.setTextSize(32);
            emoji.setPadding(0, 0, dp(12), 0);
            topRow.addView(emoji);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);

            TextView title = new TextView(this);
            title.setText(s.title);
            title.setTextColor(Color.WHITE);
            title.setTextSize(15);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            info.addView(title);

            TextView summary = new TextView(this);
            summary.setText(s.summary);
            summary.setTextColor(CrewTheme.TEXT_MUTED);
            summary.setTextSize(12);
            summary.setMaxLines(2);
            summary.setEllipsize(TextUtils.TruncateAt.END);
            summary.setPadding(0, dp(2), 0, 0);
            info.addView(summary);

            topRow.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            card.addView(topRow);

            // Bottom action row: Page count + Edit & Play buttons
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(Gravity.CENTER_VERTICAL);
            actionRow.setPadding(0, dp(10), 0, 0);

            TextView pages = new TextView(this);
            pages.setText(I18n.t(this, "共 " + s.pages.size() + " 頁", s.pages.size() + " Pages"));
            pages.setTextColor(CrewTheme.SKY_400);
            pages.setTextSize(12);
            pages.setTypeface(Typeface.DEFAULT_BOLD);
            actionRow.addView(pages, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button editBtn = new Button(this);
            editBtn.setText(I18n.t(this, "✏️ 編輯", "✏️ Edit"));
            editBtn.setTextSize(11);
            editBtn.setTextColor(Color.WHITE);
            editBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
            editBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
            editBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, StoryEditorActivity.class);
                    intent.putExtra("EXTRA_STORY_ID", s.id);
                    startActivity(intent);
                }
            });
            LinearLayout.LayoutParams ebLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            ebLp.setMargins(0, 0, dp(8), 0);
            actionRow.addView(editBtn, ebLp);

            Button playBtn = new Button(this);
            playBtn.setText(I18n.t(this, "▶️ 播放", "▶️ Play"));
            playBtn.setTextSize(11);
            playBtn.setTextColor(Color.BLACK);
            playBtn.setTypeface(Typeface.DEFAULT_BOLD);
            playBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
            GradientDrawable pbBg = new GradientDrawable();
            pbBg.setColor(CrewTheme.AMBER_400);
            pbBg.setCornerRadius(dp(8));
            playBtn.setBackground(pbBg);
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, StoryPlayerActivity.class);
                    intent.putExtra("EXTRA_STORY_ID", s.id);
                    startActivity(intent);
                }
            });
            actionRow.addView(playBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));

            card.addView(actionRow);

            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, StoryPlayerActivity.class);
                    intent.putExtra("EXTRA_STORY_ID", s.id);
                    startActivity(intent);
                }
            });

            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    String[] options = {
                            I18n.t(MainActivity.this, "▶️ 開始朗讀", "▶️ Start Listening"),
                            I18n.t(MainActivity.this, "✏️ 編輯繪本", "✏️ Edit Story"),
                            I18n.t(MainActivity.this, "🗑️ 刪除故事", "🗑️ Delete Story")
                    };
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("《" + s.title + "》")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        Intent intent = new Intent(MainActivity.this, StoryPlayerActivity.class);
                                        intent.putExtra("EXTRA_STORY_ID", s.id);
                                        startActivity(intent);
                                    } else if (which == 1) {
                                        Intent intent = new Intent(MainActivity.this, StoryEditorActivity.class);
                                        intent.putExtra("EXTRA_STORY_ID", s.id);
                                        startActivity(intent);
                                    } else if (which == 2) {
                                        StoryRepository.deleteStory(MainActivity.this, s.id);
                                        renderCurrentPage();
                                    }
                                }
                            })
                            .show();
                    return true;
                }
            });

            pageContent.addView(card);
        }
    }

    // ── Tab 1: Settings ──
    private void renderSettingsTab() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, CrewTheme.BORDER_DEFAULT, 16));

        // Gemini API Key
        TextView secTitle = new TextView(this);
        secTitle.setText(I18n.t(this, "🔑 Gemini API Key (BYOK)", "🔑 Gemini API Key (BYOK)"));
        secTitle.setTextColor(Color.WHITE);
        secTitle.setTextSize(14);
        secTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(secTitle);

        final EditText keyInput = new EditText(this);
        keyInput.setHint(I18n.t(this, "請貼上 AI Studio 取得的 API Key...", "Enter your Gemini API Key..."));
        keyInput.setText(AppConfig.getGeminiApiKey(this));
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(CrewTheme.TEXT_MUTED);
        keyInput.setTextSize(13);
        keyInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        keyInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        LinearLayout.LayoutParams kLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kLp.setMargins(0, dp(8), 0, dp(12));
        keyInput.setLayoutParams(kLp);
        card.addView(keyInput);

        Button saveKeyBtn = new Button(this);
        saveKeyBtn.setText(I18n.t(this, "儲存 API Key", "Save API Key"));
        saveKeyBtn.setTextColor(Color.BLACK);
        saveKeyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(CrewTheme.AMBER_400);
        sBg.setCornerRadius(dp(10));
        saveKeyBtn.setBackground(sBg);
        saveKeyBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppConfig.setGeminiApiKey(MainActivity.this, keyInput.getText().toString());
                Toast.makeText(MainActivity.this, I18n.t(MainActivity.this, "API Key 已成功儲存！", "API Key saved successfully!"), Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(saveKeyBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        // Voice selection section
        TextView voiceTitle = new TextView(this);
        voiceTitle.setText(I18n.t(this, "🎙️ 說書人聲線 (全 30 款音色)", "🎙️ Storyteller Voice (30 Voices)"));
        voiceTitle.setTextColor(Color.WHITE);
        voiceTitle.setTextSize(14);
        voiceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        voiceTitle.setPadding(0, dp(18), 0, dp(8));
        card.addView(voiceTitle);

        final String currentVoice = AppConfig.getVoiceName(this);

        // Current voice highlight card
        LinearLayout currentVoiceCard = new LinearLayout(this);
        currentVoiceCard.setOrientation(LinearLayout.HORIZONTAL);
        currentVoiceCard.setGravity(Gravity.CENTER_VERTICAL);
        currentVoiceCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        currentVoiceCard.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.AMBER_400, 12));
        LinearLayout.LayoutParams cvcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cvcLp.setMargins(0, 0, 0, dp(10));
        currentVoiceCard.setLayoutParams(cvcLp);

        LinearLayout cvInfo = new LinearLayout(this);
        cvInfo.setOrientation(LinearLayout.VERTICAL);
        TextView cvLabel = new TextView(this);
        cvLabel.setText(I18n.t(this, "目前使用聲線：", "Current Voice:"));
        cvLabel.setTextSize(11);
        cvLabel.setTextColor(CrewTheme.TEXT_MUTED);
        cvInfo.addView(cvLabel);

        TextView cvName = new TextView(this);
        cvName.setText(VoicePersonaDialog.getVoiceDisplayName(this, currentVoice));
        cvName.setTextColor(CrewTheme.AMBER_400);
        cvName.setTextSize(14);
        cvName.setTypeface(Typeface.DEFAULT_BOLD);
        cvName.setPadding(0, dp(2), 0, 0);
        cvInfo.addView(cvName);
        currentVoiceCard.addView(cvInfo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button openVoiceDialogBtn = new Button(this);
        openVoiceDialogBtn.setText(I18n.t(this, "🎵 聲線庫 / 試聽", "🎵 Voice Library"));
        openVoiceDialogBtn.setTextSize(11);
        openVoiceDialogBtn.setTextColor(Color.BLACK);
        openVoiceDialogBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable obBg = new GradientDrawable();
        obBg.setColor(CrewTheme.AMBER_400);
        obBg.setCornerRadius(dp(8));
        openVoiceDialogBtn.setBackground(obBg);
        openVoiceDialogBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        openVoiceDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                VoicePersonaDialog.show(MainActivity.this, new Runnable() {
                    @Override public void run() {
                        renderCurrentPage();
                    }
                });
            }
        });
        currentVoiceCard.addView(openVoiceDialogBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        card.addView(currentVoiceCard);

        // Quick popular voices horizontal scroll
        TextView popTitle = new TextView(this);
        popTitle.setText(I18n.t(this, "✨ 常用推薦聲線快速切換：", "✨ Quick Popular Voices:"));
        popTitle.setTextColor(CrewTheme.TEXT_SECONDARY);
        popTitle.setTextSize(12);
        popTitle.setPadding(0, 0, 0, dp(6));
        card.addView(popTitle);

        HorizontalScrollView popScroll = new HorizontalScrollView(this);
        popScroll.setHorizontalScrollBarEnabled(false);
        popScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout popRow = new LinearLayout(this);
        popRow.setOrientation(LinearLayout.HORIZONTAL);
        popRow.setPadding(0, dp(2), 0, dp(2));

        final String[] popularVoices = {"Puck", "Aoede", "Kore", "Fenrir", "Leda", "Charon", "Zephyr", "Despina"};

        for (int i = 0; i < popularVoices.length; i++) {
            final String pv = popularVoices[i];
            final boolean isSel = pv.equalsIgnoreCase(currentVoice);
            Button pvBtn = new Button(this);
            pvBtn.setText(pv);
            pvBtn.setSingleLine(true);
            pvBtn.setTextSize(12);
            pvBtn.setTextColor(isSel ? CrewTheme.AMBER_400 : Color.WHITE);
            pvBtn.setTypeface(isSel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            pvBtn.setBackground(CrewTheme.createCard(this, Color.parseColor(isSel ? "#1E293B" : "#0F172A"), isSel ? CrewTheme.AMBER_400 : CrewTheme.BORDER_DEFAULT, 8));
            pvBtn.setPadding(dp(14), 0, dp(14), 0);
            pvBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AppConfig.setVoiceName(MainActivity.this, pv);
                    Toast.makeText(MainActivity.this, I18n.t(MainActivity.this, "已切換聲線: " + pv, "Switched voice: " + pv), Toast.LENGTH_SHORT).show();
                    renderCurrentPage();
                }
            });
            LinearLayout.LayoutParams pvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            if (i > 0) pvLp.setMargins(dp(6), 0, 0, 0);
            popRow.addView(pvBtn, pvLp);
        }
        popScroll.addView(popRow);
        card.addView(popScroll);

        pageContent.addView(card);
    }

    // ── Create Story Dialog (Text prompt or Multiple Image Upload) ──
    private void showCreateStoryDialog() {
        selectedImageUris.clear();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CrewTheme.BG_SURFACE);

        TextView title = new TextView(this);
        title.setText(I18n.t(this, "✨ 創作繪本故事", "✨ Create Picture Book"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(I18n.t(this,
                "輸入故事主題靈感，或選取多張照片，由 Gemini 為您編寫生動的章節故事！",
                "Enter story prompt or select multiple photos to generate a book!"));
        subtitle.setTextColor(CrewTheme.TEXT_MUTED);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(10));
        layout.addView(subtitle);

        final EditText promptInput = new EditText(this);
        promptInput.setHint(I18n.t(this,
                "例如：一隻在森林裡尋找星星的小刺蝟，遇到聰明的貓頭鷹...",
                "e.g. A little hedgehog looking for stars in the enchanted forest..."));
        promptInput.setTextColor(Color.WHITE);
        promptInput.setHintTextColor(CrewTheme.TEXT_MUTED);
        promptInput.setTextSize(13);
        promptInput.setMinLines(3);
        promptInput.setGravity(Gravity.TOP);
        promptInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        promptInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        layout.addView(promptInput);

        // Upload images button & preview
        LinearLayout imgHeader = new LinearLayout(this);
        imgHeader.setOrientation(LinearLayout.HORIZONTAL);
        imgHeader.setGravity(Gravity.CENTER_VERTICAL);
        imgHeader.setPadding(0, dp(12), 0, dp(6));

        Button pickImgBtn = new Button(this);
        pickImgBtn.setText(I18n.t(this, "📷 選取多張插圖 / 照片", "📷 Pick Photos / Illustrations"));
        pickImgBtn.setTextSize(12);
        pickImgBtn.setTextColor(Color.WHITE);
        pickImgBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 10));
        pickImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, I18n.t(MainActivity.this, "選擇故事插圖", "Pick Story Images")), REQUEST_PICK_IMAGES);
            }
        });
        imgHeader.addView(pickImgBtn);

        imageCountText = new TextView(this);
        imageCountText.setText(I18n.t(this, "未選擇圖片", "No images selected"));
        imageCountText.setTextColor(CrewTheme.TEXT_MUTED);
        imageCountText.setTextSize(11);
        imageCountText.setPadding(dp(10), 0, 0, 0);
        imgHeader.addView(imageCountText);
        layout.addView(imgHeader);

        HorizontalScrollView imgScroll = new HorizontalScrollView(this);
        imgScroll.setHorizontalScrollBarEnabled(false);
        imagePreviewRow = new LinearLayout(this);
        imagePreviewRow.setOrientation(LinearLayout.HORIZONTAL);
        imgScroll.addView(imagePreviewRow);
        layout.addView(imgScroll);

        // Action buttons (Create, Cancel)
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(16), 0, 0);

        Button cancelBtn = new Button(this);
        cancelBtn.setText(I18n.t(this, "取消", "Cancel"));
        cancelBtn.setTextColor(Color.WHITE);
        cancelBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1F2937"), CrewTheme.BORDER_DEFAULT, 10));
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (createDialog != null) createDialog.dismiss(); }
        });
        actions.addView(cancelBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));

        final Button generateBtn = new Button(this);
        generateBtn.setText(I18n.t(this, "🚀 開始生成故事", "🚀 Generate Story"));
        generateBtn.setTextColor(Color.BLACK);
        generateBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gBg = new GradientDrawable();
        gBg.setColor(CrewTheme.AMBER_400);
        gBg.setCornerRadius(dp(10));
        generateBtn.setBackground(gBg);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(0, dp(44), 1.5f);
        gLp.setMargins(dp(10), 0, 0, 0);
        generateBtn.setLayoutParams(gLp);

        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String prompt = promptInput.getText().toString().trim();
                if (prompt.isEmpty() && selectedImageUris.isEmpty()) {
                    Toast.makeText(MainActivity.this, I18n.t(MainActivity.this, "請輸入故事主題靈感或選取圖片！", "Please enter a prompt or pick images!"), Toast.LENGTH_SHORT).show();
                    return;
                }

                generateBtn.setEnabled(false);
                generateBtn.setText(I18n.t(MainActivity.this, "⏳ AI 正在編寫故事...", "⏳ Writing story..."));

                String currentStoryLang = AppConfig.getStoryLanguage(MainActivity.this);
                StoryGenerator.generateStory(MainActivity.this, prompt, selectedImageUris, "溫暖童趣", currentStoryLang, new StoryGenerator.GenerateCallback() {
                    @Override
                    public void onSuccess(StoryModel story) {
                        if (createDialog != null) createDialog.dismiss();
                        StoryRepository.addStory(MainActivity.this, story);
                        Toast.makeText(MainActivity.this, I18n.t(MainActivity.this, "✨ 故事《" + story.title + "》創作完成！", "✨ Story \"" + story.title + "\" created!"), Toast.LENGTH_SHORT).show();
                        renderCurrentPage();

                        // Open player directly
                        Intent intent = new Intent(MainActivity.this, StoryPlayerActivity.class);
                        intent.putExtra("EXTRA_STORY_ID", story.id);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String error) {
                        generateBtn.setEnabled(true);
                        generateBtn.setText(I18n.t(MainActivity.this, "🚀 開始生成故事", "🚀 Generate Story"));
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        actions.addView(generateBtn);
        layout.addView(actions);

        builder.setView(layout);
        createDialog = builder.create();
        createDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            selectedImageUris.clear();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    selectedImageUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }

            if (imagePreviewRow != null) {
                imagePreviewRow.removeAllViews();
                for (Uri uri : selectedImageUris) {
                    ImageView iv = new ImageView(this);
                    iv.setImageURI(uri);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(60), dp(60));
                    lp.setMargins(0, 0, dp(8), 0);
                    iv.setLayoutParams(lp);
                    imagePreviewRow.addView(iv);
                }
            }
            if (imageCountText != null) {
                imageCountText.setText(I18n.t(this, "已選取 " + selectedImageUris.size() + " 張圖片", selectedImageUris.size() + " images selected"));
            }
        }
    }
}

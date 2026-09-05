package com.crewpocket.story;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public class StoryPlayerActivity extends Activity implements StoryLiveClient.Listener {

    private StoryModel story;
    private StoryLiveClient liveClient;
    private int currentPage = 0;

    private TextView titleText;
    private TextView pageCounterText;
    private TextView statusBadge;
    private TextView storyContentText;
    private TextView dialogueText;
    private ImageView pageImageView;
    private FrameLayout imageContainer;
    private LinearLayout imagePlaceholder;
    private TextView imageActionHint;
    private TextView placeholderEmoji;
    private SeekBar pageSeekBar;
    private Button playPauseBtn;
    private Button prevBtn;
    private Button nextBtn;
    private TextView emotionBadge;

    private int dp(float val) {
        return CrewTheme.dp(this, val);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(CrewTheme.BG_PRIMARY);
            getWindow().setNavigationBarColor(CrewTheme.BG_PRIMARY);
        }
        getWindow().getDecorView().setBackgroundColor(CrewTheme.BG_PRIMARY);

        String storyId = getIntent().getStringExtra("EXTRA_STORY_ID");
        if (storyId != null) {
            story = StoryRepository.getStoryById(this, storyId);
        }
        if (story == null || story.pages.isEmpty()) {
            Toast.makeText(this, I18n.t(this, "無法載入故事內容", "Unable to load story content"), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buildUI();
        updatePageDisplay(0);

        // Auto-start Live Storytelling Agent & Background Playback Service
        liveClient = new StoryLiveClient(this, story, 0, this);
        liveClient.start();
        StoryPlaybackService.start(this, story, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (story != null) {
            StoryModel updated = StoryRepository.getStoryById(this, story.id);
            if (updated != null) {
                story = updated;
                if (titleText != null) titleText.setText(story.coverEmoji + " " + story.title);
                if (pageSeekBar != null) pageSeekBar.setMax(Math.max(0, story.pages.size() - 1));
                if (currentPage >= story.pages.size()) currentPage = Math.max(0, story.pages.size() - 1);
                updatePageDisplay(currentPage);
            }
        }
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Top Header (Back Button, Title, Page Counter, Edit Button)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));

        TextView backBtn = new TextView(this);
        backBtn.setText(I18n.t(this, "‹ 返回", "‹ Back"));
        backBtn.setTextColor(CrewTheme.SKY_400);
        backBtn.setTextSize(16);
        backBtn.setTypeface(Typeface.DEFAULT_BOLD);
        backBtn.setPadding(0, 0, dp(12), 0);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(backBtn);

        titleText = new TextView(this);
        titleText.setText(story.coverEmoji + " " + story.title);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(16);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setSingleLine(true);
        header.addView(titleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        pageCounterText = new TextView(this);
        pageCounterText.setText("1 / " + story.pages.size());
        pageCounterText.setTextColor(CrewTheme.AMBER_400);
        pageCounterText.setTextSize(13);
        pageCounterText.setTypeface(Typeface.DEFAULT_BOLD);
        pageCounterText.setPadding(dp(6), 0, dp(10), 0);
        header.addView(pageCounterText);

        Button editBtn = new Button(this);
        editBtn.setText("✏️ " + I18n.t(this, "編輯", "Edit"));
        editBtn.setTextSize(11);
        editBtn.setTextColor(Color.WHITE);
        editBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
        editBtn.setPadding(dp(8), dp(2), dp(8), dp(2));
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (liveClient != null && !liveClient.isPaused()) {
                    liveClient.pause();
                    playPauseBtn.setText(I18n.t(StoryPlayerActivity.this, "▶️ 繼續播放", "▶️ Resume"));
                    ((GradientDrawable) playPauseBtn.getBackground()).setColor(CrewTheme.EMERALD_400);
                }
                Intent intent = new Intent(StoryPlayerActivity.this, StoryEditorActivity.class);
                intent.putExtra("EXTRA_STORY_ID", story.id);
                startActivity(intent);
            }
        });
        header.addView(editBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));

        root.addView(header);

        // 2. Status Badge Row
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, 0, 0, dp(10));

        statusBadge = new TextView(this);
        statusBadge.setText(I18n.t(this, "● 連接中...", "● Connecting..."));
        statusBadge.setTextSize(12);
        statusBadge.setTextColor(CrewTheme.TEXT_MUTED);
        statusRow.addView(statusBadge, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        emotionBadge = new TextView(this);
        emotionBadge.setText("✨ " + I18n.t(this, "說書中", "Storytelling"));
        emotionBadge.setTextSize(11);
        emotionBadge.setTextColor(CrewTheme.PURPLE_400);
        emotionBadge.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E1B4B"), Color.parseColor("#4338CA"), 8));
        emotionBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        statusRow.addView(emotionBadge);

        root.addView(statusRow);

        // 3. Book Page Main Card (Image + Text ScrollView)
        LinearLayout pageCard = new LinearLayout(this);
        pageCard.setOrientation(LinearLayout.VERTICAL);
        pageCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        pageCard.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, CrewTheme.BORDER_GOLD, 16));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.setMargins(0, 0, 0, dp(12));
        pageCard.setLayoutParams(cardLp);

        // Illustration Image Container
        imageContainer = new FrameLayout(this);
        imageContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(175)));
        imageContainer.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 12));

        pageImageView = new ImageView(this);
        pageImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageContainer.addView(pageImageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Decorative Placeholder when page has no image
        imagePlaceholder = new LinearLayout(this);
        imagePlaceholder.setOrientation(LinearLayout.VERTICAL);
        imagePlaceholder.setGravity(Gravity.CENTER);
        imagePlaceholder.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        placeholderEmoji = new TextView(this);
        placeholderEmoji.setText("📖");
        placeholderEmoji.setTextSize(36);
        placeholderEmoji.setGravity(Gravity.CENTER);
        imagePlaceholder.addView(placeholderEmoji);

        imageActionHint = new TextView(this);
        imageActionHint.setText(I18n.t(this, "✨ 繪本故事朗讀中", "✨ Story in Progress"));
        imageActionHint.setTextColor(CrewTheme.TEXT_MUTED);
        imageActionHint.setTextSize(12);
        imageActionHint.setPadding(0, dp(6), 0, 0);
        imageActionHint.setGravity(Gravity.CENTER);
        imagePlaceholder.addView(imageActionHint);
        imageContainer.addView(imagePlaceholder);

        pageCard.addView(imageContainer);

        // Text Scroll View
        ScrollView textScroll = new ScrollView(this);
        textScroll.setVerticalScrollBarEnabled(false);
        textScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        textScroll.setPadding(0, dp(10), 0, 0);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        storyContentText = new TextView(this);
        storyContentText.setTextSize(17);
        storyContentText.setTextColor(Color.parseColor("#F3F4F6"));
        storyContentText.setLineSpacing(dp(8), 1.35f);
        storyContentText.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        textLayout.addView(storyContentText);

        dialogueText = new TextView(this);
        dialogueText.setTextSize(15);
        dialogueText.setTextColor(CrewTheme.AMBER_400);
        dialogueText.setLineSpacing(dp(6), 1.25f);
        dialogueText.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        dialogueText.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable dBg = new GradientDrawable();
        dBg.setColor(Color.parseColor("#1F2937"));
        dBg.setCornerRadius(dp(10));
        dBg.setStroke(dp(1), Color.parseColor("#374151"));
        dialogueText.setBackground(dBg);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.setMargins(0, dp(12), 0, dp(6));
        dialogueText.setLayoutParams(dLp);
        dialogueText.setVisibility(View.GONE);
        textLayout.addView(dialogueText);

        textScroll.addView(textLayout);
        pageCard.addView(textScroll);
        root.addView(pageCard);

        // 4. Page Progress Slider
        pageSeekBar = new SeekBar(this);
        pageSeekBar.setMax(Math.max(0, story.pages.size() - 1));
        pageSeekBar.setProgress(0);
        pageSeekBar.setPadding(dp(8), dp(4), dp(8), dp(10));
        pageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentPage = progress;
                    updatePageDisplay(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (liveClient != null) {
                    liveClient.jumpToPage(seekBar.getProgress());
                }
            }
        });
        root.addView(pageSeekBar);

        // 5. Control Buttons (Prev, Play/Pause, Next)
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        prevBtn = new Button(this);
        prevBtn.setText(I18n.t(this, "◀ 上一頁", "◀ Prev"));
        prevBtn.setTextColor(Color.WHITE);
        prevBtn.setTextSize(13);
        prevBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1F2937"), CrewTheme.BORDER_DEFAULT, 12));
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    updatePageDisplay(currentPage);
                    if (liveClient != null) liveClient.jumpToPage(currentPage);
                }
            }
        });
        controls.addView(prevBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));

        playPauseBtn = new Button(this);
        playPauseBtn.setText(I18n.t(this, "⏸️ 暫停說書", "⏸️ Pause"));
        playPauseBtn.setTextColor(Color.BLACK);
        playPauseBtn.setTextSize(14);
        playPauseBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(CrewTheme.AMBER_400);
        pBg.setCornerRadius(dp(14));
        playPauseBtn.setBackground(pBg);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(48), 1.5f);
        pLp.setMargins(dp(10), 0, dp(10), 0);
        playPauseBtn.setLayoutParams(pLp);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                handlePlayPauseClick();
            }
        });
        controls.addView(playPauseBtn);

        nextBtn = new Button(this);
        nextBtn.setText(I18n.t(this, "下一頁 ▶", "Next ▶"));
        nextBtn.setTextColor(Color.WHITE);
        nextBtn.setTextSize(13);
        nextBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1F2937"), CrewTheme.BORDER_DEFAULT, 12));
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentPage < story.pages.size() - 1) {
                    currentPage++;
                    updatePageDisplay(currentPage);
                    if (liveClient != null) liveClient.jumpToPage(currentPage);
                }
            }
        });
        controls.addView(nextBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));

        root.addView(controls);
        setContentView(root);
    }

    private void updatePageDisplay(int index) {
        if (index < 0 || index >= story.pages.size()) return;
        this.currentPage = index;
        StoryModel.Page p = story.pages.get(index);

        pageCounterText.setText((index + 1) + " / " + story.pages.size());
        pageSeekBar.setProgress(index);
        storyContentText.setText(p.text);

        if (p.dialogue != null && !p.dialogue.trim().isEmpty()) {
            dialogueText.setVisibility(View.VISIBLE);
            dialogueText.setText("💬 " + (p.characterName != null && !p.characterName.isEmpty() ? (p.characterName + "：") : "") + "「" + p.dialogue + "」");
        } else {
            dialogueText.setVisibility(View.GONE);
        }

        emotionBadge.setText("✨ " + getEmotionLabel(p.emotion));

        // Load illustration if present
        if (p.imageUri != null && !p.imageUri.isEmpty()) {
            Bitmap bmp = StoryIllustrationGenerator.loadBitmapSafely(this, p.imageUri);
            if (bmp != null) {
                pageImageView.setImageBitmap(bmp);
                pageImageView.setVisibility(View.VISIBLE);
                imagePlaceholder.setVisibility(View.GONE);
                return;
            }
        }

        // If no image, show elegant book placeholder
        pageImageView.setImageDrawable(null);
        pageImageView.setVisibility(View.GONE);
        imagePlaceholder.setVisibility(View.VISIBLE);
        placeholderEmoji.setText(story.coverEmoji != null ? story.coverEmoji : "📖");
        imageActionHint.setText(I18n.t(this, "第 " + (index + 1) + " 頁 · " + story.title, "Page " + (index + 1) + " · " + story.title));
    }

    private String getEmotionLabel(String emotion) {
        if ("warm".equalsIgnoreCase(emotion)) return I18n.t(this, "溫暖親切", "Warm");
        if ("excited".equalsIgnoreCase(emotion)) return I18n.t(this, "興奮雀躍", "Excited");
        if ("mysterious".equalsIgnoreCase(emotion)) return I18n.t(this, "神秘探索", "Mysterious");
        if ("joyful".equalsIgnoreCase(emotion)) return I18n.t(this, "歡樂生動", "Joyful");
        if ("whisper".equalsIgnoreCase(emotion)) return I18n.t(this, "輕聲細語", "Whisper");
        return I18n.t(this, "生動說書", "Storytelling");
    }

    private void handlePlayPauseClick() {
        if (liveClient != null) {
            if (liveClient.isPaused()) {
                liveClient.resume();
                playPauseBtn.setText(I18n.t(StoryPlayerActivity.this, "⏸️ 暫停說書", "⏸️ Pause"));
                ((GradientDrawable) playPauseBtn.getBackground()).setColor(CrewTheme.AMBER_400);
            } else {
                liveClient.pause();
                playPauseBtn.setText(I18n.t(StoryPlayerActivity.this, "▶️ 繼續播放", "▶️ Resume"));
                ((GradientDrawable) playPauseBtn.getBackground()).setColor(CrewTheme.EMERALD_400);
            }
        }
    }

    private void restartStorySession(int startPage) {
        currentPage = startPage;
        updatePageDisplay(startPage);
        if (liveClient != null) {
            liveClient.stop();
            liveClient = null;
        }
        liveClient = new StoryLiveClient(this, story, startPage, this);
        liveClient.start();
        StoryPlaybackService.start(this, story, startPage);
        playPauseBtn.setText(I18n.t(this, "⏸️ 暫停說書", "⏸️ Pause"));
        ((GradientDrawable) playPauseBtn.getBackground()).setColor(CrewTheme.AMBER_400);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                handlePlayPauseClick();
            }
        });
    }

    @Override
    public void onConnected() {
        statusBadge.setText(I18n.t(this, "● 說書人已就緒", "● Storyteller Ready"));
        statusBadge.setTextColor(CrewTheme.EMERALD_400);
    }

    @Override
    public void onDisconnected(String reason) {
        statusBadge.setText(I18n.t(this, "● 連線已中斷", "● Disconnected"));
        statusBadge.setTextColor(CrewTheme.TEXT_MUTED);
    }

    @Override
    public void onError(String error) {
        statusBadge.setText(I18n.t(this, "● 錯誤: ", "● Error: ") + error);
        statusBadge.setTextColor(CrewTheme.ROSE_500);
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPageAdvanced(int newPageIndex, String chapterText) {
        updatePageDisplay(newPageIndex);
        StoryPlaybackService.updateProgress(this, newPageIndex, story.pages.size());
    }

    @Override
    public void onStoryFinished() {
        statusBadge.setText(I18n.t(this, "🎉 故事全篇圓滿結束！", "🎉 Story Completed!"));
        statusBadge.setTextColor(CrewTheme.AMBER_400);
        playPauseBtn.setText(I18n.t(this, "🔄 重新聆聽", "🔄 Replay"));
        ((GradientDrawable) playPauseBtn.getBackground()).setColor(CrewTheme.EMERALD_400);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                restartStorySession(0);
            }
        });
        StoryPlaybackService.stop(this);
    }

    @Override
    public void onAiSpeechStarted() {
        statusBadge.setText(I18n.t(this, "🎙️ 說書人正在朗讀...", "🎙️ Narrating..."));
        statusBadge.setTextColor(CrewTheme.SKY_400);
    }

    @Override
    public void onAiSpeechEnded() {
        if (liveClient != null && !liveClient.isPaused()) {
            statusBadge.setText(I18n.t(this, "● 翻頁準備中...", "● Preparing Next Page..."));
            statusBadge.setTextColor(CrewTheme.EMERALD_400);
        }
    }

    @Override
    public void onUserInterrupted() {
        // Intentionally silent or status update
    }

    @Override
    public void onStatusUpdate(String status) {
        statusBadge.setText("● " + status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StoryPlaybackService.stop(this);
        if (liveClient != null) {
            liveClient.stop();
            liveClient = null;
        }
    }
}

package com.crewpocket.story;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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

/**
 * Immersive story reader.
 *
 * crew-story-1001 goals:
 * - illustration first (book-like, not dashboard-like)
 * - normal connection details are visually quiet; errors remain visible
 * - make "you can interrupt the storyteller" discoverable
 * - centralize transient player UI state instead of mutating status labels everywhere
 * - keep StoryLiveClient / StoryPlaybackService behavior compatible with the existing app
 */
public class StoryPlayerActivity extends Activity implements StoryLiveClient.Listener {

    private StoryModel story;
    private StoryLiveClient liveClient;
    private int currentPage = 0;

    private TextView titleText;
    private TextView pageCounterText;
    private TextView statusText;
    private TextView interactionHintText;
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
    private Button talkBtn;
    private TextView emotionBadge;

    private StoryPlayerUiState uiState = StoryPlayerUiState.of(StoryPlayerUiState.Mode.CONNECTING);

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
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.CONNECTING));

        liveClient = new StoryLiveClient(this, story, 0, this);
        liveClient.start();
        // Existing service is kept as the foreground notification / wake-lock companion.
        StoryPlaybackService.start(this, story, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (story == null) return;

        StoryModel updated = StoryRepository.getStoryById(this, story.id);
        if (updated != null) {
            story = updated;
            if (titleText != null) titleText.setText(story.coverEmoji + " " + story.title);
            if (pageSeekBar != null) pageSeekBar.setMax(Math.max(0, story.pages.size() - 1));
            if (currentPage >= story.pages.size()) currentPage = Math.max(0, story.pages.size() - 1);
            updatePageDisplay(currentPage);
        }
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(14));
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildHeader());
        root.addView(buildAmbientStatus());

        LinearLayout pageCard = new LinearLayout(this);
        pageCard.setOrientation(LinearLayout.VERTICAL);
        pageCard.setPadding(dp(10), dp(10), dp(10), dp(12));
        pageCard.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, CrewTheme.BORDER_GOLD, 20));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.setMargins(0, dp(4), 0, dp(8));
        pageCard.setLayoutParams(cardLp);

        imageContainer = new FrameLayout(this);
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.35f);
        imageLp.setMargins(0, 0, 0, dp(10));
        imageContainer.setLayoutParams(imageLp);
        imageContainer.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), Color.TRANSPARENT, 16));
        imageContainer.setClipToOutline(Build.VERSION.SDK_INT >= 21);

        pageImageView = new ImageView(this);
        pageImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageContainer.addView(pageImageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        imagePlaceholder = new LinearLayout(this);
        imagePlaceholder.setOrientation(LinearLayout.VERTICAL);
        imagePlaceholder.setGravity(Gravity.CENTER);
        imagePlaceholder.setPadding(dp(16), dp(16), dp(16), dp(16));
        imageContainer.addView(imagePlaceholder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        placeholderEmoji = new TextView(this);
        placeholderEmoji.setText("📖");
        placeholderEmoji.setTextSize(48);
        placeholderEmoji.setGravity(Gravity.CENTER);
        imagePlaceholder.addView(placeholderEmoji);

        imageActionHint = new TextView(this);
        imageActionHint.setTextColor(CrewTheme.TEXT_SECONDARY);
        imageActionHint.setTextSize(12);
        imageActionHint.setPadding(0, dp(8), 0, 0);
        imageActionHint.setGravity(Gravity.CENTER);
        imagePlaceholder.addView(imageActionHint);

        pageCard.addView(imageContainer);

        ScrollView textScroll = new ScrollView(this);
        textScroll.setVerticalScrollBarEnabled(false);
        textScroll.setFillViewport(true);
        textScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.9f));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(dp(8), dp(2), dp(8), dp(4));

        storyContentText = new TextView(this);
        storyContentText.setTextSize(19);
        storyContentText.setTextColor(CrewTheme.TEXT_PRIMARY);
        storyContentText.setLineSpacing(dp(7), 1.28f);
        storyContentText.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        textLayout.addView(storyContentText);

        dialogueText = new TextView(this);
        dialogueText.setTextSize(16);
        dialogueText.setTextColor(CrewTheme.AMBER_400);
        dialogueText.setLineSpacing(dp(5), 1.18f);
        dialogueText.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        dialogueText.setPadding(dp(12), dp(10), dp(12), dp(10));
        dialogueText.setBackground(CrewTheme.createCard(this, Color.parseColor("#1A2232"), Color.parseColor("#374151"), 12));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.setMargins(0, dp(12), 0, 0);
        dialogueText.setLayoutParams(dLp);
        dialogueText.setVisibility(View.GONE);
        textLayout.addView(dialogueText);

        textScroll.addView(textLayout);
        pageCard.addView(textScroll);
        root.addView(pageCard);

        pageSeekBar = new SeekBar(this);
        pageSeekBar.setMax(Math.max(0, story.pages.size() - 1));
        pageSeekBar.setProgress(0);
        pageSeekBar.setPadding(dp(10), 0, dp(10), dp(4));
        pageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) updatePageDisplay(progress);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int target = seekBar.getProgress();
                currentPage = target;
                if (liveClient != null) liveClient.jumpToPage(target);
                StoryPlaybackService.updateProgress(StoryPlayerActivity.this, target, story.pages.size());
            }
        });
        root.addView(pageSeekBar);

        root.addView(buildControls());
        setContentView(root);
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(6));

        TextView backBtn = new TextView(this);
        backBtn.setText("‹");
        backBtn.setTextColor(CrewTheme.TEXT_PRIMARY);
        backBtn.setTextSize(30);
        backBtn.setGravity(Gravity.CENTER);
        backBtn.setContentDescription(I18n.t(this, "返回", "Back"));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(backBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));

        titleText = new TextView(this);
        titleText.setText(story.coverEmoji + " " + story.title);
        titleText.setTextColor(CrewTheme.TEXT_PRIMARY);
        titleText.setTextSize(15);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setSingleLine(true);
        titleText.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(titleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        pageCounterText = new TextView(this);
        pageCounterText.setText("1 / " + story.pages.size());
        pageCounterText.setTextColor(CrewTheme.TEXT_SECONDARY);
        pageCounterText.setTextSize(12);
        pageCounterText.setPadding(dp(8), 0, dp(8), 0);
        header.addView(pageCounterText);

        TextView editBtn = new TextView(this);
        editBtn.setText("✎");
        editBtn.setTextColor(CrewTheme.TEXT_SECONDARY);
        editBtn.setTextSize(24);
        editBtn.setGravity(Gravity.CENTER);
        editBtn.setContentDescription(I18n.t(this, "編輯故事", "Edit story"));
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pauseForEditing();
                Intent intent = new Intent(StoryPlayerActivity.this, StoryEditorActivity.class);
                intent.putExtra("EXTRA_STORY_ID", story.id);
                startActivity(intent);
            }
        });
        header.addView(editBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));

        return header;
    }

    private View buildAmbientStatus() {
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(dp(4), 0, dp(4), dp(4));

        statusText = new TextView(this);
        statusText.setTextSize(11);
        statusText.setTextColor(CrewTheme.TEXT_MUTED);
        statusText.setSingleLine(true);
        statusText.setEllipsize(TextUtils.TruncateAt.END);
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        emotionBadge = new TextView(this);
        emotionBadge.setTextSize(10);
        emotionBadge.setTextColor(CrewTheme.TEXT_SECONDARY);
        emotionBadge.setPadding(dp(7), dp(3), dp(7), dp(3));
        emotionBadge.setBackground(CrewTheme.createCard(this, Color.parseColor("#161D2A"), Color.TRANSPARENT, 8));
        statusRow.addView(emotionBadge);

        return statusRow;
    }

    private View buildControls() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        // Push-to-Talk Button (Manual Interruption / Child Speaking Turn)
        talkBtn = new Button(this);
        talkBtn.setText(I18n.t(this, "🎤 我要說話", "🎤 I want to talk"));
        talkBtn.setTextColor(Color.WHITE);
        talkBtn.setTextSize(14);
        talkBtn.setTypeface(Typeface.DEFAULT_BOLD);
        talkBtn.setEnabled(false); // Enabled when connected
        talkBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#2563EB"), Color.parseColor("#60A5FA"), 14));
        talkBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { handleTalkButtonClick(); }
        });
        LinearLayout.LayoutParams talkLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        talkLp.setMargins(0, 0, 0, dp(8));
        wrapper.addView(talkBtn, talkLp);

        interactionHintText = new TextView(this);
        interactionHintText.setText(I18n.t(this, "🎙️ 想問問題？點上方「我要說話」即可插話", "🎙️ Want to ask a question? Tap 'I want to talk'"));
        interactionHintText.setTextColor(CrewTheme.TEXT_SECONDARY);
        interactionHintText.setTextSize(11);
        interactionHintText.setGravity(Gravity.CENTER);
        interactionHintText.setPadding(0, 0, 0, dp(6));
        wrapper.addView(interactionHintText);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        prevBtn = createSideControl(I18n.t(this, "‹ 上一頁", "‹ Prev"));
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { movePage(-1); }
        });
        controls.addView(prevBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));

        playPauseBtn = new Button(this);
        playPauseBtn.setText(I18n.t(this, "⏸ 暫停", "⏸ Pause"));
        playPauseBtn.setTextColor(Color.BLACK);
        playPauseBtn.setTextSize(14);
        playPauseBtn.setTypeface(Typeface.DEFAULT_BOLD);
        playPauseBtn.setAllCaps(false);
        playPauseBtn.setBackground(makePrimaryButton(CrewTheme.AMBER_400));
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(52), 1.35f);
        pLp.setMargins(dp(10), 0, dp(10), 0);
        controls.addView(playPauseBtn, pLp);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { handlePlayPauseClick(); }
        });

        nextBtn = createSideControl(I18n.t(this, "下一頁 ›", "Next ›"));
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { movePage(1); }
        });
        controls.addView(nextBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));

        wrapper.addView(controls);
        return wrapper;
    }

    private void handleTalkButtonClick() {
        if (liveClient == null || talkBtn == null) return;

        if (liveClient.isUserSpeaking()) {
            // Second tap: Explicit end of the child's turn
            if (liveClient.endUserTurn()) {
                talkBtn.setText(I18n.t(this, "⏳ 波波老師回答中…", "⏳ Teacher is answering…"));
                talkBtn.setEnabled(false);
                setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.READY, "Thinking..."));
            }
            return;
        }

        if (liveClient.isAwaitingUserResponse()) return;

        // First tap: Intentionally interrupt narration and open the mic
        if (liveClient.beginUserTurn()) {
            talkBtn.setText(I18n.t(this, "⏹️ 說完了", "⏹️ Done speaking"));
            talkBtn.setEnabled(true);
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.LISTENING));
        }
    }

    private Button createSideControl(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(CrewTheme.TEXT_PRIMARY);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setBackground(CrewTheme.createCard(this, Color.parseColor("#171E2B"), CrewTheme.BORDER_DEFAULT, 14));
        return button;
    }

    private GradientDrawable makePrimaryButton(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(16));
        return bg;
    }

    private void movePage(int delta) {
        int target = currentPage + delta;
        if (target < 0 || target >= story.pages.size()) return;
        currentPage = target;
        updatePageDisplay(target);
        if (liveClient != null) liveClient.jumpToPage(target);
        StoryPlaybackService.updateProgress(this, target, story.pages.size());
    }

    private void updatePageDisplay(int index) {
        if (index < 0 || index >= story.pages.size()) return;
        currentPage = index;
        StoryModel.Page page = story.pages.get(index);

        pageCounterText.setText((index + 1) + " / " + story.pages.size());
        pageSeekBar.setProgress(index);
        storyContentText.setText(page.text == null ? "" : page.text);
        emotionBadge.setText(getEmotionIcon(page.emotion) + " " + getEmotionLabel(page.emotion));

        if (page.dialogue != null && !page.dialogue.trim().isEmpty()) {
            dialogueText.setVisibility(View.VISIBLE);
            String speaker = page.characterName != null && !page.characterName.trim().isEmpty()
                    ? page.characterName.trim() + "："
                    : "";
            dialogueText.setText(speaker + "「" + page.dialogue.trim() + "」");
        } else {
            dialogueText.setVisibility(View.GONE);
        }

        if (page.imageUri != null && !page.imageUri.isEmpty()) {
            Bitmap bmp = StoryIllustrationGenerator.loadBitmapSafely(this, page.imageUri);
            if (bmp != null) {
                pageImageView.setImageBitmap(bmp);
                pageImageView.setVisibility(View.VISIBLE);
                imagePlaceholder.setVisibility(View.GONE);
                updateNavEnabledState();
                return;
            }
        }

        pageImageView.setImageDrawable(null);
        pageImageView.setVisibility(View.GONE);
        imagePlaceholder.setVisibility(View.VISIBLE);
        placeholderEmoji.setText(story.coverEmoji != null ? story.coverEmoji : "📖");
        imageActionHint.setText(I18n.t(this,
                "第 " + (index + 1) + " 頁 · 故事正在這裡發生",
                "Page " + (index + 1) + " · The story is happening here"));
        updateNavEnabledState();
    }

    private void updateNavEnabledState() {
        if (prevBtn != null) {
            prevBtn.setEnabled(currentPage > 0);
            prevBtn.setAlpha(currentPage > 0 ? 1f : 0.38f);
        }
        if (nextBtn != null) {
            boolean enabled = currentPage < story.pages.size() - 1;
            nextBtn.setEnabled(enabled);
            nextBtn.setAlpha(enabled ? 1f : 0.38f);
        }
    }

    private String getEmotionIcon(String emotion) {
        if ("warm".equalsIgnoreCase(emotion)) return "☀";
        if ("excited".equalsIgnoreCase(emotion)) return "✨";
        if ("mysterious".equalsIgnoreCase(emotion)) return "☾";
        if ("joyful".equalsIgnoreCase(emotion)) return "♫";
        if ("whisper".equalsIgnoreCase(emotion)) return "☁";
        if ("scary".equalsIgnoreCase(emotion)) return "◆";
        if ("tender".equalsIgnoreCase(emotion)) return "♡";
        return "•";
    }

    private String getEmotionLabel(String emotion) {
        if ("warm".equalsIgnoreCase(emotion)) return I18n.t(this, "溫暖", "Warm");
        if ("excited".equalsIgnoreCase(emotion)) return I18n.t(this, "興奮", "Excited");
        if ("mysterious".equalsIgnoreCase(emotion)) return I18n.t(this, "神秘", "Mysterious");
        if ("joyful".equalsIgnoreCase(emotion)) return I18n.t(this, "歡樂", "Joyful");
        if ("whisper".equalsIgnoreCase(emotion)) return I18n.t(this, "輕聲", "Whisper");
        if ("scary".equalsIgnoreCase(emotion)) return I18n.t(this, "緊張", "Suspense");
        if ("tender".equalsIgnoreCase(emotion)) return I18n.t(this, "溫柔", "Tender");
        return I18n.t(this, "說書", "Story" );
    }

    private void handlePlayPauseClick() {
        if (liveClient == null) return;

        if (uiState.mode == StoryPlayerUiState.Mode.FINISHED) {
            restartStorySession(0);
            return;
        }

        if (liveClient.isPaused()) {
            liveClient.resume();
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.READY));
        } else {
            liveClient.pause();
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.PAUSED));
        }
    }

    private void pauseForEditing() {
        if (liveClient != null && !liveClient.isPaused()) {
            liveClient.pause();
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.PAUSED));
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
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.CONNECTING));
    }

    private void setUiState(StoryPlayerUiState state) {
        uiState = state;
        if (statusText == null || playPauseBtn == null || interactionHintText == null) return;

        int statusColor = CrewTheme.TEXT_MUTED;
        String status = "";
        String playText = I18n.t(this, "⏸ 暫停", "⏸ Pause");
        int playColor = CrewTheme.AMBER_400;
        String interaction = I18n.t(this, "🎙️ 可以隨時跟波波老師說話", "🎙️ You can talk to the storyteller anytime");

        switch (state.mode) {
            case CONNECTING:
                status = I18n.t(this, "正在準備說書人…", "Preparing storyteller…");
                break;
            case READY:
                status = I18n.t(this, "已就緒", "Ready");
                statusColor = CrewTheme.EMERALD_400;
                break;
            case NARRATING:
                status = I18n.t(this, "波波老師正在說故事", "Storyteller is narrating");
                statusColor = CrewTheme.SKY_400;
                interaction = I18n.t(this, "🎙️ 想問問題？直接說話就可以", "🎙️ Have a question? Just speak");
                break;
            case LISTENING:
                status = I18n.t(this, "正在聽你說…", "Listening to you…");
                statusColor = CrewTheme.PURPLE_400;
                interaction = I18n.t(this, "正在聽你說話…", "Listening…");
                break;
            case PAUSED:
                status = I18n.t(this, "故事已暫停", "Story paused");
                playText = I18n.t(this, "▶ 繼續", "▶ Resume");
                playColor = CrewTheme.EMERALD_400;
                break;
            case ERROR:
                status = TextUtils.isEmpty(state.detail)
                        ? I18n.t(this, "連線出了問題", "Connection problem")
                        : state.detail;
                statusColor = CrewTheme.ROSE_500;
                playText = I18n.t(this, "▶ 再試一次", "▶ Try again");
                playColor = CrewTheme.EMERALD_400;
                break;
            case FINISHED:
                status = I18n.t(this, "故事說完了 ✨", "Story complete ✨");
                statusColor = CrewTheme.AMBER_400;
                playText = I18n.t(this, "↻ 再聽一次", "↻ Replay");
                playColor = CrewTheme.EMERALD_400;
                interaction = I18n.t(this, "喜歡這個故事嗎？可以再聽一次", "Enjoyed it? Listen again anytime");
                break;
            case DISCONNECTED:
                status = I18n.t(this, "連線已中斷", "Disconnected");
                statusColor = CrewTheme.TEXT_SECONDARY;
                break;
        }

        statusText.setText(status);
        statusText.setTextColor(statusColor);
        playPauseBtn.setText(playText);
        playPauseBtn.setBackground(makePrimaryButton(playColor));
        interactionHintText.setText(interaction);
    }

    @Override
    public void onConnected() {
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.READY));
        if (talkBtn != null) talkBtn.setEnabled(true);
    }

    @Override
    public void onDisconnected(String reason) {
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.DISCONNECTED, reason));
        if (talkBtn != null) talkBtn.setEnabled(false);
    }

    @Override
    public void onError(String error) {
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.ERROR, error));
        if (talkBtn != null) {
            talkBtn.setText(I18n.t(this, "🎤 我要說話", "🎤 I want to talk"));
            talkBtn.setEnabled(liveClient != null && !liveClient.isPaused() && !liveClient.isAwaitingUserResponse());
        }
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPageAdvanced(int newPageIndex, String chapterText) {
        updatePageDisplay(newPageIndex);
        StoryPlaybackService.updateProgress(this, newPageIndex, story.pages.size());
    }

    @Override
    public void onStoryFinished() {
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.FINISHED));
        StoryPlaybackService.stop(this);
        if (talkBtn != null) talkBtn.setEnabled(false);
    }

    @Override
    public void onAiSpeechStarted() {
        if (liveClient != null && liveClient.isAwaitingUserResponse()) {
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.READY, I18n.t(this, "波波老師正在回答你…", "Teacher is answering…")));
            if (talkBtn != null) {
                talkBtn.setText(I18n.t(this, "⏳ 波波老師回答中…", "⏳ Teacher is answering…"));
                talkBtn.setEnabled(false);
            }
        } else {
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.NARRATING));
            if (talkBtn != null && !liveClient.isPaused() && !liveClient.isUserSpeaking() && !liveClient.isAwaitingUserResponse()) {
                talkBtn.setText(I18n.t(this, "🎤 我要說話", "🎤 I want to talk"));
                talkBtn.setEnabled(true);
            }
        }
    }

    @Override
    public void onAiSpeechEnded() {
        if (liveClient != null && !liveClient.isPaused() && uiState.mode != StoryPlayerUiState.Mode.FINISHED) {
            setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.READY));
        }
        if (talkBtn != null && liveClient != null && !liveClient.isPaused() && !liveClient.isUserSpeaking() && !liveClient.isAwaitingUserResponse()) {
            talkBtn.setText(I18n.t(this, "🎤 我要說話", "🎤 I want to talk"));
            talkBtn.setEnabled(true);
        }
    }

    @Override
    public void onUserInterrupted() {
        setUiState(StoryPlayerUiState.of(StoryPlayerUiState.Mode.LISTENING));
        if (talkBtn != null) {
            talkBtn.setText(I18n.t(this, "⏹️ 說完了", "⏹️ Done speaking"));
            talkBtn.setEnabled(true);
        }
    }

    @Override
    public void onStatusUpdate(String status) {
        if (liveClient != null && (liveClient.isUserSpeaking() || liveClient.isAwaitingUserResponse())) {
            return;
        }
        if (uiState.mode == StoryPlayerUiState.Mode.CONNECTING && !TextUtils.isEmpty(status)) {
            statusText.setText(status);
        }
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

package com.crewpocket.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class StoryEditorActivity extends Activity {
    private static final int REQUEST_PICK_PAGE_IMAGE = 501;

    private StoryModel story;
    private int pickingImagePageIndex = -1;

    private EditText titleInput;
    private EditText emojiInput;
    private EditText summaryInput;
    private LinearLayout pagesContainer;

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

        if (story == null) {
            story = new StoryModel();
            story.title = I18n.t(this, "新繪本故事", "New Story");
            story.coverEmoji = "✨";
            story.summary = "";
            StoryModel.Page p = new StoryModel.Page();
            p.pageIndex = 0;
            p.text = "";
            p.emotion = "warm";
            story.pages.add(p);
        }

        setupUI();
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Top App Bar
        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        appBar.setBackgroundColor(CrewTheme.BG_PRIMARY);

        Button backBtn = new Button(this);
        backBtn.setText("← " + I18n.t(this, "返回", "Back"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setTextSize(12);
        backBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });
        appBar.addView(backBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));

        TextView titleView = new TextView(this);
        titleView.setText(I18n.t(this, " ✏️ 繪本故事編輯器", " ✏️ Story Editor"));
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(8), 0, 0, 0);
        appBar.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button saveTopBtn = new Button(this);
        saveTopBtn.setText(I18n.t(this, "💾 儲存", "💾 Save"));
        saveTopBtn.setTextSize(12);
        saveTopBtn.setTextColor(Color.BLACK);
        saveTopBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(CrewTheme.AMBER_400);
        sBg.setCornerRadius(dp(8));
        saveTopBtn.setBackground(sBg);
        saveTopBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        saveTopBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveStoryData();
                Toast.makeText(StoryEditorActivity.this, I18n.t(StoryEditorActivity.this, "✅ 故事已成功儲存！", "✅ Story saved successfully!"), Toast.LENGTH_SHORT).show();
            }
        });
        appBar.addView(saveTopBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        root.addView(appBar);

        // Scrollable content
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(32));

        // ── Card 1: Story Meta ──
        LinearLayout metaCard = new LinearLayout(this);
        metaCard.setOrientation(LinearLayout.VERTICAL);
        metaCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        metaCard.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, CrewTheme.BORDER_DEFAULT, 14));
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mcLp.setMargins(0, 0, 0, dp(14));
        metaCard.setLayoutParams(mcLp);

        TextView metaTitle = new TextView(this);
        metaTitle.setText(I18n.t(this, "📖 基本資訊 (Story Details)", "📖 Story Details"));
        metaTitle.setTextColor(CrewTheme.AMBER_400);
        metaTitle.setTextSize(14);
        metaTitle.setTypeface(Typeface.DEFAULT_BOLD);
        metaCard.addView(metaTitle);

        // Title and Emoji Row
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, dp(10), 0, dp(8));

        emojiInput = new EditText(this);
        emojiInput.setText(story.coverEmoji != null ? story.coverEmoji : "📖");
        emojiInput.setTextColor(Color.WHITE);
        emojiInput.setTextSize(18);
        emojiInput.setGravity(Gravity.CENTER);
        emojiInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(dp(48), dp(44));
        eLp.setMargins(0, 0, dp(10), 0);
        emojiInput.setLayoutParams(eLp);
        titleRow.addView(emojiInput);

        titleInput = new EditText(this);
        titleInput.setText(story.title != null ? story.title : "");
        titleInput.setHint(I18n.t(this, "請輸入繪本故事名稱...", "Enter story title..."));
        titleInput.setTextColor(Color.WHITE);
        titleInput.setHintTextColor(CrewTheme.TEXT_MUTED);
        titleInput.setTextSize(14);
        titleInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        titleInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        titleRow.addView(titleInput, new LinearLayout.LayoutParams(0, dp(44), 1f));
        metaCard.addView(titleRow);

        // Summary
        TextView sumLabel = new TextView(this);
        sumLabel.setText(I18n.t(this, "故事簡介 (Summary)：", "Summary:"));
        sumLabel.setTextColor(CrewTheme.TEXT_SECONDARY);
        sumLabel.setTextSize(12);
        sumLabel.setPadding(0, dp(4), 0, dp(4));
        metaCard.addView(sumLabel);

        summaryInput = new EditText(this);
        summaryInput.setText(story.summary != null ? story.summary : "");
        summaryInput.setHint(I18n.t(this, "故事的簡短大綱或引言...", "Short synopsis of this story..."));
        summaryInput.setTextColor(Color.WHITE);
        summaryInput.setHintTextColor(CrewTheme.TEXT_MUTED);
        summaryInput.setTextSize(13);
        summaryInput.setMinLines(2);
        summaryInput.setGravity(Gravity.TOP);
        summaryInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        summaryInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        metaCard.addView(summaryInput);

        content.addView(metaCard);

        // ── Card 2: Chapter Pages List ──
        LinearLayout pagesHeaderRow = new LinearLayout(this);
        pagesHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        pagesHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        pagesHeaderRow.setPadding(0, dp(6), 0, dp(8));

        TextView pagesHeader = new TextView(this);
        pagesHeader.setText(I18n.t(this, "🎨 繪本跨頁內容與插圖", "🎨 Chapters & Illustrations"));
        pagesHeader.setTextColor(Color.WHITE);
        pagesHeader.setTextSize(15);
        pagesHeader.setTypeface(Typeface.DEFAULT_BOLD);
        pagesHeaderRow.addView(pagesHeader, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button batchAiBtn = new Button(this);
        batchAiBtn.setText(I18n.t(this, "🪄 批次 AI 配圖", "🪄 Batch AI Art"));
        batchAiBtn.setTextSize(11);
        batchAiBtn.setTextColor(CrewTheme.AMBER_400);
        batchAiBtn.setTypeface(Typeface.DEFAULT_BOLD);
        batchAiBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.AMBER_400, 8));
        batchAiBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        batchAiBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                autoGenerateMissingIllustrations();
            }
        });
        pagesHeaderRow.addView(batchAiBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)));
        content.addView(pagesHeaderRow);

        pagesContainer = new LinearLayout(this);
        pagesContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(pagesContainer);

        renderPagesList();

        // ➕ Add Page Button
        Button addPageBtn = new Button(this);
        addPageBtn.setText(I18n.t(this, "➕ 新增故事跨頁 (Add Page)", "➕ Add Page"));
        addPageBtn.setTextColor(Color.WHITE);
        addPageBtn.setTextSize(13);
        addPageBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 12));
        addPageBtn.setPadding(0, dp(12), 0, dp(12));
        addPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                StoryModel.Page newP = new StoryModel.Page();
                newP.pageIndex = story.pages.size();
                newP.text = "";
                newP.emotion = "warm";
                story.pages.add(newP);
                renderPagesList();
                Toast.makeText(StoryEditorActivity.this, I18n.t(StoryEditorActivity.this, "已新增第 " + story.pages.size() + " 頁！", "Added Page " + story.pages.size()), Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams apLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        apLp.setMargins(0, dp(8), 0, dp(18));
        content.addView(addPageBtn, apLp);

        // Action Buttons Row (Save & Save+Play)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button saveOnlyBtn = new Button(this);
        saveOnlyBtn.setText(I18n.t(this, "💾 儲存繪本", "💾 Save Story"));
        saveOnlyBtn.setTextColor(Color.WHITE);
        saveOnlyBtn.setTextSize(13);
        saveOnlyBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1F2937"), CrewTheme.BORDER_DEFAULT, 12));
        saveOnlyBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveStoryData();
                Toast.makeText(StoryEditorActivity.this, I18n.t(StoryEditorActivity.this, "✅ 故事修改已儲存！", "✅ Story saved!"), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        actionRow.addView(saveOnlyBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button saveAndPlayBtn = new Button(this);
        saveAndPlayBtn.setText(I18n.t(this, "▶️ 儲存並開始播放", "▶️ Save & Play"));
        saveAndPlayBtn.setTextColor(Color.BLACK);
        saveAndPlayBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveAndPlayBtn.setTextSize(13);
        GradientDrawable spBg = new GradientDrawable();
        spBg.setColor(CrewTheme.AMBER_400);
        spBg.setCornerRadius(dp(12));
        saveAndPlayBtn.setBackground(spBg);
        saveAndPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveStoryData();
                Intent intent = new Intent(StoryEditorActivity.this, StoryPlayerActivity.class);
                intent.putExtra("EXTRA_STORY_ID", story.id);
                startActivity(intent);
                finish();
            }
        });
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, dp(48), 1.4f);
        spLp.setMargins(dp(10), 0, 0, 0);
        actionRow.addView(saveAndPlayBtn, spLp);

        content.addView(actionRow);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void renderPagesList() {
        pagesContainer.removeAllViews();

        for (int i = 0; i < story.pages.size(); i++) {
            final int pageIdx = i;
            final StoryModel.Page p = story.pages.get(i);
            p.pageIndex = i;

            LinearLayout pCard = new LinearLayout(this);
            pCard.setOrientation(LinearLayout.VERTICAL);
            pCard.setPadding(dp(14), dp(12), dp(14), dp(14));
            pCard.setBackground(CrewTheme.createCard(this, CrewTheme.BG_SURFACE, Color.parseColor("#334155"), 14));
            LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pcLp.setMargins(0, 0, 0, dp(14));
            pCard.setLayoutParams(pcLp);

            // Page Card Header (Title & Up / Down / Delete)
            LinearLayout pHeader = new LinearLayout(this);
            pHeader.setOrientation(LinearLayout.HORIZONTAL);
            pHeader.setGravity(Gravity.CENTER_VERTICAL);
            pHeader.setPadding(0, 0, 0, dp(8));

            TextView pLabel = new TextView(this);
            pLabel.setText("📄 " + I18n.t(this, "第 " + (pageIdx + 1) + " 頁", "Page " + (pageIdx + 1)));
            pLabel.setTextColor(CrewTheme.AMBER_400);
            pLabel.setTextSize(14);
            pLabel.setTypeface(Typeface.DEFAULT_BOLD);
            pHeader.addView(pLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Move Up
            if (pageIdx > 0) {
                Button upBtn = new Button(this);
                upBtn.setText("⬆️");
                upBtn.setTextSize(11);
                upBtn.setTextColor(Color.WHITE);
                upBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
                upBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        StoryModel.Page removed = story.pages.remove(pageIdx);
                        story.pages.add(pageIdx - 1, removed);
                        renderPagesList();
                    }
                });
                LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(dp(36), dp(32));
                uLp.setMargins(0, 0, dp(4), 0);
                pHeader.addView(upBtn, uLp);
            }

            // Move Down
            if (pageIdx < story.pages.size() - 1) {
                Button downBtn = new Button(this);
                downBtn.setText("⬇️");
                downBtn.setTextSize(11);
                downBtn.setTextColor(Color.WHITE);
                downBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
                downBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        StoryModel.Page removed = story.pages.remove(pageIdx);
                        story.pages.add(pageIdx + 1, removed);
                        renderPagesList();
                    }
                });
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(36), dp(32));
                dLp.setMargins(0, 0, dp(4), 0);
                pHeader.addView(downBtn, dLp);
            }

            // Delete Page
            if (story.pages.size() > 1) {
                Button delBtn = new Button(this);
                delBtn.setText("🗑️");
                delBtn.setTextSize(11);
                delBtn.setTextColor(Color.RED);
                delBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
                delBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        new AlertDialog.Builder(StoryEditorActivity.this)
                                .setTitle(I18n.t(StoryEditorActivity.this, "刪除此頁", "Delete Page"))
                                .setMessage(I18n.t(StoryEditorActivity.this, "確定要刪除第 " + (pageIdx + 1) + " 頁嗎？", "Delete page " + (pageIdx + 1) + "?"))
                                .setPositiveButton(I18n.t(StoryEditorActivity.this, "刪除", "Delete"), new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface dialog, int which) {
                                        story.pages.remove(pageIdx);
                                        renderPagesList();
                                    }
                                })
                                .setNegativeButton(I18n.t(StoryEditorActivity.this, "取消", "Cancel"), null)
                                .show();
                    }
                });
                pHeader.addView(delBtn, new LinearLayout.LayoutParams(dp(36), dp(32)));
            }

            pCard.addView(pHeader);

            // ── Illustration Preview / Picker ──
            LinearLayout imgBox = new LinearLayout(this);
            imgBox.setOrientation(LinearLayout.VERTICAL);
            imgBox.setPadding(0, 0, 0, dp(10));

            if (p.imageUri != null && !p.imageUri.isEmpty()) {
                ImageView iv = new ImageView(this);
                Bitmap bmp = StoryIllustrationGenerator.loadBitmapSafely(this, p.imageUri);
                if (bmp != null) {
                    iv.setImageBitmap(bmp);
                } else {
                    try {
                        iv.setImageURI(Uri.parse(p.imageUri));
                    } catch (Exception ignored) {}
                }
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(CrewTheme.createCard(this, Color.BLACK, CrewTheme.BORDER_DEFAULT, 10));
                LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150));
                iv.setLayoutParams(ivLp);
                imgBox.addView(iv);

                LinearLayout imgActionRow = new LinearLayout(this);
                imgActionRow.setOrientation(LinearLayout.HORIZONTAL);
                imgActionRow.setPadding(0, dp(6), 0, 0);

                Button changeImgBtn = new Button(this);
                changeImgBtn.setText(I18n.t(this, "📷 相簿", "📷 Gallery"));
                changeImgBtn.setTextSize(11);
                changeImgBtn.setTextColor(Color.WHITE);
                changeImgBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
                changeImgBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        pickingImagePageIndex = pageIdx;
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent, I18n.t(StoryEditorActivity.this, "更換插圖", "Pick Image")), REQUEST_PICK_PAGE_IMAGE);
                    }
                });
                imgActionRow.addView(changeImgBtn, new LinearLayout.LayoutParams(0, dp(34), 1f));

                Button regenAiBtn = new Button(this);
                regenAiBtn.setText(I18n.t(this, "🎨 AI 重繪", "🎨 AI Redraw"));
                regenAiBtn.setTextSize(11);
                regenAiBtn.setTextColor(CrewTheme.AMBER_400);
                regenAiBtn.setTypeface(Typeface.DEFAULT_BOLD);
                regenAiBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.AMBER_400, 8));
                regenAiBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showAiIllustrationDialog(pageIdx);
                    }
                });
                LinearLayout.LayoutParams raLp = new LinearLayout.LayoutParams(0, dp(34), 1.2f);
                raLp.setMargins(dp(6), 0, 0, 0);
                imgActionRow.addView(regenAiBtn, raLp);

                Button removeImgBtn = new Button(this);
                removeImgBtn.setText(I18n.t(this, "❌ 移除", "❌ Remove"));
                removeImgBtn.setTextSize(11);
                removeImgBtn.setTextColor(Color.parseColor("#F87171"));
                removeImgBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.BORDER_DEFAULT, 8));
                removeImgBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        p.imageUri = null;
                        renderPagesList();
                    }
                });
                LinearLayout.LayoutParams rmBgLp = new LinearLayout.LayoutParams(0, dp(34), 1f);
                rmBgLp.setMargins(dp(6), 0, 0, 0);
                imgActionRow.addView(removeImgBtn, rmBgLp);

                imgBox.addView(imgActionRow);
            } else {
                LinearLayout emptyPickRow = new LinearLayout(this);
                emptyPickRow.setOrientation(LinearLayout.HORIZONTAL);

                Button pickImgBtn = new Button(this);
                pickImgBtn.setText(I18n.t(this, "📷 相簿選圖", "📷 Gallery"));
                pickImgBtn.setTextSize(12);
                pickImgBtn.setTextColor(CrewTheme.SKY_400);
                pickImgBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), Color.parseColor("#334155"), 10));
                pickImgBtn.setPadding(0, dp(10), 0, dp(10));
                pickImgBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        pickingImagePageIndex = pageIdx;
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent, I18n.t(StoryEditorActivity.this, "選擇插圖", "Pick Image")), REQUEST_PICK_PAGE_IMAGE);
                    }
                });
                emptyPickRow.addView(pickImgBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                Button genAiBtn = new Button(this);
                genAiBtn.setText(I18n.t(this, "🎨 AI 生成插圖", "🎨 AI Illustration"));
                genAiBtn.setTextSize(12);
                genAiBtn.setTextColor(CrewTheme.AMBER_400);
                genAiBtn.setTypeface(Typeface.DEFAULT_BOLD);
                genAiBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1E293B"), CrewTheme.AMBER_400, 10));
                genAiBtn.setPadding(0, dp(10), 0, dp(10));
                genAiBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        showAiIllustrationDialog(pageIdx);
                    }
                });
                LinearLayout.LayoutParams gaLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
                gaLp.setMargins(dp(8), 0, 0, 0);
                emptyPickRow.addView(genAiBtn, gaLp);

                imgBox.addView(emptyPickRow);
            }
            pCard.addView(imgBox);

            // ── Narration Text ──
            TextView textLabel = new TextView(this);
            textLabel.setText(I18n.t(this, "旁白故事內容 (Narration)：", "Narration:"));
            textLabel.setTextColor(CrewTheme.TEXT_SECONDARY);
            textLabel.setTextSize(12);
            textLabel.setPadding(0, 0, 0, dp(4));
            pCard.addView(textLabel);

            final EditText textInput = new EditText(this);
            textInput.setText(p.text != null ? p.text : "");
            textInput.setHint(I18n.t(this, "請輸入這頁的旁白內容...", "Narration text for this page..."));
            textInput.setTextColor(Color.WHITE);
            textInput.setHintTextColor(CrewTheme.TEXT_MUTED);
            textInput.setTextSize(13);
            textInput.setMinLines(3);
            textInput.setGravity(Gravity.TOP);
            textInput.setPadding(dp(10), dp(8), dp(10), dp(8));
            textInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 8));
            textInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { p.text = s.toString(); }
                @Override public void afterTextChanged(Editable s) {}
            });
            pCard.addView(textInput);

            // ── Character & Emotion Row ──
            LinearLayout ceRow = new LinearLayout(this);
            ceRow.setOrientation(LinearLayout.HORIZONTAL);
            ceRow.setPadding(0, dp(8), 0, dp(4));

            final EditText charInput = new EditText(this);
            charInput.setText(p.characterName != null ? p.characterName : "");
            charInput.setHint(I18n.t(this, "角色 (例: 白雪公主)", "Character Name"));
            charInput.setTextColor(Color.WHITE);
            charInput.setHintTextColor(CrewTheme.TEXT_MUTED);
            charInput.setTextSize(12);
            charInput.setPadding(dp(8), dp(6), dp(8), dp(6));
            charInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 8));
            charInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { p.characterName = s.toString(); }
                @Override public void afterTextChanged(Editable s) {}
            });
            ceRow.addView(charInput, new LinearLayout.LayoutParams(0, dp(38), 1f));

            final EditText emoInput = new EditText(this);
            emoInput.setText(p.emotion != null ? p.emotion : "warm");
            emoInput.setHint(I18n.t(this, "情緒 (warm, angry, excited)", "Emotion"));
            emoInput.setTextColor(CrewTheme.AMBER_400);
            emoInput.setHintTextColor(CrewTheme.TEXT_MUTED);
            emoInput.setTextSize(12);
            emoInput.setPadding(dp(8), dp(6), dp(8), dp(6));
            emoInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 8));
            emoInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { p.emotion = s.toString(); }
                @Override public void afterTextChanged(Editable s) {}
            });
            LinearLayout.LayoutParams emoLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            emoLp.setMargins(dp(8), 0, 0, 0);
            ceRow.addView(emoInput, emoLp);
            pCard.addView(ceRow);

            // ── Dialogue Text ──
            final EditText diagInput = new EditText(this);
            diagInput.setText(p.dialogue != null ? p.dialogue : "");
            diagInput.setHint(I18n.t(this, "💬 角色生動對白 (選填)...", "💬 Character dialogue (optional)..."));
            diagInput.setTextColor(CrewTheme.SKY_400);
            diagInput.setHintTextColor(CrewTheme.TEXT_MUTED);
            diagInput.setTextSize(13);
            diagInput.setPadding(dp(10), dp(8), dp(10), dp(8));
            diagInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 8));
            diagInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { p.dialogue = s.toString(); }
                @Override public void afterTextChanged(Editable s) {}
            });
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dLp.setMargins(0, dp(4), 0, 0);
            pCard.addView(diagInput, dLp);

            pagesContainer.addView(pCard);
        }
    }

    private void saveStoryData() {
        if (titleInput != null) {
            String t = titleInput.getText().toString().trim();
            if (!t.isEmpty()) story.title = t;
        }
        if (emojiInput != null) {
            String e = emojiInput.getText().toString().trim();
            if (!e.isEmpty()) story.coverEmoji = e;
        }
        if (summaryInput != null) {
            story.summary = summaryInput.getText().toString().trim();
        }

        for (int i = 0; i < story.pages.size(); i++) {
            story.pages.get(i).pageIndex = i;
        }

        StoryRepository.saveStory(this, story);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PAGE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (pickingImagePageIndex >= 0 && pickingImagePageIndex < story.pages.size()) {
                story.pages.get(pickingImagePageIndex).imageUri = data.getData().toString();
                renderPagesList();
                Toast.makeText(this, I18n.t(this, "第 " + (pickingImagePageIndex + 1) + " 頁插圖已更新！", "Page " + (pickingImagePageIndex + 1) + " illustration updated!"), Toast.LENGTH_SHORT).show();
            }
            pickingImagePageIndex = -1;
        }
    }

    private void showAiIllustrationDialog(final int pageIdx) {
        if (pageIdx < 0 || pageIdx >= story.pages.size()) return;
        final StoryModel.Page p = story.pages.get(pageIdx);

        String apiKey = AppConfig.getGeminiApiKey(this);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(this, I18n.t(this, "請先至設定填寫 Gemini API Key！", "Please set your Gemini API Key in Settings!"), Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(18), dp(20), dp(18));
        layout.setBackgroundColor(CrewTheme.BG_SURFACE);

        TextView title = new TextView(this);
        title.setText(I18n.t(this, "🎨 AI 生成繪本插圖 (Imagen 3)", "🎨 AI Generate Illustration"));
        title.setTextColor(CrewTheme.AMBER_400);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(I18n.t(this, "選擇風格或微調提示詞，由 Google Imagen 3 為第 " + (pageIdx + 1) + " 頁彩繪插圖：", "Select style and prompt for Page " + (pageIdx + 1) + ":"));
        subtitle.setTextColor(CrewTheme.TEXT_MUTED);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        layout.addView(subtitle);

        // Style Selector Horizontal Scroll
        TextView styleLabel = new TextView(this);
        styleLabel.setText(I18n.t(this, "✨ 藝術風格：", "✨ Art Style:"));
        styleLabel.setTextColor(Color.WHITE);
        styleLabel.setTextSize(12);
        styleLabel.setTypeface(Typeface.DEFAULT_BOLD);
        styleLabel.setPadding(0, 0, 0, dp(6));
        layout.addView(styleLabel);

        final String[] styleKeys = {
                StoryIllustrationGenerator.STYLE_WATERCOLOR,
                StoryIllustrationGenerator.STYLE_3D,
                StoryIllustrationGenerator.STYLE_CRAYON,
                StoryIllustrationGenerator.STYLE_CLASSIC,
                StoryIllustrationGenerator.STYLE_ANIME
        };
        final String[] styleNames = {
                I18n.t(this, "🎨 溫馨水彩", "🎨 Watercolor"),
                I18n.t(this, "🧸 3D 動畫", "🧸 3D Cartoon"),
                I18n.t(this, "🖍️ 童趣蠟筆", "🖍️ Crayon"),
                I18n.t(this, "🏰 復古繪本", "🏰 Classic"),
                I18n.t(this, "🌸 夢幻動漫", "🌸 Anime")
        };

        final String[] selectedStyle = {StoryIllustrationGenerator.STYLE_WATERCOLOR};
        final Button[] styleBtns = new Button[styleKeys.length];

        HorizontalScrollView styleScroll = new HorizontalScrollView(this);
        styleScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);

        final EditText promptInput = new EditText(this);

        for (int i = 0; i < styleKeys.length; i++) {
            final int idx = i;
            final String sk = styleKeys[i];
            final Button sb = new Button(this);
            sb.setText(styleNames[i]);
            sb.setTextSize(11);
            sb.setSingleLine(true);
            sb.setPadding(dp(12), 0, dp(12), 0);
            boolean isSel = idx == 0;
            sb.setTextColor(isSel ? CrewTheme.AMBER_400 : Color.WHITE);
            sb.setTypeface(isSel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            sb.setBackground(CrewTheme.createCard(this, Color.parseColor(isSel ? "#1E293B" : "#0F172A"), isSel ? CrewTheme.AMBER_400 : CrewTheme.BORDER_DEFAULT, 8));

            sb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectedStyle[0] = sk;
                    for (int j = 0; j < styleBtns.length; j++) {
                        boolean sel = (j == idx);
                        styleBtns[j].setTextColor(sel ? CrewTheme.AMBER_400 : Color.WHITE);
                        styleBtns[j].setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                        styleBtns[j].setBackground(CrewTheme.createCard(StoryEditorActivity.this, Color.parseColor(sel ? "#1E293B" : "#0F172A"), sel ? CrewTheme.AMBER_400 : CrewTheme.BORDER_DEFAULT, 8));
                    }
                    promptInput.setText(StoryIllustrationGenerator.buildPrompt(story.title, p.text, sk));
                }
            });

            styleBtns[i] = sb;
            LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            if (i > 0) sbLp.setMargins(dp(6), 0, 0, 0);
            styleRow.addView(sb, sbLp);
        }
        styleScroll.addView(styleRow);
        layout.addView(styleScroll);

        // Prompt edit
        TextView promptLabel = new TextView(this);
        promptLabel.setText(I18n.t(this, "📝 提示詞 (可自由微調)：", "📝 Prompt (Editable):"));
        promptLabel.setTextColor(Color.WHITE);
        promptLabel.setTextSize(12);
        promptLabel.setTypeface(Typeface.DEFAULT_BOLD);
        promptLabel.setPadding(0, dp(12), 0, dp(6));
        layout.addView(promptLabel);

        promptInput.setText(StoryIllustrationGenerator.buildPrompt(story.title, p.text, selectedStyle[0]));
        promptInput.setTextColor(Color.WHITE);
        promptInput.setHintTextColor(CrewTheme.TEXT_MUTED);
        promptInput.setTextSize(12);
        promptInput.setMinLines(3);
        promptInput.setMaxLines(5);
        promptInput.setGravity(Gravity.TOP);
        promptInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        promptInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        layout.addView(promptInput);

        // Progress indicator container
        final LinearLayout progressBox = new LinearLayout(this);
        progressBox.setOrientation(LinearLayout.HORIZONTAL);
        progressBox.setGravity(Gravity.CENTER_VERTICAL);
        progressBox.setPadding(0, dp(12), 0, 0);
        progressBox.setVisibility(View.GONE);

        android.widget.ProgressBar pb = new android.widget.ProgressBar(this);
        progressBox.addView(pb, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView progressTxt = new TextView(this);
        progressTxt.setText(I18n.t(this, " 🎨 Google Imagen 3 正在彩繪插圖中...", " 🎨 Imagen 3 drawing illustration..."));
        progressTxt.setTextColor(CrewTheme.AMBER_400);
        progressTxt.setTextSize(12);
        progressTxt.setPadding(dp(8), 0, 0, 0);
        progressBox.addView(progressTxt);
        layout.addView(progressBox);

        // Action Buttons
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(16), 0, 0);

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        final Button cancelBtn = new Button(this);
        cancelBtn.setText(I18n.t(this, "取消", "Cancel"));
        cancelBtn.setTextColor(Color.WHITE);
        cancelBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#1F2937"), CrewTheme.BORDER_DEFAULT, 10));
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            }
        });
        actions.addView(cancelBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));

        final Button genBtn = new Button(this);
        genBtn.setText(I18n.t(this, "✨ 開始生成插圖", "✨ Generate"));
        genBtn.setTextColor(Color.BLACK);
        genBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable gBg = new GradientDrawable();
        gBg.setColor(CrewTheme.AMBER_400);
        gBg.setCornerRadius(dp(10));
        genBtn.setBackground(gBg);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(0, dp(44), 1.5f);
        gLp.setMargins(dp(10), 0, 0, 0);
        genBtn.setLayoutParams(gLp);

        genBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String prompt = promptInput.getText().toString().trim();
                if (prompt.isEmpty()) {
                    Toast.makeText(StoryEditorActivity.this, I18n.t(StoryEditorActivity.this, "提示詞不能為空！", "Prompt cannot be empty!"), Toast.LENGTH_SHORT).show();
                    return;
                }

                genBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                progressBox.setVisibility(View.VISIBLE);

                StoryIllustrationGenerator.generateIllustration(StoryEditorActivity.this, prompt, new StoryIllustrationGenerator.IllustrationCallback() {
                    @Override
                    public void onSuccess(String imageUri) {
                        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                        p.imageUri = imageUri;
                        renderPagesList();
                        Toast.makeText(StoryEditorActivity.this, I18n.t(StoryEditorActivity.this, "🎉 第 " + (pageIdx + 1) + " 頁插圖生成成功！", "🎉 Page " + (pageIdx + 1) + " illustration created!"), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        genBtn.setEnabled(true);
                        cancelBtn.setEnabled(true);
                        progressBox.setVisibility(View.GONE);
                        Toast.makeText(StoryEditorActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        actions.addView(genBtn);
        layout.addView(actions);

        builder.setView(layout);
        dialogHolder[0] = builder.create();
        dialogHolder[0].show();
    }

    private void autoGenerateMissingIllustrations() {
        final List<Integer> missingIndices = new ArrayList<>();
        for (int i = 0; i < story.pages.size(); i++) {
            StoryModel.Page p = story.pages.get(i);
            if (p.imageUri == null || p.imageUri.trim().isEmpty()) {
                missingIndices.add(i);
            }
        }

        if (missingIndices.isEmpty()) {
            Toast.makeText(this, I18n.t(this, "所有頁面皆已有插圖！", "All pages already have illustrations!"), Toast.LENGTH_SHORT).show();
            return;
        }

        String apiKey = AppConfig.getGeminiApiKey(this);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(this, I18n.t(this, "請先至設定填寫 Gemini API Key！", "Please set your Gemini API Key in Settings!"), Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(I18n.t(this, "🪄 批次 AI 生成插圖", "🪄 Batch AI Illustration"))
                .setMessage(I18n.t(this, "即將為 " + missingIndices.size() + " 個尚未配圖的跨頁依序生成水彩插圖，是否開始？", "Generate illustrations for " + missingIndices.size() + " pages without image?"))
                .setPositiveButton(I18n.t(this, "開始生成", "Start"), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        generateNextMissingIllustration(missingIndices, 0);
                    }
                })
                .setNegativeButton(I18n.t(this, "取消", "Cancel"), null)
                .show();
    }

    private void generateNextMissingIllustration(final List<Integer> indices, final int currentPos) {
        if (currentPos >= indices.size()) {
            Toast.makeText(this, I18n.t(this, "🎉 全部缺圖頁面插圖已生成完成！", "🎉 All illustrations completed!"), Toast.LENGTH_SHORT).show();
            renderPagesList();
            return;
        }

        final int pageIdx = indices.get(currentPos);
        final StoryModel.Page p = story.pages.get(pageIdx);
        Toast.makeText(this, I18n.t(this, "🎨 正在生成第 " + (pageIdx + 1) + " 頁插圖 (" + (currentPos + 1) + "/" + indices.size() + ")...", "Drawing illustration for page " + (pageIdx + 1) + "..."), Toast.LENGTH_SHORT).show();

        String prompt = StoryIllustrationGenerator.buildPrompt(story.title, p.text, StoryIllustrationGenerator.STYLE_WATERCOLOR);
        StoryIllustrationGenerator.generateIllustration(this, prompt, new StoryIllustrationGenerator.IllustrationCallback() {
            @Override
            public void onSuccess(String imageUri) {
                p.imageUri = imageUri;
                renderPagesList();
                generateNextMissingIllustration(indices, currentPos + 1);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(StoryEditorActivity.this, "第 " + (pageIdx + 1) + " 頁生成失敗: " + error, Toast.LENGTH_SHORT).show();
                generateNextMissingIllustration(indices, currentPos + 1);
            }
        });
    }
}

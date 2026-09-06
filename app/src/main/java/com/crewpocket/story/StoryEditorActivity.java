package com.crewpocket.story;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

/**
 * 1003 page-centric Story Editor.
 *
 * Unlike the old form/CMS layout, only one page is edited at a time.
 * A filmstrip at the bottom switches pages.
 */
public class StoryEditorActivity extends Activity {
    private static final int REQUEST_PICK_PAGE_IMAGE = 501;

    private StoryModel story;
    private int selectedPageIndex = 0;

    private EditText titleInput;
    private EditText emojiInput;
    private EditText summaryInput;

    private ImageView pageImage;
    private EditText narrationInput;
    private EditText characterInput;
    private EditText dialogueInput;
    private TextView pageCounter;
    private LinearLayout emotionRow;
    private LinearLayout filmstripRow;

    private String selectedEmotion = "warm";

    private int dp(float value) {
        return CrewTheme.dp(this, value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(CrewTheme.BG_PRIMARY);
            getWindow().setNavigationBarColor(CrewTheme.BG_PRIMARY);
        }

        getWindow().getDecorView().setBackgroundColor(CrewTheme.BG_PRIMARY);
        loadStory();
        buildUi();
        bindSelectedPage();
    }

    private void loadStory() {
        String storyId = getIntent().getStringExtra("EXTRA_STORY_ID");
        if (storyId != null) {
            story = StoryRepository.getStoryById(this, storyId);
        }

        if (story == null) {
            story = new StoryModel();
            story.id = UUID.randomUUID().toString();
            story.title = I18n.t(this, "新繪本故事", "New Story");
            story.coverEmoji = "✨";
            story.summary = "";
            story.createdAt = System.currentTimeMillis();
            story.pages.add(createBlankPage(0));
        }

        if (story.pages == null || story.pages.isEmpty()) {
            story.pages.add(createBlankPage(0));
        }
    }

    private StoryModel.Page createBlankPage(int index) {
        StoryModel.Page page = new StoryModel.Page();
        page.pageIndex = index;
        page.text = "";
        page.emotion = "warm";
        page.characterName = "";
        page.dialogue = "";
        page.imageUri = "";
        return page;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CrewTheme.BG_PRIMARY);

        root.addView(buildTopBar());

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(20));

        content.addView(buildStoryMeta());
        content.addView(buildPageEditor());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        root.addView(buildFilmstrip());
        setContentView(root);
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(10), dp(12), dp(10));

        Button back = simpleButton("‹", 42);
        back.setTextSize(28);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveCurrentInputsIntoModel();
                finish();
            }
        });
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(42)));

        TextView title = new TextView(this);
        title.setText(I18n.t(this, "編輯繪本", "Edit story"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(10), 0, 0, 0);
        bar.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        Button save = simpleButton(
                I18n.t(this, "儲存", "Save"),
                42
        );
        save.setTextColor(Color.BLACK);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(CrewTheme.AMBER_400);
        saveBg.setCornerRadius(dp(12));
        save.setBackground(saveBg);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveStory();
                Toast.makeText(
                        StoryEditorActivity.this,
                        I18n.t(
                                StoryEditorActivity.this,
                                "故事已儲存",
                                "Story saved"
                        ),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        bar.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        ));

        return bar;
    }

    private View buildStoryMeta() {
        LinearLayout card = sectionCard();

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);

        emojiInput = edit(story.coverEmoji, "📖", 18);
        emojiInput.setGravity(Gravity.CENTER);
        firstRow.addView(emojiInput, new LinearLayout.LayoutParams(
                dp(54),
                dp(50)
        ));

        titleInput = edit(
                story.title,
                I18n.t(this, "故事名稱", "Story title"),
                16
        );
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
        );
        titleLp.setMargins(dp(10), 0, 0, 0);
        firstRow.addView(titleInput, titleLp);

        card.addView(firstRow);

        summaryInput = edit(
                story.summary,
                I18n.t(this, "一句話介紹這個故事…", "Describe this story…"),
                13
        );
        summaryInput.setMinLines(2);
        summaryInput.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams summaryLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        summaryLp.setMargins(0, dp(10), 0, 0);
        card.addView(summaryInput, summaryLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);
        return card;
    }

    private View buildPageEditor() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        pageCounter = new TextView(this);
        pageCounter.setTextColor(Color.WHITE);
        pageCounter.setTextSize(17);
        pageCounter.setTypeface(Typeface.DEFAULT_BOLD);
        heading.addView(pageCounter, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        Button delete = simpleButton(
                I18n.t(this, "刪除此頁", "Delete page"),
                38
        );
        delete.setTextColor(CrewTheme.ROSE_500);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                deleteCurrentPage();
            }
        });
        heading.addView(delete);
        section.addView(heading);

        LinearLayout imageCard = sectionCard();
        pageImage = new ImageView(this);
        pageImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        pageImage.setBackgroundColor(Color.parseColor("#111827"));
        imageCard.addView(pageImage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
        ));

        LinearLayout imgBtnRow = new LinearLayout(this);
        imgBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        imgBtnRow.setPadding(0, dp(8), 0, 0);

        Button chooseImage = simpleButton(
                I18n.t(this, "🖼 本機相片", "🖼 Choose Photo"),
                42
        );
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pickImage();
            }
        });
        imgBtnRow.addView(chooseImage, new LinearLayout.LayoutParams(0, dp(42), 1f));

        Button aiImageBtn = simpleButton(
                I18n.t(this, "🎨 AI 生成插圖", "🎨 AI Generate"),
                42
        );
        aiImageBtn.setTextColor(CrewTheme.AMBER_400);
        aiImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showAiIllustrationDialog(selectedPageIndex);
            }
        });
        LinearLayout.LayoutParams aiLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        aiLp.setMargins(dp(8), 0, 0, 0);
        imgBtnRow.addView(aiImageBtn, aiLp);

        imageCard.addView(imgBtnRow);

        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        imageLp.setMargins(0, dp(10), 0, dp(12));
        section.addView(imageCard, imageLp);

        TextView narrationLabel = label(
                I18n.t(this, "故事文字", "Narration")
        );
        section.addView(narrationLabel);

        narrationInput = edit(
                "",
                I18n.t(this, "這一頁發生了什麼？", "What happens on this page?"),
                16
        );
        narrationInput.setMinLines(5);
        narrationInput.setGravity(Gravity.TOP);
        section.addView(narrationInput);

        TextView emotionLabel = label(
                I18n.t(this, "說故事的感覺", "Storytelling mood")
        );
        emotionLabel.setPadding(0, dp(14), 0, dp(6));
        section.addView(emotionLabel);

        emotionRow = new LinearLayout(this);
        emotionRow.setOrientation(LinearLayout.HORIZONTAL);
        section.addView(emotionRow);

        TextView dialogueLabel = label(
                I18n.t(this, "角色對話（選填）", "Character dialogue (optional)")
        );
        dialogueLabel.setPadding(0, dp(14), 0, dp(6));
        section.addView(dialogueLabel);

        characterInput = edit(
                "",
                I18n.t(this, "角色名稱", "Character"),
                13
        );
        section.addView(characterInput);

        dialogueInput = edit(
                "",
                I18n.t(this, "角色說了什麼？", "What does the character say?"),
                14
        );
        dialogueInput.setMinLines(3);
        dialogueInput.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams dialogueLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialogueLp.setMargins(0, dp(8), 0, dp(12));
        section.addView(dialogueInput, dialogueLp);

        return section;
    }

    private View buildFilmstrip() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(10), dp(8), dp(10), dp(10));
        shell.setBackgroundColor(CrewTheme.BG_SURFACE);

        TextView hint = new TextView(this);
        hint.setText(I18n.t(
                this,
                "故事頁面",
                "Pages"
        ));
        hint.setTextColor(CrewTheme.TEXT_MUTED);
        hint.setTextSize(11);
        hint.setPadding(dp(4), 0, 0, dp(5));
        shell.addView(hint);

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);

        filmstripRow = new LinearLayout(this);
        filmstripRow.setOrientation(LinearLayout.HORIZONTAL);
        scroller.addView(filmstripRow);

        shell.addView(scroller);
        renderFilmstrip();
        return shell;
    }

    private void bindSelectedPage() {
        if (story.pages.isEmpty()) {
            story.pages.add(createBlankPage(0));
        }

        selectedPageIndex = Math.max(
                0,
                Math.min(selectedPageIndex, story.pages.size() - 1)
        );

        StoryModel.Page page = story.pages.get(selectedPageIndex);
        selectedEmotion = page.emotion != null && !page.emotion.isEmpty()
                ? page.emotion
                : "warm";

        pageCounter.setText(I18n.t(
                this,
                "第 " + (selectedPageIndex + 1) + " 頁",
                "Page " + (selectedPageIndex + 1)
        ));

        narrationInput.setText(page.text != null ? page.text : "");
        characterInput.setText(
                page.characterName != null ? page.characterName : ""
        );
        dialogueInput.setText(page.dialogue != null ? page.dialogue : "");

        showPageImage(page);
        renderEmotionChips();
        renderFilmstrip();
    }

    private void saveCurrentInputsIntoModel() {
        if (story == null || story.pages.isEmpty()) return;

        story.title = titleInput != null
                ? titleInput.getText().toString().trim()
                : story.title;
        story.coverEmoji = emojiInput != null
                ? emojiInput.getText().toString().trim()
                : story.coverEmoji;
        story.summary = summaryInput != null
                ? summaryInput.getText().toString().trim()
                : story.summary;

        StoryModel.Page page = story.pages.get(selectedPageIndex);
        page.text = narrationInput.getText().toString().trim();
        page.characterName = characterInput.getText().toString().trim();
        page.dialogue = dialogueInput.getText().toString().trim();
        page.emotion = selectedEmotion;
        normalizePageIndexes();
    }

    private void switchPage(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= story.pages.size()) return;
        saveCurrentInputsIntoModel();
        selectedPageIndex = targetIndex;
        bindSelectedPage();
    }

    private void addPage() {
        saveCurrentInputsIntoModel();
        story.pages.add(createBlankPage(story.pages.size()));
        selectedPageIndex = story.pages.size() - 1;
        bindSelectedPage();
    }

    private void deleteCurrentPage() {
        if (story.pages.size() <= 1) {
            Toast.makeText(
                    this,
                    I18n.t(this, "故事至少要保留一頁", "Keep at least one page"),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        story.pages.remove(selectedPageIndex);
        selectedPageIndex = Math.min(
                selectedPageIndex,
                story.pages.size() - 1
        );
        normalizePageIndexes();
        bindSelectedPage();
    }

    private void normalizePageIndexes() {
        for (int i = 0; i < story.pages.size(); i++) {
            story.pages.get(i).pageIndex = i;
        }
    }

    private void renderFilmstrip() {
        if (filmstripRow == null) return;
        filmstripRow.removeAllViews();

        for (int i = 0; i < story.pages.size(); i++) {
            final int pageIndex = i;
            boolean selected = pageIndex == selectedPageIndex;

            Button pageButton = new Button(this);
            pageButton.setText(String.valueOf(pageIndex + 1));
            pageButton.setTextSize(13);
            pageButton.setTypeface(
                    selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT
            );
            pageButton.setTextColor(
                    selected ? Color.BLACK : Color.WHITE
            );

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(
                    selected
                            ? CrewTheme.AMBER_400
                            : Color.parseColor("#1F2937")
            );
            bg.setStroke(
                    dp(1),
                    selected
                            ? CrewTheme.AMBER_400
                            : CrewTheme.BORDER_DEFAULT
            );
            pageButton.setBackground(bg);
            pageButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    switchPage(pageIndex);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(52),
                    dp(46)
            );
            lp.setMargins(0, 0, dp(8), 0);
            filmstripRow.addView(pageButton, lp);
        }

        Button add = new Button(this);
        add.setText("+");
        add.setTextSize(20);
        add.setTextColor(CrewTheme.AMBER_400);
        add.setBackground(CrewTheme.createCard(
                this,
                Color.parseColor("#111827"),
                CrewTheme.BORDER_GOLD,
                12
        ));
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                addPage();
            }
        });
        filmstripRow.addView(add, new LinearLayout.LayoutParams(
                dp(52),
                dp(46)
        ));
    }

    private void renderEmotionChips() {
        emotionRow.removeAllViews();

        addEmotionChip("😊", I18n.t(this, "溫柔", "Warm"), "warm");
        addEmotionChip("🤩", I18n.t(this, "興奮", "Excited"), "excited");
        addEmotionChip("😮", I18n.t(this, "神秘", "Mysterious"), "mysterious");
        addEmotionChip("🤫", I18n.t(this, "輕聲", "Whisper"), "whisper");
        addEmotionChip("😨", I18n.t(this, "緊張", "Scary"), "scary");
    }

    private void addEmotionChip(
            String emoji,
            String label,
            final String value
    ) {
        boolean selected = value.equals(selectedEmotion);

        Button button = new Button(this);
        button.setText(emoji + " " + label);
        button.setTextSize(11);
        button.setTextColor(selected ? Color.BLACK : Color.WHITE);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(
                selected
                        ? CrewTheme.AMBER_400
                        : Color.parseColor("#1F2937")
        );
        button.setBackground(bg);

        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectedEmotion = value;
                renderEmotionChips();
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
        );
        lp.setMargins(0, 0, dp(5), 0);
        emotionRow.addView(button, lp);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_PAGE_IMAGE);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_PICK_PAGE_IMAGE
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }

        StoryModel.Page page = story.pages.get(selectedPageIndex);
        page.imageUri = uri.toString();
        showPageImage(page);
        renderFilmstrip();
    }

    private void showPageImage(StoryModel.Page page) {
        if (pageImage == null) return;

        pageImage.setImageDrawable(null);
        pageImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (page.imageUri == null || page.imageUri.trim().isEmpty()) {
            pageImage.setBackgroundColor(Color.parseColor("#111827"));
            pageImage.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        try {
            pageImage.setImageURI(Uri.parse(page.imageUri));
        } catch (Exception e) {
            pageImage.setBackgroundColor(Color.parseColor("#111827"));
            pageImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    private void showAiIllustrationDialog(final int pageIdx) {
        if (pageIdx < 0 || pageIdx >= story.pages.size()) return;
        saveCurrentInputsIntoModel();
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

        // Prompt edit header with AI Enhance button
        LinearLayout promptHeaderRow = new LinearLayout(this);
        promptHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        promptHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        promptHeaderRow.setPadding(0, dp(12), 0, dp(6));

        TextView promptLabel = new TextView(this);
        promptLabel.setText(I18n.t(this, "📝 提示詞：", "📝 Prompt:"));
        promptLabel.setTextColor(Color.WHITE);
        promptLabel.setTextSize(12);
        promptLabel.setTypeface(Typeface.DEFAULT_BOLD);
        promptHeaderRow.addView(promptLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Button enhancePromptBtn = new Button(this);
        enhancePromptBtn.setText(I18n.t(this, "🪄 AI 魔法優化", "🪄 AI Enhance"));
        enhancePromptBtn.setTextSize(11);
        enhancePromptBtn.setTextColor(CrewTheme.SKY_400);
        enhancePromptBtn.setTypeface(Typeface.DEFAULT_BOLD);
        enhancePromptBtn.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.SKY_400, 6));
        enhancePromptBtn.setPadding(dp(10), 0, dp(10), 0);
        promptHeaderRow.addView(enhancePromptBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)));
        layout.addView(promptHeaderRow);

        promptInput.setText(StoryIllustrationGenerator.buildPrompt(story.title, p.text, selectedStyle[0]));
        promptInput.setTextColor(Color.WHITE);
        promptInput.setHintTextColor(CrewTheme.TEXT_MUTED);
        promptInput.setTextSize(12);
        promptInput.setMinLines(4);
        promptInput.setMaxLines(10);
        promptInput.setGravity(Gravity.TOP);
        promptInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        promptInput.setBackground(CrewTheme.createCard(this, Color.parseColor("#0F172A"), CrewTheme.BORDER_DEFAULT, 10));
        layout.addView(promptInput);

        enhancePromptBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                enhancePromptBtn.setEnabled(false);
                enhancePromptBtn.setText(I18n.t(StoryEditorActivity.this, "⏳ 構思中...", "⏳ Enhancing..."));
                StoryIllustrationGenerator.enhancePromptWithGemini(
                        StoryEditorActivity.this,
                        story.title,
                        p.text,
                        p.characterName,
                        p.emotion,
                        selectedStyle[0],
                        new StoryIllustrationGenerator.PromptEnhanceCallback() {
                            @Override
                            public void onSuccess(String enhancedPrompt) {
                                enhancePromptBtn.setEnabled(true);
                                enhancePromptBtn.setText(I18n.t(StoryEditorActivity.this, "🪄 AI 魔法優化", "🪄 AI Enhance"));
                                promptInput.setText(enhancedPrompt);
                                Toast.makeText(StoryEditorActivity.this, I18n.t(StoryEditorActivity.this, "✨ 已由 Gemini 擴充為頂級繪本提示詞！", "✨ Enhanced by Gemini!"), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                enhancePromptBtn.setEnabled(true);
                                enhancePromptBtn.setText(I18n.t(StoryEditorActivity.this, "🪄 AI 魔法優化", "🪄 AI Enhance"));
                                Toast.makeText(StoryEditorActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        });

        // Progress indicator container
        final LinearLayout progressBox = new LinearLayout(this);
        progressBox.setOrientation(LinearLayout.HORIZONTAL);
        progressBox.setGravity(Gravity.CENTER_VERTICAL);
        progressBox.setPadding(0, dp(12), 0, 0);
        progressBox.setVisibility(View.GONE);

        ProgressBar pb = new ProgressBar(this);
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
                        showPageImage(p);
                        renderFilmstrip();
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

    private void saveStory() {
        saveCurrentInputsIntoModel();

        if (story.title == null || story.title.trim().isEmpty()) {
            story.title = I18n.t(this, "未命名故事", "Untitled story");
        }
        if (story.coverEmoji == null || story.coverEmoji.trim().isEmpty()) {
            story.coverEmoji = "📖";
        }

        StoryRepository.saveStory(this, story);
    }

    private LinearLayout sectionCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(CrewTheme.createCard(
                this,
                CrewTheme.BG_SURFACE,
                CrewTheme.BORDER_DEFAULT,
                16
        ));
        return card;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(CrewTheme.TEXT_SECONDARY);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, 0, 0, dp(6));
        return view;
    }

    private EditText edit(String value, String hint, int textSize) {
        EditText input = new EditText(this);
        input.setText(value != null ? value : "");
        input.setHint(hint);
        input.setTextSize(textSize);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(CrewTheme.TEXT_MUTED);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(CrewTheme.createCard(
                this,
                Color.parseColor("#0F172A"),
                CrewTheme.BORDER_DEFAULT,
                12
        ));
        return input;
    }

    private Button simpleButton(String text, int heightDp) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setBackground(CrewTheme.createCard(
                this,
                Color.parseColor("#1E293B"),
                CrewTheme.BORDER_DEFAULT,
                10
        ));
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setMinHeight(dp(heightDp));
        return button;
    }

    @Override
    public void onBackPressed() {
        saveCurrentInputsIntoModel();
        super.onBackPressed();
    }
}

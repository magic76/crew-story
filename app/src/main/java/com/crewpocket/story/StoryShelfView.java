package com.crewpocket.story;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Child-first shelf surface.
 *
 * Keeps MainActivity focused on navigation/settings instead of constructing
 * every story card itself.
 */
public final class StoryShelfView extends LinearLayout {
    public interface Listener {
        void onPlayStory(StoryModel story);
        void onEditStory(StoryModel story);
        void onDeleteStory(StoryModel story);
        void onCreateStory();
    }

    private final Context context;
    private final Listener listener;

    public StoryShelfView(Context context, Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;

        setOrientation(VERTICAL);
        render();
    }

    private int dp(float value) {
        return CrewTheme.dp(context, value);
    }

    private void render() {
        removeAllViews();

        TextView greeting = new TextView(context);
        greeting.setText(I18n.t(
                context,
                "今晚想聽哪個故事？",
                "Which story should we read?"
        ));
        greeting.setTextColor(Color.WHITE);
        greeting.setTextSize(24);
        greeting.setTypeface(Typeface.DEFAULT_BOLD);
        greeting.setPadding(0, dp(8), 0, dp(4));
        addView(greeting);

        TextView sub = new TextView(context);
        sub.setText(I18n.t(
                context,
                "點一本繪本，就讓波波老師開始說故事。",
                "Pick a book and let the storyteller begin."
        ));
        sub.setTextColor(CrewTheme.TEXT_SECONDARY);
        sub.setTextSize(13);
        sub.setPadding(0, 0, 0, dp(18));
        addView(sub);

        List<StoryModel> stories = StoryRepository.getStories(context);
        for (final StoryModel story : stories) {
            addView(buildBookCard(story));
        }

        Button create = new Button(context);
        create.setText(I18n.t(
                context,
                "＋ 創作新故事",
                "＋ Create a new story"
        ));
        create.setTextSize(14);
        create.setTextColor(Color.BLACK);
        create.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CrewTheme.AMBER_400);
        bg.setCornerRadius(dp(14));
        create.setBackground(bg);
        create.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onCreateStory();
            }
        });

        LayoutParams lp = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        lp.setMargins(0, dp(8), 0, dp(24));
        addView(create, lp);
    }

    private View buildBookCard(final StoryModel story) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(14), dp(16));
        card.setBackground(CrewTheme.createCard(
                context,
                CrewTheme.BG_SURFACE,
                CrewTheme.BORDER_DEFAULT,
                18
        ));

        TextView emoji = new TextView(context);
        emoji.setText(
                story.coverEmoji != null && !story.coverEmoji.isEmpty()
                        ? story.coverEmoji
                        : "📖"
        );
        emoji.setTextSize(42);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, new LayoutParams(dp(64), dp(72)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(VERTICAL);
        info.setPadding(dp(12), 0, dp(8), 0);

        TextView title = new TextView(context);
        title.setText(story.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title);

        TextView meta = new TextView(context);
        meta.setText(I18n.t(
                context,
                story.pages.size() + " 頁",
                story.pages.size() + " pages"
        ));
        meta.setTextColor(CrewTheme.TEXT_MUTED);
        meta.setTextSize(12);
        meta.setPadding(0, dp(5), 0, 0);
        info.addView(meta);

        card.addView(info, new LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView play = new TextView(context);
        play.setText("▶");
        play.setTextColor(Color.BLACK);
        play.setTextSize(18);
        play.setTypeface(Typeface.DEFAULT_BOLD);
        play.setGravity(Gravity.CENTER);

        GradientDrawable playBg = new GradientDrawable();
        playBg.setColor(CrewTheme.AMBER_400);
        playBg.setShape(GradientDrawable.OVAL);
        play.setBackground(playBg);

        card.addView(play, new LayoutParams(dp(48), dp(48)));

        card.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onPlayStory(story);
            }
        });

        card.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBookActions(story);
                return true;
            }
        });

        LayoutParams lp = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private void showBookActions(final StoryModel story) {
        String[] options = new String[] {
                I18n.t(context, "▶ 開始朗讀", "▶ Start reading"),
                I18n.t(context, "✏ 編輯繪本", "✏ Edit story"),
                I18n.t(context, "🗑 刪除故事", "🗑 Delete story")
        };

        new AlertDialog.Builder(context)
                .setTitle("《" + story.title + "》")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener == null) return;
                        if (which == 0) listener.onPlayStory(story);
                        if (which == 1) listener.onEditStory(story);
                        if (which == 2) listener.onDeleteStory(story);
                    }
                })
                .show();
    }
}

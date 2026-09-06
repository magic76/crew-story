package com.crewpocket.story;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Current persistence implementation.
 *
 * Kept intentionally compatible with the existing keys/data so 1003 does not
 * invalidate any stories already saved on the device.
 */
public final class SharedPreferencesStoryStore implements StoryStore {
    private static final String PREF_STORIES = "crew_saved_stories";
    private static final String KEY_STORY_LIST = "story_json_list";

    @Override
    public synchronized List<StoryModel> list(Context context) {
        List<StoryModel> result = new ArrayList<StoryModel>();
        SharedPreferences sp = context.getSharedPreferences(
                PREF_STORIES,
                Context.MODE_PRIVATE
        );
        String raw = sp.getString(KEY_STORY_LIST, "[]");

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) result.add(StoryModel.fromJson(obj));
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    @Override
    public synchronized StoryModel get(Context context, String storyId) {
        if (storyId == null) return null;
        for (StoryModel story : list(context)) {
            if (storyId.equals(story.id)) return story;
        }
        return null;
    }

    @Override
    public synchronized void save(Context context, StoryModel story) {
        if (story == null) return;

        List<StoryModel> all = list(context);
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            StoryModel existing = all.get(i);
            if (existing != null
                    && existing.id != null
                    && existing.id.equals(story.id)) {
                all.set(i, story);
                replaced = true;
                break;
            }
        }

        if (!replaced) all.add(0, story);
        saveAll(context, all);
    }

    @Override
    public synchronized void saveAll(Context context, List<StoryModel> stories) {
        JSONArray arr = new JSONArray();
        if (stories != null) {
            for (StoryModel story : stories) {
                if (story != null) arr.put(story.toJson());
            }
        }

        context.getSharedPreferences(PREF_STORIES, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STORY_LIST, arr.toString())
                .apply();
    }

    @Override
    public synchronized void delete(Context context, String storyId) {
        if (storyId == null) return;

        List<StoryModel> all = list(context);
        for (int i = all.size() - 1; i >= 0; i--) {
            StoryModel story = all.get(i);
            if (story != null && storyId.equals(story.id)) {
                all.remove(i);
            }
        }
        saveAll(context, all);
    }
}

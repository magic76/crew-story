package com.crewpocket.story;

import android.content.Context;

import java.util.List;

/**
 * Persistence boundary for stories.
 *
 * UI and domain code should depend on this contract rather than on
 * SharedPreferences/Room directly. 1003 keeps the current storage format but
 * makes a future Room migration local to one implementation.
 */
public interface StoryStore {
    List<StoryModel> list(Context context);
    StoryModel get(Context context, String storyId);
    void save(Context context, StoryModel story);
    void saveAll(Context context, List<StoryModel> stories);
    void delete(Context context, String storyId);
}

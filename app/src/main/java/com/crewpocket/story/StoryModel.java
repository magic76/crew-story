package com.crewpocket.story;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StoryModel implements Serializable {
    public String id;
    public String title;
    public String summary;
    public String coverEmoji;
    public String coverImageUri;
    public long createdAt;
    public List<Page> pages = new ArrayList<>();

    public static class Page implements Serializable {
        public int pageIndex; // 0-based
        public String imageUri; // Local image URI or base64 or empty
        public String text;     // Narrator / story text for this page
        public String emotion;  // e.g. "warm", "excited", "mysterious", "whisper"
        public String characterName; // Optional character speaking
        public String dialogue; // Direct spoken dialogue

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("pageIndex", pageIndex);
                obj.put("imageUri", imageUri != null ? imageUri : "");
                obj.put("text", text != null ? text : "");
                obj.put("emotion", emotion != null ? emotion : "normal");
                obj.put("characterName", characterName != null ? characterName : "");
                obj.put("dialogue", dialogue != null ? dialogue : "");
            } catch (Exception ignored) {}
            return obj;
        }

        public static Page fromJson(JSONObject obj) {
            Page p = new Page();
            p.pageIndex = obj.optInt("pageIndex", 0);
            p.imageUri = obj.optString("imageUri", "");
            p.text = obj.optString("text", "");
            p.emotion = obj.optString("emotion", "normal");
            p.characterName = obj.optString("characterName", "");
            p.dialogue = obj.optString("dialogue", "");
            return p;
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("summary", summary);
            obj.put("coverEmoji", coverEmoji != null ? coverEmoji : "📖");
            obj.put("coverImageUri", coverImageUri != null ? coverImageUri : "");
            obj.put("createdAt", createdAt);

            JSONArray pArray = new JSONArray();
            for (Page p : pages) {
                pArray.put(p.toJson());
            }
            obj.put("pages", pArray);
        } catch (Exception ignored) {}
        return obj;
    }

    public static StoryModel fromJson(JSONObject obj) {
        StoryModel s = new StoryModel();
        s.id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
        s.title = obj.optString("title", "未命名故事");
        s.summary = obj.optString("summary", "");
        s.coverEmoji = obj.optString("coverEmoji", "📖");
        s.coverImageUri = obj.optString("coverImageUri", "");
        s.createdAt = obj.optLong("createdAt", System.currentTimeMillis());

        JSONArray pArray = obj.optJSONArray("pages");
        if (pArray != null) {
            for (int i = 0; i < pArray.length(); i++) {
                JSONObject po = pArray.optJSONObject(i);
                if (po != null) {
                    s.pages.add(Page.fromJson(po));
                }
            }
        }
        return s;
    }
}

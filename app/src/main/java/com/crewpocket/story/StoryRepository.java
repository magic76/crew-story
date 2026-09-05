package com.crewpocket.story;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StoryRepository {
    private static final String PREF_STORIES = "crew_saved_stories";
    private static final String KEY_STORY_LIST = "story_json_list";
    private static final String KEY_SNOW_WHITE_INITIALIZED = "snow_white_init_v1";

    public static synchronized List<StoryModel> getStories(Context context) {
        List<StoryModel> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_STORIES, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_STORY_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    list.add(StoryModel.fromJson(obj));
                }
            }
        } catch (Exception ignored) {}

        boolean hasSnowWhite = false;
        for (StoryModel s : list) {
            if ("sample_snow_white".equals(s.id) || (s.title != null && s.title.contains("白雪公主"))) {
                hasSnowWhite = true;
                break;
            }
        }

        if (!hasSnowWhite) {
            list.add(0, createSnowWhiteStory());
            saveStories(context, list);
        }

        return list;
    }

    public static synchronized void saveStories(Context context, List<StoryModel> stories) {
        JSONArray arr = new JSONArray();
        for (StoryModel s : stories) {
            arr.put(s.toJson());
        }
        context.getSharedPreferences(PREF_STORIES, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STORY_LIST, arr.toString())
                .apply();
    }

    public static synchronized void addStory(Context context, StoryModel story) {
        List<StoryModel> list = getStories(context);
        list.add(0, story);
        saveStories(context, list);
    }

    public static synchronized void saveStory(Context context, StoryModel story) {
        updateStory(context, story);
    }

    public static synchronized void updateStory(Context context, StoryModel story) {
        if (story == null) return;
        List<StoryModel> list = getStories(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(story.id)) {
                list.set(i, story);
                saveStories(context, list);
                return;
            }
        }
        list.add(0, story);
        saveStories(context, list);
    }

    public static synchronized void deleteStory(Context context, String storyId) {
        List<StoryModel> list = getStories(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(storyId)) {
                list.remove(i);
                break;
            }
        }
        saveStories(context, list);
    }

    public static synchronized StoryModel getStoryById(Context context, String storyId) {
        List<StoryModel> list = getStories(context);
        for (StoryModel s : list) {
            if (s.id.equals(storyId)) return s;
        }
        return null;
    }

    public static StoryModel createSnowWhiteStory() {
        StoryModel s = new StoryModel();
        s.id = "sample_snow_white";
        s.title = "白雪公主與七個小矮人";
        s.summary = "美麗善良的白雪公主在森林中遇見了七個可愛的小矮人，展開了一段充滿愛、勇氣與魔法的經典童話冒險。";
        s.coverEmoji = "🍎";
        s.createdAt = System.currentTimeMillis();

        StoryModel.Page p1 = new StoryModel.Page();
        p1.pageIndex = 0;
        p1.characterName = "魔鏡與壞王后";
        p1.emotion = "mysterious";
        p1.text = "在很久很久以前的一座美麗城堡裡，住著一位皮膚白得像雪、嘴唇紅得像玫瑰的小公主，大家都叫她「白雪公主」。但是壞王后非常嫉妒她的美麗，每天都會對著魔鏡問：「魔鏡魔鏡，誰是世界上最美麗的女人？」";
        p1.dialogue = "魔鏡回答：『王后啊，您雖然美麗，但森林深處的白雪公主比您美麗一千倍！』";
        s.pages.add(p1);

        StoryModel.Page p2 = new StoryModel.Page();
        p2.pageIndex = 1;
        p2.characterName = "白雪公主";
        p2.emotion = "excited";
        p2.text = "善良的獵人不忍心傷害白雪公主，悄悄讓她逃進了廣闊神秘的大森林。在可愛的小松鼠和小鹿的帶領下，白雪公主穿過花叢，驚喜地發現了一座精緻小巧的森林木屋！";
        p2.dialogue = "哇！這裡有七張小巧的床和七把可愛的椅子，連小盤子也是小小的，是誰住在這麼溫馨的地方呢？";
        s.pages.add(p2);

        StoryModel.Page p3 = new StoryModel.Page();
        p3.pageIndex = 2;
        p3.characterName = "萬事通小矮人";
        p3.emotion = "warm";
        p3.text = "天色漸漸變暗了，在礦山辛苦工作了一整天的七個小矮人唱著歡快的歌回到家，驚奇地發現了正在熟睡的白雪公主。當白雪公主醒來向他們述說自己的遭遇後，小矮人們熱情地邀請她留下來。";
        p3.dialogue = "善良的白雪公主，請留在我們的小屋一起生活吧！我們會齊心協力保護妳的安全！";
        s.pages.add(p3);

        StoryModel.Page p4 = new StoryModel.Page();
        p4.pageIndex = 3;
        p4.characterName = "老巫婆";
        p4.emotion = "scary";
        p4.text = "然而，壞王后得知白雪公主還活著，便用黑魔法把自己偽裝成賣水果的老農婦，趁著小矮人們出門工作時，來到木屋窗前，遞給白雪公主一顆鮮紅誘人的魔法蘋果。";
        p4.dialogue = "美麗的姑娘，嚐一口這顆全天下最香甜的紅蘋果吧，只要咬上一口，就能實現妳所有願望喔……";
        s.pages.add(p4);

        StoryModel.Page p5 = new StoryModel.Page();
        p5.pageIndex = 4;
        p5.characterName = "英勇的王子";
        p5.emotion = "tender";
        p5.text = "白雪公主咬了一口蘋果，便陷入了深深的沉睡。傷心欲絕的小矮人們為她打造了一座透明的水晶棺。這時，一位英勇正直的王子穿過森林來到這裡，被白雪公主的善良與純潔深深打動。";
        p5.dialogue = "沉睡中的美麗公主，請醒過來吧！願真誠與善良的力量驅散所有黑暗！";
        s.pages.add(p5);

        StoryModel.Page p6 = new StoryModel.Page();
        p6.pageIndex = 5;
        p6.characterName = "白雪公主";
        p6.emotion = "joyful";
        p6.text = "王子真摯的呼喚打破了邪惡的詛咒，白雪公主奇蹟般地睜開了雙眼！小矮人們高興得又唱又跳，整座森林的動物們都歡欣鼓舞。從此，白雪公主和大家過著幸福快樂的美好生活。";
        p6.dialogue = "謝謝大家！只要心中永遠保有愛與善良，世界上就沒有克服不了的困難！";
        s.pages.add(p6);

        return s;
    }
}

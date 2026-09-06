package com.crewpocket.story;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain-facing story repository.
 *
 * 1003 architecture:
 * Activity/View -> StoryRepository -> StoryStore -> persistence implementation
 *
 * The public static API is intentionally preserved so older call sites keep
 * compiling. The storage mechanism is now replaceable.
 */
public final class StoryRepository {
    private static StoryStore store = new SharedPreferencesStoryStore();

    private StoryRepository() {
    }

    /**
     * Package-visible on purpose: tests or a future application bootstrap can
     * swap storage without changing Activity code.
     */
    static synchronized void setStoreForTesting(StoryStore replacement) {
        store = replacement != null
                ? replacement
                : new SharedPreferencesStoryStore();
    }

    public static synchronized List<StoryModel> getStories(Context context) {
        List<StoryModel> list = store.list(context);
        ensureBuiltInStories(context, list);
        return list;
    }

    public static synchronized StoryModel getStoryById(
            Context context,
            String storyId
    ) {
        StoryModel story = store.get(context, storyId);
        if (story != null) return story;

        // Ensure the bundled sample still materializes for old/new installs.
        List<StoryModel> list = getStories(context);
        for (StoryModel item : list) {
            if (item != null && storyId != null && storyId.equals(item.id)) {
                return item;
            }
        }
        return null;
    }

    public static synchronized void saveStories(
            Context context,
            List<StoryModel> stories
    ) {
        store.saveAll(context, stories);
    }

    public static synchronized void addStory(
            Context context,
            StoryModel story
    ) {
        store.save(context, story);
    }

    public static synchronized void saveStory(
            Context context,
            StoryModel story
    ) {
        store.save(context, story);
    }

    public static synchronized void updateStory(
            Context context,
            StoryModel story
    ) {
        store.save(context, story);
    }

    public static synchronized void deleteStory(
            Context context,
            String storyId
    ) {
        store.delete(context, storyId);
    }

    private static void ensureBuiltInStories(
            Context context,
            List<StoryModel> list
    ) {
        boolean hasSnowWhite = false;

        for (StoryModel story : list) {
            if (story == null) continue;
            if ("sample_snow_white".equals(story.id)
                    || (story.title != null && story.title.contains("白雪公主"))) {
                hasSnowWhite = true;
                break;
            }
        }

        if (!hasSnowWhite) {
            list.add(0, createSnowWhiteStory());
            store.saveAll(context, list);
        }
    }

    public static StoryModel createSnowWhiteStory() {
        StoryModel s = new StoryModel();
        s.id = "sample_snow_white";
        s.title = "白雪公主與七個小矮人";
        s.summary = "美麗善良的白雪公主在森林中遇見了七個可愛的小矮人，展開了一段充滿愛、勇氣與魔法的經典童話冒險。";
        s.coverEmoji = "🍎";
        s.createdAt = System.currentTimeMillis();

        s.pages.add(page(
                0,
                "魔鏡與壞王后",
                "mysterious",
                "在很久很久以前的一座美麗城堡裡，住著一位皮膚白得像雪、嘴唇紅得像玫瑰的小公主，大家都叫她「白雪公主」。但是壞王后非常嫉妒她的美麗，每天都會對著魔鏡問：「魔鏡魔鏡，誰是世界上最美麗的女人？」",
                "魔鏡回答：『王后啊，您雖然美麗，但森林深處的白雪公主比您美麗一千倍！』"
        ));
        s.pages.add(page(
                1,
                "白雪公主",
                "excited",
                "善良的獵人不忍心傷害白雪公主，悄悄讓她逃進了廣闊神秘的大森林。在可愛的小松鼠和小鹿的帶領下，白雪公主穿過花叢，驚喜地發現了一座精緻小巧的森林木屋！",
                "哇！這裡有七張小巧的床和七把可愛的椅子，連小盤子也是小小的，是誰住在這麼溫馨的地方呢？"
        ));
        s.pages.add(page(
                2,
                "萬事通小矮人",
                "warm",
                "天色漸漸變暗了，在礦山辛苦工作了一整天的七個小矮人唱著歡快的歌回到家，驚奇地發現了正在熟睡的白雪公主。當白雪公主醒來向他們述說自己的遭遇後，小矮人們熱情地邀請她留下來。",
                "善良的白雪公主，請留在我們的小屋一起生活吧！我們會齊心協力保護妳的安全！"
        ));
        s.pages.add(page(
                3,
                "老巫婆",
                "scary",
                "然而，壞王后得知白雪公主還活著，便用黑魔法把自己偽裝成賣水果的老農婦，趁著小矮人們出門工作時，來到木屋窗前，遞給白雪公主一顆鮮紅誘人的魔法蘋果。",
                "美麗的姑娘，嚐一口這顆全天下最香甜的紅蘋果吧，只要咬上一口，就能實現妳所有願望喔……"
        ));
        s.pages.add(page(
                4,
                "英勇的王子",
                "tender",
                "白雪公主咬了一口蘋果，便陷入了深深的沉睡。傷心欲絕的小矮人們為她打造了一座透明的水晶棺。這時，一位英勇正直的王子穿過森林來到這裡，被白雪公主的善良與純潔深深打動。",
                "沉睡中的美麗公主，請醒過來吧！願真誠與善良的力量驅散所有黑暗！"
        ));
        s.pages.add(page(
                5,
                "白雪公主",
                "joyful",
                "王子真摯的呼喚打破了邪惡的詛咒，白雪公主奇蹟般地睜開了雙眼！小矮人們高興得又唱又跳，整座森林的動物們都歡欣鼓舞。從此，白雪公主和大家過著幸福快樂的美好生活。",
                "謝謝大家！只要心中永遠保有愛與善良，世界上就沒有克服不了的困難！"
        ));

        return s;
    }

    private static StoryModel.Page page(
            int index,
            String character,
            String emotion,
            String text,
            String dialogue
    ) {
        StoryModel.Page page = new StoryModel.Page();
        page.pageIndex = index;
        page.characterName = character;
        page.emotion = emotion;
        page.text = text;
        page.dialogue = dialogue;
        return page;
    }
}

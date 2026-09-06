package com.crewpocket.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StoryGenerator {
    private static final String TAG = "StoryGenerator";

    public static final String[] CANDIDATE_MODELS = {
            "gemini-3.6-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.0-flash",
            "gemini-3-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    };

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .build();

    public interface GenerateCallback {
        void onSuccess(StoryModel story);
        void onError(String error);
    }

    public static void generateStory(final Context context,
                                     final String userPrompt,
                                     final List<Uri> imageUris,
                                     final String style,
                                     final String language,
                                     final GenerateCallback callback) {

        final String apiKey = AppConfig.getGeminiApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            notifyError(callback, "請先至設定頁面填入有效的 Gemini API Key (BYOK)！");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Prepare images if any
                    List<String> base64Images = new ArrayList<>();
                    if (imageUris != null) {
                        for (Uri uri : imageUris) {
                            String b64 = encodeImageToBase64(context, uri);
                            if (b64 != null && !b64.isEmpty()) {
                                base64Images.add(b64);
                            }
                        }
                    }

                    // Build Gemini REST Request Payload
                    JSONObject requestBody = new JSONObject();
                    JSONArray contentsArray = new JSONArray();
                    JSONObject contentObj = new JSONObject();
                    contentObj.put("role", "user");
                    JSONArray partsArray = new JSONArray();

                    // Prompt instruction
                    String systemPrompt = "你正在為 Crew Story 的故事夥伴「阿奇 Archie」準備一本高品質兒童繪本。" +
                            "請根據使用者提供的創作靈感或圖片，創作出一篇引人入勝、生動溫暖、有起承轉合的故事繪本。\n\n" +
                            "【故事設定要求】\n" +
                            "- 語言：" + language + "\n" +
                            "- 風格：" + style + "\n" +
                            "- 請生成 4 到 8 個章節/跨頁（pages）。\n" +
                            "- 每個章節需包含 text (生動旁白)、dialogue (角色對白)、emotion (朗讀語氣如 warm, excited, mysterious, joyful, dramatic, whisper)、characterName (說話角色)。\n" +
                            "- 請務必嚴格輸出為合法 JSON 格式，不得包含 Markdown 代碼區塊外的雜訊。格式如下：\n" +
                            "{\n" +
                            "  \"title\": \"故事名稱\",\n" +
                            "  \"summary\": \"一段吸引人的簡短故事大綱\",\n" +
                            "  \"coverEmoji\": \"代表故事的可愛 Emoji 例如 🦄 🚀 🐻\",\n" +
                            "  \"pages\": [\n" +
                            "    {\n" +
                            "      \"pageIndex\": 0,\n" +
                            "      \"text\": \"故事第一頁旁白...\",\n" +
                            "      \"dialogue\": \"角色講的第一句話...\",\n" +
                            "      \"emotion\": \"warm\",\n" +
                            "      \"characterName\": \"小主角名字\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}";

                    JSONObject textPart = new JSONObject();
                    textPart.put("text", systemPrompt + "\n\n【使用者的創作靈感與要求】\n" + (userPrompt != null && !userPrompt.trim().isEmpty() ? userPrompt : "請根據圖片中的元素發揮想像力創作一個精彩動人的故事。"));
                    partsArray.put(textPart);

                    // Attach images parts
                    for (String b64 : base64Images) {
                        JSONObject imgPart = new JSONObject();
                        JSONObject inlineData = new JSONObject();
                        inlineData.put("mimeType", "image/jpeg");
                        inlineData.put("data", b64);
                        imgPart.put("inlineData", inlineData);
                        partsArray.put(imgPart);
                    }

                    contentObj.put("parts", partsArray);
                    contentsArray.put(contentObj);
                    requestBody.put("contents", contentsArray);

                    // Generation config
                    JSONObject genConfig = new JSONObject();
                    genConfig.put("temperature", 0.7);
                    try {
                        genConfig.put("responseMimeType", "application/json");
                    } catch (Exception ignored) {}
                    requestBody.put("generationConfig", genConfig);

                    tryGenerateStoryAt(0, apiKey.trim(), requestBody.toString(), imageUris, callback);

                } catch (Exception e) {
                    notifyError(callback, "故事生成前置處理錯誤: " + e.getMessage());
                }
            }
        }).start();
    }

    private static void tryGenerateStoryAt(final int modelIdx,
                                          final String apiKey,
                                          final String jsonPayload,
                                          final List<Uri> imageUris,
                                          final GenerateCallback callback) {
        if (modelIdx >= CANDIDATE_MODELS.length) {
            notifyError(callback, "所有 Gemini 模型端點請求均失敗，請檢查 API Key 權限與網路連線！");
            return;
        }

        final String model = CANDIDATE_MODELS[modelIdx];
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonPayload);
        Request request = new Request.Builder().url(endpoint).post(body).build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "Model " + model + " request failed: " + e.getMessage());
                tryGenerateStoryAt(modelIdx + 1, apiKey, jsonPayload, imageUris, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resJson = "";
                try {
                    if (response.body() != null) {
                        resJson = response.body().string();
                    }

                    if (response.isSuccessful() && !resJson.isEmpty()) {
                        JSONObject responseObj = new JSONObject(resJson);
                        JSONArray candidates = responseObj.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.optJSONObject("content");
                            if (content != null) {
                                JSONArray parts = content.optJSONArray("parts");
                                if (parts != null && parts.length() > 0) {
                                    String storyJsonStr = parts.getJSONObject(0).optString("text", "");
                                    storyJsonStr = cleanJsonOutput(storyJsonStr);
                                    JSONObject parsedStory = new JSONObject(storyJsonStr);

                                    StoryModel story = StoryModel.fromJson(parsedStory);
                                    story.id = "story_" + System.currentTimeMillis();
                                    story.createdAt = System.currentTimeMillis();

                                    // Assign image URIs to pages if images were provided
                                    if (imageUris != null && !imageUris.isEmpty()) {
                                        story.coverImageUri = imageUris.get(0).toString();
                                        for (int i = 0; i < story.pages.size(); i++) {
                                            if (i < imageUris.size()) {
                                                story.pages.get(i).imageUri = imageUris.get(i).toString();
                                            }
                                        }
                                    }

                                    notifySuccess(callback, story);
                                    return;
                                }
                            }
                        }
                    }
                    Log.w(TAG, "Model " + model + " returned HTTP " + response.code() + ": " + resJson);
                } catch (Exception e) {
                    Log.w(TAG, "Model " + model + " parse error: " + e.getMessage());
                } finally {
                    if (response != null) {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }

                // Fallback to next model
                tryGenerateStoryAt(modelIdx + 1, apiKey, jsonPayload, imageUris, callback);
            }
        });
    }

    private static String encodeImageToBase64(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (bitmap == null) return null;

            // Resize if too big (max 1024x1024)
            int maxDim = 1024;
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w > maxDim || h > maxDim) {
                float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(w * scale), Math.round(h * scale), true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    private static String cleanJsonOutput(String raw) {
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        int firstBrace = s.indexOf("{");
        int lastBrace = s.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            s = s.substring(firstBrace, lastBrace + 1);
        }
        return s.trim();
    }

    private static void notifySuccess(final GenerateCallback cb, final StoryModel story) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() { cb.onSuccess(story); }
        });
    }

    private static void notifyError(final GenerateCallback cb, final String error) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() { cb.onError(error); }
        });
    }
}

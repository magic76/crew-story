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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StoryIllustrationGenerator {
    private static final String TAG = "StoryIllustrationGen";

    public static final String[] IMAGEN_MODELS = {
            "imagen-3.0-generate-002",
            "imagen-3.0-fast-generate-001",
            "imagen-3.0-generate-001"
    };

    public static final String STYLE_WATERCOLOR = "watercolor";
    public static final String STYLE_3D = "3d";
    public static final String STYLE_CRAYON = "crayon";
    public static final String STYLE_CLASSIC = "classic";
    public static final String STYLE_ANIME = "anime";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface IllustrationCallback {
        void onSuccess(String imageUri);
        void onError(String error);
    }

    public static String buildPrompt(String storyTitle, String pageNarration, String styleKey) {
        String styleDesc;
        if (STYLE_3D.equalsIgnoreCase(styleKey)) {
            styleDesc = "Cute 3D animated film style, vibrant soft studio lighting, charming characters, colorful, whimsical storybook scene";
        } else if (STYLE_CRAYON.equalsIgnoreCase(styleKey)) {
            styleDesc = "Childlike crayon and colored pencil illustration, warm textured paper, playful, vibrant colors, sweet fairy tale atmosphere";
        } else if (STYLE_CLASSIC.equalsIgnoreCase(styleKey)) {
            styleDesc = "Classic vintage fairy tale illustration, golden age picture book art, richly detailed, soft warm glowing colors, magical ambiance";
        } else if (STYLE_ANIME.equalsIgnoreCase(styleKey)) {
            styleDesc = "Dreamy anime storybook art, bright gentle colors, expressive characters, beautiful soft background lighting";
        } else {
            // Default: Watercolor
            styleDesc = "Children's storybook watercolor and ink illustration, soft pastel tones, cozy, whimsical, high quality picture book art";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(styleDesc).append(". ");
        if (storyTitle != null && !storyTitle.trim().isEmpty()) {
            sb.append("Story Theme: ").append(storyTitle.trim()).append(". ");
        }
        if (pageNarration != null && !pageNarration.trim().isEmpty()) {
            sb.append("Scene description: ").append(pageNarration.trim()).append(". ");
        }
        sb.append("No text, no letters, no words in the image, pure illustration.");
        return sb.toString();
    }

    public static void generateIllustration(final Context context,
                                           final String prompt,
                                           final IllustrationCallback callback) {
        if (AppConfig.isImagenUnavailable(context)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    tryPollinationsFallback(context, prompt, "Free tier", callback);
                }
            }).start();
            return;
        }

        final String apiKey = AppConfig.getGeminiApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            notifyError(callback, I18n.t(context, "請先至設定填寫 Gemini API Key！", "Please enter Gemini API Key in Settings!"));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                tryImagenModel(context, 0, apiKey.trim(), prompt, "", callback);
            }
        }).start();
    }

    private static void tryImagenModel(final Context context,
                                       final int modelIdx,
                                       final String apiKey,
                                       final String prompt,
                                       final String lastError,
                                       final IllustrationCallback callback) {
        if (modelIdx >= IMAGEN_MODELS.length || AppConfig.isImagenUnavailable(context)) {
            tryPollinationsFallback(context, prompt, lastError, callback);
            return;
        }

        final String model = IMAGEN_MODELS[modelIdx];
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":predict?key=" + apiKey;

        try {
            JSONObject requestJson = new JSONObject();
            JSONArray instances = new JSONArray();
            JSONObject instance = new JSONObject();
            instance.put("prompt", prompt);
            instances.put(instance);
            requestJson.put("instances", instances);

            JSONObject params = new JSONObject();
            params.put("sampleCount", 1);
            params.put("aspectRatio", "4:3");
            params.put("outputMimeType", "image/jpeg");
            requestJson.put("parameters", params);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    requestJson.toString()
            );

            Request request = new Request.Builder()
                    .url(endpoint)
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.w(TAG, "Imagen model " + model + " failed: " + e.getMessage());
                    tryImagenModel(context, modelIdx + 1, apiKey, prompt, e.getMessage(), callback);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resStr = "";
                    String currentErr = "";
                    try {
                        if (response.body() != null) {
                            resStr = response.body().string();
                        }

                        if (response.isSuccessful() && !resStr.isEmpty()) {
                            JSONObject resObj = new JSONObject(resStr);
                            JSONArray predictions = resObj.optJSONArray("predictions");
                            if (predictions != null && predictions.length() > 0) {
                                JSONObject pred = predictions.getJSONObject(0);
                                String b64 = pred.optString("bytesBase64Encoded", "");
                                if (!b64.isEmpty()) {
                                    String savedUri = saveBase64Image(context, b64);
                                    if (savedUri != null) {
                                        notifySuccess(callback, savedUri);
                                        return;
                                    }
                                }
                            }
                        }

                        if (response.code() == 404 || response.code() == 403) {
                            // Free-tier key or unbilled project -> mark unavailable and directly fallback
                            AppConfig.setImagenUnavailable(context, true);
                            tryPollinationsFallback(context, prompt, "Free tier key", callback);
                            return;
                        }

                        if (!resStr.isEmpty()) {
                            try {
                                JSONObject errObj = new JSONObject(resStr);
                                JSONObject errorChild = errObj.optJSONObject("error");
                                if (errorChild != null) {
                                    currentErr = "HTTP " + response.code() + ": " + errorChild.optString("message", resStr);
                                } else {
                                    currentErr = "HTTP " + response.code() + ": " + resStr;
                                }
                            } catch (Exception ignored) {
                                currentErr = "HTTP " + response.code();
                            }
                        } else {
                            currentErr = "HTTP " + response.code() + " " + response.message();
                        }

                        Log.w(TAG, "Imagen " + model + " error: " + currentErr);
                    } catch (Exception e) {
                        currentErr = "Parse error: " + e.getMessage();
                        Log.w(TAG, "Imagen " + model + " parse error: " + e.getMessage());
                    } finally {
                        if (response != null) {
                            try { response.close(); } catch (Exception ignored) {}
                        }
                    }

                    // Fallback to next model
                    tryImagenModel(context, modelIdx + 1, apiKey, prompt, currentErr, callback);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to create payload for " + model, e);
            tryImagenModel(context, modelIdx + 1, apiKey, prompt, e.getMessage(), callback);
        }
    }

    private static void tryPollinationsFallback(final Context context,
                                                final String prompt,
                                                final String lastError,
                                                final IllustrationCallback callback) {
        try {
            String encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8");
            String url = "https://image.pollinations.ai/prompt/" + encodedPrompt + "?width=800&height=600&nologo=true&seed=" + (System.currentTimeMillis() % 1000000);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    notifyError(callback, I18n.t(context, "AI 繪圖生成失敗 (" + lastError + ")", "AI Image generation failed: " + lastError));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            byte[] imgBytes = response.body().bytes();
                            if (imgBytes != null && imgBytes.length > 500) {
                                String savedUri = saveRawBytesImage(context, imgBytes);
                                if (savedUri != null) {
                                    notifySuccess(callback, savedUri);
                                    return;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    finally {
                        if (response != null) {
                            try { response.close(); } catch (Exception ignored) {}
                        }
                    }
                    notifyError(callback, I18n.t(context, "AI 繪圖生成失敗 (" + lastError + ")", "AI Image generation failed (" + lastError + ")"));
                }
            });
        } catch (Exception e) {
            notifyError(callback, I18n.t(context, "AI 繪圖生成失敗 (" + lastError + ")", "AI Image generation failed (" + lastError + ")"));
        }
    }

    private static String saveRawBytesImage(Context context, byte[] imageBytes) {
        try {
            File dir = new File(context.getFilesDir(), "illustrations");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "story_art_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(imageBytes);
            fos.flush();
            fos.close();
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save raw image to disk", e);
            return null;
        }
    }

    private static String saveBase64Image(Context context, String base64Data) {
        try {
            byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return saveRawBytesImage(context, imageBytes);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode base64 image", e);
            return null;
        }
    }

    public static Bitmap loadBitmapSafely(Context context, String uriString) {
        if (uriString == null || uriString.isEmpty()) return null;
        try {
            Uri uri = Uri.parse(uriString);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return BitmapFactory.decodeFile(uri.getPath());
            } else if (uriString.startsWith("/")) {
                return BitmapFactory.decodeFile(uriString);
            } else {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is != null) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    return bmp;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "loadBitmapSafely error for " + uriString + ": " + e.getMessage());
        }
        return null;
    }

    private static void notifySuccess(final IllustrationCallback cb, final String imageUri) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() { cb.onSuccess(imageUri); }
        });
    }

    private static void notifyError(final IllustrationCallback cb, final String error) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() { cb.onError(error); }
        });
    }
}

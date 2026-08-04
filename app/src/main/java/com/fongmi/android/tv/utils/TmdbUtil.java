package com.fongmi.android.tv.utils;

import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.net.OkHttp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * TMDB API 工具类
 * 用于通过 TMDB API 搜索影视内容并获取横屏背景图(backdrop)。
 * 需要在设置中配置 TMDB API Key 后才能使用。
 * 支持自定义 API 地址和图片地址（用于国内镜像）。
 */
public class TmdbUtil {

    private static final String TAG = "TmdbUtil";
    private static final String DEFAULT_BASE_URL = "https://api.tmdb.org/3";
    private static final String DEFAULT_IMAGE_BASE = "https://image.tmdb.org/t/p";

    private static String getBaseUrl() {
        String url = Setting.getTmdbApiUrl();
        if (TextUtils.isEmpty(url)) return DEFAULT_BASE_URL;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        // 确保以 /3 结尾
        if (!url.endsWith("/3")) {
            if (url.endsWith("/3/")) url = url.substring(0, url.length() - 1);
            else if (!url.contains("/3")) url = url + "/3";
        }
        return url;
    }

    private static String getImageBase() {
        String url = Setting.getTmdbImageUrl();
        if (TextUtils.isEmpty(url)) return DEFAULT_IMAGE_BASE;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private static String buildImageUrl(String backdropPath) {
        if (TextUtils.isEmpty(backdropPath)) return "";
        return getImageBase() + "/original" + backdropPath;
    }

    /**
     * 搜索影视内容并返回横屏背景图URL。
     * 依次尝试 multi 搜索、movie 搜索、tv 搜索。
     *
     * @param name 影视名称
     * @return 横屏背景图完整URL，未找到返回空字符串
     */
    public static String searchBackdrop(String name) {
        if (TextUtils.isEmpty(name) || !Setting.hasTmdbApiKey()) return "";
        String apiKey = Setting.getTmdbApiKey();
        String baseUrl = getBaseUrl();
        Log.d(TAG, "searchBackdrop: name=" + name + ", baseUrl=" + baseUrl);

        // 尝试 multi 搜索
        String result = searchMulti(baseUrl, apiKey, name);
        if (!TextUtils.isEmpty(result)) {
            Log.d(TAG, "Found backdrop via multi search: " + result);
            return result;
        }

        // 尝试 movie 搜索
        result = searchMovie(baseUrl, apiKey, name);
        if (!TextUtils.isEmpty(result)) {
            Log.d(TAG, "Found backdrop via movie search: " + result);
            return result;
        }

        // 尝试 tv 搜索
        result = searchTv(baseUrl, apiKey, name);
        if (!TextUtils.isEmpty(result)) {
            Log.d(TAG, "Found backdrop via tv search: " + result);
            return result;
        }

        Log.w(TAG, "No backdrop found for: " + name);
        return "";
    }

    private static String searchMulti(String baseUrl, String apiKey, String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/search/multi?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1&include_adult=true";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            return extractBackdropFromResults(json);
        } catch (Exception e) {
            Log.w(TAG, "searchMulti failed: " + e.getMessage());
            return "";
        }
    }

    private static String searchMovie(String baseUrl, String apiKey, String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/search/movie?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1&include_adult=true";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            return extractBackdropFromResults(json);
        } catch (Exception e) {
            Log.w(TAG, "searchMovie failed: " + e.getMessage());
            return "";
        }
    }

    private static String searchTv(String baseUrl, String apiKey, String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/search/tv?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1&include_adult=true";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            return extractBackdropFromResults(json);
        } catch (Exception e) {
            Log.w(TAG, "searchTv failed: " + e.getMessage());
            return "";
        }
    }

    private static String extractBackdropFromResults(String json) {
        if (TextUtils.isEmpty(json)) return "";
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray results = root.has("results") ? root.getAsJsonArray("results") : null;
            if (results == null || results.isEmpty()) return "";
            for (JsonElement element : results) {
                JsonObject item = element.getAsJsonObject();
                if (item.has("backdrop_path") && !item.get("backdrop_path").isJsonNull()) {
                    String backdropPath = item.get("backdrop_path").getAsString();
                    if (!TextUtils.isEmpty(backdropPath)) {
                        return buildImageUrl(backdropPath);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "extractBackdropFromResults failed: " + e.getMessage());
        }
        return "";
    }

    /**
     * 异步搜索横屏背景图，通过回调返回结果。
     * 结果会缓存到 Setting 中，避免重复请求。
     *
     * @param name     影视名称
     * @param callback 回调接口
     */
    public static void searchBackdropAsync(String name, BackdropCallback callback) {
        if (TextUtils.isEmpty(name) || !Setting.hasTmdbApiKey()) {
            callback.onResult("");
            return;
        }
        // 先查缓存
        String cached = Setting.getTmdbBackdrop(name);
        if (!TextUtils.isEmpty(cached)) {
            Log.d(TAG, "Using cached backdrop for: " + name);
            callback.onResult(cached);
            return;
        }
        new Thread(() -> {
            String backdropUrl = searchBackdrop(name);
            if (!TextUtils.isEmpty(backdropUrl)) {
                Setting.putTmdbBackdrop(name, backdropUrl);
            }
            callback.onResult(backdropUrl);
        }, "tmdb-search").start();
    }

    /**
     * 清除所有 TMDB 缓存（在 API 配置变更时调用）。
     */
    public static void clearCache() {
        // Prefers 不支持遍历，这里只是占位
        // 实际缓存清除在 SettingActivity 中处理
    }

    public interface BackdropCallback {
        void onResult(String backdropUrl);
    }
}

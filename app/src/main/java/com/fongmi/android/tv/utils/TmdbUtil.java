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
 */
public class TmdbUtil {

    private static final String TAG = "TmdbUtil";
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/original";
    private static final String IMAGE_BASE_W780 = "https://image.tmdb.org/t/p/w780";

    /**
     * 搜索影视内容并返回横屏背景图URL。
     * 优先使用 multi 搜索（包含电影和电视剧），取第一个有 backdrop_path 的结果。
     *
     * @param name 影视名称
     * @return 横屏背景图完整URL，未找到返回空字符串
     */
    public static String searchBackdrop(String name) {
        if (TextUtils.isEmpty(name) || !Setting.hasTmdbApiKey()) return "";
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String apiKey = Setting.getTmdbApiKey();
            String url = BASE_URL + "/search/multi?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            if (TextUtils.isEmpty(json)) return "";
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray results = root.has("results") ? root.getAsJsonArray("results") : null;
            if (results == null || results.isEmpty()) return "";
            for (JsonElement element : results) {
                JsonObject item = element.getAsJsonObject();
                if (item.has("backdrop_path") && !item.get("backdrop_path").isJsonNull()) {
                    String backdropPath = item.get("backdrop_path").getAsString();
                    if (!TextUtils.isEmpty(backdropPath)) {
                        return IMAGE_BASE + backdropPath;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "searchBackdrop failed for: " + name, e);
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

    public interface BackdropCallback {
        void onResult(String backdropUrl);
    }
}

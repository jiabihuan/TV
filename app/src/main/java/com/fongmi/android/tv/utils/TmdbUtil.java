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
 * 用于通过 TMDB API 搜索影视内容并获取横屏背景图(backdrop)和剧情简介(overview)。
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
     * 搜索影视内容并返回包含横屏背景图和剧情简介的结果。
     * 依次尝试 multi 搜索、movie 搜索、tv 搜索。
     *
     * @param name 影视名称
     * @return TmdbResult，未找到返回 backdropUrl 和 overview 均为空的 TmdbResult
     */
    public static TmdbResult search(String name) {
        if (TextUtils.isEmpty(name) || !Setting.hasTmdbApiKey()) return new TmdbResult("", "");
        String apiKey = Setting.getTmdbApiKey();
        String baseUrl = getBaseUrl();
        Log.d(TAG, "search: name=" + name + ", baseUrl=" + baseUrl);

        // 尝试 multi 搜索
        TmdbResult result = searchMulti(baseUrl, apiKey, name);
        if (result.hasBackdrop()) {
            Log.d(TAG, "Found via multi search: " + result.getBackdropUrl());
            return result;
        }

        // 尝试 movie 搜索
        result = searchMovie(baseUrl, apiKey, name);
        if (result.hasBackdrop()) {
            Log.d(TAG, "Found via movie search: " + result.getBackdropUrl());
            return result;
        }

        // 尝试 tv 搜索
        result = searchTv(baseUrl, apiKey, name);
        if (result.hasBackdrop()) {
            Log.d(TAG, "Found via tv search: " + result.getBackdropUrl());
            return result;
        }

        Log.w(TAG, "No result found for: " + name);
        return new TmdbResult("", "");
    }

    private static TmdbResult searchMulti(String baseUrl, String apiKey, String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/search/multi?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1&include_adult=true";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            return extractFromResults(json);
        } catch (Exception e) {
            Log.w(TAG, "searchMulti failed: " + e.getMessage());
            return new TmdbResult("", "");
        }
    }

    private static TmdbResult searchMovie(String baseUrl, String apiKey, String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/search/movie?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1&include_adult=true";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            return extractFromResults(json);
        } catch (Exception e) {
            Log.w(TAG, "searchMovie failed: " + e.getMessage());
            return new TmdbResult("", "");
        }
    }

    private static TmdbResult searchTv(String baseUrl, String apiKey, String name) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = baseUrl + "/search/tv?api_key=" + apiKey + "&query=" + encodedName + "&language=zh-CN&page=1&include_adult=true";
            String json = OkHttp.string(url, Map.of("Accept", "application/json"));
            return extractFromResults(json);
        } catch (Exception e) {
            Log.w(TAG, "searchTv failed: " + e.getMessage());
            return new TmdbResult("", "");
        }
    }

    private static TmdbResult extractFromResults(String json) {
        if (TextUtils.isEmpty(json)) return new TmdbResult("", "");
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray results = root.has("results") ? root.getAsJsonArray("results") : null;
            if (results == null || results.isEmpty()) return new TmdbResult("", "");
            for (JsonElement element : results) {
                JsonObject item = element.getAsJsonObject();
                String backdropUrl = "";
                String overview = "";

                if (item.has("backdrop_path") && !item.get("backdrop_path").isJsonNull()) {
                    String backdropPath = item.get("backdrop_path").getAsString();
                    if (!TextUtils.isEmpty(backdropPath)) {
                        backdropUrl = buildImageUrl(backdropPath);
                    }
                }
                if (item.has("overview") && !item.get("overview").isJsonNull()) {
                    overview = item.get("overview").getAsString();
                }

                // 优先返回有 backdrop 的结果
                if (!TextUtils.isEmpty(backdropUrl)) {
                    return new TmdbResult(backdropUrl, overview);
                }
            }
            // 如果没有 backdrop，但第一个结果有 overview，也返回
            JsonObject firstItem = results.get(0).getAsJsonObject();
            if (firstItem.has("overview") && !firstItem.get("overview").isJsonNull()) {
                String overview = firstItem.get("overview").getAsString();
                if (!TextUtils.isEmpty(overview)) {
                    return new TmdbResult("", overview);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "extractFromResults failed: " + e.getMessage());
        }
        return new TmdbResult("", "");
    }

    /**
     * 异步搜索影视内容，通过回调返回包含背景图和简介的结果。
     * 背景图结果会缓存到 Setting 中，避免重复请求。
     *
     * @param name     影视名称
     * @param callback 回调接口
     */
    public static void searchAsync(String name, SearchResultCallback callback) {
        if (TextUtils.isEmpty(name) || !Setting.hasTmdbApiKey()) {
            callback.onResult(new TmdbResult("", ""));
            return;
        }
        // 先查缓存
        String cachedBackdrop = Setting.getTmdbBackdrop(name);
        String cachedOverview = Setting.getTmdbOverview(name);
        if (!TextUtils.isEmpty(cachedBackdrop)) {
            Log.d(TAG, "Using cached result for: " + name);
            callback.onResult(new TmdbResult(cachedBackdrop, cachedOverview));
            return;
        }
        new Thread(() -> {
            TmdbResult result = search(name);
            if (result.hasBackdrop()) {
                Setting.putTmdbBackdrop(name, result.getBackdropUrl());
            }
            if (result.hasOverview()) {
                Setting.putTmdbOverview(name, result.getOverview());
            }
            callback.onResult(result);
        }, "tmdb-search").start();
    }

    /**
     * 兼容旧接口：仅搜索背景图
     */
    public static String searchBackdrop(String name) {
        return search(name).getBackdropUrl();
    }

    /**
     * 兼容旧接口：仅异步搜索背景图
     */
    public static void searchBackdropAsync(String name, BackdropCallback callback) {
        searchAsync(name, result -> callback.onResult(result.getBackdropUrl()));
    }

    public interface SearchResultCallback {
        void onResult(TmdbResult result);
    }

    public interface BackdropCallback {
        void onResult(String backdropUrl);
    }
}

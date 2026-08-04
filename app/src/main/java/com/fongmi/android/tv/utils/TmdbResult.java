package com.fongmi.android.tv.utils;

/**
 * TMDB 搜索结果，包含横屏背景图URL和剧情简介。
 */
public class TmdbResult {

    private final String backdropUrl;
    private final String overview;

    public TmdbResult(String backdropUrl, String overview) {
        this.backdropUrl = backdropUrl;
        this.overview = overview;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public String getOverview() {
        return overview;
    }

    public boolean hasBackdrop() {
        return backdropUrl != null && !backdropUrl.isEmpty();
    }

    public boolean hasOverview() {
        return overview != null && !overview.isEmpty();
    }
}

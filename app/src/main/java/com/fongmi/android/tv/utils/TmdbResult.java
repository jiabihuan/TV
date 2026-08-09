package com.fongmi.android.tv.utils;

/**
 * TMDB 搜索结果，包含横屏背景图URL、剧情简介和标题 Logo。
 */
public class TmdbResult {

    private final String id;
    private final String mediaType;
    private final String backdropUrl;
    private final String overview;
    private String logoUrl;

    public TmdbResult(String id, String mediaType, String backdropUrl, String overview) {
        this.id = id;
        this.mediaType = mediaType;
        this.backdropUrl = backdropUrl;
        this.overview = overview;
    }

    public TmdbResult(String backdropUrl, String overview) {
        this("", "", backdropUrl, overview);
    }

    public String getId() {
        return id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public String getOverview() {
        return overview;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public boolean hasId() {
        return id != null && !id.isEmpty();
    }

    public boolean hasBackdrop() {
        return backdropUrl != null && !backdropUrl.isEmpty();
    }

    public boolean hasOverview() {
        return overview != null && !overview.isEmpty();
    }

    public boolean hasLogoUrl() {
        return logoUrl != null && !logoUrl.isEmpty();
    }
}

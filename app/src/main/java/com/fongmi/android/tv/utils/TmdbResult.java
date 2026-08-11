package com.fongmi.android.tv.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * TMDB 搜索结果，包含多张横屏背景图、剧情简介和标题 Logo。
 */
public class TmdbResult {

    private final int id;
    private final String mediaType;
    private final String backdropUrl;
    private final List<String> backdrops;
    private final String overview;
    private final String logoUrl;

    public TmdbResult() {
        this(0, "", "", new ArrayList<>(), "", "");
    }

    public TmdbResult(String backdropUrl, String overview) {
        this(0, "", backdropUrl, new ArrayList<>(), overview, "");
    }

    public TmdbResult(int id, String mediaType, String backdropUrl, List<String> backdrops, String overview, String logoUrl) {
        this.id = id;
        this.mediaType = mediaType;
        this.backdropUrl = backdropUrl;
        this.backdrops = backdrops == null ? new ArrayList<>() : backdrops;
        this.overview = overview;
        this.logoUrl = logoUrl;
    }

    public int getId() {
        return id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public List<String> getBackdrops() {
        return backdrops;
    }

    public String getOverview() {
        return overview;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean hasId() {
        return id > 0;
    }

    public boolean hasBackdrop() {
        return backdropUrl != null && !backdropUrl.isEmpty();
    }

    public boolean hasBackdrops() {
        return backdrops != null && !backdrops.isEmpty();
    }

    public boolean hasOverview() {
        return overview != null && !overview.isEmpty();
    }

    public boolean hasLogoUrl() {
        return logoUrl != null && !logoUrl.isEmpty();
    }

    public boolean isEmpty() {
        return !hasId() && !hasBackdrop() && !hasBackdrops() && !hasOverview() && !hasLogoUrl();
    }

    public String getRandomBackdrop() {
        if (backdrops == null || backdrops.isEmpty()) return backdropUrl;
        int index = (int) (Math.random() * backdrops.size());
        return backdrops.get(index);
    }

    public static TmdbResult empty() {
        return new TmdbResult();
    }
}

package com.fongmi.android.tv.player.exo;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.PlayerHelper;
import com.fongmi.android.tv.player.engine.PlaySpec;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.UrlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ExoUtil {

    public static void setPlayerView(PlayerView view) {
        view.setRender(PlayerSetting.getRender());
        view.getSubtitleView().setStyle(getCaptionStyle());
        view.getSubtitleView().setApplyEmbeddedStyles(true);
        view.getSubtitleView().setApplyEmbeddedFontSizes(false);
        if (PlayerSetting.getSubtitlePosition() != 0) view.getSubtitleView().setBottomPosition(PlayerSetting.getSubtitlePosition());
        if (PlayerSetting.getSubtitleTextSize() != 0) view.getSubtitleView().setFractionalTextSize(PlayerSetting.getSubtitleTextSize());
    }

    public static ExoPlayer buildPlayer(int decode, Player.Listener listener) {
        ExoPlayer player = new ExoPlayer.Builder(App.get()).setLoadControl(buildLoadControl()).setTrackSelector(buildTrackSelector()).setRenderersFactory(buildRenderersFactory(getRenderMode(decode))).setMediaSourceFactory(buildMediaSourceFactory()).build();
        if (BuildConfig.DEBUG) player.addAnalyticsListener(new EventLogger());
        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
        player.setHandleAudioBecomingNoisy(true);
        player.setPlayWhenReady(true);
        player.addListener(listener);
        return player;
    }

    public static void applyDolbyVisionPolicy(Player player) {
        // DV7 specific handling: detect selected DV7 track and apply fallback policy.
        // When a DV7 track is selected and the handling mode requires stripping
        // (DV7_STRIP always, or DV7_AUTO when device lacks native DV7 support),
        // we force fallback to a non-DV video track (HEVC/HDR10).
        boolean dv7Fallback = false;
        if (getDv7HandlingMode() != PlayerSetting.DV7_OFF) {
            for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
                if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
                for (int i = 0; i < group.length; i++) {
                    if (group.isTrackSelected(i) && isDolbyVisionProfile7(group.getTrackFormat(i))) {
                        dv7Fallback = shouldStripDv7();
                        break;
                    }
                }
                if (dv7Fallback) break;
            }
        }
        // If no DV7 fallback is needed and DV passthrough is allowed, do nothing.
        if (!dv7Fallback && allowDolbyVision()) return;
        Tracks tracks = player.getCurrentTracks();
        TrackGroup bestGroup = null;
        int bestIndex = -1;
        long bestScore = -1;
        boolean hasDolbyVision = false;
        boolean selectedDolbyVision = false;
        boolean selectedNonDolbyVision = false;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
            for (int i = 0; i < group.length; i++) {
                Format format = group.getTrackFormat(i);
                boolean dolbyVision = isDolbyVision(format);
                hasDolbyVision |= dolbyVision;
                selectedDolbyVision |= dolbyVision && group.isTrackSelected(i);
                selectedNonDolbyVision |= !dolbyVision && group.isTrackSelected(i);
                if (dolbyVision || !group.isTrackSupported(i)) continue;
                long score = videoScore(format);
                if (score > bestScore) {
                    bestScore = score;
                    bestGroup = group.getMediaTrackGroup();
                    bestIndex = i;
                }
            }
        }
        if (!hasDolbyVision || bestGroup == null || (selectedNonDolbyVision && !selectedDolbyVision)) return;
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(bestGroup, List.of(bestIndex))).build());
    }

    public static boolean hasSelectedDolbyVision(Player player) {
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
            for (int i = 0; i < group.length; i++) {
                if (group.isTrackSelected(i) && isDolbyVision(group.getTrackFormat(i))) return true;
            }
        }
        return false;
    }

    public static MediaItem getMediaItem(PlaySpec spec, int decode) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(spec.getUri());
        builder.setSubtitleConfigurations(buildSubtitleConfigs(spec.getSubs()));
        builder.setDrmConfiguration(buildDrmConfig(spec.getDrm()));
        builder.setRequestMetadata(buildRequestMetadata(spec));
        builder.setMediaMetadata(spec.getMetadata());
        builder.setAdblock(Setting.isAdblock());
        builder.setMimeType(spec.getFormat());
        builder.setImageDurationMs(15000);
        builder.setMediaId(spec.getKey());
        builder.setDecode(decode);
        return builder.build();
    }

    public static String getMimeType(int errorCode) {
        if (errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED || errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED || errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED) return MimeTypes.APPLICATION_M3U8;
        if (errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED || errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED) return MimeTypes.APPLICATION_OCTET_STREAM;
        return null;
    }

    public static Map<String, String> extractHeaders(MediaItem item) {
        Bundle extras = item.requestMetadata.extras;
        if (extras == null) return new HashMap<>();
        return extras.keySet().stream().filter(key -> extras.getString(key) != null).collect(Collectors.toMap(key -> key, extras::getString));
    }

    private static int getRenderMode(int decode) {
        return decode == PlayerEngine.HARD ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
    }

    private static CaptionStyleCompat getCaptionStyle() {
        return PlayerSetting.isCaption() ? CaptionStyleCompat.createFromCaptionStyle(((CaptioningManager) App.get().getSystemService(Context.CAPTIONING_SERVICE)).getUserStyle()) : new CaptionStyleCompat(Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null);
    }

    private static LoadControl buildLoadControl() {
        return new DefaultLoadControl.Builder().setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * PlayerSetting.getBuffer(), DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * PlayerSetting.getBuffer(), 500, 1500).build();
    }

    private static TrackSelector buildTrackSelector() {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(App.get());
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
        if (PlayerSetting.isPreferAAC()) builder.setPreferredAudioMimeType(MimeTypes.AUDIO_AAC);
        // DV7 handling: when stripping DV7 (STRIP mode or AUTO without device support),
        // prefer non-DV video mime types to avoid selecting DV7 tracks.
        boolean dv7Strip = shouldStripDv7();
        if (!allowDolbyVision() || dv7Strip) builder.setPreferredVideoMimeTypes(MimeTypes.VIDEO_H265, MimeTypes.VIDEO_H264, MimeTypes.VIDEO_AV1, MimeTypes.VIDEO_VP9, MimeTypes.VIDEO_VP8);
        builder.setPreferredTextLanguage(Locale.getDefault().getISO3Language());
        builder.setTunnelingEnabled(PlayerSetting.isTunnel());
        builder.setForceHighestSupportedBitrate(allowDolbyVision() && !dv7Strip);
        trackSelector.setParameters(builder.build());
        return trackSelector;
    }

    private static RenderersFactory buildRenderersFactory(int renderMode) {
        return new DefaultRenderersFactory(App.get()).setEnableDecoderFallback(true).setExtensionRendererMode(renderMode);
    }

    private static MediaSource.Factory buildMediaSourceFactory() {
        return new MediaSourceFactory();
    }

    private static boolean allowDolbyVision() {
        return PlayerSetting.isExoDolbyVisionPassthrough() && hasDolbyVisionDecoder() && hasDolbyVisionDisplay();
    }

    private static boolean hasDolbyVisionDecoder() {
        try {
            return !MediaCodecUtil.getDecoderInfos(MimeTypes.VIDEO_DOLBY_VISION, false, PlayerSetting.isTunnel()).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasDolbyVisionDisplay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        WindowManager manager = (WindowManager) App.get().getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) return false;
        Display display = manager.getDefaultDisplay();
        if (display == null) return false;
        for (int type : display.getHdrCapabilities().getSupportedHdrTypes()) {
            if (type == Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION) return true;
        }
        return false;
    }

    private static boolean isDolbyVision(Format format) {
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType)) return true;
        String codecs = format.codecs == null ? "" : format.codecs.toLowerCase(Locale.US);
        return codecs.contains("dvhe") || codecs.contains("dvh1") || codecs.contains("dva1") || codecs.contains("dvav");
    }

    /**
     * 检测 Format 是否为 Dolby Vision Profile 7 (DV7)。
     * DV7 codec string 包含 dvhe.07 / dvh1.07 / dva1.07 / dvav.07。
     */
    public static boolean isDolbyVisionProfile7(Format format) {
        if (format == null) return false;
        String codecs = format.codecs == null ? "" : format.codecs.toLowerCase(Locale.US);
        return codecs.contains("dvhe.07") || codecs.contains("dvh1.07") || codecs.contains("dva1.07") || codecs.contains("dvav.07");
    }

    /**
     * 将 DV7 codec string 重写为 DV8.1 (Profile 8)。
     * 例如 dvhe.07.06 -> dvhe.08.06
     * DV8.1 是 HDR10 兼容的 DV profile，大多数支持 DV 的设备都能解码。
     */
    public static String rewriteDv7ToDv81(String codecs) {
        if (codecs == null) return null;
        return codecs.replaceAll("(?i)(dvhe|dvav|dvh1|dva1)\\.0[57]\\.", "$1.08.");
    }

    /**
     * 检测设备是否支持 DV Profile 7 原生解码。
     * 通过遍历所有硬件解码器，检查是否有支持 DolbyVisionProfileDvheDtbh (profile 7) 的解码器。
     */
    public static boolean hasDolbyVisionProfile7Support() {
        try {
            android.media.MediaCodecInfo[] codecInfos = android.media.MediaCodecList.getCodecList().getCodecInfos();
            for (android.media.MediaCodecInfo codecInfo : codecInfos) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && codecInfo.isEncoder()) continue;
                android.media.MediaCodecInfo.CodecProfileLevel[] profiles = codecInfo.getSupportedProfiles();
                if (profiles == null) continue;
                for (android.media.MediaCodecInfo.CodecProfileLevel level : profiles) {
                    // DolbyVisionProfileDvheDtbh == 7 (DV Profile 7)
                    if (level.profile == 7) return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 获取 DV7 处理模式，委托给 PlayerSetting。
     */
    public static int getDv7HandlingMode() {
        return PlayerSetting.getDv7HandlingMode();
    }

    /**
     * 判断当前 DV7 处理策略是否需要将 DV7 回退为非 DV 轨道（HEVC/HDR10）。
     * DV7_STRIP：始终回退。
     * DV7_AUTO：设备不支持 DV7 原生解码时回退。
     * DV7_CONVERT / DV7_OFF：不回退（CONVERT 在 MediaSource 层重写 codec string）。
     */
    private static boolean shouldStripDv7() {
        int mode = getDv7HandlingMode();
        if (mode == PlayerSetting.DV7_STRIP) return true;
        if (mode == PlayerSetting.DV7_AUTO) return !hasDolbyVisionProfile7Support();
        return false;
    }

    private static long videoScore(Format format) {
        long width = Math.max(format.width, 0);
        long height = Math.max(format.height, 0);
        long bitrate = Math.max(format.bitrate, 0);
        return width * height * 1_000_000L + bitrate;
    }

    private static MediaItem.RequestMetadata buildRequestMetadata(PlaySpec spec) {
        return new MediaItem.RequestMetadata.Builder().setMediaUri(spec.getUri()).setExtras(PlayerHelper.toBundle(spec.getHeaders())).build();
    }

    private static List<MediaItem.SubtitleConfiguration> buildSubtitleConfigs(List<Sub> subs) {
        List<MediaItem.SubtitleConfiguration> configs = new ArrayList<>();
        if (subs != null) for (Sub sub : subs) configs.add(buildSubConfig(sub));
        return configs;
    }

    private static MediaItem.SubtitleConfiguration buildSubConfig(Sub sub) {
        return new MediaItem.SubtitleConfiguration.Builder(Uri.parse(UrlUtil.convert(sub.getUrl()))).setLabel(sub.getName()).setMimeType(sub.getFormat()).setSelectionFlags(sub.getFlag()).setLanguage(sub.getLang()).build();
    }

    private static MediaItem.DrmConfiguration buildDrmConfig(Drm drm) {
        return drm == null ? null : new MediaItem.DrmConfiguration.Builder(drm.getUUID()).setMultiSession(!C.CLEARKEY_UUID.equals(drm.getUUID())).setForceDefaultLicenseUri(drm.isForceKey()).setLicenseRequestHeaders(drm.getHeader()).setLicenseUri(drm.getKey()).build();
    }
}

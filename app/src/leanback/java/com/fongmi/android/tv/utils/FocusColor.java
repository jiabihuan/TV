package com.fongmi.android.tv.utils;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

import com.fongmi.android.tv.setting.Setting;

public class FocusColor {

    public static final int[] COLORS = {
            0xFFFFFFFF,
            0xFFFFD54F,
            0xFF00E5FF,
            0xFFFF1744,
            0xFF69F0AE,
            0xFFE040FB,
            0xFFFF9100,
            0xFF448AFF
    };

    public static int getIndex() {
        int index = Setting.getFocusColor();
        if (index < 0 || index >= COLORS.length) return 0;
        return index;
    }

    public static int get() {
        return COLORS[getIndex()];
    }

    public static Drawable posterForeground() {
        int color = get();
        StateListDrawable selector = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(withAlpha(color, 0x4D));
        focused.setCornerRadius(ResUtil.dp2px(10));
        focused.setStroke(ResUtil.dp2px(3), color);
        selector.addState(new int[]{android.R.attr.state_focused}, focused);
        return selector;
    }

    public static Drawable navBackground() {
        int color = get();
        StateListDrawable selector = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(withAlpha(color, 0x66));
        focused.setCornerRadius(ResUtil.dp2px(16));
        focused.setStroke(ResUtil.dp2px(2), color);
        selector.addState(new int[]{android.R.attr.state_focused}, focused);
        GradientDrawable selected = new GradientDrawable();
        selected.setColor(withAlpha(color, 0x40));
        selected.setCornerRadius(ResUtil.dp2px(16));
        selector.addState(new int[]{android.R.attr.state_selected}, selected);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);
        selector.addState(new int[]{}, normal);
        return selector;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}

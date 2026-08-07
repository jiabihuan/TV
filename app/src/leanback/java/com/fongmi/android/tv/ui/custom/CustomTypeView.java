package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.KeyUtil;
import com.google.android.material.textview.MaterialTextView;

public class CustomTypeView extends MaterialTextView {

    private Listener listener;
    private boolean coolDown;

    public CustomTypeView(@NonNull Context context) {
        super(context);
    }

    public CustomTypeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private boolean hasEvent(KeyEvent event) {
        return !coolDown && KeyUtil.isActionDown(event) && KeyUtil.isUpKey(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (listener != null && hasEvent(event)) return onKeyDown();
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown() {
        // 刷新冷却从 3000ms 缩短到 800ms，避免遥控器连按 UP 键刷新被长时间忽略
        App.post(() -> coolDown = false, 800);
        listener.onRefresh();
        coolDown = true;
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        App.post(() -> coolDown = false, 500);
        if (focused) {
            coolDown = true;
            animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start();
        } else {
            animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    public interface Listener {

        void onRefresh();
    }
}

package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

/**
 * 按键/手势控制器，逻辑严格对齐反编译 APK（FongMi leanback 5.0.8）。
 * <p>
 * VideoActivity（speedOnDown=true）全屏下控制条隐藏时的按键行为：
 * 左/右 按下 -> 快进/快退seek；松开 -> seekEnd
 * 上     松开 -> 显示控制条（onKeyUp）
 * 下     松开 -> 显示控制条（onKeyDown）；下 长按 -> 倍速（onSpeedUp），松开 -> 倍速结束（onSpeedEnd）
 * 确定   松开 -> 播放/暂停（onKeyCenter）
 * <p>
 * CastActivity（speedOnDown=false）全屏下控制条隐藏时的按键行为：
 * 左/右 按下 -> 快进/快退seek；松开 -> seekEnd
 * 上     松开 -> 显示控制条（onKeyUp）；上 长按 -> 倍速（onSpeedUp），松开 -> 倍速结束（onSpeedEnd）
 * 下     松开 -> 显示控制条（onKeyDown）
 * 确定   松开 -> 播放/暂停（onKeyCenter）
 * <p>
 * 触摸手势（与 APK H3.f 一致）：
 * 单击   -> 切换控制条显示（onSingleTap）
 * 双击   -> 播放/暂停（onDoubleTap）
 */
public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final Listener listener;
    private boolean changeSpeed;
    private boolean full;
    private boolean speedOnDown;
    private long holdTime;

    public static CustomKeyDownVod create(Activity activity) {
        return new CustomKeyDownVod(activity);
    }

    private CustomKeyDownVod(Activity activity) {
        this.detector = new GestureDetector(activity, this);
        this.listener = (Listener) activity;
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (!full) return false;
        return detector.onTouchEvent(e);
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public void setSpeedOnDown(boolean speedOnDown) {
        this.speedOnDown = speedOnDown;
    }

    public boolean hasEvent(KeyEvent event) {
        return KeyUtil.isEnterKey(event) || KeyUtil.isUpKey(event) || KeyUtil.isDownKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
    }

    public boolean onKeyDown(KeyEvent event) {
        check(event);
        return true;
    }

    private void check(KeyEvent event) {
        if (KeyUtil.isActionDown(event) && KeyUtil.isLeftKey(event)) {
            listener.onSeeking(subTime());
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isRightKey(event)) {
            listener.onSeeking(addTime());
        } else if (KeyUtil.isActionUp(event) && (KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event))) {
            App.post(() -> listener.onSeekEnd(holdTime), 250);
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isUpKey(event)) {
            if (!speedOnDown && changeSpeed) {
                listener.onSpeedEnd();
            } else {
                listener.onKeyUp();
            }
            if (!speedOnDown) changeSpeed = false;
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isDownKey(event)) {
            if (speedOnDown && changeSpeed) {
                listener.onSpeedEnd();
            } else {
                listener.onKeyDown();
            }
            if (speedOnDown) changeSpeed = false;
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isEnterKey(event)) {
            listener.onKeyCenter();
        } else if (event.isLongPress() && KeyUtil.isDownKey(event) && speedOnDown) {
            listener.onSpeedUp();
            changeSpeed = true;
        } else if (event.isLongPress() && KeyUtil.isUpKey(event) && !speedOnDown) {
            listener.onSpeedUp();
            changeSpeed = true;
        }
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        listener.onSingleTap();
        return true;
    }

    private long addTime() {
        return holdTime = holdTime + Constant.INTERVAL_SEEK;
    }

    private long subTime() {
        return holdTime = holdTime - Constant.INTERVAL_SEEK;
    }

    public void reset() {
        holdTime = 0;
    }

    public interface Listener {

        void onSeeking(long time);

        void onSeekEnd(long time);

        void onSpeedUp();

        void onSpeedEnd();

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();

        void onSingleTap();

        void onDoubleTap();
    }
}

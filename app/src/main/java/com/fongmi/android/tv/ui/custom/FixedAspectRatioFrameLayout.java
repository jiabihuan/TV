package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FrameLayout that always forces itself to a fixed width/height aspect ratio,
 * regardless of any child content. The width is taken from the parent (e.g. via
 * layout_weight) and the height is derived from it.
 */
public class FixedAspectRatioFrameLayout extends FrameLayout {

    private float mRatio = 16f / 9f; // width : height

    public FixedAspectRatioFrameLayout(@NonNull Context context) {
        super(context);
    }

    public FixedAspectRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedAspectRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setRatio(float widthHeightRatio) {
        mRatio = widthHeightRatio;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        if (width <= 0) return;
        int height = (int) (width / mRatio);
        if (height <= 0) return;
        super.onMeasure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }
}

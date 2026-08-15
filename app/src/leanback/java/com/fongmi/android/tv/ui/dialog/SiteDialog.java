package com.fongmi.android.tv.ui.dialog;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.net.OkHttp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Response;

public class SiteDialog extends BaseAlertDialog implements SiteAdapter.OnClickListener {

    private static final int GRID_COUNT = 10;
    private static final int SPAN_COUNT = 3;
    private static final int SPACING = 0;
    private static final long SPEED_TIMEOUT = 3000;

    private RecyclerView.ItemDecoration decoration;
    private DialogSiteBinding binding;
    private SiteListener listener;
    private SiteAdapter adapter;
    private boolean action;
    private boolean testing;
    private int type;

    public static SiteDialog create() {
        return new SiteDialog();
    }

    public SiteDialog search() {
        type = 1;
        return this;
    }

    public SiteDialog action() {
        action = true;
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        if (activity instanceof SiteListener) listener = (SiteListener) activity;
    }

    private boolean list() {
        return false;
    }

    private int getCount() {
        return SPAN_COUNT;
    }

    private int getIcon() {
        return list() ? com.fongmi.android.tv.R.drawable.ic_site_grid : com.fongmi.android.tv.R.drawable.ic_site_list;
    }

    private float getWidth() {
        int maxTextWidth = 0;
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, requireContext().getResources().getDisplayMetrics()));
        for (Site site : VodConfig.get().getSites()) {
            if (site.isHide()) continue;
            maxTextWidth = Math.max(maxTextWidth, (int) paint.measureText(site.getName()));
        }
        int itemWidth = maxTextWidth + ResUtil.dp2px(6 * 2 + 10 + 24);
        int dialogWidth = itemWidth * SPAN_COUNT + ResUtil.dp2px(SPACING * (SPAN_COUNT - 1) + 22 * 2);
        if (action) dialogWidth += ResUtil.dp2px(40 + 16);
        float ratio = (float) dialogWidth / ResUtil.getScreenWidth();
        // 不强制最小宽度：对话框按 3 列内容精确铺满，多余屏宽不会被平摊成列间距
        return Math.min(ratio, 0.88f);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        adapter = new SiteAdapter(this);
        if (action) binding.action.setVisibility(View.VISIBLE);
        setType(type);
        setRecyclerView();
        setMode();
    }

    @Override
    protected void initEvent() {
        binding.mode.setOnClickListener(this::onMode);
        binding.select.setOnClickListener(v -> adapter.selectAll());
        binding.cancel.setOnClickListener(v -> adapter.cancelAll());
        binding.search.setOnClickListener(v -> setType(v.isSelected() ? 0 : 1));
        binding.change.setOnClickListener(v -> setType(v.isSelected() ? 0 : 2));
        binding.speed.setOnClickListener(this::onSpeed);
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        if (decoration != null) binding.recycler.removeItemDecoration(decoration);
        binding.recycler.addItemDecoration(decoration = new SpaceItemDecoration(getCount(), SPACING, 8));
        binding.recycler.setLayoutManager(new GridLayoutManager(requireContext(), getCount()));
        if (!binding.mode.hasFocus()) binding.recycler.post(() -> binding.recycler.scrollToPosition(VodConfig.getHomeIndex()));
    }

    private void setType(int type) {
        binding.search.setSelected(type == 1);
        binding.change.setSelected(type == 2);
        binding.speed.setSelected(type == 3);
        binding.select.setClickable(type > 0);
        binding.cancel.setClickable(type > 0);
        adapter.setType(this.type = type);
    }

    private void onSpeed(View view) {
        if (testing) return;
        if (type != 3) {
            setType(3);
            adapter.selectAll();
        } else {
            startSpeedTest();
        }
    }

    private void startSpeedTest() {
        List<Site> items = adapter.getSelectedItems();
        if (items.isEmpty()) return;
        FragmentActivity activity = requireActivity();
        testing = true;
        binding.speed.setEnabled(false);
        for (Site site : items) site.setDelay(0);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        CountDownLatch latch = new CountDownLatch(items.size());
        for (Site site : items) {
            Task.largeExecutor().execute(() -> {
                site.setDelay(testSite(site));
                latch.countDown();
            });
        }
        Task.largeExecutor().execute(() -> {
            try {
                latch.await(SPEED_TIMEOUT + 2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                testing = false;
                binding.speed.setEnabled(true);
                setType(0);
            });
        });
    }

    private long testSite(Site site) {
        String url = site.getApi();
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) return -1;
        try {
            long start = System.currentTimeMillis();
            Request request = new Request.Builder().url(url).head().build();
            try (Response res = OkHttp.client(SPEED_TIMEOUT).newCall(request).execute()) {
                return System.currentTimeMillis() - start;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private void setMode() {
        binding.mode.setVisibility(View.GONE);
        binding.mode.setImageResource(getIcon());
    }

    private void setWidth() {
        setWidth(getWidth());
    }

    private void onMode(View view) {
        Setting.putSiteMode(Math.abs(Setting.getSiteMode() - 1));
        setRecyclerView();
        setMode();
        setWidth();
    }

    @Override
    public void onItemClick(Site item) {
        if (listener != null) listener.setSite(item);
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setGravity(Gravity.CENTER);
        }
        if (adapter.getItemCount() == 0) dismiss();
        else setWidth();
    }
}

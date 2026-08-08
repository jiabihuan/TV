package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.HomeBanner;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterHomeBannerBinding;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页 Banner Presenter — 简单横向海报列表
 *
 * 使用 HorizontalGridView + ItemBridgeAdapter 展示推荐内容，
 * 每个海报卡片可独立聚焦、左右滚动、点击进入详情。
 */
public class HomeBannerPresenter extends Presenter {

    private final HomeActivity activity;

    public HomeBannerPresenter(HomeActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new BannerViewHolder(AdapterHomeBannerBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, Object object) {
        HomeBanner item = (HomeBanner) object;
        BannerViewHolder holder = (BannerViewHolder) viewHolder;

        List<Vod> recommends = item.getRecommends();
        if (recommends == null) recommends = new ArrayList<>();

        // 创建简单横向列表适配器
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new BannerVodPresenter(activity));
        if (!recommends.isEmpty()) {
            adapter.addAll(0, recommends);
        }

        // 通过 ItemBridgeAdapter 桥接到 HorizontalGridView
        ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(adapter);
        holder.binding.middleCard.setAdapter(bridgeAdapter);
        holder.binding.middleCard.setFocusScrollStrategy(HorizontalGridView.FOCUS_SCROLL_ITEM);
        holder.binding.middleCard.setHorizontalSpacing(ResUtil.dp2px(12));

        holder.adapter = adapter;
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {
        BannerViewHolder holder = (BannerViewHolder) viewHolder;
        if (holder.binding.middleCard != null) {
            holder.binding.middleCard.setAdapter(null);
        }
        holder.adapter = null;
    }

    // ========================
    // ViewHolder
    // ========================

    public static class BannerViewHolder extends Presenter.ViewHolder {
        public final AdapterHomeBannerBinding binding;
        public ArrayObjectAdapter adapter;

        public BannerViewHolder(@NonNull AdapterHomeBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // ========================
    // 海报卡片 Presenter
    // ========================

    private static class BannerVodPresenter extends Presenter {

        private final HomeActivity activity;

        BannerVodPresenter(HomeActivity activity) {
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_home_banner_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, Object object) {
            Vod vod = (Vod) object;
            View view = viewHolder.view;

            ImageView image = view.findViewById(R.id.image);
            TextView name = view.findViewById(R.id.name);

            name.setText(vod.getName());
            ImgUtil.load(vod.getName(), vod.getPic(), image);

            view.setOnClickListener(v -> activity.onItemClick(vod));
            view.setOnLongClickListener(v -> activity.onLongClick(vod));

            // 聚焦缩放动画
            view.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.1f : 1.0f;
                float z = hasFocus ? 8f : 0f;
                v.animate().scaleX(scale).scaleY(scale).translationZ(z).setDuration(150).start();
            });
        }

        @Override
        public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {
            ImageView image = viewHolder.view.findViewById(R.id.image);
            if (image != null) {
                try { Glide.with(image).clear(image); } catch (Exception ignored) {}
            }
            viewHolder.view.setOnFocusChangeListener(null);
            viewHolder.view.setOnClickListener(null);
            viewHolder.view.setOnLongClickListener(null);
        }
    }
}

package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.HomeBanner;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterHomeBannerBinding;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页 Banner Presenter — 网格海报列表
 *
 * 使用 RecyclerView + GridLayoutManager 展示推荐内容，
 * 海报以网格排列（多行多列），按下键可在网格内继续浏览下方海报。
 */
public class HomeBannerPresenter extends Presenter {

    private static final int SPAN_COUNT = 6;

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

        GridLayoutManager layoutManager = new GridLayoutManager(
            holder.binding.middleCard.getContext(), SPAN_COUNT);
        holder.binding.middleCard.setLayoutManager(layoutManager);
        holder.binding.middleCard.setHasFixedSize(true);
        holder.binding.middleCard.setItemAnimator(null);

        BannerVodAdapter adapter = new BannerVodAdapter(recommends, activity);
        holder.binding.middleCard.setAdapter(adapter);
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {
        BannerViewHolder holder = (BannerViewHolder) viewHolder;
        if (holder.binding.middleCard != null) {
            holder.binding.middleCard.setAdapter(null);
        }
    }

    // ========================
    // ViewHolder
    // ========================

    public static class BannerViewHolder extends Presenter.ViewHolder {
        public final AdapterHomeBannerBinding binding;

        public BannerViewHolder(@NonNull AdapterHomeBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // ========================
    // 海报网格 Adapter
    // ========================

    private static class BannerVodAdapter extends RecyclerView.Adapter<BannerVodAdapter.VodViewHolder> {

        private final List<Vod> vods;
        private final HomeActivity activity;

        BannerVodAdapter(List<Vod> vods, HomeActivity activity) {
            this.vods = vods;
            this.activity = activity;
        }

        @NonNull
        @Override
        public VodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_home_banner_item, parent, false);
            return new VodViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VodViewHolder holder, int position) {
            if (position < 0 || position >= vods.size()) return;
            Vod vod = vods.get(position);

            holder.name.setText(vod.getName());
            ImgUtil.load(vod.getName(), vod.getPic(), holder.image);

            holder.itemView.setOnClickListener(v -> activity.onItemClick(vod));
            holder.itemView.setOnLongClickListener(v -> activity.onLongClick(vod));

            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.08f : 1.0f;
                float z = hasFocus ? 8f : 0f;
                v.animate().scaleX(scale).scaleY(scale).translationZ(z).setDuration(150).start();
            });
        }

        @Override
        public int getItemCount() {
            return vods.size();
        }

        @Override
        public void onViewRecycled(@NonNull VodViewHolder holder) {
            if (holder.image != null) {
                try { Glide.with(holder.image).clear(holder.image); } catch (Exception ignored) {}
            }
            holder.itemView.setOnFocusChangeListener(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        }

        static class VodViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView name;

            VodViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.image);
                name = itemView.findViewById(R.id.name);
            }
        }
    }
}

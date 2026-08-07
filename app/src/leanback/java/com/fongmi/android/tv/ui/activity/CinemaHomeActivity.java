package com.fongmi.android.tv.ui.activity;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;
import androidx.leanback.widget.HorizontalGridView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.ActivityCinemaHomeBinding;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.adapter.CinemaCategoryAdapter;
import com.fongmi.android.tv.ui.adapter.CinemaPosterAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CinemaHomeActivity extends BaseActivity implements
        CinemaPosterAdapter.OnClickListener,
        CinemaCategoryAdapter.OnClickListener,
        com.fongmi.android.tv.impl.SiteListener {

    private ActivityCinemaHomeBinding mBinding;
    private CinemaPosterAdapter mPosterAdapter;
    private CinemaCategoryAdapter mCategoryAdapter;
    private SiteViewModel mViewModel;
    private Result mResult;
    private Clock mClock;
    private boolean mConfigReady;
    private boolean mHasMovieSelected;
    private String mLastCoverUrl = "";
    private String mLastTitleUrl = "";

    private final BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNetworkState();
        }
    };

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCinemaHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mClock = Clock.create(mBinding.clock).format("MM/dd E HH:mm");
        mBinding.loading.setVisibility(View.VISIBLE);
        mBinding.empty.setText(R.string.home_loading);
        com.fongmi.android.tv.utils.PermissionUtil.requestNotify(this);
        com.fongmi.android.tv.service.DLNARendererService.start(this);
        Updater.create().start(this);
        setPosterAdapter();
        setCategoryAdapter();
        setViewModel();
        setHero();
        initConfig();
        checkAction(getIntent());
    }

    private void setPosterAdapter() {
        mPosterAdapter = new CinemaPosterAdapter(this);
        mBinding.posters.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.posters.setAdapter(mPosterAdapter);
        mBinding.posters.addOnChildViewHolderSelectedListener(new androidx.leanback.widget.OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                updateHero(position);
            }
        });
    }

    private void setCategoryAdapter() {
        mCategoryAdapter = new CinemaCategoryAdapter(this);
        mBinding.categories.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.categories.setAdapter(mCategoryAdapter);

        List<Class> items = new ArrayList<>();
        Class home = new Class();
        home.setTypeName(ResUtil.getString(R.string.vod_home));
        home.setTypeId("home");
        items.add(home);
        mCategoryAdapter.setItems(items);
        mBinding.categories.setVisibility(View.VISIBLE);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            mBinding.loading.setVisibility(View.GONE);
            if (result != null && result.getList() != null && !result.getList().isEmpty()) {
                mResult = result;
                mPosterAdapter.setItems(result.getList());
                setCategories(result.getTypes());
                if (!result.getList().isEmpty()) {
                    updateHero(0);
                    mBinding.posters.requestFocus();
                }
                com.fongmi.android.tv.bean.Cache.clear().put(result);
            } else {
                mBinding.loading.setVisibility(View.VISIBLE);
                mBinding.empty.setText(R.string.home_empty);
                mBinding.loadingProgress.setVisibility(View.GONE);
            }
        });
    }

    private void setHero() {
        if (!mHasMovieSelected) {
            mBinding.appTitle.setText(getString(R.string.app_name));
            mBinding.tagRow.setVisibility(View.GONE);
        }
    }

    private void updateHero(int position) {
        Vod item = mPosterAdapter.getItem(position);
        if (item == null) return;
        mHasMovieSelected = true;
        mBinding.appTitle.setText(item.getName());
        if (!TextUtils.isEmpty(item.getActor())) {
            mBinding.actor.setText("主演：" + item.getActor());
            mBinding.actor.setVisibility(View.VISIBLE);
        } else {
            mBinding.actor.setVisibility(View.GONE);
        }
        String tipText = item.getContent();
        if (!TextUtils.isEmpty(tipText)) {
            mBinding.tip.setText("简介：" + tipText);
        } else if (!TextUtils.isEmpty(item.getRemarks())) {
            mBinding.tip.setText(item.getRemarks());
        } else {
            mBinding.tip.setText(R.string.home_tip);
        }
        updateTagRow(item);
        updateCoverBg(item);
    }

    private void updateTagRow(Vod item) {
        boolean hasTag = false;
        String year = item.getYear();
        String area = item.getArea();
        String type = item.getTypeName();
        if (!TextUtils.isEmpty(year)) {
            mBinding.tagYear.setText(year);
            mBinding.tagYear.setVisibility(View.VISIBLE);
            hasTag = true;
        } else {
            mBinding.tagYear.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(area)) {
            mBinding.tagArea.setText(area);
            mBinding.tagArea.setVisibility(View.VISIBLE);
            hasTag = true;
        } else {
            mBinding.tagArea.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(type)) {
            mBinding.tagType.setText(type);
            mBinding.tagType.setVisibility(View.VISIBLE);
            hasTag = true;
        } else {
            mBinding.tagType.setVisibility(View.GONE);
        }
        mBinding.tagRow.setVisibility(hasTag ? View.VISIBLE : View.GONE);
    }

    private void updateCoverBg(Vod item) {
        String itemName = item.getName();
        // 优先使用 TMDB API 获取横屏背景图和剧情简介
        if (Setting.hasTmdbApiKey()) {
            final String fallbackUrl = getFallbackCoverUrl(item);
            com.fongmi.android.tv.utils.TmdbUtil.searchAsync(itemName, result -> {
                runOnUiThread(() -> {
                    if (result.hasBackdrop()) {
                        loadCoverBg(itemName, result.getBackdropUrl());
                    } else if (!TextUtils.isEmpty(fallbackUrl)) {
                        loadCoverBg(itemName, fallbackUrl);
                    } else {
                        hideCoverBg();
                    }
                    // 使用 TMDB 剧情简介更新
                    if (result.hasOverview()) {
                        mBinding.tip.setText(result.getOverview());
                    }
                });
            });
            return;
        }
        // 没有配置 TMDB API Key，使用站点提供的图片
        String coverUrl = getFallbackCoverUrl(item);
        if (TextUtils.isEmpty(coverUrl)) {
            hideCoverBg();
            return;
        }
        loadCoverBg(itemName, coverUrl);
    }

    private String getFallbackCoverUrl(Vod item) {
        String backdrop = item.getBackdrop();
        if (!TextUtils.isEmpty(backdrop)) return backdrop;
        return item.getPic();
    }

    private void hideCoverBg() {
        mLastCoverUrl = "";
        mBinding.coverTint.setBackgroundResource(R.drawable.bg_tv_cinema_backdrop_tint);
        mBinding.coverTint.setAlpha(1.0f);
        mBinding.homeMask.setAlpha(0.5f);
        mBinding.coverBg.setVisibility(View.INVISIBLE);
    }

    private void loadCoverBg(String name, String coverUrl) {
        if (TextUtils.equals(mLastCoverUrl, coverUrl)) return;
        mLastCoverUrl = coverUrl;
        boolean isFirstLoad = mBinding.coverBg.getVisibility() != View.VISIBLE;
        mBinding.coverTint.setBackgroundResource(R.drawable.bg_tv_cinema_cover_tint);
        mBinding.coverTint.setAlpha(0.5f);
        mBinding.homeMask.setAlpha(0.5f);
        if (isFirstLoad) {
            mBinding.coverBg.setVisibility(View.VISIBLE);
            mBinding.coverBg.setAlpha(0f);
            ImgUtil.loadBackdrop(name, coverUrl, mBinding.coverBg);
            mBinding.coverBg.animate().alpha(0.92f).setDuration(400).start();
        } else {
            mBinding.coverBg.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                ImgUtil.loadBackdrop(name, coverUrl, mBinding.coverBg);
                mBinding.coverBg.animate().alpha(0.92f).setDuration(400).start();
            }).start();
        }
    }

    private void setCategories(List<Class> types) {
        if (types == null || types.isEmpty()) {
            mBinding.categories.setVisibility(View.GONE);
            return;
        }
        List<Class> items = new ArrayList<>();
        Class home = new Class();
        home.setTypeName(ResUtil.getString(R.string.vod_home));
        home.setTypeId("home");
        items.add(home);
        items.addAll(types);
        mCategoryAdapter.setItems(items);
        mBinding.categories.setVisibility(View.VISIBLE);
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                mConfigReady = true;
                setTitle();
                loadHomeContent();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                mConfigReady = true;
            }
        };
    }

    private void setTitle() {
        ImgUtil.logo(mBinding.logo);
        updateSiteName();
        if (mHasMovieSelected) return;
        List<String> items = Arrays.asList(getHome() != null ? getHome().getName() : "", getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.appTitle.setText(s));
    }

    private void updateSiteName() {
        Site home = getHome();
        if (home != null && !home.isEmpty()) {
            mBinding.siteName.setText(home.getName());
            mBinding.siteName.setVisibility(View.VISIBLE);
        } else {
            mBinding.siteName.setVisibility(View.GONE);
        }
    }

    private void loadHomeContent() {
        if (getHome() == null || TextUtils.isEmpty(getHome().getKey())) {
            return;
        }
        mHasMovieSelected = false;
        mLastCoverUrl = "";
        mBinding.tagRow.setVisibility(View.GONE);
        mBinding.loading.setVisibility(View.VISIBLE);
        mBinding.loadingProgress.setVisibility(View.VISIBLE);
        mBinding.empty.setText(R.string.home_loading);
        mViewModel.homeContent();
    }

    private void checkAction(Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    @Override
    protected void initEvent() {
        mBinding.siteName.setOnClickListener(view -> {
            if (getHome() != null) {
                com.fongmi.android.tv.ui.dialog.SiteDialog.create().show(this);
            }
        });
        mBinding.search.setOnClickListener(view -> SearchActivity.start(this));
        mBinding.live.setOnClickListener(view -> LiveActivity.start(this));
        mBinding.keep.setOnClickListener(view -> KeepActivity.start(this));
        mBinding.history.setOnClickListener(view -> HistoryActivity.start(this));
        mBinding.push.setOnClickListener(view -> PushActivity.start(this));
        mBinding.setting.setOnClickListener(view -> SettingActivity.start(this));
        mBinding.net.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } catch (Exception e2) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            }
        });
    }

    @Override
    public void onItemClick(Vod item) {
        if (getHome() == null) return;
        String siteKey = TextUtils.isEmpty(item.getSiteKey()) ? getHome().getKey() : item.getSiteKey();
        String vodId = item.getId();
        if (vodId == null || vodId.isEmpty()) {
            SearchActivity.start(this, item.getName());
        } else {
            VideoActivity.start(this, siteKey, vodId, item.getName(), item.getPic());
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    @Override
    public void onItemClick(Class item, int position) {
        if (position == 0) {
            loadHomeContent();
        } else {
            if (mResult != null) {
                VodActivity.start(this, getHome().getKey(), mResult);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.home();
                setTitle();
                break;
            case COMMON:
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                setTitle();
                loadHomeContent();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(this, event.history().save(VodConfig.getCid()));
        } else {
            VodConfig.load(event.config(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    private void updateNetworkState() {
        int state = Util.getNetworkState();
        if (state == 1) {
            mBinding.net.setImageResource(R.drawable.ic_net_wifi);
            mBinding.net.setVisibility(View.VISIBLE);
        } else if (state == 2) {
            mBinding.net.setImageResource(R.drawable.ic_net_ethernet);
            mBinding.net.setVisibility(View.VISIBLE);
        } else {
            mBinding.net.setImageResource(R.drawable.ic_net_disconnected);
            mBinding.net.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNavFocused() {
        View focus = getCurrentFocus();
        return focus == mBinding.search || focus == mBinding.live || focus == mBinding.keep || focus == mBinding.history
                || focus == mBinding.push || focus == mBinding.setting || focus == mBinding.net
                || focus == mBinding.logo || focus == mBinding.siteName;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) {
            if (getHome() != null) {
                com.fongmi.android.tv.ui.dialog.SiteDialog.create().show(this);
            }
        }
        if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event)) {
            if (isNavFocused()) {
                if (mBinding.categories.getVisibility() == View.VISIBLE) {
                    return mBinding.categories.requestFocus();
                } else if (mBinding.posters.getChildCount() > 0) {
                    return mBinding.posters.requestFocus();
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        updateNetworkState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
        unregisterReceiver(mNetworkReceiver);
    }

    @Override
    protected void onBackInvoked() {
        if (isNavFocused() && mBinding.posters.getChildCount() > 0) {
            mBinding.posters.requestFocus();
        } else if (mBinding.posters.hasFocus()) {
            if (mBinding.categories.getVisibility() == View.VISIBLE) {
                mBinding.categories.requestFocus();
            } else {
                mBinding.search.requestFocus();
            }
        } else {
            exitApp();
        }
    }

    private void exitApp() {
        super.onBackInvoked();
    }

    @Override
    protected void onDestroy() {
        com.fongmi.android.tv.service.DLNARendererService.stop(this);
        if (isFinishing() || isChangingConfigurations()) {
            LiveConfig.get().clear();
            VodConfig.get().clear();
            com.github.catvod.net.OkHttp.get().clear();
            com.fongmi.android.tv.player.Source.get().exit();
            com.fongmi.android.tv.server.Server.get().stop();
        }
        com.fongmi.android.tv.db.AppDatabase.backup();
        super.onDestroy();
    }
}

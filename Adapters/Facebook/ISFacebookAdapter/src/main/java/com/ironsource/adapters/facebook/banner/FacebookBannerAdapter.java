package com.ironsource.adapters.facebook.banner;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FacebookBannerAdapter extends AbstractBannerAdapter<FacebookAdapter> {

    // Banner maps
    private final ConcurrentHashMap<String, BannerSmashListener> mPlacementIdToSmashListener;
    protected ConcurrentHashMap<String, AdView> mPlacementIdToAd;


    public FacebookBannerAdapter(FacebookAdapter adapter) {
        super(adapter);

        mPlacementIdToSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToAd = new ConcurrentHashMap<>();
    }

    @Override
    public void initBanners(String appKey, String userId, @NonNull JSONObject config, @NonNull BannerSmashListener listener) {
        initBannersInternal(config, listener);
    }

    @Override
    public void initBannerForBidding(String appKey, String userId, @NonNull JSONObject config, @NonNull BannerSmashListener listener) {
        initBannersInternal(config, listener);
    }

    private void initBannersInternal(@NonNull final JSONObject config, @NonNull final BannerSmashListener listener) {
        final String placementIdKey = getAdapter().getPlacementIdKey();
        final String allPlacementIdsKey = getAdapter().getAllPlacementIdsKey();
        final String placementId = getConfigStringValueFromKey(config, placementIdKey);
        final String allPlacementIds = getConfigStringValueFromKey(config, allPlacementIdsKey);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementIdKey));
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(placementIdKey), IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(allPlacementIdsKey));
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(allPlacementIdsKey), IronSourceConstants.BANNER_AD_UNIT));
            return;
        }
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        //add banner to listener map
        mPlacementIdToSmashListener.put(placementId, listener);


        if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onBannerInitSuccess - placementId = " + placementId);
            listener.onBannerInitSuccess();
        } else if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onBannerInitFailed - placementId = " + placementId);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.BANNER_AD_UNIT));
        } else {
            getAdapter().initSDK(allPlacementIds);
        }

    }


    @Override
    public void onNetworkInitCallbackSuccess() {
        for (BannerSmashListener listener : mPlacementIdToSmashListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (BannerSmashListener listener : mPlacementIdToSmashListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    @Override
    public void loadBanner(@NonNull final JSONObject config, final JSONObject adData, @NonNull final IronSourceBannerLayout banner, @NonNull final BannerSmashListener listener) {
        loadBannerInternal(config, null, banner, listener);
    }

    @Override
    public void loadBannerForBidding(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final IronSourceBannerLayout banner, @NonNull final BannerSmashListener listener) {
        loadBannerInternal(config, serverData, banner, listener);
    }

    private void loadBannerInternal(@NonNull JSONObject config, final String serverData, @NonNull final IronSourceBannerLayout banner, @NonNull final BannerSmashListener listener) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // check banner
        if (banner == null) {
            IronLog.INTERNAL.error("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"));
            return;
        }

        // check size
        final Context context = ContextProvider.getInstance().getApplicationContext();
        final AdSize adSize = calculateBannerSize(banner.getSize(), context);
        if (adSize == null) {
            IronLog.INTERNAL.error("size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getAdapter().getProviderName()));
            return;
        }

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdView adView = new AdView(context, placementId, adSize);
                    FrameLayout.LayoutParams layoutParams = calcLayoutParams(banner.getSize(), context);

                    // create banner
                    FacebookBannerAdListener bannerAdListener = new FacebookBannerAdListener(FacebookBannerAdapter.this, layoutParams, placementId, listener);
                    AdView.AdViewLoadConfigBuilder configBuilder = adView.buildLoadAdConfig();
                    configBuilder.withAdListener(bannerAdListener);
                    if (serverData != null) {
                        // add server data to banner bidder instance
                        configBuilder.withBid(serverData);
                    }

                    mPlacementIdToAd.put(placementId, adView);
                    adView.loadAd(configBuilder.build());
                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("Meta loadBanner exception " + e.getMessage());
                    listener.onBannerAdLoadFailed(error);
                }
            }
        });
    }

    @Override
    public void destroyBanner(@NonNull final JSONObject config) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mPlacementIdToAd.containsKey(placementId)) {
                        mPlacementIdToAd.get(placementId).destroy();
                        mPlacementIdToAd.remove(placementId);
                    }
                } catch (Exception e) {
                    IronLog.INTERNAL.error("destroyBanner failed for placementId - " + placementId + " with an exception = " + e);
                }
            }
        });
    }

    @Override
    public void releaseMemory(@NonNull IronSource.AD_UNIT adUnit, JSONObject config) {
        // release banner ads
        for (AdView adView : mPlacementIdToAd.values()) {
            postOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (adView != null) {
                        adView.destroy();
                    }
                }
            });
        }
        mPlacementIdToAd.clear();
        mPlacementIdToSmashListener.clear();
    }

    public Map<String, Object> getBannerBiddingData(@NonNull JSONObject config, JSONObject adData) {
        return getAdapter().getBiddingData();
    }

    private AdSize calculateBannerSize(ISBannerSize size, Context context) {
        switch (size.getDescription()) {
            case "BANNER":
                return AdSize.BANNER_HEIGHT_50;

            case "LARGE":
                return AdSize.BANNER_HEIGHT_90;

            case "RECTANGLE":
                return AdSize.RECTANGLE_HEIGHT_250;

            case "SMART":
                return AdapterUtils.isLargeScreen(context) ? AdSize.BANNER_HEIGHT_90 : AdSize.BANNER_HEIGHT_50;

            case "CUSTOM":
                if (size.getHeight() == 50) {
                    return AdSize.BANNER_HEIGHT_50;
                } else if (size.getHeight() == 90) {
                    return AdSize.BANNER_HEIGHT_90;
                } else if (size.getHeight() == 250) {
                    return AdSize.RECTANGLE_HEIGHT_250;
                }
                break;
        }
        return null;
    }

    protected FrameLayout.LayoutParams calcLayoutParams(ISBannerSize size, Context context) {
        int widthDp = 320;
        if (size.getDescription().equals("RECTANGLE")) {
            widthDp = 300;
        } else if (size.getDescription().equals("SMART") && AdapterUtils.isLargeScreen(context)) {
            widthDp = 728;
        }

        int widthPixel = AdapterUtils.dpToPixels(context, widthDp);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPixel, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        return layoutParams;
    }

}

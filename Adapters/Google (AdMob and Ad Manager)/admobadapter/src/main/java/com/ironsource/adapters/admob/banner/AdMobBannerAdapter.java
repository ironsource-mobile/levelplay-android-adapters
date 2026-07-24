package com.ironsource.adapters.admob.banner;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.unity3d.mediation.LevelPlayAdSize;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class AdMobBannerAdapter extends AbstractBannerAdapter<AdMobAdapter> {

    private static final String IS_NATIVE = "isNative";

    // Banner maps
    private final ConcurrentHashMap<String, BannerSmashListener> mAdUnitIdToListener;
    private final ConcurrentHashMap<String, AdView> mAdUnitIdToBannerAd;
    public final ConcurrentHashMap<String, NativeAd> mAdUnitIdToNativeBannerAd;

    public AdMobBannerAdapter(AdMobAdapter adapter) {
        super(adapter);

        mAdUnitIdToListener = new ConcurrentHashMap<>();
        mAdUnitIdToBannerAd = new ConcurrentHashMap<>();
        mAdUnitIdToNativeBannerAd = new ConcurrentHashMap<>();

    }

    public void initBanners(String appKey, String userId, @NonNull final JSONObject config,
                            @NonNull final BannerSmashListener listener) {
        initBannersInternal(config, listener);
    }

    @Override
    public void initBannerForBidding(String appKey, String userId, @NonNull JSONObject config, @NonNull BannerSmashListener listener) {
        initBannersInternal(config, listener);
    }

    private void initBannersInternal(@NonNull final JSONObject config, @NonNull final BannerSmashListener listener) {
        final String adUnitIdKey = getAdapter().getAdUnitIdKey();
        final String adUnitId = getConfigStringValueFromKey(config, adUnitIdKey);
        if (TextUtils.isEmpty(adUnitId)) {
            IronSourceError error = ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(adUnitIdKey), IronSourceConstants.BANNER_AD_UNIT);
            listener.onBannerInitFailed(error);
            return;
        }
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        //add banner to listener map
        mAdUnitIdToListener.put(adUnitId, listener);

        if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onBannerInitSuccess - adUnitId = " + adUnitId);
            listener.onBannerInitSuccess();
        } else if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onBannerInitFailed - adUnitId = " + adUnitId);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.BANNER_AD_UNIT));
        } else {
            getAdapter().initSDK(config);
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        for (BannerSmashListener listener : mAdUnitIdToListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (BannerSmashListener listener : mAdUnitIdToListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    @Override
    public void loadBanner(@NonNull final JSONObject config, final JSONObject adData, @NonNull final ISBannerSize bannerSize, @NonNull final BannerSmashListener listener) {
        loadBannerInternal(config, adData, null, bannerSize, listener);
    }

    @Override
    public void loadBannerForBidding(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final ISBannerSize bannerSize, @NonNull final BannerSmashListener listener) {
        loadBannerInternal(config, adData, serverData, bannerSize, listener);
    }

    private void loadBannerInternal(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final ISBannerSize bannerSize, @NonNull final BannerSmashListener listener) {
        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        // check banner
        if (bannerSize == null) {
            IronLog.INTERNAL.error("banner size is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner size is null"));
            return;
        }

        final boolean isNative = Boolean.parseBoolean(getConfigStringValueFromKey(config, IS_NATIVE));

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNative) {
                        loadNativeBanner(bannerSize, listener, adUnitId, serverData, config);
                    } else {
                        //get banner size
                        final AdSize adMobBannerSize = getAdSize(bannerSize, AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()));

                        if (adMobBannerSize == null) {
                            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getAdapter().getProviderName()));
                            return;
                        }

                        AdView adView = new AdView(ContextProvider.getInstance().getApplicationContext());
                        adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                        AdMobBannerAdListener adMobBannerAdListener = new AdMobBannerAdListener(listener, adUnitId, adView);

                        //add banner ad to map
                        mAdUnitIdToBannerAd.put(adUnitId, adView);

                        if (serverData != null) {
                            IronLog.ADAPTER_API.verbose("loadAd with serverData");
                            adView.loadFromAdResponse(serverData, adMobBannerAdListener);
                        } else {
                            BannerAdRequest adRequest = getAdapter().createBannerAdRequest(adUnitId, adMobBannerSize, adData);
                            IronLog.ADAPTER_API.verbose("loadAd");
                            adView.loadAd(adRequest, adMobBannerAdListener);
                        }
                    }
                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("AdMobAdapter loadBanner exception " + e.getMessage());
                    listener.onBannerAdLoadFailed(error);
                }
            }
        });
    }

    private void loadNativeBanner(ISBannerSize bannerSize, BannerSmashListener listener, String adUnitId, String serverData, JSONObject config) {
        // verify size
        if (!isNativeBannerSizeSupported(bannerSize, AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()))) {
            IronLog.INTERNAL.error("size not supported, size = " + bannerSize.getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getAdapter().getProviderName()));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        NativeTemplateType templateType = NativeTemplateType.createTemplateType(config, bannerSize);

        AdMobNativeBannerAdListener adMobNativeBannerAdListener = new AdMobNativeBannerAdListener(AdMobBannerAdapter.this, listener, adUnitId, bannerSize, templateType);

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        if (serverData != null) {
            IronLog.ADAPTER_API.verbose("loading native banner with serverData");
            NativeAdLoader.loadFromAdResponse(serverData, adMobNativeBannerAdListener);
        } else {
            NativeAdRequest adRequest = new NativeAdRequest.Builder(adUnitId, Collections.singletonList(NativeAd.NativeAdType.NATIVE))
                    .setVideoOptions(videoOptions)
                    .setAdChoicesPlacement(templateType.getAdChoicesPlacement())
                    .setMediaAspectRatio(templateType.getMediaAspectRatio())
                    .build();

            NativeAdLoader.load(adRequest, adMobNativeBannerAdListener);
        }
    }

    // destroy banner ad and clear banner ad map
    public void destroyBanner(@NonNull final JSONObject config) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
                    IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

                    // Banner
                    if (mAdUnitIdToBannerAd.containsKey(adUnitId)) {
                        AdView ad = mAdUnitIdToBannerAd.get(adUnitId);

                        if (ad != null) {
                            ad.destroy();
                        }

                        mAdUnitIdToBannerAd.remove(adUnitId);
                    }

                    // Native Banner
                    if (mAdUnitIdToNativeBannerAd.containsKey(adUnitId)) {
                        NativeAd ad = mAdUnitIdToNativeBannerAd.get(adUnitId);

                        if (ad != null) {
                            ad.destroy();
                        }

                        mAdUnitIdToNativeBannerAd.remove(adUnitId);
                    }
                } catch (Exception e) {
                    IronLog.ADAPTER_API.error("e = " + e);
                }
            }
        });
    }

    @Override
    public void collectBannerBiddingData(@NonNull JSONObject config, JSONObject adData, @NotNull BiddingDataCallback biddingDataCallback) {
        Bundle extras = new Bundle();
        if (adData != null) {
            ISBannerSize bannerSize = (ISBannerSize) adData.opt(IronSourceConstants.BANNER_SIZE);

            if (bannerSize != null) {
                if (bannerSize.isAdaptive()) {
                    AdSize adMobBannerSize = getAdSize(bannerSize, AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()));
                    extras.putInt("adaptive_banner_w", adMobBannerSize.getWidth());
                    extras.putInt("adaptive_banner_h", adMobBannerSize.getHeight());
                    IronLog.ADAPTER_API.verbose("adaptive banner width = " + adMobBannerSize.getWidth() + ", height = " + adMobBannerSize.getHeight());
                }
            }
        }
        getAdapter().collectBiddingData(biddingDataCallback, AdFormat.BANNER, extras);
    }

    @Override
    public int getAdaptiveHeight(int width) {
        int height = getAdaptiveBannerSize(width).getHeight();
        IronLog.ADAPTER_API.verbose("height - " + height + " for width - " + width);
        return height;
    }

    public AdSize getAdSize(ISBannerSize selectedBannerSize, boolean isLargeScreen) {
        AdSize adSize;
        switch (selectedBannerSize.getDescription()) {
            case "BANNER": {
                adSize = AdSize.BANNER;
                break;
            }
            case "LARGE": {
                adSize = AdSize.LARGE_BANNER;
                break;
            }

            case "RECTANGLE": {
                adSize = AdSize.MEDIUM_RECTANGLE;
                break;
            }
            case "SMART": {
                adSize = isLargeScreen ? AdSize.LEADERBOARD : AdSize.BANNER;
                break;
            }
            case "CUSTOM": {
                adSize = new AdSize(selectedBannerSize.getWidth(), selectedBannerSize.getHeight());
                break;
            }

            default:
                adSize = null;
        }

        if (selectedBannerSize.isAdaptive() && adSize != null) {
            LevelPlayAdSize levelPlayAdaptiveSize = selectedBannerSize.toLevelPlayAdSize(ContextProvider.getInstance().getApplicationContext());
            AdSize adMobAdaptiveSize = getAdaptiveBannerSize(levelPlayAdaptiveSize.getWidth());

            if (adMobAdaptiveSize != null) {
                IronLog.INTERNAL.verbose(
                    "default height - " + adSize.getHeight() +
                        " adMobAdaptive height - " + adMobAdaptiveSize.getHeight() +
                        " levelPlayAdaptive height - " + levelPlayAdaptiveSize.getHeight() +
                        " default width - " + adSize.getWidth() +
                        " levelPlayAdaptive width - " + levelPlayAdaptiveSize.getWidth());

                return adMobAdaptiveSize;
            }
        }

        return adSize;
    }

    private static AdSize getAdaptiveBannerSize(int width) {
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(
                ContextProvider.getInstance().getApplicationContext(),
                width);
    }

    private boolean isNativeBannerSizeSupported(ISBannerSize size, boolean isLargeScreen) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
            case "RECTANGLE":
                return true;
            case "SMART":
                return !isLargeScreen;
        }

        return false;
    }

}

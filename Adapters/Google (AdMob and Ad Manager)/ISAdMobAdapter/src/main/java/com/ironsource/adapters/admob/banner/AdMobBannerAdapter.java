package com.ironsource.adapters.admob.banner;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class AdMobBannerAdapter extends AbstractBannerAdapter<AdMobAdapter> {

    private final String IS_NATIVE = "isNative";

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

    public void initBanners(String appKey, String userId, final JSONObject config,
                            final BannerSmashListener listener) {
        initBannersInternal(config, listener);
    }

    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        initBannersInternal(config, listener);
    }

    private void initBannersInternal(final JSONObject config, final BannerSmashListener listener) {
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
    public void loadBanner(final JSONObject config, final JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        loadBannerInternal(banner, config, adData, listener, null);
    }

    @Override
    public void loadBannerForBidding(final JSONObject config, final JSONObject adData, final String serverData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        loadBannerInternal(banner, config, adData, listener, serverData);
    }

    private void loadBannerInternal(final IronSourceBannerLayout banner, JSONObject config, final JSONObject adData, final BannerSmashListener listener, final String serverData) {
        if (banner == null) {
            IronLog.ADAPTER_API.error("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"));
            return;
        }

        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        final boolean isNative = Boolean.parseBoolean(getConfigStringValueFromKey(config, IS_NATIVE));

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdRequest adRequest = getAdapter().createAdRequest(adData, serverData);

                    if (isNative) {
                        loadNativeBanner(banner, listener, adUnitId, adRequest);
                    } else {
                        //get banner size
                        final AdSize size = getAdSize(banner.getSize(), AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()));

                        if (size == null) {
                            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getAdapter().getProviderName()));
                            return;
                        }

                        AdView adView = new AdView(ContextProvider.getInstance().getApplicationContext());
                        adView.setAdSize(size);
                        adView.setAdUnitId(adUnitId);
                        AdMobBannerAdListener adMobBannerAdListener = new AdMobBannerAdListener(listener, adUnitId, adView);

                        adView.setAdListener(adMobBannerAdListener);

                        //add banner ad to map
                        mAdUnitIdToBannerAd.put(adUnitId, adView);

                        IronLog.ADAPTER_API.verbose("loadAd");
                        adView.loadAd(adRequest);
                    }
                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("AdMobAdapter loadBanner exception " + e.getMessage());
                    listener.onBannerAdLoadFailed(error);
                }
            }
        });
    }

    private void loadNativeBanner(final IronSourceBannerLayout banner, final BannerSmashListener listener, final String adUnitId, AdRequest adRequest) {
        // verify size
        if (!isNativeBannerSizeSupported(banner.getSize(), AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()))) {
            IronLog.INTERNAL.error("size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getAdapter().getProviderName()));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        NativeAdOptions.Builder optionsBuilder = new NativeAdOptions.Builder();
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();
        optionsBuilder.setVideoOptions(videoOptions);

        AdMobNativeBannerAdListener adMobNativeBannerAdListener = new AdMobNativeBannerAdListener(AdMobBannerAdapter.this, listener, adUnitId, banner.getSize());

        AdLoader adLoader = new AdLoader.Builder(banner.getContext(), adUnitId)
                .forNativeAd(adMobNativeBannerAdListener)
                .withNativeAdOptions(optionsBuilder.build())
                .withAdListener(adMobNativeBannerAdListener)
                .build();

        adLoader.loadAd(adRequest);
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
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        // release banner ads
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (AdView adView : mAdUnitIdToBannerAd.values()) {
                    adView.destroy();
                }
                for (NativeAd nativeAd : mAdUnitIdToNativeBannerAd.values()) {
                    nativeAd.destroy();
                }
                // clear banner maps
                mAdUnitIdToBannerAd.clear();
                mAdUnitIdToListener.clear();
                mAdUnitIdToNativeBannerAd.clear();

            }
        });
    }

    @Override
    public void collectBannerBiddingData(JSONObject config, JSONObject adData, @NotNull BiddingDataCallback biddingDataCallback) {
        Bundle extras = new Bundle();
        if (adData != null) {
            IronSourceBannerLayout bannerLayout = (IronSourceBannerLayout) adData.opt(IronSourceConstants.BANNER_LAYOUT);

            if (bannerLayout != null) {
                ISBannerSize size = bannerLayout.getSize();

                if (size.isAdaptive()) {
                    AdSize adSize = getAdSize(size, AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()));
                    extras.putInt("adaptive_banner_w", adSize.getWidth());
                    extras.putInt("adaptive_banner_h", adSize.getHeight());
                    IronLog.ADAPTER_API.verbose("adaptive banner width = " + adSize.getWidth() + ", height = " + adSize.getHeight());
                }
            }
        }
        getAdapter().collectBiddingData(biddingDataCallback, AdFormat.BANNER, extras);
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
            AdSize adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ContextProvider.getInstance().getApplicationContext(), adSize.getWidth());
            IronLog.INTERNAL.verbose("original height - " + adSize.getHeight() + " adaptive height - " + adaptiveSize.getHeight());
            return adaptiveSize;
        }

        return adSize;
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

package com.ironsource.adapters.admob.banner;

import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

// AdMob banner listener
public class AdMobBannerAdListener implements AdLoadCallback<BannerAd>, BannerAdEventCallback {
    // data
    private BannerSmashListener mListener;
    private String mAdUnitId;
    private AdView mAdView;

    AdMobBannerAdListener(BannerSmashListener listener, String adUnitId, AdView adView) {
        mListener = listener;
        mAdUnitId = adUnitId;
        mAdView = adView;
    }

    // ad finished loading
    @Override
    public void onAdLoaded(@NotNull BannerAd bannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdView == null) {
            IronLog.INTERNAL.verbose("adView is null");
            return;
        }

        // Set event callback
        bannerAd.setAdEventCallback(this);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        ResponseInfo responseInfo = bannerAd.getResponseInfo();
        String creativeId = (responseInfo != null) ? responseInfo.getResponseId() : null;

        if (TextUtils.isEmpty(creativeId)) {
            mListener.onBannerAdLoaded(mAdView, layoutParams);
        } else {
            Map<String, Object> extraData = new HashMap<>();
            extraData.put(AdMobAdapter.CREATIVE_ID_KEY, creativeId);
            IronLog.ADAPTER_CALLBACK.verbose(AdMobAdapter.CREATIVE_ID_KEY + " = " + creativeId);
            mListener.onBannerAdLoaded(mAdView, layoutParams, extraData);
          }
    }

    // ad request failed
    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        String adapterError;
        IronSourceError ironSourceErrorObject;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

        ironSourceErrorObject = AdMobAdapter.isNoFillError(loadAdError.getCode()) ?
                new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError) :
                new IronSourceError(IronSourceError.ERROR_CODE_GENERIC, adapterError);

        IronLog.ADAPTER_CALLBACK.error(adapterError);
        mListener.onBannerAdLoadFailed(ironSourceErrorObject);
    }

    // Called when impression is recorded for the ad
    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdShown();
    }

    // banner was clicked
    @Override
    public void onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }

    //ad opened an overlay that covers the screen after a click
    @Override
    public void onAdShowedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenPresented();
    }

    // the user is about to return to the app after clicking on an ad.
    @Override
    public void onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenDismissed();
    }
}

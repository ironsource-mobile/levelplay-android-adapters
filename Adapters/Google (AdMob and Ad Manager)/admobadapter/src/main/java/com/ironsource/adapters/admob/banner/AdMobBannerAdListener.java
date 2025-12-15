package com.ironsource.adapters.admob.banner;

import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.util.HashMap;
import java.util.Map;

// AdMob banner listener
public class AdMobBannerAdListener extends AdListener {
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
    public void onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdView == null) {
            IronLog.INTERNAL.verbose("adView is null");
            return;
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        ResponseInfo responseInfo = mAdView.getResponseInfo();
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
    public void onAdFailedToLoad(LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        String adapterError;
        IronSourceError ironSourceErrorObject;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (loadAdError != null) {
            adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

            if (loadAdError.getCause() != null) {
                adapterError = adapterError + " Caused by - " + loadAdError.getCause();
            }

            ironSourceErrorObject = AdMobAdapter.isNoFillError(loadAdError.getCode()) ?
                new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError) :
                ErrorBuilder.buildLoadFailedError(adapterError);
        } else {
            adapterError = "Banner failed to load (loadAdError is null)";
            ironSourceErrorObject = ErrorBuilder.buildLoadFailedError(adapterError);
        }

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
    public void onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenPresented();
    }

    // the user is about to return to the app after clicking on an ad.
    @Override
    public void onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenDismissed();
    }
}
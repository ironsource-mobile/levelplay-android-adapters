package com.ironsource.adapters.admob.interstitial;

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import org.jetbrains.annotations.NotNull;

// AdMob interstitial show listener
public class AdMobInterstitialAdShowListener implements InterstitialAdEventCallback {
    // data
    private InterstitialSmashListener mListener;
    private String mAdUnitId;

    AdMobInterstitialAdShowListener(String adUnitId, InterstitialSmashListener listener) {
        mListener = listener;
        mAdUnitId = adUnitId;
    }

    // Called when fullscreen content is shown.
    @Override
    public void onAdShowedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
    }

    // Called when fullscreen content failed to show.
    @Override
    public void onAdFailedToShowFullScreenContent(@NotNull FullScreenContentError error) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        String adapterError = error.getMessage() + "( " + error.getCode() + " )";

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);
        mListener.onInterstitialAdShowFailed(new IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "onInterstitialAdShowFailed " + mAdUnitId + " " + adapterError));
    }

    // Called when impression is recorded for the ad
    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
        mListener.onInterstitialAdShowSucceeded();
    }

    // Called when an ad was clicked
    @Override
    public void onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    // Called when fullscreen content is dismissed.
    @Override
    public void onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClosed();
    }
}

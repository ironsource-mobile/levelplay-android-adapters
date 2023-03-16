package com.ironsource.adapters.admob.interstitial;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import org.jetbrains.annotations.NotNull;

// AdMob interstitial show listener
public class AdMobInterstitialAdShowListener extends FullScreenContentCallback {
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
    public void onAdFailedToShowFullScreenContent(@NotNull AdError adError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        int errorCode = adError.getCode();

        String adapterError = adError.getMessage() + "( " + errorCode + " )";

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (adError.getCause() != null) {
            adapterError = adapterError + " Caused by - " + adError.getCause();
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);
        mListener.onInterstitialAdShowFailed(new IronSourceError(errorCode, "onInterstitialAdShowFailed " + mAdUnitId + " " + adapterError));
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

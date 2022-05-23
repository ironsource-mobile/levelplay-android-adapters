package com.ironsource.adapters.admob;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

// AdMob interstitial show listener
public class AdMobInterstitialAdShowListener extends FullScreenContentCallback {
    // data
    private String mAdUnitId;
    private WeakReference<AdMobAdapter> mAdapter;
    private InterstitialSmashListener mListener;

    AdMobInterstitialAdShowListener(AdMobAdapter adapter, InterstitialSmashListener listener, String adUnitId) {
        mAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mAdUnitId = adUnitId;
    }

    // Called when fullscreen content is shown.
    @Override
    public void onAdShowedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
    }


    // Called when fullscreen content failed to show.
    @Override
    public void onAdFailedToShowFullScreenContent(@NotNull AdError adError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        int errorCode = adError.getCode();;
        String adapterError = adError.getMessage() + "( " + errorCode + " )";

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        if (adError.getCause() != null) {
            adapterError = adapterError + " Caused by - " + adError.getCause();
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);
        mListener.onInterstitialAdShowFailed(new IronSourceError(errorCode, mAdapter.get().getProviderName() + "onInterstitialAdShowFailed " + mAdUnitId + " " + adapterError));
    }

    // Called when impression is recorded for the ad
    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

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

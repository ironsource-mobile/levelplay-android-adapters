package com.ironsource.adapters.admob;

import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class AdMobInterstitialAdLoadListener extends InterstitialAdLoadCallback {

    // data
    private String mAdUnitId;
    private WeakReference<AdMobAdapter> mAdapter;
    private InterstitialSmashListener mListener;

    AdMobInterstitialAdLoadListener(AdMobAdapter adapter, String adUnitId, InterstitialSmashListener listener) {
        mAdapter = new WeakReference<>(adapter);
        mAdUnitId = adUnitId;
        mListener = listener;
    }

    @Override
    public void onAdLoaded(@NotNull InterstitialAd interstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }
        mAdapter.get().mAdIdToInterstitialAd.put(mAdUnitId, interstitialAd);
        mAdapter.get().mInterstitialAdsAvailability.put(mAdUnitId, true);

        mListener.onInterstitialAdReady();
    }

    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        int errorCode = loadAdError.getCode();
        String adapterError = loadAdError.getMessage() + "( " + errorCode + " ) ";

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        if (mAdapter.get().isNoFillError(errorCode)) {
            errorCode = IronSourceError.ERROR_IS_LOAD_NO_FILL;
            adapterError = "No Fill";
        }

        if (loadAdError.getCause() != null) {
            adapterError = adapterError + " Caused by - " + loadAdError.getCause();
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        mListener.onInterstitialAdLoadFailed(new IronSourceError(errorCode, adapterError));

    }
}





package com.ironsource.adapters.admob.interstitial;

import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

// AdMob interstitial load listener
public class AdMobInterstitialAdLoadListener extends InterstitialAdLoadCallback {

    // data
    private final WeakReference<AdMobInterstitialAdapter> mAdapter;
    private final String mAdUnitId;
    private final InterstitialSmashListener mListener;


    AdMobInterstitialAdLoadListener(AdMobInterstitialAdapter adapter, String adUnitId, InterstitialSmashListener listener) {
        mAdapter = new WeakReference<>(adapter);
        mAdUnitId = adUnitId;
        mListener = listener;
    }

    //interstitial ad was loaded
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

        mAdapter.get().onInterstitialAdLoaded(mAdUnitId, interstitialAd);

        mListener.onInterstitialAdReady();
    }

    //interstitial ad failed to load
    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        int errorCode = loadAdError.getCode();
        String adapterError = loadAdError.getMessage() + "( " + errorCode + " ) ";

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        //check if error is no fill error
        if (AdMobAdapter.isNoFillError(errorCode)) {
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





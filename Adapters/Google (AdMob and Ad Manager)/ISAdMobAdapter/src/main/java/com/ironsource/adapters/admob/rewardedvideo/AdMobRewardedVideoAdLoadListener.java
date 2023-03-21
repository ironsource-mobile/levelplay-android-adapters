package com.ironsource.adapters.admob.rewardedvideo;

import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

// AdMob rewarded video load listener
public class AdMobRewardedVideoAdLoadListener extends RewardedAdLoadCallback {

    // data
    private WeakReference<AdMobRewardedVideoAdapter> mRewardedVideoAdapter;
    private RewardedVideoSmashListener mListener;
    private String mAdUnitId;

    AdMobRewardedVideoAdLoadListener(AdMobRewardedVideoAdapter adapter, String adUnitId, RewardedVideoSmashListener listener) {
        mRewardedVideoAdapter = new WeakReference<>(adapter);
        mAdUnitId = adUnitId;
        mListener = listener;
    }

    //rewarded video ad was loaded
    @Override
    public void onAdLoaded(@NotNull RewardedAd rewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mRewardedVideoAdapter == null || mRewardedVideoAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mRewardedVideoAdapter.get().onRewardedVideoAdLoaded(mAdUnitId, rewardedAd);
        mListener.onRewardedVideoAvailabilityChanged(true);
    }

    //rewarded video ad failed to load
    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        int errorCode;
        String adapterError;

        errorCode = loadAdError.getCode();
        adapterError = loadAdError.getMessage() + "( " + errorCode + " )";

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        //check if error is no fill error
        if (AdMobAdapter.isNoFillError(errorCode)) {
            errorCode = IronSourceError.ERROR_RV_LOAD_NO_FILL;
            adapterError = "No Fill";
        }

        if (loadAdError.getCause() != null) {
            adapterError = adapterError + "Caused by " + loadAdError.getCause();
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        mListener.onRewardedVideoAvailabilityChanged(false);
        mListener.onRewardedVideoLoadFailed(new IronSourceError(errorCode, adapterError));
    }
}




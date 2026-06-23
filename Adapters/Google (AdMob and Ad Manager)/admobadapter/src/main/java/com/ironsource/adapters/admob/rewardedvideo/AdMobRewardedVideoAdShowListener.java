package com.ironsource.adapters.admob.rewardedvideo;

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import org.jetbrains.annotations.NotNull;

// AdMob rewarded video show listener
public class AdMobRewardedVideoAdShowListener implements RewardedAdEventCallback, OnUserEarnedRewardListener {

    // data
    private RewardedVideoSmashListener mListener;
    private String mAdUnitId;

    AdMobRewardedVideoAdShowListener(String adUnitId, RewardedVideoSmashListener listener) {
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
        mListener.onRewardedVideoAdShowFailed(new IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "onRewardedAdFailedToShow " + mAdUnitId + " " + adapterError));
    }

    // Called when impression is recorded for the ad
    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdOpened();
    }

    // Called when an ad was clicked
    @Override
    public void onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClicked();
    }

    // Called when a reward was earned
    @Override
    public void onUserEarnedReward(@NotNull RewardItem rewardItem) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdRewarded();
    }

    // Called when fullscreen content is dismissed.
    @Override
    public void onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClosed();
    }
}

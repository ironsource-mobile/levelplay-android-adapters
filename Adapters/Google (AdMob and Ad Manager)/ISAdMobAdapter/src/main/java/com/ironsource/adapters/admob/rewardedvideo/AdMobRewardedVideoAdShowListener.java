package com.ironsource.adapters.admob.rewardedvideo;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import org.jetbrains.annotations.NotNull;

// AdMob rewarded video show listener
public class AdMobRewardedVideoAdShowListener extends FullScreenContentCallback implements OnUserEarnedRewardListener {

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
        mListener.onRewardedVideoAdShowFailed(new IronSourceError(errorCode,"onRewardedAdFailedToShow " + mAdUnitId + " " + adapterError));
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

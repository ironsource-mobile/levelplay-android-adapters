package com.ironsource.adapters.adcolony;

import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import java.lang.ref.WeakReference;

import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_RV_EXPIRED_ADS;

final class AdColonyRewardedVideoAdListener extends AdColonyInterstitialListener implements AdColonyRewardListener {

    // data
    private String mZoneId;
    private RewardedVideoSmashListener mListener;
    private WeakReference<AdColonyAdapter> mAdapter;

    AdColonyRewardedVideoAdListener(AdColonyAdapter adapter, RewardedVideoSmashListener listener, String zoneId) {
        mAdapter = new WeakReference<>(adapter);
        mZoneId = zoneId;
        mListener = listener;
    }

    @Override
    public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mZoneIdToRewardedVideoAdObject.put(mZoneId, adColonyInterstitial);
        mListener.onRewardedVideoAvailabilityChanged(true);
    }

    public void onRequestNotFilled(AdColonyZone zone) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAvailabilityChanged(false);
    }

    public void onOpened(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdOpened();
        mListener.onRewardedVideoAdStarted();
    }

    public void onClicked(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClicked();
    }

    public void onReward(AdColonyReward adColonyReward) {
        IronLog.ADAPTER_CALLBACK.verbose("adColonyReward.success() = " + adColonyReward.success());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (adColonyReward.success()) {
            mListener.onRewardedVideoAdRewarded();
        }
    }

    @Override
    public void onExpiring(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoLoadFailed(new IronSourceError(ERROR_RV_EXPIRED_ADS, "ads are expired"));
    }

    public void onClosed(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdEnded();
        mListener.onRewardedVideoAdClosed();
    }


}

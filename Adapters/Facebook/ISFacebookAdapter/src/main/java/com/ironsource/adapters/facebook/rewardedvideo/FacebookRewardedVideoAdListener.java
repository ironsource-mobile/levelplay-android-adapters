package com.ironsource.adapters.facebook.rewardedvideo;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAdExtendedListener;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import java.lang.ref.WeakReference;

public class FacebookRewardedVideoAdListener implements RewardedVideoAdExtendedListener {
    // data
    private String mPlacementId;
    private RewardedVideoSmashListener mListener;
    private WeakReference<FacebookRewardedVideoAdapter> mAdapter;

    // Ad closed indication
    private boolean mDidCallClosed;

    public FacebookRewardedVideoAdListener(FacebookRewardedVideoAdapter adapter, String placementId, RewardedVideoSmashListener listener) {
        mAdapter = new WeakReference<>(adapter);
        mPlacementId = placementId;
        mListener = listener;
    }

    @Override
    public void onAdLoaded(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mAdsAvailability.put(mPlacementId, true);
        mListener.onRewardedVideoAvailabilityChanged(true);

    }

    @Override
    public void onError(Ad ad, AdError adError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId + " error = " + adError.getErrorCode() + ", " + adError.getErrorMessage());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mAdsAvailability.put(mPlacementId, false);
        int errorCode = FacebookAdapter.isNoFillError(adError) ? IronSourceError.ERROR_RV_LOAD_NO_FILL : adError.getErrorCode();
        IronSourceError ironSourceError = new IronSourceError(errorCode, adError.getErrorMessage());


        if (mAdapter.get().mPlacementIdToShowAttempts.get(mPlacementId)) {
            mListener.onRewardedVideoAdShowFailed(ironSourceError);
        } else {
            mListener.onRewardedVideoAvailabilityChanged(false);
            mListener.onRewardedVideoLoadFailed(ironSourceError);
        }

    }

    @Override
    public void onLoggingImpression(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mDidCallClosed = false;
        mListener.onRewardedVideoAdOpened();
        mListener.onRewardedVideoAdStarted();
    }

    @Override
    public void onAdClicked(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClicked();
    }

    @Override
    public void onRewardedVideoCompleted() {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdEnded();
        mListener.onRewardedVideoAdRewarded();
    }

    @Override
    public void onRewardedVideoClosed() {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mDidCallClosed = true;
        mListener.onRewardedVideoAdClosed();
    }

    @Override
    public void onRewardedVideoActivityDestroyed() {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        /*
         * This callback will only be triggered if the Rewarded Video activity has
         * been destroyed without being properly closed. This can happen if an
         * app with launchMode:singleTask (such as a Unity game) goes to
         * background and is then relaunched by tapping the icon.
         */
        if (!mDidCallClosed) {
            mListener.onRewardedVideoAdClosed();
        }
    }

}

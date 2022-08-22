package com.ironsource.adapters.facebook;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import java.lang.ref.WeakReference;

public class FacebookInterstitialAdListener implements InterstitialAdExtendedListener {
    // data
    private String mPlacementId;
    private WeakReference<FacebookAdapter> mAdapter;
    private InterstitialSmashListener mListener;

    // Ad closed indication
    private boolean mDidCallClosed;
    FacebookInterstitialAdListener(FacebookAdapter adapter, InterstitialSmashListener listener, String placement) {
        mAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mPlacementId = placement;
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

        mAdapter.get().mInterstitialAdsAvailability.put(mPlacementId, true);
        mListener.onInterstitialAdReady();

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

        mAdapter.get().mInterstitialAdsAvailability.put(mPlacementId, false);
        int errorCode = adError.getErrorCode() == AdError.NO_FILL_ERROR_CODE ? IronSourceError.ERROR_IS_LOAD_NO_FILL : adError.getErrorCode();
        IronSourceError ironSourceError = new IronSourceError(errorCode, adError.getErrorMessage());

        if (mAdapter.get().mInterstitialPlacementIdShowCalled.get(mPlacementId)) {
            mListener.onInterstitialAdShowFailed(ironSourceError);
        } else {
            mListener.onInterstitialAdLoadFailed(ironSourceError);
        }

    }

    @Override
    public void onInterstitialDisplayed(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);
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
        mListener.onInterstitialAdOpened();
        mListener.onInterstitialAdShowSucceeded();
    }

    @Override
    public void onAdClicked(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    @Override
    public void onInterstitialDismissed(Ad ad) {
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
        mListener.onInterstitialAdClosed();
    }

    @Override
    public void onInterstitialActivityDestroyed() {
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
         * This callback will only be triggered if the Interstitial activity has
         * been destroyed without being properly closed. This can happen if an
         * app with launchMode:singleTask (such as a Unity game) goes to
         * background and is then relaunched by tapping the icon.
         */
        if (!mDidCallClosed) {
            mListener.onInterstitialAdClosed();
        }
    }


    @Override
    public void onRewardedAdCompleted() {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);
    }

    @Override
    public void onRewardedAdServerSucceeded() {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);
    }

    @Override
    public void onRewardedAdServerFailed() {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);
    }


}

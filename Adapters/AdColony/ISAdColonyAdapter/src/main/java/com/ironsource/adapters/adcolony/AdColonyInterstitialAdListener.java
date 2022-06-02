package com.ironsource.adapters.adcolony;

import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.lang.ref.WeakReference;

final class AdColonyInterstitialAdListener extends AdColonyInterstitialListener {

    // data
    private String mZoneId;
    private InterstitialSmashListener mListener;
    private WeakReference<AdColonyAdapter> mAdapter;

    AdColonyInterstitialAdListener(AdColonyAdapter adapter, InterstitialSmashListener listener, String zoneId) {
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

        mAdapter.get().mZoneIdToInterstitialAdObject.put(mZoneId, adColonyInterstitial);
        mListener.onInterstitialAdReady();
    }

    public void onRequestNotFilled(AdColonyZone zone) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError("Request Not Filled"));
    }

    public void onOpened(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
        mListener.onInterstitialAdShowSucceeded();
    }

    public void onClicked(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    @Override
    public void onExpiring(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);
    }

    public void onClosed(AdColonyInterstitial adColonyInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClosed();
    }


}
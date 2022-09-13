package com.ironsource.adapters.adcolony;

import android.widget.FrameLayout;

import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.lang.ref.WeakReference;

final class AdColonyBannerAdListener extends AdColonyAdViewListener {

    // data
    private String mZoneId;
    private BannerSmashListener mListener;
    private WeakReference<AdColonyAdapter> mAdapter;
    private FrameLayout.LayoutParams mBannerLayoutParams;

    AdColonyBannerAdListener(AdColonyAdapter adapter, BannerSmashListener listener, String zoneId, FrameLayout.LayoutParams bannerLayoutParams) {
        mAdapter = new WeakReference<>(adapter);
        mZoneId = zoneId;
        mListener = listener;
        mBannerLayoutParams = bannerLayoutParams;
    }

    @Override
    public void onRequestFilled(AdColonyAdView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mZoneIdToBannerAdView.put(mZoneId, bannerView);
        mListener.onBannerAdLoaded(bannerView, mBannerLayoutParams);
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Request Not Filled"));
    }

    @Override
    public void onShow(AdColonyAdView ad) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdShown();
    }

    // This callback is not called when showing all banner types and therefore cannot be used for the show callback
    @Override
    public void onOpened(AdColonyAdView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);
    }

    @Override
    public void onClicked(AdColonyAdView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }

    @Override
    public void onLeftApplication(AdColonyAdView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdLeftApplication();
    }
}
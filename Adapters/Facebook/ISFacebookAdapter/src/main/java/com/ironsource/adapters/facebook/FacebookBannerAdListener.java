package com.ironsource.adapters.facebook;

import android.widget.FrameLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdView;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;

import java.lang.ref.WeakReference;

public class FacebookBannerAdListener implements AdListener {
    // data
    private String mPlacementId;
    private BannerSmashListener mListener;
    private WeakReference<FacebookAdapter> mAdapter;
    private FrameLayout.LayoutParams mBannerLayoutParams;

    public FacebookBannerAdListener(FacebookAdapter adapter, BannerSmashListener listener, String placement, FrameLayout.LayoutParams bannerLayoutParams) {
        mAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mPlacementId = placement;
        mBannerLayoutParams = bannerLayoutParams;
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

        AdView adView = mAdapter.get().mBannerPlacementIdToAd.get(mPlacementId);
        if (adView != null) {
            mListener.onBannerAdLoaded(adView, mBannerLayoutParams);
        }
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId + " error = " + adError.getErrorCode() + ", " + adError.getErrorMessage());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        int errorCode = adError.getErrorCode() == AdError.NO_FILL_ERROR_CODE ? IronSourceError.ERROR_BN_LOAD_NO_FILL : adError.getErrorCode();
        mListener.onBannerAdLoadFailed(new IronSourceError(errorCode, adError.getErrorMessage()));
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdShown();
    }

    @Override
    public void onAdClicked(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }
}

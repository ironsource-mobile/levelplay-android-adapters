package com.ironsource.adapters.unityads;

import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.unity3d.services.banners.BannerErrorCode;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;

import java.lang.ref.WeakReference;

final class UnityAdsBannerListener implements BannerView.IListener {

    // data
    private String mPlacementId;
    private BannerSmashListener mListener;
    private WeakReference<UnityAdsAdapter> mAdapter;

    UnityAdsBannerListener(UnityAdsAdapter adapter, BannerSmashListener listener, String placementId) {
        mAdapter = new WeakReference<>(adapter);
        mPlacementId = placementId;
        mListener = listener;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mListener.onBannerAdLoaded(bannerView, mAdapter.get().createLayoutParams(bannerView.getSize()));
        mListener.onBannerAdShown();
    }

    @Override
    public void onBannerShown(BannerView bannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }
        mListener.onBannerAdShown();
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        String message = mAdapter.get().getProviderName() + " banner, onAdLoadFailed placementId " + mPlacementId + " with error: " + bannerErrorInfo.errorMessage;
        IronSourceError ironSourceError;

        if (bannerErrorInfo.errorCode == BannerErrorCode.NO_FILL) {
            ironSourceError = new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, message);
        } else {
            ironSourceError = ErrorBuilder.buildLoadFailedError(message);
        }

        IronLog.ADAPTER_CALLBACK.error("placementId = " + mPlacementId + " ironSourceError = " + ironSourceError);

        mListener.onBannerAdLoadFailed(ironSourceError);
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdLeftApplication();
    }
}

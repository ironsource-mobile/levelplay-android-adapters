package com.ironsource.adapters.vungle;

import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import java.lang.ref.WeakReference;

public class VungleBannerLoadListener implements LoadAdCallback {
    private BannerSmashListener mListener;
    private WeakReference<VungleAdapter> mAdapter;
    private ISBannerSize mBannerSize;

    VungleBannerLoadListener(VungleAdapter adapter, BannerSmashListener listener, ISBannerSize bannerSize) {
        mAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mBannerSize = bannerSize;
    }

    /**
     * Callback used to notify that the advertisement assets have been downloaded and are ready to play.
     * @Params - placementId – The placement identifier for which the advertisement assets have been downloaded.
     */
    @Override
    public void onAdLoad(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        if(mBannerSize == null) {
            IronLog.INTERNAL.verbose("banner size is null");
            return;
        }

        AdConfig.AdSize vungleBannerSize = mAdapter.get().getBannerSize(mBannerSize);
        if (!mAdapter.get().isBannerAdAvailableInternal(placementId, vungleBannerSize)) {
            IronLog.ADAPTER_CALLBACK.error("can't play ad");
            mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("can't play ad"));
            return;
        }

        VungleBanner vungleBanner = mAdapter.get().createVungleBannerAdView(placementId, vungleBannerSize);

        if (vungleBanner != null) {
            mListener.onBannerAdLoaded(vungleBanner, mAdapter.get().getBannerLayoutParams(mBannerSize));
        } else {
            IronLog.ADAPTER_CALLBACK.error("banner view is null");
            mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(mAdapter.get().getProviderName() + " LoadBanner failed - banner view is null"));
        }
    }

    /**
     * Callback used to notify that an error has occurred while downloading assets. This indicates an unrecoverable error within the SDK, such as lack of network or out of disk space on the device.
     * @Params - placementId – The identifier for the placement for which the error occurred.
     * @Params - exception – exception which will usually be an instance of VungleException when the cause is known.
     */
    @Override
    public void onError(String placementId, VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId + ", exception = " + exception);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        IronSourceError error;
        if (exception.getExceptionCode() == VungleException.NO_SERVE) {
            error = new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, exception.getLocalizedMessage());
        } else {
            error = ErrorBuilder.buildLoadFailedError(exception.getLocalizedMessage());
        }

        mListener.onBannerAdLoadFailed(error);
    }
}
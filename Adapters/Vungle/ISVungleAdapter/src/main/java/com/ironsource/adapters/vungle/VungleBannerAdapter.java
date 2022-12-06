package com.ironsource.adapters.vungle;

import android.app.Activity;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.vungle.ads.AdConfig;
import com.vungle.ads.AdSize;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BannerView;
import com.vungle.ads.BaseAd;
import com.vungle.ads.VungleException;

final class VungleBannerAdapter implements BannerAdListener {

    private BannerSmashListener mListener;
    private BannerAd mBannerAd;
    private ISBannerSize mISBannerSize;
    private boolean mAdLoaded;

    VungleBannerAdapter(String placementId, ISBannerSize size, AdConfig adConfig, BannerSmashListener listener) {
        this.mListener = listener;
        this.mISBannerSize = size;

        if (adConfig == null) {
            adConfig = new AdConfig();
        }

        mBannerAd = new BannerAd(ContextProvider.getInstance().getApplicationContext(), placementId, adConfig);
        mBannerAd.setAdListener(this);
    }

    void load() {
        mBannerAd.load(null);
    }

    public void loadWithBid(String serverData) {
        mBannerAd.load(serverData);
    }

    public void destroy() {
        if (mBannerAd != null) {
            mBannerAd.finishAd();
            mBannerAd = null;
        }
        mListener = null;
        mISBannerSize = null;
    }

    @Override
    public void adLoaded(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        mAdLoaded = true;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mISBannerSize == null) {
            IronLog.INTERNAL.verbose("banner size is null");
            return;
        }

        if (!mBannerAd.canPlayAd()) {
            IronLog.ADAPTER_CALLBACK.error("can't play ad");
            mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("can't play ad"));
            return;
        }

        BannerView bannerView = mBannerAd.getBannerView();

        if (bannerView != null) {
            mListener.onBannerAdLoaded(bannerView, getBannerLayoutParams(mISBannerSize));
        } else {
            IronLog.ADAPTER_CALLBACK.error("banner view is null");
            mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Vungle LoadBanner failed - banner view is null"));
        }
    }

    @Override
    public void adStart(BaseAd baseAd) {
        // no-op
    }

    @Override
    public void adImpression(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdShown();
    }

    @Override
    public void adClick(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }

    @Override
    public void adEnd(BaseAd baseAd) {
        // no-op
    }

    @Override
    public void error(BaseAd baseAd, VungleException exception) {
        if (mAdLoaded) {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId() + ", exception = " + exception);
        } else {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

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

    @Override
    public void onAdLeftApplication(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdLeftApplication();
    }

    static AdSize getBannerSize(ISBannerSize size) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                return AdSize.BANNER;
            case "RECTANGLE":
                return AdSize.VUNGLE_MREC;
            case "SMART":
                return AdapterUtils.isLargeScreen(ContextProvider.getInstance().getCurrentActiveActivity()) ? AdSize.BANNER_LEADERBOARD : AdSize.BANNER;
        }

        return null;
    }

    private FrameLayout.LayoutParams getBannerLayoutParams(ISBannerSize size) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, 0);

        Activity activity = ContextProvider.getInstance().getCurrentActiveActivity();

        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                break;
            case "RECTANGLE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 300), AdapterUtils.dpToPixels(activity, 250));
                break;
            case "SMART":
                if (AdapterUtils.isLargeScreen(activity)) {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 728), AdapterUtils.dpToPixels(activity, 90));
                } else {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                }

                break;
        }

        // set gravity
        layoutParams.gravity = Gravity.CENTER;

        return layoutParams;
    }

}

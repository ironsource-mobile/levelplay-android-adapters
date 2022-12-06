package com.ironsource.adapters.vungle;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.VungleException;

final class VungleInterstitialAdapter implements InterstitialAdListener {

    private InterstitialSmashListener mListener;
    private InterstitialAd mInterstitialAd;
    private boolean mAdLoaded;

    VungleInterstitialAdapter(String placementId, AdConfig adConfig, InterstitialSmashListener listener) {
        this.mListener = listener;

        if (adConfig == null) {
            adConfig = new AdConfig();
        }

        mInterstitialAd = new InterstitialAd(ContextProvider.getInstance().getApplicationContext(), placementId, adConfig);
        mInterstitialAd.setAdListener(this);
    }

    void load() {
        mInterstitialAd.load(null);
    }

    public void loadWithBid(String serverData) {
        mInterstitialAd.load(serverData);
    }

    public boolean canPlayAd() {
        return mInterstitialAd.canPlayAd();
    }

    public void play() {
        mInterstitialAd.play();
    }

    public void destroy() {
        mListener = null;
        mInterstitialAd = null;
    }

    @Override
    public void adLoaded(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        mAdLoaded = true;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdReady();
    }

    @Override
    public void adStart(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdShowSucceeded();
    }

    @Override
    public void adImpression(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
    }

    @Override
    public void adClick(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    @Override
    public void adEnd(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClosed();
    }

    @Override
    public void error(BaseAd baseAd, VungleException exception) {
        if (mAdLoaded) {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

            if (mListener == null) {
                IronLog.INTERNAL.verbose("listener is null");
                return;
            }

            String errorMessage = " reason = " + exception.getLocalizedMessage() + " errorCode = " + exception.getExceptionCode();
            mListener.onInterstitialAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, errorMessage));
        } else {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

            if (mListener == null) {
                IronLog.INTERNAL.verbose("listener is null");
                return;
            }

            IronSourceError error;
            if (exception.getExceptionCode() == VungleException.NO_SERVE) {
                error = new IronSourceError(IronSourceError.ERROR_IS_LOAD_NO_FILL, exception.getLocalizedMessage());
            } else {
                error = ErrorBuilder.buildLoadFailedError(exception.getLocalizedMessage());
            }

            mListener.onInterstitialAdLoadFailed(error);
        }
    }

    @Override
    public void onAdLeftApplication(BaseAd baseAd) {
        // no-op
    }

}

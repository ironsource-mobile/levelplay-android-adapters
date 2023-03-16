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
    public void onAdLoaded(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdReady();
    }

    @Override
    public void onAdStart(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdShowSucceeded();
    }

    @Override
    public void onAdImpression(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
    }

    @Override
    public void onAdClicked(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    @Override
    public void onAdEnd(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClosed();
    }

    @Override
    public void onAdFailedToPlay(BaseAd baseAd, VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        String errorMessage = " reason = " + exception.getLocalizedMessage() + " errorCode = " + exception.getExceptionCode();
        mListener.onInterstitialAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, errorMessage));
    }

    @Override
    public void onAdFailedToLoad(BaseAd baseAd, VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToLoad placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

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

    @Override
    public void onAdLeftApplication(BaseAd baseAd) {
        // no-op
    }

}

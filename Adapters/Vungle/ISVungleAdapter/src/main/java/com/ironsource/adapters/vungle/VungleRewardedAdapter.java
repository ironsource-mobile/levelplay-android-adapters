package com.ironsource.adapters.vungle;

import android.text.TextUtils;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleError;

final class VungleRewardedAdapter implements RewardedAdListener {

    private RewardedVideoSmashListener mListener;
    private RewardedAd mRewardedAd;

    VungleRewardedAdapter(String placementId, AdConfig adConfig, RewardedVideoSmashListener listener) {
        this.mListener = listener;

        if (adConfig == null) {
            adConfig = new AdConfig();
        }

        mRewardedAd = new RewardedAd(ContextProvider.getInstance().getApplicationContext(), placementId, adConfig);
        mRewardedAd.setAdListener(this);
    }

    void load() {
        mRewardedAd.load(null);
    }

    public void loadWithBid(String serverData) {
        mRewardedAd.load(serverData);
    }

    public boolean canPlayAd() {
        return mRewardedAd.canPlayAd();
    }

    public void play() {
        mRewardedAd.play();
    }

    public void destroy() {
        mListener = null;
        mRewardedAd = null;
    }

    public void setIncentivizedFields(final String userID,
                                      final String title,
                                      final String body,
                                      final String keepWatching,
                                      final String close) {
        if (!TextUtils.isEmpty(userID)) {
            mRewardedAd.setUserId(userID);
        }
        if (!TextUtils.isEmpty(title)) {
            mRewardedAd.setAlertTitleText(title);
        }
        if (!TextUtils.isEmpty(body)) {
            mRewardedAd.setAlertBodyText(body);
        }
        if (!TextUtils.isEmpty(keepWatching)) {
            mRewardedAd.setAlertContinueButtonText(keepWatching);
        }
        if (!TextUtils.isEmpty(close)) {
            mRewardedAd.setAlertCloseButtonText(close);
        }
    }

    @Override
    public void onAdLoaded(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAvailabilityChanged(true);
    }

    @Override
    public void onAdStart(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdStarted();
    }

    @Override
    public void onAdImpression(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdOpened();
    }

    @Override
    public void onAdRewarded(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdRewarded();
    }

    @Override
    public void onAdClicked(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClicked();
    }

    @Override
    public void onAdEnd(BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdEnded();
        mListener.onRewardedVideoAdClosed();
    }

    @Override
    public void onAdFailedToPlay(BaseAd baseAd, VungleError e) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = " + baseAd.getPlacementId() + ", error = " + e);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, e.getErrorMessage()));
    }

    @Override
    public void onAdFailedToLoad(BaseAd baseAd, VungleError e) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToLoad placementId = " + baseAd.getPlacementId() + ", error = " + e);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        IronSourceError error;
        if (e.getCode() == VungleError.NO_SERVE) {
            error = new IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, e.getErrorMessage());
        } else {
            error = ErrorBuilder.buildLoadFailedError(e.getErrorMessage());
        }

        mListener.onRewardedVideoAvailabilityChanged(false);
        mListener.onRewardedVideoLoadFailed(error);
    }

    @Override
    public void onAdLeftApplication(BaseAd baseAd) {
        // no-op
    }

}

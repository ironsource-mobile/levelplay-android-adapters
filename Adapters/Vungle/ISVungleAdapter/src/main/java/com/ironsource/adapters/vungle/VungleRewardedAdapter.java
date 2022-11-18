package com.ironsource.adapters.vungle;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.vungle.ads.VungleException;

final class VungleRewardedAdapter implements RewardedAdListener {

    private RewardedVideoSmashListener mListener;
    private RewardedAd mRewardedAd;
    private boolean mAdLoaded;

    VungleRewardedAdapter(String placementId, AdConfig adConfig, RewardedVideoSmashListener listener) {
        this.mListener = listener;

        if (adConfig == null) {
            adConfig = new AdConfig();
        }

        mRewardedAd = new RewardedAd(ContextProvider.getInstance().getApplicationContext(), placementId, adConfig);
        mRewardedAd.setAdListener(this);
    }

    void load() {
        mRewardedAd.load();
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

    public void setIncentivizedFields(@Nullable final String userID,
                                      @Nullable final String title,
                                      final @Nullable String body,
                                      final @Nullable String keepWatching,
                                      final @Nullable String close) {
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
    public void adLoaded(@NonNull BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        mAdLoaded = true;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAvailabilityChanged(true);
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdStarted();
    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdOpened();
    }

    @Override
    public void adRewarded(@NonNull BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdRewarded();
    }

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClicked();
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdEnded();
        mListener.onRewardedVideoAdClosed();
    }

    @Override
    public void error(@NonNull BaseAd baseAd, @NonNull VungleException exception) {
        if (mAdLoaded) {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

            if (mListener == null) {
                IronLog.INTERNAL.verbose("listener is null");
                return;
            }

            mListener.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, exception.getLocalizedMessage()));
        } else {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.getPlacementId() + ", exception = " + exception);

            if (mListener == null) {
                IronLog.INTERNAL.verbose("listener is null");
                return;
            }

            IronSourceError error;
            if (exception.getExceptionCode() == VungleException.NO_SERVE) {
                error = new IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, exception.getLocalizedMessage());
            } else {
                error = ErrorBuilder.buildLoadFailedError(exception.getLocalizedMessage());
            }

            mListener.onRewardedVideoAvailabilityChanged(false);
            mListener.onRewardedVideoLoadFailed(error);
        }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
        // no-op
    }

}

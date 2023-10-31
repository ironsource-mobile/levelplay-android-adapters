package com.ironsource.adapters.chartboost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.callbacks.RewardedCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.RewardEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import java.lang.ref.WeakReference;

final class ChartboostRewardedVideoAdListener implements RewardedCallback {

    // data
    private String mLocationId;
    private RewardedVideoSmashListener mListener;

    ChartboostRewardedVideoAdListener(RewardedVideoSmashListener listener, String locationId) {
        mLocationId = locationId;
        mListener = listener;
    }

    @Override
    public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (cacheError != null) {
            IronLog.ADAPTER_CALLBACK.error("error = " + cacheError.toString());

            IronSourceError isError;

            if (cacheError.getCode() == CacheError.Code.NO_AD_FOUND) {
                isError = new IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, "load failed - rewarded video no fill");
            } else {
                isError = new IronSourceError(cacheError.getCode().getErrorCode(), cacheError.toString());
            }

            mListener.onRewardedVideoAvailabilityChanged(false);
            mListener.onRewardedVideoLoadFailed(isError);

        } else {
            mListener.onRewardedVideoAvailabilityChanged(true);
        }
    }

    @Override
    public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);
    }

    @Override
    public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (showError != null) {
            IronLog.ADAPTER_CALLBACK.error("error = " + showError.toString());
            mListener.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, showError.toString()));
        }
    }

    @Override
    public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdOpened();
    }

    @Override
    public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (clickError != null) {
            IronLog.ADAPTER_CALLBACK.verbose("clickError = " + clickError.toString());
        }

        mListener.onRewardedVideoAdClicked();
    }

    @Override
    public void onRewardEarned(@NonNull RewardEvent rewardEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdRewarded();
    }

    @Override
    public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClosed();
    }
}
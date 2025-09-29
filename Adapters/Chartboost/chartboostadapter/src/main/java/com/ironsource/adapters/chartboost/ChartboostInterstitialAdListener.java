package com.ironsource.adapters.chartboost;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.callbacks.InterstitialCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ExpirationEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import java.util.HashMap;
import java.util.Map;

final class ChartboostInterstitialAdListener implements InterstitialCallback {

    // data
    private String mLocationId;
    private InterstitialSmashListener mListener;

    ChartboostInterstitialAdListener(InterstitialSmashListener listener, String locationId) {
        mLocationId = locationId;
        mListener = listener;
    }

    @Override
    public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
        String creativeId = cacheEvent.getAdID();
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId + " creativeId = " + creativeId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (cacheError != null) {
            IronLog.ADAPTER_CALLBACK.error("error = " + cacheError.toString());

            IronSourceError isError;

            if (cacheError.getCode() == CacheError.Code.NO_AD_FOUND) {
                isError = new IronSourceError(IronSourceError.ERROR_IS_LOAD_NO_FILL, " load failed - interstitial no fill");
            } else {
                isError = new IronSourceError(cacheError.getCode().getErrorCode(), cacheError.toString());
            }

            mListener.onInterstitialAdLoadFailed(isError);
        } else {
            if (TextUtils.isEmpty(creativeId)) {
                mListener.onInterstitialAdReady();
            } else {
                Map<String, Object> extraData = new HashMap<>();
                extraData.put(ChartboostAdapter.CREATIVE_ID_KEY, creativeId);
                mListener.onInterstitialAdReady(extraData);
            }
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
            mListener.onInterstitialAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, showError.toString()));
        }
    }

    @Override
    public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        String creativeId = impressionEvent.getAdID();
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId + " creativeId = " + creativeId);

        if (TextUtils.isEmpty(creativeId)) {
            mListener.onInterstitialAdOpened();
            mListener.onInterstitialAdShowSucceeded();
        } else {
            Map<String, Object> extraData = new HashMap<>();
            extraData.put(ChartboostAdapter.CREATIVE_ID_KEY, creativeId);
            mListener.onInterstitialAdOpened(extraData);
            mListener.onInterstitialAdShowSucceeded(extraData);
        }
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

        mListener.onInterstitialAdClicked();
    }

    @Override
    public void onAdExpired(@NonNull ExpirationEvent expirationEvent) {
        IronLog.ADAPTER_CALLBACK.verbose();
    }

    @Override
    public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClosed();
    }
}

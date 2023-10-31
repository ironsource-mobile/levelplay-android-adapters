package com.ironsource.adapters.chartboost;

import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.callbacks.BannerCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.lang.ref.WeakReference;

final class ChartboostBannerAdListener implements BannerCallback {

    // data
    private String mLocationId;
    private BannerSmashListener mListener;
    private WeakReference<ChartboostAdapter> mAdapter;
    private FrameLayout.LayoutParams mBannerLayoutParams;

    ChartboostBannerAdListener(ChartboostAdapter adapter, BannerSmashListener listener, String locationId, FrameLayout.LayoutParams bannerLayoutParams) {
        mAdapter = new WeakReference<>(adapter);
        mLocationId = locationId;
        mListener = listener;
        mBannerLayoutParams = bannerLayoutParams;
    }

    @Override
    public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        Banner bannerView = mAdapter.get().mLocationIdToBannerAd.get(mLocationId);

        if (bannerView == null) {
            IronLog.ADAPTER_CALLBACK.error("bannerView is null");
            IronSourceError error = ErrorBuilder.buildLoadFailedError(mAdapter.get().getProviderName() + " load failed - bannerView is null");
            mListener.onBannerAdLoadFailed(error);
            return;
        }


        // check if there is some error to load the banner
        if (cacheError != null) {

            IronLog.ADAPTER_CALLBACK.error("error = " + cacheError.toString());

            IronSourceError error;

            if (cacheError.getCode() == CacheError.Code.NO_AD_FOUND) {
                error = new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, " load failed - banner no fill");
            } else {
                error = new IronSourceError(cacheError.getCode().getErrorCode(), cacheError.toString());
            }

            mListener.onBannerAdLoadFailed(error);
        } else {
            mListener.onBannerAdLoaded(bannerView, mBannerLayoutParams);
            bannerView.show();
        }
    }

    @Override
    public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);
    }

    @Override
    public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);
    }

    @Override
    public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
        IronLog.ADAPTER_CALLBACK.verbose("locationId = " + mLocationId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdShown();
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

        mListener.onBannerAdClicked();
    }
}
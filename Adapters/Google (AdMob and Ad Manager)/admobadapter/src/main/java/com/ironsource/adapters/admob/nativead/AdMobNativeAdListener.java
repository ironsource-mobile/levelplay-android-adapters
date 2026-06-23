package com.ironsource.adapters.admob.nativead;

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData;
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder;
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdSmashListener;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

// AdMob native ad listener
public class AdMobNativeAdListener implements NativeAdLoaderCallback, NativeAdEventCallback {
    // data
    private final WeakReference<AdMobNativeAdAdapter> mAdapter;
    private final String mAdUnitId;
    private final NativeAdSmashListener mListener;

    AdMobNativeAdListener(AdMobNativeAdAdapter adapter, String adUnitId, NativeAdSmashListener listener) {
        mAdapter = new WeakReference<>(adapter);
        mAdUnitId = adUnitId;
        mListener = listener;
    }

    // ad finished loading
    @Override
    public void onNativeAdLoaded(@NotNull NativeAd nativeAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        // Set event callback
        nativeAd.setAdEventCallback(this);

        mAdapter.get().mAd = new WeakReference<>(nativeAd);

        AdapterNativeAdData adapterNativeAdData = new AdMobNativeAdData(nativeAd);
        AdapterNativeAdViewBinder nativeAdViewBinder = new AdMobNativeAdViewBinder(nativeAd);

        mListener.onNativeAdLoaded(adapterNativeAdData, nativeAdViewBinder);
    }

    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        String adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        IronSourceError ironSourceErrorObject = AdMobAdapter.isNoFillError(loadAdError.getCode()) ?
                new IronSourceError(IronSourceError.ERROR_NT_LOAD_NO_FILL, adapterError) :
                new IronSourceError(IronSourceError.ERROR_CODE_GENERIC, adapterError);

        mListener.onNativeAdLoadFailed(ironSourceErrorObject);
    }

    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onNativeAdShown();
    }

    @Override
    public void onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onNativeAdClicked();
    }

    @Override
    public void onAdShowedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
    }

    @Override
    public void onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
    }
}

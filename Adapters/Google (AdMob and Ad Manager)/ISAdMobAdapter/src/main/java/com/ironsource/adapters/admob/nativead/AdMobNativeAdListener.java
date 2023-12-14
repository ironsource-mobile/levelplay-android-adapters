package com.ironsource.adapters.admob.nativead;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData;
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder;
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdSmashListener;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.lang.ref.WeakReference;

// AdMob native ad listener
public class AdMobNativeAdListener extends AdListener implements NativeAd.OnNativeAdLoadedListener {
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
    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mAd = new WeakReference<>(nativeAd);

        AdapterNativeAdData adapterNativeAdData = new AdMobNativeAdData(nativeAd);
        AdapterNativeAdViewBinder nativeAdViewBinder = new AdMobNativeAdViewBinder(nativeAd);

        mListener.onNativeAdLoaded(adapterNativeAdData, nativeAdViewBinder);
    }

    @Override
    public void onAdLoaded() {
        // not used for native ads
    }

    @Override
    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        String adapterError;
        IronSourceError ironSourceErrorObject;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

        if (loadAdError.getCause() != null) {
            adapterError = adapterError + " Caused by - " + loadAdError.getCause();
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        ironSourceErrorObject = AdMobAdapter.isNoFillError(loadAdError.getCode()) ?
                new IronSourceError(IronSourceError.ERROR_NT_LOAD_NO_FILL, adapterError) :
                ErrorBuilder.buildLoadFailedError(adapterError);

        mListener.onNativeAdLoadFailed(ironSourceErrorObject);
    }
    
    @Override
    public void onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
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
    public void onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

    }
}

package com.ironsource.adapters.admob;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.lang.ref.WeakReference;

// AdMob native banner listener
public class AdMobNativeBannerAdListener extends AdListener implements NativeAd.OnNativeAdLoadedListener {
    // data
    private final String mAdUnitId;
    private final WeakReference<AdMobAdapter> mAdapter;
    private final BannerSmashListener mListener;
    private final ISBannerSize mBannerSize;
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    AdMobNativeBannerAdListener(AdMobAdapter adapter, BannerSmashListener listener, String adUnitId, ISBannerSize bannerSize) {
        mAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mAdUnitId = adUnitId;
        mBannerSize = bannerSize;
    }

    @Override
    public void onNativeAdLoaded(@NonNull final NativeAd nativeAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                Activity activity = ContextProvider.getInstance().getCurrentActiveActivity();
                AdMobNativeBannerLayout adMobNativeLayout = new AdMobNativeBannerLayout(mBannerSize,activity);
                AdMobNativeBannerViewBinder nativeBannerBinder = new AdMobNativeBannerViewBinder();
                nativeBannerBinder.bindView(nativeAd,adMobNativeLayout);

                //add native banner ad to map
                mAdapter.get().mAdUnitIdToNativeBannerAd.put(mAdUnitId, nativeAd);

                mListener.onBannerAdLoaded(adMobNativeLayout.getNativeAdView(), adMobNativeLayout.getLayoutParams());
            }
        });
    }

    // ad request failed
    @Override
    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        String adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

        if (loadAdError.getCause() != null) {
            adapterError = adapterError + " Caused by - " + loadAdError.getCause();
        }
        IronSourceError ironSourceErrorObject = mAdapter.get().isNoFillError(loadAdError.getCode()) ?
                new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError) :
                ErrorBuilder.buildLoadFailedError(adapterError);

        IronLog.ADAPTER_CALLBACK.error(adapterError + adapterError);
        mListener.onBannerAdLoadFailed(ironSourceErrorObject);
    }

    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        mListener.onBannerAdShown();
    }

    // banner was clicked
    @Override
    public void onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }
        mListener.onBannerAdClicked();
    }

    //ad opened an overlay that covers the screen after a click
    @Override
    public void onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenPresented();
    }

    // the user is about to return to the app after clicking on an ad.
    @Override
    public void onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }
        mListener.onBannerAdScreenDismissed();
    }
}

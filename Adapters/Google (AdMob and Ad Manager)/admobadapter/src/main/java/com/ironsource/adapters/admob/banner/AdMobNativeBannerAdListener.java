package com.ironsource.adapters.admob.banner;

import android.content.Context;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

// AdMob native banner listener
public class AdMobNativeBannerAdListener extends AdListener implements NativeAd.OnNativeAdLoadedListener {
    // data
    private final String mAdUnitId;
    private final WeakReference<AdMobBannerAdapter> mBannerAdapter;
    private final BannerSmashListener mListener;
    private final ISBannerSize mBannerSize;
    private final NativeTemplateType mTemplateType;

    AdMobNativeBannerAdListener(AdMobBannerAdapter adapter, BannerSmashListener listener, String adUnitId, ISBannerSize bannerSize, NativeTemplateType templateType) {
        mBannerAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mAdUnitId = adUnitId;
        mBannerSize = bannerSize;
        mTemplateType = templateType;
    }

    @Override
    public void onNativeAdLoaded(@NotNull final NativeAd nativeAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        AdMobAdapter.postOnUIThread(new Runnable() {
            @Override
            public void run() {
                
                if (mListener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                if (mBannerAdapter == null || mBannerAdapter.get() == null) {
                    IronLog.INTERNAL.verbose("adapter is null");
                    return;
                }

                Context context = ContextProvider.getInstance().getApplicationContext();
                AdMobNativeBannerViewHandler nativeBannerHandler = new AdMobNativeBannerViewHandler(mBannerSize, mTemplateType, context);
                AdMobNativeBannerViewBinder nativeBannerBinder = new AdMobNativeBannerViewBinder();
                nativeBannerBinder.bindView(nativeAd, nativeBannerHandler.getNativeAdView(), mTemplateType);

                //add native banner ad to map
                mBannerAdapter.get().mAdUnitIdToNativeBannerAd.put(mAdUnitId, nativeAd);

                mListener.onBannerAdLoaded(nativeBannerHandler.getNativeAdView(), nativeBannerHandler.getLayoutParams());
            }
        });
    }

    // ad request failed
    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        String adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

        if (loadAdError.getCause() != null) {
            adapterError = adapterError + " Caused by - " + loadAdError.getCause();
        }

        IronSourceError ironSourceErrorObject = AdMobAdapter.isNoFillError(loadAdError.getCode()) ?
                new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError) :
                ErrorBuilder.buildLoadFailedError(adapterError);

        IronLog.ADAPTER_CALLBACK.error(adapterError + adapterError);
        mListener.onBannerAdLoadFailed(ironSourceErrorObject);
    }

    @Override
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

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

package com.ironsource.adapters.facebook.nativead;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdListener;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.environment.workerthread.WorkerManager;
import com.ironsource.environment.workerthread.WorkerResult;
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData;
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdOptionsPosition;
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdSmashListener;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FacebookNativeAdListener implements NativeAdListener {
    // data
    private final NativeAdSmashListener mListener;
    private final String mPlacementId;
    private final AdOptionsPosition mAdOptionsPosition;
    private final Context mContext;

    public FacebookNativeAdListener(Context context, String placementId, AdOptionsPosition adOptionsPosition, NativeAdSmashListener listener) {
        mListener = listener;
        mPlacementId = placementId;
        mAdOptionsPosition = adOptionsPosition;
        mContext = context;
    }

    @Override
    public void onMediaDownloaded(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("mPlacementId = " + mPlacementId);

    }

    @Override
    public void onAdLoaded(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("mPlacementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (ad instanceof NativeAd) {
            final NativeAd nativeAd = (NativeAd) ad;
            nativeAd.unregisterView();
            downloadDrawableFromUrlAndSendOnAdLoaded(nativeAd);

        } else {
            String errorString = "Expected an instance of " + NativeAd.class.getName() + ", received " + ad.getClass().getName();
            mListener.onNativeAdLoadFailed(new IronSourceError(IronSourceError.ERROR_CODE_GENERIC, errorString));
        }
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        IronLog.ADAPTER_CALLBACK.error("error = " + adError.getErrorCode() + ", " + adError.getErrorMessage());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        int errorCode = FacebookAdapter.isNoFillError(adError) ? IronSourceError.ERROR_NT_LOAD_NO_FILL : adError.getErrorCode();
        IronSourceError ironSourceError = new IronSourceError(errorCode, adError.getErrorMessage());

        mListener.onNativeAdLoadFailed(ironSourceError);

    }

    @Override
    public void onLoggingImpression(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("mPlacementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onNativeAdShown();
    }

    @Override
    public void onAdClicked(Ad ad) {
        IronLog.ADAPTER_CALLBACK.verbose("mPlacementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onNativeAdClicked();
    }


    private void downloadDrawableFromUrlAndSendOnAdLoaded(final NativeAd nativeAd) {
        final WorkerManager<Drawable> workerManager = new WorkerManager<>(Executors.newSingleThreadExecutor());
        Callable<Drawable> callable = new Callable<Drawable>() {
            @Override
            public Drawable call() throws Exception {
                // Download the image
                if (nativeAd.getAdIcon() != null && nativeAd.getAdIcon().getUrl() != null) {
                    InputStream inputStream = new java.net.URL(nativeAd.getAdIcon().getUrl()).openStream();
                    Bitmap image = BitmapFactory.decodeStream(inputStream);
                    return new BitmapDrawable(mContext.getResources(), image);
                }

                return null;
            }
        };
        workerManager.addCallable(callable);
        workerManager.startWork(new WorkerManager.WorkEndedListener<Drawable>() {
            @Override
            public void onWorkCompleted(List<WorkerResult<Drawable>> responsesList, long totalWorkDurationMillis) {
                WorkerResult<Drawable> response = responsesList.get(0);
                Drawable drawable = null;
                if (response instanceof WorkerResult.Completed) {
                    drawable = ((WorkerResult.Completed<Drawable>) response).data;
                }
                handleOnAdLoaded(drawable, nativeAd);
            }

            @Override
            public void onWorkFailed(String error) {
                IronLog.INTERNAL.verbose("error while trying to download the native ad icon resource - " + error);

                handleOnAdLoaded(null, nativeAd);
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void handleOnAdLoaded(Drawable drawable, NativeAd nativeAd) {
        AdapterNativeAdData adapterNativeAdData = new FacebookNativeAdData(nativeAd, drawable);
        AdapterNativeAdViewBinder nativeAdViewBinder = new FacebookNativeAdViewBinder(nativeAd, mAdOptionsPosition);
        mListener.onNativeAdLoaded(adapterNativeAdData, nativeAdViewBinder);
    }
}

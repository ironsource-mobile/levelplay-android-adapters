package com.ironsource.adapters.admob.nativead

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.NativeAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class AdMobNativeAdListener(
    private val adapter: WeakReference<AdMobNativeAdAdapter>,
    private val listener: NativeAdListener
) : NativeAdLoaderCallback, NativeAdEventCallback {

    /**
     * Called when the native ad was loaded successfully
     * @param nativeAd - the loaded native ad instance
     */
    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        nativeAd.adEventCallback = this
        adapter.get()?.setNativeAd(nativeAd)

        val adapterNativeAdData = AdMobNativeAdData(nativeAd)
        val nativeAdViewBinder = AdMobNativeAdViewBinder(nativeAd)
        listener.onAdLoadSuccess(adapterNativeAdData, nativeAdViewBinder)
    }

    /**
     * Called when the native ad failed to load
     * @param adError - the load error
     */
    override fun onAdFailedToLoad(adError: LoadAdError) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.FAILED_TO_LOAD.format(adError.code, adError.message))
        listener.onAdLoadFailed(AdMobAdapter.getLoadErrorType(adError.code), adError.code.ordinal, "${adError.message} (${adError.code})")
    }

    /**
     * Called when an impression is recorded for the native ad
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the native ad was clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the native ad opened a fullscreen overlay after a click
     */
    override fun onAdShowedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the fullscreen overlay was dismissed and the user returns to the app
     */
    override fun onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

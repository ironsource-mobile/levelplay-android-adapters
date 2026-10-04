package com.ironsource.adapters.admob.interstitial

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class AdMobInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<AdMobInterstitialAdapter>
) : AdLoadCallback<InterstitialAd>, InterstitialAdEventCallback {

    /**
     * Called when the interstitial ad was loaded successfully
     * @param ad - the loaded interstitial ad instance
     */
    override fun onAdLoaded(ad: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setInterstitialAd(ad)
        adapter.get()?.setInterstitialAdAvailability(true)

        val creativeId = ad.getResponseInfo()?.responseId
        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            IronLog.ADAPTER_CALLBACK.verbose(AdMobConstants.Logs.CREATIVE_ID.format(creativeId))
            listener.onAdLoadSuccess(mapOf(AdMobConstants.CREATIVE_ID_KEY to creativeId))
        }
    }

    /**
     * Called when the interstitial ad failed to load
     * @param adError - the load error
     */
    override fun onAdFailedToLoad(adError: LoadAdError) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.FAILED_TO_LOAD.format(adError.code, adError.message))
        adapter.get()?.setInterstitialAdAvailability(false)

        listener.onAdLoadFailed(AdMobAdapter.getLoadErrorType(adError.code), adError.code.ordinal, "${adError.message} (${adError.code})")
    }

    /**
     * Called when an impression is recorded for the interstitial ad
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the interstitial ad was clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the interstitial ad was dismissed
     */
    override fun onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the interstitial ad failed to show
     * @param error - the show error
     */
    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.FAILED_TO_SHOW.format("${error.message} (${error.code})"))
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, "${error.message} (${error.code})")
    }
}

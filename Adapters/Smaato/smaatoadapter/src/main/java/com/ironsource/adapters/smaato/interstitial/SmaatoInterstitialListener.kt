package com.ironsource.adapters.smaato.interstitial

import com.ironsource.adapters.smaato.SmaatoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.smaato.sdk.interstitial.EventListener
import com.smaato.sdk.interstitial.InterstitialAd
import com.smaato.sdk.interstitial.InterstitialError
import com.smaato.sdk.interstitial.InterstitialRequestError
import java.lang.ref.WeakReference

class SmaatoInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<SmaatoInterstitialAdapter>
) : EventListener {

    /**
     * Called when the interstitial finished loading.
     */
    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        val creativeId = interstitialAd.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(SmaatoConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        adapter.get()?.setInterstitialAd(interstitialAd)

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(SmaatoConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when the interstitial failed to load.
     */
    override fun onAdFailedToLoad(interstitialRequestError: InterstitialRequestError) {
        val interstitialError = interstitialRequestError.interstitialError
        IronLog.ADAPTER_CALLBACK.error(SmaatoConstants.Logs.FAILED_TO_LOAD.format(interstitialError.toString()))

        val errorType = if (interstitialError == InterstitialError.NO_AD_AVAILABLE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }
        listener.onAdLoadFailed(errorType, interstitialError.ordinal, interstitialError.toString())
    }

    /**
     * Called when the interstitial failed to show.
     */
    override fun onAdError(interstitialAd: InterstitialAd, interstitialError: InterstitialError) {
        IronLog.ADAPTER_CALLBACK.error(SmaatoConstants.Logs.FAILED_TO_SHOW.format(interstitialError.toString()))
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, interstitialError.toString())
    }

    /**
     * Called when the interstitial is about to be shown.
     */
    override fun onAdOpened(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the interstitial records an impression.
     */
    override fun onAdImpression(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the interstitial is clicked.
     */
    override fun onAdClicked(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the interstitial is dismissed.
     */
    override fun onAdClosed(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the cached interstitial has expired.
     */
    override fun onAdTTLExpired(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

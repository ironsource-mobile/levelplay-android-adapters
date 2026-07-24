package com.ironsource.adapters.pubmatic.interstitial

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial

class PubMaticInterstitialListener(
    private val listener: InterstitialAdListener
) : POBInterstitial.POBInterstitialListener() {

    /**
     * Notifies that an ad has been received successfully.
     * @param interstitialAd Interstitial ad instance.
     */
    override fun onAdReceived(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Notifies an error encountered while loading an ad.
     * @param interstitialAd Interstitial ad instance.
     * @param error PubMatic ad error.
     */
    override fun onAdFailedToLoad(interstitialAd: POBInterstitial, error: POBError) {
        IronLog.ADAPTER_CALLBACK.error(
            PubMaticConstants.Logs.LOAD_FAILED.format(error.errorCode, error.errorMessage)
        )
        listener.onAdLoadFailed(PubMaticAdapter.getLoadError(error), error.errorCode, error.errorMessage)
    }

    /**
     * Notifies that an impression event occurred.
     * @param interstitialAd Interstitial ad instance.
     */
    override fun onAdImpression(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Notifies an error encountered while rendering an ad.
     * @param interstitialAd Interstitial ad instance.
     * @param error PubMatic ad error.
     */
    override fun onAdFailedToShow(interstitialAd: POBInterstitial, error: POBError) {
        IronLog.ADAPTER_CALLBACK.error(
            PubMaticConstants.Logs.SHOW_FAILED.format(error.errorCode, error.errorMessage)
        )
        listener.onAdShowFailed(error.errorCode, error.errorMessage)
    }

    /**
     * Notifies an ad click.
     * @param interstitialAd Interstitial ad instance.
     */
    override fun onAdClicked(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Notifies that the interstitial ad has been animated off the screen.
     * @param interstitialAd Interstitial ad instance.
     */
    override fun onAdClosed(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

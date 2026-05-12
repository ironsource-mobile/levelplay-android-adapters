package com.ironsource.adapters.bidmachine.interstitial

import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.utils.BMError

class BidMachineInterstitialListener(
    private val listener: InterstitialAdListener
) : InterstitialListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        val creativeId = interstitialAd.auctionResult?.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(BidMachineConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(BidMachineConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when Ad failed to load
     * @param interstitialAd - Interstitial instance
     * @param error - BMError with additional info about error
     */
    override fun onAdLoadFailed(interstitialAd: InterstitialAd, error: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_LOAD.format(error.code, error.message))
        val errorType = BidMachineAdapter.getLoadErrorType(error)
        listener.onAdLoadFailed(errorType, error.code, error.message)
    }

    /**
     * Called when Ad Impression has been tracked
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdImpression(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when Ad show failed
     * @param interstitialAd - Interstitial instance
     * @param error - BMError with additional info about error
     */
    override fun onAdShowFailed(interstitialAd: InterstitialAd, error: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_SHOW.format(error.code, error.message))
        listener.onAdShowFailed(error.code, error.message)
    }

    /**
     * Called when Ad has been clicked
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdClicked(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when Ad was closed (e.g - user click close button)
     * @param interstitialAd - Interstitial instance
     * @param finished - Value for indicated, if ads was finished
     */
    override fun onAdClosed(interstitialAd: InterstitialAd, finished: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when Ad expired
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdExpired(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

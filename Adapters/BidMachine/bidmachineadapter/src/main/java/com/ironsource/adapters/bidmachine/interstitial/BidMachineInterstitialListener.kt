package com.ironsource.adapters.bidmachine.interstitial

import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.format.interstitialad.InterstitialAd
import io.bidmachine.format.interstitialad.InterstitialListener

class BidMachineInterstitialListener(
    private val listener: InterstitialAdListener
) : AdLoadCallback<InterstitialAd>, InterstitialListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     * @param ad - Interstitial instance
     */
    override fun onAdLoaded(ad: InterstitialAd) {
        val creativeId = (ad as Ad).getAuctionInfo().creativeId
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
     * @param bmError - BMError with additional info about error
     */
    override fun onAdLoadFailed(bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_LOAD.format(bmError.code, bmError.message))
        val errorType = BidMachineAdapter.getLoadErrorType(bmError)
        listener.onAdLoadFailed(errorType, bmError.code, bmError.message)
    }

    override fun onAdPresented(ad: InterstitialAd) {
        // Keep reporting onAdOpened when the impression is tracked.
    }

    /**
     * Called when Ad Impression has been tracked
     * @param ad - Interstitial instance
     */
    override fun onAdImpression(ad: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when Ad show failed
     * @param ad - Interstitial instance
     * @param bmError - BMError with additional info about error
     */
    override fun onAdShowFailed(ad: InterstitialAd, bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_SHOW.format(bmError.code, bmError.message))
        listener.onAdShowFailed(bmError.code, bmError.message)
    }

    /**
     * Called when Ad has been clicked
     * @param ad - Interstitial instance
     */
    override fun onAdClicked(ad: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when Ad was closed (e.g - user click close button)
     * @param ad - Interstitial instance
     */
    override fun onAdClosed(ad: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when Ad expired
     * @param ad - Interstitial instance
     */
    override fun onAdExpired(ad: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

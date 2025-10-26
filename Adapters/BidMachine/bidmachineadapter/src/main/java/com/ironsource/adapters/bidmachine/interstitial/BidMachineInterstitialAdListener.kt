package com.ironsource.adapters.bidmachine.interstitial

import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.bidmachine.interstitial.InterstitialListener
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.utils.BMError
import java.lang.ref.WeakReference

class BidMachineInterstitialAdListener(
    private val mAdapter: WeakReference<BidMachineInterstitialAdapter>,
    private val mListener: InterstitialSmashListener) : InterstitialListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        val creativeId = interstitialAd.auctionResult?.creativeId
        IronLog.ADAPTER_CALLBACK.verbose("creativeId = $creativeId")
        mAdapter.get()?.setInterstitialAd(interstitialAd)
        mAdapter.get()?.setInterstitialAdAvailability(true)
        if (creativeId.isNullOrEmpty()) {
            mListener.onInterstitialAdReady()
        } else {
            val extraData: Map<String, Any> = mapOf(BidMachineAdapter.CREATIVE_ID_KEY to creativeId)
            mListener.onInterstitialAdReady(extraData)
        }
    }

    /**
     * Called when Ad failed to load
     *
     * @param interstitialAd - Interstitial instance
     * @param bmError - BMError with additional info about error
     */
    override fun onAdLoadFailed(interstitialAd: InterstitialAd, bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${bmError.code}, errorMessage = ${bmError.message}")
        mAdapter.get()?.setInterstitialAdAvailability(false)
        mListener.onInterstitialAdLoadFailed(
            BidMachineAdapter.getLoadErrorAndCheckNoFill(
                bmError,
                IronSourceError.ERROR_IS_LOAD_NO_FILL
            )
        )
        mAdapter.get()?.destroyInterstitialAd()
    }

    /**
     * Called when Ad Impression has been tracked
     *
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdImpression(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * Called when Ad show failed
     *
     * @param interstitialAd - Interstitial instance
     * @param bmError - BMError with additional info about error
     */
    override fun onAdShowFailed(interstitialAd: InterstitialAd, bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${bmError.code}, errorMessage = ${bmError.message}")
        val interstitialError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT,
            bmError.message
        )
        mListener.onInterstitialAdShowFailed(interstitialError)
    }

    /**
     * Called when Ad has been clicked
     *
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdClicked(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    /**
     * Called when Ad was closed (e.g - user click close button)
     *
     * @param interstitialAd - Interstitial instance
     * @param finished - Value for indicated, if ads was finished
     */
    override fun onAdClosed(interstitialAd: InterstitialAd, finished: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
        mAdapter.get()?.destroyInterstitialAd()
    }

    /**
     * Called when Ad expired
     *
     * @param interstitialAd - Interstitial instance
     */
    override fun onAdExpired(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

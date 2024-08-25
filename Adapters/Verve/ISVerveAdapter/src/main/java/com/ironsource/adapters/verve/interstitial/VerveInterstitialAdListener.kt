package com.ironsource.adapters.verve.interstitial

import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd

class VerveInterstitialAdListener (
        private val mListener: InterstitialSmashListener,
) : HyBidInterstitialAd.Listener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     */
    override fun onInterstitialLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdReady()
    }

    /**
     * Called when Ad failed to load
     *
     * @param error - Throwable error
     */
    override fun onInterstitialLoadFailed(error: Throwable?) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorMessage = ${error?.message} , errorCause = ${error?.cause}")
        mListener.onInterstitialAdLoadFailed(VerveAdapter.getLoadError(error))
    }


    /**
     * Called when Ad Impression has been tracked
     *
     */
    override fun onInterstitialImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * Called when ad content is dismissed.
     *
     */
    override fun onInterstitialDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()

    }

    /**
     * Called when Ad has been clicked.
     *
     */
    override fun onInterstitialClick() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

}
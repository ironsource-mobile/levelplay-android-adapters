package com.ironsource.adapters.bigo.interstitial

import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.InterstitialAd
import java.lang.ref.WeakReference

class BigoInterstitialAdListener(
    private val mAdapter: WeakReference<BigoInterstitialAdapter>,
    private val mListener: InterstitialSmashListener) : AdInteractionListener, AdLoadListener<InterstitialAd> {

    /**
     * Called when ad request succeeds
     *
     * @param ad - Interstitial ad
     */
    override fun onAdLoaded(ad: InterstitialAd) {
        IronLog.INTERNAL.verbose("onAdLoaded")
        mAdapter.get()?.setInterstitialAd(ad)
        mAdapter.get()?.setInterstitialAdAvailability(true)
        mListener.onInterstitialAdReady()
    }

    /**
     * Called when something wrong during ad loading
     *
     * @param error - bigo ad error
     */
    override fun onError(error: AdError) {
        IronLog.INTERNAL.verbose("onError code: ${error.code}, ${error.message}")
        mListener.onInterstitialAdLoadFailed(BigoAdapter.getLoadError(error))
    }

    /**
     * There's something wrong when using this ad
     *
     * @param error - bigo ad error
     */
    override fun onAdError(error: AdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.message}")
        val interstitialError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT,
            error.message
        )
        mListener.onInterstitialAdShowFailed(interstitialError)
    }


    /**
     * Indicates that the ad has been displayed successfully on the device screen.
     *
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * When the fullscreen ad covers the screen.
     *
     */
    override fun onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Indicates that the ad is clicked by the user.
     *
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    /**
     * When the fullsceen ad closes.
     *
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
        mAdapter.get()?.destroyInterstitialAd()
    }
}

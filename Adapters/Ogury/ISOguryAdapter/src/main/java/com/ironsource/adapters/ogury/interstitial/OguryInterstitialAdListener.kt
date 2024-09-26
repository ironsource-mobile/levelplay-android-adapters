package com.ironsource.adapters.ogury.interstitial

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ogury.core.OguryError
import com.ogury.ed.OguryInterstitialAdListener

class OguryInterstitialAdListener (
    private val mListener: InterstitialSmashListener,
    private val mAdapter: OguryInterstitialAdapter
) : OguryInterstitialAdListener {

    /**
     * The SDK is ready to display the ad provided by the ad server.
     *
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdReady()
    }

    /**
     * The ad failed to load or display.
     *
     */
    override fun onAdError(error: OguryError) {
        when (mAdapter.getAdState()) {
            OguryInterstitialAdapter.AdState.STATE_NONE,
            OguryInterstitialAdapter.AdState.STATE_LOAD -> {
                logAdError("load", error)
                mListener.onInterstitialAdLoadFailed(OguryAdapter.getLoadError(error))
            }
            OguryInterstitialAdapter.AdState.STATE_SHOW -> {
                logAdError("show", error)
                mListener.onInterstitialAdShowFailed(OguryAdapter.getLoadError(error))
            }
        }
    }

    private fun logAdError(context: String, error: OguryError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to $context, errorMessage = ${error.message}, " +
            "errorCause = ${error.cause}, errorCode = ${error.errorCode}")
    }

    /**
     * The ad has been displayed on the screen.
     *
     */
    override fun onAdDisplayed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * The ad has been clicked by the user.
     *
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    /**
     * The ad has been closed by the user.
     *
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
    }

}
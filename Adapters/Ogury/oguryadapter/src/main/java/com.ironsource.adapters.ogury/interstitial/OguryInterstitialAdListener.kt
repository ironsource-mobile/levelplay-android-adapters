package com.ironsource.adapters.ogury.interstitial

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ogury.ad.OguryAdError
import com.ogury.ad.OguryInterstitialAd
import com.ogury.ad.OguryInterstitialAdListener

class OguryInterstitialAdListener (
    private val mListener: InterstitialSmashListener,
) : OguryInterstitialAdListener {

    /**
     * The SDK is ready to display the ad provided by the ad server.
     * @param ad - InterstitialAd instance
     */
    override fun onAdLoaded(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdReady()
    }

    /**
     * The ad failed to load or display.
     * @param ad - InterstitialAd instance
     * @param error - Ogury Ad Error
     */
    override fun onAdError(ad: OguryInterstitialAd, error: OguryAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to ${error.type}, errorMessage = ${error.message}," +
            " errorCode = ${error.code}")
        if(error.type == OguryAdError.Type.LOAD_ERROR) {
            mListener.onInterstitialAdLoadFailed(OguryAdapter.getLoadError(error))
        } else {
            mListener.onInterstitialAdShowFailed(OguryAdapter.getLoadError(error))
        }
    }

    /**
     * Called when Ad Impression has been tracked.
     * @param ad - InterstitialAd instance
     *
     */
    override fun onAdImpression(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * The ad has been clicked by the user.
     * @param ad - InterstitialAd instance
     */
    override fun onAdClicked(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    /**
     * The ad has been closed by the user.
     * @param ad - InterstitialAd instance
     */
    override fun onAdClosed(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
    }
}
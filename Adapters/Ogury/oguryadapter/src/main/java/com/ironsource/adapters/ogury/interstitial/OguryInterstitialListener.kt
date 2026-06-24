package com.ironsource.adapters.ogury.interstitial

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.ogury.ad.OguryAdError
import com.ogury.ad.OguryInterstitialAd
import com.ogury.ad.OguryInterstitialAdListener

class OguryInterstitialListener(
    private val listener: InterstitialAdListener
) : OguryInterstitialAdListener {

    /**
     * Called when the ad is ready to be displayed.
     */
    override fun onAdLoaded(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the ad fails to load or display.
     */
    override fun onAdError(ad: OguryInterstitialAd, error: OguryAdError) {
        if (error.type == OguryAdError.Type.LOAD_ERROR) {
            IronLog.ADAPTER_CALLBACK.error(OguryConstants.Logs.LOAD_FAILED.format(error.code, error.message))
            listener.onAdLoadFailed(
                OguryAdapter.getLoadErrorType(error.code),
                error.code,
                error.message
            )
        } else {
            IronLog.ADAPTER_CALLBACK.error(OguryConstants.Logs.SHOW_FAILED.format(error.code, error.message))
            listener.onAdShowFailed(error.code, error.message)
        }
    }

    /**
     * Called when the ad records an impression.
     */
    override fun onAdImpression(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked.
     */
    override fun onAdClicked(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad is closed.
     */
    override fun onAdClosed(ad: OguryInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

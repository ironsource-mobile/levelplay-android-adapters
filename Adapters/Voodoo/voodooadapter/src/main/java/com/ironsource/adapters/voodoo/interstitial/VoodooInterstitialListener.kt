package com.ironsource.adapters.voodoo.interstitial

import com.ironsource.adapters.voodoo.VoodooAdapter
import com.ironsource.adapters.voodoo.VoodooConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import io.adn.sdk.publisher.AdnAdError
import io.adn.sdk.publisher.AdnAdInfo
import io.adn.sdk.publisher.AdnFullscreenAdListener

class VoodooInterstitialListener(
    private val listener: InterstitialAdListener
) : AdnFullscreenAdListener {

    /**
     * Called when the ad finished loading
     * @param adInfo - the loaded ad info
     */
    override fun onAdLoaded(adInfo: AdnAdInfo) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the ad failed to load
     * @param error - the load error
     */
    override fun onAdLoadFailed(error: AdnAdError) {
        IronLog.ADAPTER_CALLBACK.error(VoodooConstants.Logs.FAILED_TO_LOAD.format(error.errorCode, error.errorMessage))
        listener.onAdLoadFailed(VoodooAdapter.getLoadError(error), error.errorCode, error.errorMessage)
    }

    /**
     * Called when the ad is shown
     * @param adInfo - the shown ad info
     */
    override fun onAdShown(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad failed to show
     * @param adInfo - the ad info
     * @param error - the show error
     */
    override fun onAdShowFailed(adInfo: AdnAdInfo?, error: AdnAdError) {
        IronLog.ADAPTER_CALLBACK.error(VoodooConstants.Logs.FAILED_TO_SHOW.format(error.errorCode, error.errorMessage))
        listener.onAdShowFailed(error.errorCode, error.errorMessage)
    }

    /**
     * Called when the ad recorded an impression
     * @param adInfo - the ad info
     */
    override fun onAdImpression(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked
     * @param adInfo - the ad info
     */
    override fun onAdClicked(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad is closed
     * @param adInfo - the ad info
     */
    override fun onAdClosed(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

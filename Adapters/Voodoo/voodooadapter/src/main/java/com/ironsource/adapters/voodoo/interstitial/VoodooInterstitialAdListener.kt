package com.ironsource.adapters.voodoo.interstitial

import com.ironsource.adapters.voodoo.VoodooAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.adn.sdk.publisher.AdnAdError
import io.adn.sdk.publisher.AdnAdInfo
import io.adn.sdk.publisher.AdnFullscreenAdListener

class VoodooInterstitialAdListener(
    private val placementId: String,
    private val listener: InterstitialSmashListener
) : AdnFullscreenAdListener {

    override fun onAdLoaded(adInfo: AdnAdInfo) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onInterstitialAdReady()
    }

    override fun onAdLoadFailed(error: AdnAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId, errorCode = ${error.errorCode} , errorMessage = ${error.errorMessage}")
        listener.onInterstitialAdLoadFailed(VoodooAdapter.getLoadAdError(error, IronSourceError.ERROR_IS_LOAD_NO_FILL))
    }

    override fun onAdShown(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onInterstitialAdShowSucceeded()
    }

    override fun onAdShowFailed(adInfo: AdnAdInfo?, error: AdnAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId, errorCode = ${error.errorCode} , errorMessage = ${error.errorMessage}")
        val interstitialError: IronSourceError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT, error.errorMessage
        )
        listener.onInterstitialAdShowFailed(interstitialError)
    }

    override fun onAdImpression(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onInterstitialAdOpened()
    }

    override fun onAdClicked(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onInterstitialAdClicked()
    }

    override fun onAdClosed(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onInterstitialAdClosed()
    }
}
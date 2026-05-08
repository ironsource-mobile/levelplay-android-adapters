package com.ironsource.adapters.mobilefuse.interstitial

import com.ironsource.adapters.mobilefuse.MobileFuseConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseInterstitialAd

class MobileFuseInterstitialListener(
    private val listener: InterstitialAdListener
) : MobileFuseInterstitialAd.Listener {

    /**
     * Called when the interstitial ad has loaded successfully
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the interstitial ad failed to fill
     */
    override fun onAdNotFilled() {
        IronLog.ADAPTER_CALLBACK.verbose(MobileFuseConstants.INTERSTITIAL_AD_NOT_FILLED)
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            MobileFuseConstants.INTERSTITIAL_AD_NOT_FILLED
        )
    }

    /**
     * Called when the interstitial ad has been rendered on screen
     */
    override fun onAdRendered() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the interstitial ad is clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the interstitial ad has expired
     */
    override fun onAdExpired() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when an error occurs with the interstitial ad
     */
    override fun onAdError(error: AdError?) {
        val code = error?.errorCode ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
        val message = error?.errorMessage ?: MobileFuseConstants.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(MobileFuseConstants.Logs.AD_LOAD_ERROR.format(code, message))

        // Check if it's a load error or show error based on the error type
        if (error == AdError.AD_ALREADY_LOADED || error == AdError.AD_LOAD_ERROR) {
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                code,
                message
            )
        } else {
            listener.onAdShowFailed(code, message)
        }
    }

    /**
     * Called when the interstitial ad is closed
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

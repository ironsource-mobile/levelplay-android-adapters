package com.ironsource.adapters.inmobi.interstitial

import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.ironsource.adapters.inmobi.InMobiConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog

internal class InMobiInterstitialListener(
    private val listener: InterstitialAdListener
) : InterstitialAdEventListener() {

    /**
     * Called to indicate that an ad was loaded and it can now be shown
     */
    override fun onAdLoadSucceeded(
        inMobiInterstitial: InMobiInterstitial,
        adMetaInfo: AdMetaInfo
    ) {
        val creativeId = adMetaInfo.creativeID
        IronLog.ADAPTER_CALLBACK.verbose(InMobiConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(InMobiConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called to signal that a request to fetch an ad failed
     */
    override fun onAdLoadFailed(
        inMobiInterstitial: InMobiInterstitial,
        inMobiAdRequestStatus: InMobiAdRequestStatus
    ) {
        val errorMessage = inMobiAdRequestStatus.message ?: inMobiAdRequestStatus.statusCode.toString()

        val adapterErrorType = if (inMobiAdRequestStatus.statusCode == InMobiAdRequestStatus.StatusCode.NO_FILL) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }

        IronLog.ADAPTER_CALLBACK.error(InMobiConstants.Logs.FAILED_TO_LOAD.format(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage))
        listener.onAdLoadFailed(adapterErrorType, AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
    }

    /**
     * Called to indicate that the ad will be launching a fullscreen overlay
     */
    override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called to indicate that the fullscreen overlay is now the topmost screen
     */
    override fun onAdDisplayed(
        inMobiInterstitial: InMobiInterstitial,
        adMetaInfo: AdMetaInfo
    ) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called to indicate that an impression was registered
     */
    override fun onAdImpression(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called to indicate that a request to show an ad failed
     */
    override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, InMobiConstants.Logs.AD_DISPLAY_FAILED)
    }

    /**
     * Called to indicate that an ad interaction was observed (click)
     */
    override fun onAdClicked(
        inMobiInterstitial: InMobiInterstitial,
        params: Map<Any?, Any?>?
    ) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called to indicate that the user may leave the application
     */
    override fun onUserLeftApplication(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called to indicate that rewards have been unlocked
     */
    override fun onRewardsUnlocked(
        inMobiInterstitial: InMobiInterstitial,
        rewards: Map<Any?, Any?>?
    ) {
        // Not used for interstitials
    }

    /**
     * Called to indicate that the fullscreen overlay was closed
     */
    override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

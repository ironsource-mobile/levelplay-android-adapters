package com.ironsource.adapters.voodoo.rewardedvideo

import com.ironsource.adapters.voodoo.VoodooAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.adn.sdk.publisher.AdnAdError
import io.adn.sdk.publisher.AdnAdInfo
import io.adn.sdk.publisher.AdnFullscreenAdListener

class VoodooRewardedVideoAdListener(
    private val placementId: String,
    private val listener: RewardedVideoSmashListener
) : AdnFullscreenAdListener {

    override fun onAdLoaded(adInfo: AdnAdInfo) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onRewardedVideoAvailabilityChanged(true)
    }

    override fun onAdLoadFailed(error: AdnAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load placementId = $placementId, errorCode = ${error.errorCode} , errorMessage = ${error.errorMessage}")
        listener.onRewardedVideoAvailabilityChanged(false)
        listener.onRewardedVideoLoadFailed(VoodooAdapter.getLoadAdError(error, IronSourceError.ERROR_RV_LOAD_NO_FILL))
    }

    override fun onAdShown(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onRewardedVideoAdStarted()
    }

    override fun onAdShowFailed(adInfo: AdnAdInfo?, error: AdnAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId, errorCode = ${error.errorCode} , errorMessage = ${error.errorMessage}")
        val rewardedVideoError: IronSourceError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT, error.errorMessage
        )
        listener.onRewardedVideoAdShowFailed(rewardedVideoError)
    }

    override fun onAdImpression(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onRewardedVideoAdOpened()
    }

    override fun onAdClicked(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onRewardedVideoAdClicked()
    }

    override fun onAdRewarded(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onRewardedVideoAdRewarded()
    }

    override fun onAdClosed(adInfo: AdnAdInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        listener.onRewardedVideoAdEnded()
        listener.onRewardedVideoAdClosed()
    }
}
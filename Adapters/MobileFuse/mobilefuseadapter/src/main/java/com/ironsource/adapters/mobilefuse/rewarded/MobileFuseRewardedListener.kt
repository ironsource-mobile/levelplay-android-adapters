package com.ironsource.adapters.mobilefuse.rewarded

import com.ironsource.adapters.mobilefuse.MobileFuseConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseRewardedAd

class MobileFuseRewardedListener(
    private val listener: RewardedVideoAdListener
) : MobileFuseRewardedAd.Listener {

    /**
     * Called when the rewarded video ad has loaded successfully
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the rewarded video ad failed to fill
     */
    override fun onAdNotFilled() {
        IronLog.ADAPTER_CALLBACK.verbose(MobileFuseConstants.REWARDED_AD_NOT_FILLED)
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            MobileFuseConstants.REWARDED_AD_NOT_FILLED
        )
    }

    /**
     * Called when the rewarded video ad has been rendered on screen
     */
    override fun onAdRendered() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the rewarded video ad is clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the rewarded video ad has expired
     */
    override fun onAdExpired() {
        IronLog.ADAPTER_CALLBACK.verbose(MobileFuseConstants.AD_EXPIRED)
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_AD_EXPIRED,
            AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
            MobileFuseConstants.AD_EXPIRED
        )
    }

    /**
     * Called when an error occurs with the rewarded video ad
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
     * Called when the rewarded video ad is closed
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the user has earned a reward
     */
    override fun onUserEarnedReward() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }
}

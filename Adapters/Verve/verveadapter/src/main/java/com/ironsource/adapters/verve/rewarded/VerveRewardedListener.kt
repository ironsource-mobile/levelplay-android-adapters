package com.ironsource.adapters.verve.rewarded

import com.ironsource.adapters.verve.VerveConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import net.pubnative.lite.sdk.HyBidError
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd

class VerveRewardedListener(
    private val listener: RewardedVideoAdListener
) : HyBidRewardedAd.Listener {

    /**
     * Called when Ad was loaded and ready to be displayed
     */
    override fun onRewardedLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called when Ad failed to load
     *
     * @param error - Throwable error
     */
    override fun onRewardedLoadFailed(error: Throwable?) {
        val hybidError = error as? HyBidError
        val errorCode = hybidError?.errorCode?.code ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
        val errorMessage = hybidError?.errorCode?.message ?: error?.message ?: VerveConstants.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(VerveConstants.Logs.FAILED_TO_LOAD.format(errorCode, errorMessage))
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            errorCode,
            errorMessage
        )
    }

    /**
     * Called when Ad has been opened
     */
    override fun onRewardedOpened() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when ad content is dismissed
     */
    override fun onRewardedClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when Ad has been clicked
     */
    override fun onRewardedClick() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when a user is rewarded
     */
    override fun onReward() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }
}

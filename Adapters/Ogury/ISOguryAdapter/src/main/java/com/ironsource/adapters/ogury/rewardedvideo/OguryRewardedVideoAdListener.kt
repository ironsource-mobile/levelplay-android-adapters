package com.ironsource.adapters.ogury.rewardedvideo

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ogury.core.OguryError
import com.ogury.ed.OguryOptinVideoAdListener
import com.ogury.ed.OguryReward

class OguryRewardedVideoAdListener(
    private val mListener: RewardedVideoSmashListener,
    private val mAdapter: OguryRewardedVideoAdapter
)
: OguryOptinVideoAdListener {

    /**
     * The SDK is ready to display the ad provided by the ad server.
     *
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * The ad failed to load or display.
     *
     */
    override fun onAdError(error: OguryError) {
        when (mAdapter.getAdState()) {
            OguryRewardedVideoAdapter.AdState.STATE_NONE,
            OguryRewardedVideoAdapter.AdState.STATE_LOAD -> {
                logAdError("load", error)
                mListener.onRewardedVideoAvailabilityChanged(false)
                mListener.onRewardedVideoLoadFailed(OguryAdapter.getLoadError(error))
            }
            OguryRewardedVideoAdapter.AdState.STATE_SHOW -> {
                logAdError("show", error)
                mListener.onRewardedVideoAdShowFailed(OguryAdapter.getLoadError(error))
            }
        }
    }

    private fun logAdError(context: String, error: OguryError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to $context, errorMessage = ${error.message}," +
            " errorCause = ${error.cause}, errorCode = ${error.errorCode}")
    }

    /**
     * The ad has been displayed on the screen.
     *
     */
    override fun onAdDisplayed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * The user must be rewarded, as they has watched the Opt-in Video Ad.
     *
     */
    override fun onAdRewarded(reward: OguryReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdRewarded()
    }

    /**
     * The ad has been clicked by the user.
     *
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * The ad has been closed by the user.
     *
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClosed()
    }

}

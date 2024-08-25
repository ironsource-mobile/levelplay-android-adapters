package com.ironsource.adapters.verve.rewardedvideo

import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd

class VerveRewardedVideoAdListener (
    private val mListener: RewardedVideoSmashListener,
) : HyBidRewardedAd.Listener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     */
    override fun onRewardedLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Called when Ad failed to load
     *
     * @param error - Throwable error
     */
    override fun onRewardedLoadFailed(error: Throwable?) {
        // ask about the null check
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorMessage = ${error?.message}")
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(VerveAdapter.getLoadError(error))
    }

    /**
     * Called when Ad has been opened.
     *
     */
    override fun onRewardedOpened() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * Called when a user is rewarded.
     *
     */
    override fun onRewardedClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClosed()
    }

    /**
     * Called when Ad has been clicked.
     *
     */
    override fun onRewardedClick() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * Called when a user is rewarded.
     *
     */
    override fun onReward() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdRewarded()
    }

}
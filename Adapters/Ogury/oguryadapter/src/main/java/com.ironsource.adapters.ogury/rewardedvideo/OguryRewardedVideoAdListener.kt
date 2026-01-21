package com.ironsource.adapters.ogury.rewardedvideo

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ogury.ad.OguryAdError
import com.ogury.ad.OguryReward
import com.ogury.ad.OguryRewardedAd
import com.ogury.ad.OguryRewardedAdListener

class OguryRewardedVideoAdListener(
    private val mListener: RewardedVideoSmashListener
)
: OguryRewardedAdListener {

    /**
     * The SDK is ready to display the ad provided by the ad server.
     * @param ad - RewardedVideoAd instance
     */
    override fun onAdLoaded(ad: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * The ad failed to load or display.
     * @param ad - RewardedVideoAd instance
     * @param error - Ogury Ad Error
     */
    override fun onAdError(ad: OguryRewardedAd, error: OguryAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to ${error.type}, errorMessage = ${error.message}," +
            " errorCode = ${error.code}")
        if(error.type == OguryAdError.Type.LOAD_ERROR) {
            mListener.onRewardedVideoAvailabilityChanged(false)
            mListener.onRewardedVideoLoadFailed(OguryAdapter.getLoadError(error))
        } else {
            mListener.onRewardedVideoAdShowFailed(OguryAdapter.getLoadError(error))
        }
    }

    /**
     * Called when Ad Impression has been tracked.
     * @param ad - RewardedVideoAd instance
     */
    override fun onAdImpression(ad: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * The user must be rewarded, as they has watched the Opt-in Video Ad.
     * @param ad - RewardedVideoAd instance
     * @param reward - Ogury Reward
     */
    override fun onAdRewarded(ad: OguryRewardedAd, reward: OguryReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdRewarded()
    }

    /**
     * The ad has been clicked by the user.
     * @param ad - RewardedVideoAd instance
     */
    override fun onAdClicked(ad: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * The ad has been closed by the user.
     * @param ad - RewardedVideoAd instance
     */
    override fun onAdClosed(ad: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClosed()
    }
}

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
     *
     */
    override fun onAdLoaded(rewardedAd: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * The ad failed to load or display.
     *
     */
    override fun onAdError(rewardedAd: OguryRewardedAd, error: OguryAdError) {
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
     * @param rewardedAd - RewardedVideoAd instance
     *
     */
    override fun onAdImpression(rewardedAd: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * The user must be rewarded, as they has watched the Opt-in Video Ad.
     *
     */
    override fun onAdRewarded(rewardedAd: OguryRewardedAd, reward: OguryReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdRewarded()
    }

    /**
     * The ad has been clicked by the user.
     *
     */
    override fun onAdClicked(rewardedAd: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * The ad has been closed by the user.
     *
     */
    override fun onAdClosed(rewardedAd: OguryRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClosed()
    }
}

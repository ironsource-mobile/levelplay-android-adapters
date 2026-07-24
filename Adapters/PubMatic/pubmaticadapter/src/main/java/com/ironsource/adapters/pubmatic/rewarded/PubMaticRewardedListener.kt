package com.ironsource.adapters.pubmatic.rewarded

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.POBReward
import com.pubmatic.sdk.rewardedad.POBRewardedAd

class PubMaticRewardedListener(
    private val listener: RewardedVideoAdListener
) : POBRewardedAd.POBRewardedAdListener() {

    /**
     * Notifies that an ad has been received successfully.
     * @param rewardedAd Rewarded ad instance.
     */
    override fun onAdReceived(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Notifies an error encountered while loading an ad.
     * @param rewardedAd Rewarded ad instance.
     * @param error PubMatic ad error.
     */
    override fun onAdFailedToLoad(rewardedAd: POBRewardedAd, error: POBError) {
        IronLog.ADAPTER_CALLBACK.error(
            PubMaticConstants.Logs.LOAD_FAILED.format(error.errorCode, error.errorMessage)
        )
        listener.onAdLoadFailed(PubMaticAdapter.getLoadError(error), error.errorCode, error.errorMessage)
    }

    /**
     * Notifies that an impression event occurred.
     * @param rewardedAd Rewarded ad instance.
     */
    override fun onAdImpression(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Notifies an error encountered while rendering an ad.
     * @param rewardedAd Rewarded ad instance.
     * @param error PubMatic ad error.
     */
    override fun onAdFailedToShow(rewardedAd: POBRewardedAd, error: POBError) {
        IronLog.ADAPTER_CALLBACK.error(
            PubMaticConstants.Logs.SHOW_FAILED.format(error.errorCode, error.errorMessage)
        )
        listener.onAdShowFailed(error.errorCode, error.errorMessage)
    }

    /**
     * Notifies an ad click.
     * @param rewardedAd Rewarded ad instance.
     */
    override fun onAdClicked(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Notifies about a reward received by the user.
     * @param rewardedAd Rewarded ad instance.
     * @param reward The reward granted to the user.
     */
    override fun onReceiveReward(rewardedAd: POBRewardedAd, reward: POBReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Notifies that the rewarded ad has been animated off the screen.
     * @param rewardedAd Rewarded ad instance.
     */
    override fun onAdClosed(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

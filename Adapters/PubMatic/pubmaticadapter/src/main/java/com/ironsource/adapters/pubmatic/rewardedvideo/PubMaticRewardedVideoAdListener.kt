package com.ironsource.adapters.pubmatic.rewardedvideo

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.POBReward
import com.pubmatic.sdk.rewardedad.POBRewardedAd

class PubMaticRewardedVideoAdListener(
    private val mListener: RewardedVideoSmashListener,
    private val mAdUnitId: String,
    ) : POBRewardedAd.POBRewardedAdListener() {

    /**
     * Callback method notifies that an ad has been received successfully.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAdReceived(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Callback method notifies an error encountered while loading an ad.
     * @param rewardedAd - RewardedVideo ad instance.
     * @param error - PubMatic Ad Error.
     */
    override fun onAdFailedToLoad(rewardedAd: POBRewardedAd, error: POBError) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId, errorCode = ${error.errorCode}, errorMessage = ${error.errorMessage}")
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(
            PubMaticAdapter.getLoadErrorAndCheckNoFill(
                error,
                IronSourceError.ERROR_RV_LOAD_NO_FILL
            )
        )    }

    /**
     * Callback method Notifies that an impression event occurred.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAdImpression(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * Callback method notifies an error encountered while rendering an ad.
     * @param rewardedAd - RewardedVideo ad instance.
     * @param error - PubMatic Ad Error.
     */
    override fun onAdFailedToShow(rewardedAd: POBRewardedAd, error: POBError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId, errorMessage = $error.errorMessage, errorCode = $error.errorCode")
        mListener.onRewardedVideoAdShowFailed(IronSourceError(error.errorCode, error.errorMessage))
    }

    /**
     * Callback method notifies ad click
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAdClicked(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * Callback method notifies that a user interaction will open another app (e.g. Play store), leaving the current app.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAppLeaving(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
    }

    /**
     * Callback method notifies that the rewarded ad will be presented as a modal on top of the current view.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAdOpened(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
    }

    /**
     * Callback method notifies about rewards received.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onReceiveReward(rewardedAd: POBRewardedAd, error: POBReward) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
        mListener.onRewardedVideoAdRewarded()
    }

    /**
     * Callback method notifies that the rewarded ad has been animated off the screen.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAdClosed(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdClosed()
    }

    /**
     * Callback method notifies that the rewarded ad has been expired.
     * @param rewardedAd - RewardedVideo ad instance.
     */
    override fun onAdExpired(rewardedAd: POBRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("madUnitId = $mAdUnitId")
        mListener.onRewardedVideoLoadFailed(
            IronSourceError(
            IronSourceError.ERROR_RV_EXPIRED_ADS,
            "ads are expired")
        )
    }
}

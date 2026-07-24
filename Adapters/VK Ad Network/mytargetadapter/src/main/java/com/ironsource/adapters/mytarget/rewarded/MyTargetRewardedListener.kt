package com.ironsource.adapters.mytarget.rewarded

import com.ironsource.adapters.mytarget.MyTargetConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.my.target.ads.Reward
import com.my.target.ads.RewardedAd
import com.my.target.common.models.IAdLoadingError
import java.lang.ref.WeakReference

class MyTargetRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<MyTargetRewardedAdapter>
) : RewardedAd.RewardedAdListener {

    /**
     * Called when the ad assets have been loaded and the ad is ready to be shown.
     * @param rewardedAd - the ad instance that was loaded.
     */
    override fun onLoad(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setRewardedAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the ad failed to load.
     * @param iAdLoadingError - the error details suggesting the cause of failure.
     * @param rewardedAd - the ad instance that failed to load.
     */
    override fun onNoAd(iAdLoadingError: IAdLoadingError, rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.error(MyTargetConstants.Logs.FAILED_TO_LOAD.format(iAdLoadingError.code, iAdLoadingError.message))
        adapter.get()?.setRewardedAdAvailability(false)
        listener.onAdLoadFailed(AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL, iAdLoadingError.code, iAdLoadingError.message)
    }

    /**
     * Called when the ad is displayed (impression).
     * @param rewardedAd - the ad instance that was displayed.
     */
    override fun onDisplay(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad failed to show.
     * @param rewardedAd - the ad instance that failed to show.
     */
    override fun onFailedToShow(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.error(MyTargetConstants.Logs.AD_SHOW_FAILED)
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, MyTargetConstants.Logs.AD_SHOW_FAILED)
    }

    /**
     * Called when the user clicks on the ad.
     * @param rewardedAd - the ad instance that was clicked.
     */
    override fun onClick(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad is dismissed and the user returns to the app.
     * @param rewardedAd - the ad instance that was dismissed.
     */
    override fun onDismiss(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the user has earned a reward.
     * @param reward - the reward details.
     * @param rewardedAd - the ad instance that granted the reward.
     */
    override fun onReward(reward: Reward, rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }
}

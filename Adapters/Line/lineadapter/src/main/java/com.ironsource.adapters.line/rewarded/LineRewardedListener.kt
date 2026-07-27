package com.ironsource.adapters.line.rewarded

import com.five_corp.ad.AdLoader
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdVideoReward
import com.five_corp.ad.FiveAdVideoRewardEventListener
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.adapters.line.LineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class LineRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<LineRewardedAdapter>
) : FiveAdVideoRewardEventListener, AdLoader.LoadRewardAdCallback {

    /**
     * Called when the ad was loaded and is ready to be displayed
     * @param rewardedAd - Ad instance
     */
    override fun onLoad(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setRewardedAd(rewardedAd)
        adapter.get()?.setRewardedAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the ad failed to load
     * @param errorCode - The load error code
     */
    override fun onError(errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.error(LineConstants.Logs.FAILED_TO_LOAD.format(errorCode.value, errorCode.name))
        adapter.get()?.setRewardedAdAvailability(false)
        listener.onAdLoadFailed(LineAdapter.getLoadErrorType(errorCode), errorCode.value, errorCode.name)
    }

    /**
     * Called when the ad failed to show
     * @param rewardedAd - Ad instance
     * @param errorCode - The show error code
     */
    override fun onViewError(rewardedAd: FiveAdVideoReward, errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.error(LineConstants.Logs.FAILED_TO_SHOW.format(errorCode.value, errorCode.name))
        listener.onAdShowFailed(errorCode.value, errorCode.name)
    }

    /**
     * Called when the ad impression is tracked
     * @param rewardedAd - Ad instance
     */
    override fun onImpression(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked
     * @param rewardedAd - Ad instance
     */
    override fun onClick(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user earns a reward
     * @param rewardedAd - Ad instance
     */
    override fun onReward(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the ad is closed
     * @param rewardedAd - Ad instance
     */
    override fun onFullScreenClose(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the ad opens a fullscreen overlay
     * @param rewardedAd - Ad instance
     */
    override fun onFullScreenOpen(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad playback starts
     * @param rewardedAd - Ad instance
     */
    override fun onPlay(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad playback pauses
     * @param rewardedAd - Ad instance
     */
    override fun onPause(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad playback reaches the view-through point
     * @param rewardedAd - Ad instance
     */
    override fun onViewThrough(rewardedAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

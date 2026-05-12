package com.ironsource.adapters.pangle.rewarded

import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener
import com.ironsource.adapters.pangle.PangleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class PangleRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<PangleRewardedAdapter>
) : PAGRewardedAdLoadListener, PAGRewardedAdInteractionListener {

    /**
     * Called when an ad material is loaded successfully
     * @param rewardedAd - Rewarded ad instance
     */
    override fun onAdLoaded(rewardedAd: PAGRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setRewardedAd(rewardedAd)
        adapter.get()?.setRewardedAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when an ad fails to load
     * @param code - Error code
     * @param message - Error message
     */
    override fun onError(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.FAILED_TO_LOAD.format(code, message))
        adapter.get()?.setRewardedAdAvailability(false)
        val errorType = if (code == PangleConstants.PANGLE_NO_FILL_ERROR_CODE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }
        listener.onAdLoadFailed(errorType, code, message)
    }

    /**
     * Called when the ad is displayed
     */
    override fun onAdShowed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked by the user
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user should be rewarded
     * @param rewardedAd - Reward item
     */
    override fun onUserEarnedReward(rewardedAd: PAGRewardItem) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the user rewarded failed
     * @param code - Error code
     * @param message - Error message
     */
    override fun onUserEarnedRewardFail(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.FAILED_TO_REWARD.format(code, message))
    }

    /**
     * Called when the ad disappears
     */
    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

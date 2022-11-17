package com.ironsource.adapters.pangle

import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import java.lang.ref.WeakReference

class PangleRewardedVideoAdListener(private val mListener: RewardedVideoSmashListener?,
                                    private val mAdapter: WeakReference<PangleAdapter>?,
                                    private val mSlotId: String) : PAGRewardedAdLoadListener, PAGRewardedAdInteractionListener {

    // PAGRewardedAdLoadListener

    //This method is executed when an ad material is loaded successfully.
    override fun onAdLoaded(rewardedAd: PAGRewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mAdapter?.get()?.setRewardedVideoAd(mSlotId, rewardedAd)
        mAdapter?.get()?.setRewardedVideoAdAvailability(mSlotId, true)
        mListener?.onRewardedVideoAvailabilityChanged(true)
    }

    //This method is invoked when an ad fails to load. It includes an error parameter of type Error that indicates what type of failure occurred.
    override fun onError(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load slotId = $mSlotId, error code = $code, message = $message")
        mAdapter?.get()?.setRewardedVideoAdAvailability(mSlotId, false)
        mListener?.onRewardedVideoAvailabilityChanged(false)
        val errorCode = if (code == PangleAdapter.PANGLE_NO_FILL_ERROR_CODE) IronSourceError.ERROR_RV_LOAD_NO_FILL else code
        mListener?.onRewardedVideoLoadFailed(IronSourceError(errorCode, message))
    }

    // PAGRewardedAdInteractionListener

    //This method is invoked when the ad is displayed, covering the device's screen.
    override fun onAdShowed() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onRewardedVideoAdOpened()
        mListener?.onRewardedVideoAdStarted()
    }

    //This method is invoked when the ad is clicked by the user.
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onRewardedVideoAdClicked()
    }

    //The method is invoked when the user should be rewarded.
    override fun onUserEarnedReward(rewardedAd: PAGRewardItem) {
        IronLog.ADAPTER_CALLBACK.verbose("onUserEarnedReward - slotId = $mSlotId, reward amount = ${rewardedAd.rewardAmount}, reward name = ${rewardedAd.rewardName}")
        mListener?.onRewardedVideoAdRewarded()
    }

    //The method is invoked when the user rewarded failed.
    override fun onUserEarnedRewardFail(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to reward slotId = $mSlotId, error code = $code, message = $message")
    }

    //This method is invoked when the ad disappears.
    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onRewardedVideoAdEnded()
        mListener?.onRewardedVideoAdClosed()
    }
}
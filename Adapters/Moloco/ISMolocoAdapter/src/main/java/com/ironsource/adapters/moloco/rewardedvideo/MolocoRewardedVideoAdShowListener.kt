package com.ironsource.adapters.moloco.rewardedvideo

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.RewardedInterstitialAdShowListener
import java.lang.ref.WeakReference

class MolocoRewardedVideoAdShowListener (
    private val mListener: RewardedVideoSmashListener,
    private val mAdapter: WeakReference<MolocoRewardedVideoAdapter>
) : RewardedInterstitialAdShowListener {
    private var hasEarnedReward: Boolean = false

    /**
     * Called when an ad starts displaying. Impression can be recorded.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdShowSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
    }

    /**
     * Called when a rewarded video starts.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onRewardedVideoStarted(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * Called when Ad show failed
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdShowFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_SHOW_ERROR
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${errorCode}, errorMessage = ${molocoAdError.description}")
        val rewardedVideoError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
            molocoAdError.description
        )
        mListener.onRewardedVideoAdShowFailed(rewardedVideoError)
    }

    /**
     * Called when Ad has been clicked
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdClicked(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * Called when a user is rewarded.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onUserRewarded(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        hasEarnedReward = true
    }

    /**
     * Called when a rewarded video is completed.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onRewardedVideoCompleted(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
    }

    /**
     * Called when an ad is hidden.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdHidden(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        if (hasEarnedReward){
            mListener.onRewardedVideoAdRewarded()
            hasEarnedReward = false
        }
        mListener.onRewardedVideoAdClosed()
        mAdapter.get()?.destroyRewardedVideoAd()
    }
}
package com.ironsource.adapters.line.rewardedvideo

import com.five_corp.ad.AdLoader
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdVideoReward
import com.five_corp.ad.FiveAdVideoRewardEventListener
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import java.lang.ref.WeakReference

class LineRewardedVideoAdListener(
    private val mListener: RewardedVideoSmashListener,
    private val mAdapter: WeakReference<LineRewardedVideoAdapter>,
)
: FiveAdVideoRewardEventListener, AdLoader.LoadRewardAdCallback {

    override fun onLoad(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setRewardedVideoAd(rewardedVideoAd)
        mAdapter.get()?.setRewardedVideoAdAvailability(true)
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    override fun onError(errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode: ${errorCode.name}")
        mAdapter.get()?.setRewardedVideoAdAvailability(false)
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(
            LineAdapter.getLoadError(errorCode)
        )
        mAdapter.get()?.destroyRewardedVideoAd()
    }

    override fun onViewError(rewardedVideoAd: FiveAdVideoReward, errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, error = ${errorCode.name}")
        mListener.onRewardedVideoAdShowFailed(
            ErrorBuilder.buildShowFailedError(
                IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
                errorCode.name
            )
        )
    }

    override fun onImpression(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    override fun onClick(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    override fun onReward(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdRewarded()
    }

    override fun onFullScreenOpen(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onFullScreenClose(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClosed()
        mAdapter.get()?.destroyRewardedVideoAd()
    }

    override fun onPlay(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onPause(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onViewThrough(rewardedVideoAd: FiveAdVideoReward) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

}

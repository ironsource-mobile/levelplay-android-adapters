package com.ironsource.adapters.unityads.rewarded

import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.RewardedShowListener
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental

@OptIn(UnityAdsExperimental::class)
class UnityAdsRewardedShowListener(
    private val listener: RewardedVideoAdListener
) : RewardedShowListener {

    override fun onStarted(unityAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    override fun onFailed(unityAd: RewardedAd, error: UnityAdsError) {
        IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.FAILED_TO_SHOW.format(error.code, error.message.orEmpty()))
        listener.onAdShowFailed(error.code, error.message.orEmpty())
    }

    override fun onClicked(unityAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    override fun onRewarded(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    override fun onCompleted(unityAd: RewardedAd, state: ShowFinishState) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

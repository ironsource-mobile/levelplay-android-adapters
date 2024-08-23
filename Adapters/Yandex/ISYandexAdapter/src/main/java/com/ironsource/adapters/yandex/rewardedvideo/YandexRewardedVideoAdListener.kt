package com.ironsource.adapters.yandex.rewardedvideo

import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import java.lang.ref.WeakReference

class YandexRewardedVideoAdListener (
    private val mListener: RewardedVideoSmashListener,
    private val mAdapter: WeakReference<YandexRewardedVideoAdapter>,
) : RewardedAdLoadListener, RewardedAdEventListener {

    override fun onAdLoaded(rewarded: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setRewardedVideoAd(rewarded)
        mAdapter.get()?.setRewardedVideoAdAvailability(true)
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    override fun onAdFailedToLoad(error: AdRequestError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.description}")
        mAdapter.get()?.setRewardedVideoAdAvailability(false)
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(
            YandexAdapter.getLoadErrorAndCheckNoFill(
                error,
                IronSourceError.ERROR_RV_LOAD_NO_FILL
            )
        )
        mAdapter.get()?.destroyRewardedVideoAd()
    }

    override fun onAdShown() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }


    override fun onAdImpression(impressionData: ImpressionData?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    override fun onAdFailedToShow(adError: AdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorMessage = ${adError.description}")
        val rewardedVideoError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
            adError.description
        )
        mListener.onRewardedVideoAdShowFailed(rewardedVideoError)
    }

    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()

    }

    override fun onRewarded(reward: Reward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdRewarded()
    }

    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClosed()
        mAdapter.get()?.destroyRewardedVideoAd()
    }

}

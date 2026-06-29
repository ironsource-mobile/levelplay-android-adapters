package com.ironsource.adapters.smaato.rewarded

import com.ironsource.adapters.smaato.SmaatoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.smaato.sdk.rewarded.EventListener
import com.smaato.sdk.rewarded.RewardedError
import com.smaato.sdk.rewarded.RewardedInterstitialAd
import com.smaato.sdk.rewarded.RewardedRequestError
import java.lang.ref.WeakReference

class SmaatoRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<SmaatoRewardedAdapter>
) : EventListener {

    /**
     * Called when the rewarded ad finished loading.
     */
    override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
        val creativeId = rewardedInterstitialAd.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(SmaatoConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        adapter.get()?.setRewardedAd(rewardedInterstitialAd)

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(SmaatoConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when the rewarded ad failed to load.
     */
    override fun onAdFailedToLoad(rewardedRequestError: RewardedRequestError) {
        val rewardedError = rewardedRequestError.rewardedError
        IronLog.ADAPTER_CALLBACK.error(SmaatoConstants.Logs.FAILED_TO_LOAD.format(rewardedError.toString()))

        val errorType = if (rewardedError == RewardedError.NO_AD_AVAILABLE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }
        listener.onAdLoadFailed(errorType, rewardedError.ordinal, rewardedError.toString())
    }

    /**
     * Called when the rewarded ad failed to show.
     */
    override fun onAdError(rewardedInterstitialAd: RewardedInterstitialAd, rewardedError: RewardedError) {
        IronLog.ADAPTER_CALLBACK.error(SmaatoConstants.Logs.FAILED_TO_SHOW.format(rewardedError.toString()))
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, rewardedError.toString())
    }

    /**
     * Called when the rewarded ad is shown.
     */
    override fun onAdStarted(rewardedInterstitialAd: RewardedInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the rewarded ad is clicked.
     */
    override fun onAdClicked(rewardedInterstitialAd: RewardedInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user earns a reward.
     */
    override fun onAdReward(rewardedInterstitialAd: RewardedInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the rewarded ad is dismissed.
     */
    override fun onAdClosed(rewardedInterstitialAd: RewardedInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the cached rewarded ad has expired.
     */
    override fun onAdTTLExpired(rewardedInterstitialAd: RewardedInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

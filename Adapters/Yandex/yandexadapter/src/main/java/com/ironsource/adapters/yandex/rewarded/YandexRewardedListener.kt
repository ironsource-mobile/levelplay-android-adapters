package com.ironsource.adapters.yandex.rewarded

import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.adapters.yandex.YandexConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import java.lang.ref.WeakReference

class YandexRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<YandexRewardedAdapter>
) : RewardedAdLoadListener, RewardedAdEventListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     * @param rewarded - Ad instance
     */
    override fun onAdLoaded(rewarded: RewardedAd) {
        adapter.get()?.setRewardedAd(rewarded)
        adapter.get()?.setRewardedAdAvailability(true)

        // Extract creative IDs and pass as extra data if available
        val creativeId = rewarded.info?.creatives
            ?.map { it.creativeId }
            ?.let { YandexAdapter.buildCreativeIdString(it) }
            ?: ""
        IronLog.ADAPTER_CALLBACK.verbose(YandexConstants.Logs.CREATIVE_ID.format(creativeId))

        if (creativeId.isEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(YandexConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when Ad failed to load
     * @param error - The error details
     */
    override fun onAdFailedToLoad(error: AdRequestError) {
        IronLog.ADAPTER_CALLBACK.error(YandexConstants.Logs.FAILED_TO_LOAD.format(error.code, error.description))
        adapter.get()?.setRewardedAdAvailability(false)
        listener.onAdLoadFailed(YandexAdapter.getLoadError(error), error.code, error.description)
        adapter.get()?.destroyRewardedAd()
    }

    /**
     * Called when Ad was shown
     */
    override fun onAdShown() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when Ad impression is tracked
     * @param impressionData - Impression data
     */
    override fun onAdImpression(impressionData: ImpressionData?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
        listener.onAdStarted()
    }

    /**
     * Called when Ad failed to show
     * @param adError - The error details
     */
    override fun onAdFailedToShow(adError: AdError) {
        IronLog.ADAPTER_CALLBACK.error(YandexConstants.Logs.FAILED_TO_SHOW.format(adError.description))
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, adError.description)
    }

    /**
     * Called when Ad is clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when user receives a reward
     * @param reward - Reward details
     */
    override fun onRewarded(reward: Reward) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdEnded()
        listener.onAdRewarded()
    }

    /**
     * Called when Ad is dismissed
     */
    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
        adapter.get()?.destroyRewardedAd()
    }
}

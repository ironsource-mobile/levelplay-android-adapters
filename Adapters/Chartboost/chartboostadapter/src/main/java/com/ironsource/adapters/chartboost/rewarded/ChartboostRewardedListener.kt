package com.ironsource.adapters.chartboost.rewarded

import com.chartboost.sdk.callbacks.RewardedCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.DismissEvent
import com.chartboost.sdk.events.ExpirationEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.RewardEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.ironsource.adapters.chartboost.ChartboostConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog

class ChartboostRewardedListener(
    private val listener: RewardedVideoAdListener
) : RewardedCallback {

    /**
     * Called when the ad finished caching or failed to cache.
     */
    override fun onAdLoaded(event: CacheEvent, error: CacheError?) {
        val creativeId = event.adID
        IronLog.ADAPTER_CALLBACK.verbose(ChartboostConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (error != null) {
            IronLog.ADAPTER_CALLBACK.error(ChartboostConstants.Logs.CACHE_ERROR.format(error.toString()))
            val errorType = if (error.code == CacheError.Code.NO_AD_FOUND) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
            listener.onAdLoadFailed(errorType, error.code.errorCode, error.toString())
            return
        }

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(ChartboostConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when the ad is about to be shown.
     */
    override fun onAdRequestedToShow(event: ShowEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad is shown or failed to show.
     */
    override fun onAdShown(event: ShowEvent, error: ShowError?) {
        IronLog.ADAPTER_CALLBACK.verbose()

        if (error != null) {
            IronLog.ADAPTER_CALLBACK.error(ChartboostConstants.Logs.SHOW_ERROR.format(error.toString()))
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, error.toString())
        }
    }

    /**
     * Called when the ad records an impression.
     */
    override fun onImpressionRecorded(event: ImpressionEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked.
     */
    override fun onAdClicked(event: ClickEvent, error: ClickError?) {
        IronLog.ADAPTER_CALLBACK.verbose()

        if (error != null) {
            IronLog.ADAPTER_CALLBACK.verbose(ChartboostConstants.Logs.CLICK_ERROR.format(error.toString()))
        }

        listener.onAdClicked()
    }

    /**
     * Called when the user earns a reward.
     */
    override fun onRewardEarned(event: RewardEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the cached ad has expired.
     */
    override fun onAdExpired(event: ExpirationEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad is dismissed.
     */
    override fun onAdDismiss(event: DismissEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

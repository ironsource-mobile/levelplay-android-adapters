package com.ironsource.adapters.chartboost.banner

import android.widget.FrameLayout
import com.chartboost.sdk.callbacks.BannerCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ClickEvent
import com.chartboost.sdk.events.ExpirationEvent
import com.chartboost.sdk.events.ImpressionEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.ironsource.adapters.chartboost.ChartboostConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class ChartboostBannerListener(
    private val listener: BannerAdListener,
    private val layoutParams: FrameLayout.LayoutParams,
    private val adapter: WeakReference<ChartboostBannerAdapter>
) : BannerCallback {

    /**
     * Called when the banner finished caching or failed to cache.
     */
    override fun onAdLoaded(event: CacheEvent, error: CacheError?) {
        val creativeId = event.adID
        IronLog.ADAPTER_CALLBACK.verbose(ChartboostConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        val bannerView = adapter.get()?.bannerAdView
        if (bannerView == null) {
            IronLog.ADAPTER_CALLBACK.error(ChartboostConstants.Logs.BANNER_VIEW_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                ChartboostConstants.Logs.BANNER_VIEW_NULL
            )
            return
        }

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
            listener.onAdLoadSuccess(bannerView, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(ChartboostConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(bannerView, layoutParams, extraData)
        }
        bannerView.show()
    }

    /**
     * Called when the banner is about to be shown.
     */
    override fun onAdRequestedToShow(event: ShowEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the banner is shown.
     */
    override fun onAdShown(event: ShowEvent, error: ShowError?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the banner records an impression.
     */
    override fun onImpressionRecorded(event: ImpressionEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the banner is clicked.
     */
    override fun onAdClicked(event: ClickEvent, error: ClickError?) {
        IronLog.ADAPTER_CALLBACK.verbose()

        if (error != null) {
            IronLog.ADAPTER_CALLBACK.verbose(ChartboostConstants.Logs.CLICK_ERROR.format(error.toString()))
        }

        listener.onAdClicked()
    }

    /**
     * Called when the cached banner has expired.
     */
    override fun onAdExpired(event: ExpirationEvent) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

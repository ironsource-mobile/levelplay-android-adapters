package com.ironsource.adapters.yandex.banner

import android.widget.FrameLayout
import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.adapters.yandex.YandexConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import java.lang.ref.WeakReference

class YandexBannerListener(
    private val listener: BannerAdListener,
    private val adapter: WeakReference<YandexBannerAdapter>,
    private val adView: BannerAdView,
    private val layoutParams: FrameLayout.LayoutParams
) : BannerAdEventListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     */
    override fun onAdLoaded() {
        adapter.get()?.setBannerView(adView)

        // Extract creative IDs and pass as extra data if available
        val creativeId = adView.adInfo?.creatives
            ?.map { it.creativeId }
            ?.let { YandexAdapter.buildCreativeIdString(it) }
            ?: ""
        IronLog.ADAPTER_CALLBACK.verbose(YandexConstants.Logs.CREATIVE_ID.format(creativeId))

        if (creativeId.isEmpty()) {
            listener.onAdLoadSuccess(adView, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(YandexConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(adView, layoutParams, extraData)
        }
    }

    /**
     * Called when Ad failed to load
     * @param error - The error details
     */
    override fun onAdFailedToLoad(error: AdRequestError) {
        IronLog.ADAPTER_CALLBACK.error(YandexConstants.Logs.FAILED_TO_LOAD.format(error.code, error.description))
        listener.onAdLoadFailed(YandexAdapter.getLoadError(error), error.code, error.description)
    }

    /**
     * Called when Ad impression is tracked
     * @param impressionData - Impression data
     */
    override fun onImpression(impressionData: ImpressionData?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when Ad is clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when user leaves application
     */
    override fun onLeftApplication() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Called when user returns to application
     */
    override fun onReturnedToApplication() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

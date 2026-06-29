package com.ironsource.adapters.smaato.banner

import android.widget.FrameLayout
import com.ironsource.adapters.smaato.SmaatoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import com.smaato.sdk.banner.widget.BannerError
import com.smaato.sdk.banner.widget.BannerView

class SmaatoBannerListener(
    private val listener: BannerAdListener,
    private val layoutParams: FrameLayout.LayoutParams
) : BannerView.EventListener {

    /**
     * Called when the banner finished loading.
     */
    override fun onAdLoaded(bannerView: BannerView) {
        val creativeId = bannerView.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(SmaatoConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess(bannerView, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(SmaatoConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(bannerView, layoutParams, extraData)
        }
    }

    /**
     * Called when the banner failed to load.
     */
    override fun onAdFailedToLoad(bannerView: BannerView, bannerError: BannerError) {
        IronLog.ADAPTER_CALLBACK.error(SmaatoConstants.Logs.FAILED_TO_LOAD.format(bannerError.toString()))

        val errorType = if (bannerError == BannerError.NO_AD_AVAILABLE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }
        listener.onAdLoadFailed(errorType, bannerError.ordinal, bannerError.toString())
    }

    /**
     * Called when the banner records an impression.
     */
    override fun onAdImpression(bannerView: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the banner is clicked.
     */
    override fun onAdClicked(bannerView: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the cached banner has expired.
     */
    override fun onAdTTLExpired(bannerView: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

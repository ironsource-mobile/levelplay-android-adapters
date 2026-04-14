package com.ironsource.adapters.mobilefuse.banner

import android.widget.FrameLayout
import com.ironsource.adapters.mobilefuse.MobileFuseConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseBannerAd

class MobileFuseBannerListener(
    private val listener: BannerAdListener,
    private val adView: MobileFuseBannerAd,
    private val layoutParams: FrameLayout.LayoutParams
) : MobileFuseBannerAd.Listener {

    /**
     * Called when the banner ad has loaded successfully
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess(adView, layoutParams)
    }

    /**
     * Called when the banner ad failed to fill
     */
    override fun onAdNotFilled() {
        IronLog.ADAPTER_CALLBACK.verbose(MobileFuseConstants.BANNER_AD_NOT_FILLED)
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            MobileFuseConstants.BANNER_AD_NOT_FILLED
        )
    }

    /**
     * Called when the banner ad has been rendered on screen
     */
    override fun onAdRendered() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
        listener.onAdScreenPresented()
    }

    /**
     * Called when the banner ad is clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the banner ad has expired
     */
    override fun onAdExpired() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when an error occurs with the banner ad
     */
    override fun onAdError(error: AdError?) {
        val code = error?.errorCode ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
        val message = error?.errorMessage ?: MobileFuseConstants.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(MobileFuseConstants.Logs.AD_LOAD_ERROR.format(code, message))
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            code,
            message
        )
    }

    /**
     * Called when the banner ad has been expanded to fullscreen
     */
    override fun onAdExpanded() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the banner ad has been collapsed from fullscreen
     */
    override fun onAdCollapsed() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

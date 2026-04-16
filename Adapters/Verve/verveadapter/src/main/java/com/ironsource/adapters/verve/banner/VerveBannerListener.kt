package com.ironsource.adapters.verve.banner

import android.widget.FrameLayout
import com.ironsource.adapters.verve.VerveConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import net.pubnative.lite.sdk.HyBidError
import net.pubnative.lite.sdk.views.HyBidAdView

class VerveBannerListener(
    private val listener: BannerAdListener,
    private val adView: HyBidAdView,
    private val layoutParams: FrameLayout.LayoutParams
) : HyBidAdView.Listener {

    /**
     * Called when Ad was loaded and ready to be displayed
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess(adView, layoutParams)
    }

    /**
     * Called when Ad failed to load
     *
     * @param error - Throwable error
     */
    override fun onAdLoadFailed(error: Throwable?) {
        val hybidError = error as? HyBidError
        val errorCode = hybidError?.errorCode?.code ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
        val errorMessage = hybidError?.errorCode?.message ?: error?.message ?: VerveConstants.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(VerveConstants.Logs.FAILED_TO_LOAD.format(errorCode, errorMessage))
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            errorCode,
            errorMessage
        )
    }

    /**
     * Called when Ad Impression has been tracked
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when Ad has been clicked
     */
    override fun onAdClick() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }
}

package com.ironsource.adapters.pangle.banner

import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.ironsource.adapters.pangle.PangleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class PangleBannerListener(
    private val listener: BannerAdListener,
    private val adapter: WeakReference<PangleBannerAdapter>,
    private val layoutParams: FrameLayout.LayoutParams
) : PAGBannerAdLoadListener, PAGBannerAdInteractionListener {

    /**
     * Called when an ad material is loaded successfully
     * @param bannerAd - Banner ad instance
     */
    override fun onAdLoaded(bannerAd: PAGBannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setBannerAd(bannerAd)
        listener.onAdLoadSuccess(bannerAd.bannerView, layoutParams)
    }

    /**
     * Called when an ad fails to load
     * @param code - Error code
     * @param message - Error message
     */
    override fun onError(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.FAILED_TO_LOAD.format(code, message))
        val errorCode = if (code == PangleConstants.PANGLE_NO_FILL_ERROR_CODE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }
        listener.onAdLoadFailed(errorCode, code, message)
    }

    /**
     * Called when the ad is displayed
     */
    override fun onAdShowed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked by the user
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad disappears
     */
    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

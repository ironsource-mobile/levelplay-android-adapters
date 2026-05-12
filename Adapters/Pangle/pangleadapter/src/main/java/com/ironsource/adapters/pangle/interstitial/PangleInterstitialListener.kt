package com.ironsource.adapters.pangle.interstitial

import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener
import com.ironsource.adapters.pangle.PangleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class PangleInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<PangleInterstitialAdapter>
) : PAGInterstitialAdLoadListener, PAGInterstitialAdInteractionListener {

    /**
     * Called when an ad material is loaded successfully
     * @param interstitialAd - Interstitial ad instance
     */
    override fun onAdLoaded(interstitialAd: PAGInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setInterstitialAd(interstitialAd)
        adapter.get()?.setInterstitialAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when an ad fails to load
     * @param code - Error code
     * @param message - Error message
     */
    override fun onError(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.FAILED_TO_LOAD.format(code, message))
        adapter.get()?.setInterstitialAdAvailability(false)
        val errorType = if (code == PangleConstants.PANGLE_NO_FILL_ERROR_CODE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }
        listener.onAdLoadFailed(errorType, code, message)
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
        listener.onAdClosed()
    }
}

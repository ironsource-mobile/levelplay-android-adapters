package com.ironsource.adapters.hyprmx.interstitial

import com.hyprmx.android.sdk.core.HyprMXErrors
import com.hyprmx.android.sdk.placement.HyprMXShowListener
import com.hyprmx.android.sdk.placement.Placement
import com.ironsource.adapters.hyprmx.HyprMXConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog

class HyprMXInterstitialListener(
    private val listener: InterstitialAdListener
) : HyprMXShowListener {

    /**
     * Called when the ad starts playing.
     */
    override fun onAdStarted(placement: Placement) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad records an impression.
     */
    override fun onAdImpression(placement: Placement) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is closed.
     */
    override fun onAdClosed(placement: Placement, finished: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when an error occurs while displaying the ad.
     */
    override fun onAdDisplayError(placement: Placement, hyprMXError: HyprMXErrors) {
        IronLog.ADAPTER_CALLBACK.error(HyprMXConstants.Logs.DISPLAY_ERROR.format(hyprMXError))
        listener.onAdShowFailed(hyprMXError.ordinal, HyprMXConstants.Logs.DISPLAY_ERROR.format(hyprMXError))
    }
}

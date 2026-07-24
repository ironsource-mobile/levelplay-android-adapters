package com.ironsource.adapters.hyprmx.banner

import com.hyprmx.android.sdk.banner.HyprMXBannerView
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.hyprmx.android.sdk.banner.HyprMXBannerListener as HyprMXSdkBannerListener

class HyprMXBannerListener(
    private val listener: BannerAdListener
) : HyprMXSdkBannerListener {

    /**
     * Called when the banner presents a fullscreen overlay.
     */
    override fun onAdOpened(view: HyprMXBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    /**
     * Called when the banner records an impression.
     */
    override fun onAdImpression(view: HyprMXBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the banner is clicked.
     */
    override fun onAdClicked(view: HyprMXBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user is taken out of the app after a click.
     */
    override fun onAdLeftApplication(view: HyprMXBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Called when the banner's fullscreen overlay is dismissed.
     */
    override fun onAdClosed(view: HyprMXBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }
}

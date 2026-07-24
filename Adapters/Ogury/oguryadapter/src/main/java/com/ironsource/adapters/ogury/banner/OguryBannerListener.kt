package com.ironsource.adapters.ogury.banner

import android.widget.FrameLayout
import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.ogury.ad.OguryAdError
import com.ogury.ad.OguryBannerAdView
import com.ogury.ad.OguryBannerAdViewListener

class OguryBannerListener(
    private val listener: BannerAdListener,
    private val bannerAdView: OguryBannerAdView,
    private val layoutParams: FrameLayout.LayoutParams
) : OguryBannerAdViewListener {

    /**
     * Called when the banner ad is ready to be displayed.
     */
    override fun onAdLoaded(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess(bannerAdView, layoutParams)
    }

    /**
     * Called when the banner ad fails to load.
     */
    override fun onAdError(ad: OguryBannerAdView, error: OguryAdError) {
        IronLog.ADAPTER_CALLBACK.error(OguryConstants.Logs.LOAD_FAILED.format(error.code, error.message))
        listener.onAdLoadFailed(
            OguryAdapter.getLoadErrorType(error.code),
            error.code,
            error.message
        )
    }

    /**
     * Called when the banner ad records an impression.
     */
    override fun onAdImpression(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the banner ad is clicked.
     */
    override fun onAdClicked(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the banner ad's fullscreen overlay is dismissed.
     */
    override fun onAdClosed(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }
}

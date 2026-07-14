package com.ironsource.adapters.aps.banner

import android.widget.FrameLayout
import com.amazon.aps.ads.ApsAd
import com.amazon.aps.ads.listeners.ApsAdListener
import com.ironsource.adapters.aps.APSConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class APSBannerListener(
    private val listener: BannerAdListener,
    private val adapter: WeakReference<APSBannerAdapter>,
    private val layoutParams: FrameLayout.LayoutParams
) : ApsAdListener {

    /** Called when the banner ad was loaded successfully */
    override fun onAdLoaded(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        val bannerView = adapter.get()?.getBannerView()
        if (bannerView != null) {
            listener.onAdLoadSuccess(bannerView, layoutParams)
        } else {
            IronLog.INTERNAL.error(APSConstants.Logs.BANNER_VIEW_MISSING)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                APSConstants.Logs.BANNER_VIEW_MISSING
            )
        }
    }

    /** Called when the banner ad failed to load */
    override fun onAdFailedToLoad(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            APSConstants.Logs.BANNER_LOAD_FAILED
        )
    }

    /** Called when the banner ad encountered an error */
    override fun onAdError(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /** Called when the banner ad impression was fired */
    override fun onImpressionFired(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /** Called when the banner ad presents a fullscreen overlay */
    override fun onAdOpen(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    /** Called when the banner ad was clicked */
    override fun onAdClicked(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /** Called when the banner ad fullscreen overlay is dismissed */
    override fun onAdClosed(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }
}

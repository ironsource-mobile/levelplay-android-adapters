package com.ironsource.adapters.aps.interstitial

import com.amazon.aps.ads.ApsAd
import com.amazon.aps.ads.listeners.ApsAdListener
import com.ironsource.adapters.aps.APSConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class APSInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<APSInterstitialAdapter>
) : ApsAdListener {

    /** Called when the interstitial ad was loaded successfully */
    override fun onAdLoaded(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /** Called when the interstitial ad failed to load */
    override fun onAdFailedToLoad(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setAdAvailability(false)
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            APSConstants.Logs.INTERSTITIAL_LOAD_FAILED
        )
    }

    /** Called when the interstitial ad presents a fullscreen overlay */
    override fun onAdOpen(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /** Called when the interstitial ad impression was fired */
    override fun onImpressionFired(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /** Called when the interstitial ad encountered an error */
    override fun onAdError(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        // Do not call onAdShowFailed() because onAdError() is sometimes fired when ad display is successful.
    }

    /** Called when the interstitial ad was clicked */
    override fun onAdClicked(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /** Called when the interstitial video ad completed */
    override fun onVideoCompleted(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /** Called when the interstitial ad was closed */
    override fun onAdClosed(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

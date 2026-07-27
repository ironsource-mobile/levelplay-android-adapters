package com.ironsource.adapters.line.interstitial

import com.five_corp.ad.AdLoader
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdInterstitialEventListener
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.adapters.line.LineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class LineInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<LineInterstitialAdapter>
) : FiveAdInterstitialEventListener, AdLoader.LoadInterstitialAdCallback {

    /**
     * Called when the ad was loaded and is ready to be displayed
     * @param interstitialAd - Ad instance
     */
    override fun onLoad(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setInterstitialAd(interstitialAd)
        adapter.get()?.setInterstitialAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the ad failed to load
     * @param errorCode - The load error code
     */
    override fun onError(errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.error(LineConstants.Logs.FAILED_TO_LOAD.format(errorCode.value, errorCode.name))
        adapter.get()?.setInterstitialAdAvailability(false)
        listener.onAdLoadFailed(LineAdapter.getLoadErrorType(errorCode), errorCode.value, errorCode.name)
    }

    /**
     * Called when the ad failed to show
     * @param interstitialAd - Ad instance
     * @param errorCode - The show error code
     */
    override fun onViewError(interstitialAd: FiveAdInterstitial, errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.error(LineConstants.Logs.FAILED_TO_SHOW.format(errorCode.value, errorCode.name))
        listener.onAdShowFailed(errorCode.value, errorCode.name)
    }

    /**
     * Called when the ad impression is tracked
     * @param interstitialAd - Ad instance
     */
    override fun onImpression(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked
     * @param interstitialAd - Ad instance
     */
    override fun onClick(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad is closed
     * @param interstitialAd - Ad instance
     */
    override fun onFullScreenClose(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the ad opens a fullscreen overlay
     * @param interstitialAd - Ad instance
     */
    override fun onFullScreenOpen(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad playback starts
     * @param interstitialAd - Ad instance
     */
    override fun onPlay(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad playback pauses
     * @param interstitialAd - Ad instance
     */
    override fun onPause(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the ad playback reaches the view-through point
     * @param interstitialAd - Ad instance
     */
    override fun onViewThrough(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

package com.ironsource.adapters.facebook.interstitial

import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAdExtendedListener
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class FacebookInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<FacebookInterstitialAdapter>
) : InterstitialAdExtendedListener {

    internal var showAttempted = false
    private var didCallClosed = false

    /**
     * Called when the interstitial ad was loaded successfully
     */
    override fun onAdLoaded(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setInterstitialAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the interstitial ad failed to load or show
     * @param ad the ad instance
     * @param adError the error describing the failure
     */
    override fun onError(ad: Ad, adError: AdError) {
        IronLog.ADAPTER_CALLBACK.error(FacebookConstants.Logs.FAILED_TO_LOAD.format(adError.errorCode, adError.errorMessage))
        if (showAttempted) {
            listener.onAdShowFailed(adError.errorCode, adError.errorMessage)
        } else {
            adapter.get()?.setInterstitialAdAvailability(false)
            listener.onAdLoadFailed(FacebookAdapter.getLoadErrorType(adError), adError.errorCode, adError.errorMessage)
        }
    }

    /**
     * Called when the interstitial ad is displayed
     */
    override fun onInterstitialDisplayed(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when an impression is recorded for the interstitial ad
     */
    override fun onLoggingImpression(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        didCallClosed = false
        listener.onAdOpened()
    }

    /**
     * Called when the interstitial ad was clicked
     */
    override fun onAdClicked(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the interstitial ad was dismissed
     */
    override fun onInterstitialDismissed(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        didCallClosed = true
        listener.onAdClosed()
    }

    /**
     * Called when the interstitial activity was destroyed without being properly closed.
     * This can happen for singleTask launch mode apps relaunched from background.
     */
    override fun onInterstitialActivityDestroyed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        if (!didCallClosed) {
            didCallClosed = true
            listener.onAdClosed()
        }
    }

    override fun onRewardedAdCompleted() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onRewardedAdServerSucceeded() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onRewardedAdServerFailed() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

package com.ironsource.adapters.applovin.interstitial

import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.ironsource.adapters.applovin.AppLovinAdapter
import com.ironsource.adapters.applovin.AppLovinConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class AppLovinInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<AppLovinInterstitialAdapter>
) : AppLovinAdLoadListener,
    AppLovinAdClickListener,
    AppLovinAdDisplayListener,
    AppLovinAdVideoPlaybackListener {

    /**
     * Called by AppLovin when a new interstitial ad has been received.
     * @param appLovinAd the newly received ad, guaranteed not to be null.
     */
    override fun adReceived(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setInterstitialAd(appLovinAd)
        adapter.get()?.setInterstitialAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called by AppLovin when an interstitial ad could not be retrieved from the server.
     * @param errorCode the reason the ad failed to load.
     */
    override fun failedToReceiveAd(errorCode: Int) {
        val errorMessage = AppLovinAdapter.getErrorString(errorCode)
        IronLog.ADAPTER_CALLBACK.error(AppLovinConstants.Logs.LOAD_FAILED.format(errorCode, errorMessage))
        adapter.get()?.setInterstitialAdAvailability(false)
        listener.onAdLoadFailed(AppLovinAdapter.getLoadErrorType(errorCode), errorCode, errorMessage)
    }

    /**
     * Called by AppLovin when the interstitial ad is displayed.
     * @param appLovinAd the ad that was just displayed.
     */
    override fun adDisplayed(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called by AppLovin when the interstitial ad is clicked.
     * @param appLovinAd the ad that was just clicked.
     */
    override fun adClicked(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called by AppLovin when the interstitial ad is hidden.
     * @param appLovinAd the ad that was just hidden.
     */
    override fun adHidden(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called by AppLovin when a video begins playing in the interstitial ad.
     * @param appLovinAd the ad in which playback began.
     */
    override fun videoPlaybackBegan(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by AppLovin when a video stops playing in the interstitial ad.
     * @param appLovinAd the ad in which playback ended.
     * @param percentViewed percent of the video which the user watched.
     * @param fullyWatched whether the video was watched to, or very near, completion.
     */
    override fun videoPlaybackEnded(appLovinAd: AppLovinAd, percentViewed: Double, fullyWatched: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}
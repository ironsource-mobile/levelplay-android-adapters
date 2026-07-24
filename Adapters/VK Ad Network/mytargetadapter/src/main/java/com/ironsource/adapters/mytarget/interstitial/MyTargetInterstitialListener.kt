package com.ironsource.adapters.mytarget.interstitial

import com.ironsource.adapters.mytarget.MyTargetConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.my.target.ads.InterstitialAd
import com.my.target.common.models.IAdLoadingError
import java.lang.ref.WeakReference

class MyTargetInterstitialListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<MyTargetInterstitialAdapter>
) : InterstitialAd.InterstitialAdListener {

    /**
     * Called when the ad assets have been loaded and the ad is ready to be shown.
     * @param interstitialAd - the ad instance that was loaded.
     */
    override fun onLoad(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setInterstitialAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the ad failed to load.
     * @param iAdLoadingError - the error details suggesting the cause of failure.
     * @param interstitialAd - the ad instance that failed to load.
     */
    override fun onNoAd(iAdLoadingError: IAdLoadingError, interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.error(MyTargetConstants.Logs.FAILED_TO_LOAD.format(iAdLoadingError.code, iAdLoadingError.message))
        adapter.get()?.setInterstitialAdAvailability(false)
        listener.onAdLoadFailed(AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL, iAdLoadingError.code, iAdLoadingError.message)
    }

    /**
     * Called when the ad failed to show.
     * @param interstitialAd - the ad instance that failed to show.
     */
    override fun onFailedToShow(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.error(MyTargetConstants.Logs.AD_SHOW_FAILED)
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, MyTargetConstants.Logs.AD_SHOW_FAILED)
    }

    /**
     * Called when the ad is displayed (impression).
     * @param interstitialAd - the ad instance that was displayed.
     */
    override fun onDisplay(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the user clicks on the ad.
     * @param interstitialAd - the ad instance that was clicked.
     */
    override fun onClick(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad is dismissed and the user returns to the app.
     * @param interstitialAd - the ad instance that was dismissed.
     */
    override fun onDismiss(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the video inside the interstitial has finished playing.
     * @param interstitialAd - the ad instance whose video completed.
     */
    override fun onVideoCompleted(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

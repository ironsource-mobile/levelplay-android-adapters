package com.ironsource.adapters.pubmatic.interstitial

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial

class PubMaticInterstitialAdListener (
    private val mListener: InterstitialSmashListener,
    private val mAdUnitId: String,
    ) : POBInterstitial.POBInterstitialListener() {

    /**
     * Callback method notifies that an ad has been received successfully.
     * @param interstitialAd - Interstitial Ad instance.
     */
    override fun onAdReceived(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onInterstitialAdReady()
    }

    /**
     * Callback method notifies an error encountered while loading an ad.
     * @param interstitialAd - Interstitial Ad instance.
     * @param error - PubMatic Ad Error
     */
    override fun onAdFailedToLoad(interstitialAd: POBInterstitial, error: POBError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId, errorCode = ${error.errorCode}, errorMessage = ${error.errorMessage}")
        mListener.onInterstitialAdLoadFailed(
            PubMaticAdapter.getLoadErrorAndCheckNoFill(
                error,
                IronSourceError.ERROR_IS_LOAD_NO_FILL
            )
        )
    }

    /**
     * Callback method Notifies that an impression event occurred.
     * @param interstitialAd - Interstitial Ad instance.
     */
    override fun onAdImpression(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * Callback method notifies an error encountered while rendering an ad.
     * @param interstitialAd - Interstitial Ad instance.
     * @param error - PubMatic Ad Error
     */
    override fun onAdFailedToShow(interstitialAd: POBInterstitial, error: POBError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId, errorMessage = $error.errorMessage, errorCode = $error.errorCode")
        mListener.onInterstitialAdShowFailed(IronSourceError(error.errorCode, error.errorMessage))
    }

    /**
     * Callback method notifies ad click
     * @param interstitialAd - Interstitial Ad instance.
     */
    override fun onAdClicked(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onInterstitialAdClicked()
    }

    /**
     * Callback method notifies that a user interaction will open another app (e.g. Play store), leaving the current app.
     * @param interstitialAd - Interstitial Ad instance.
     */
    override fun onAppLeaving(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
    }

    /**
     * Callback method notifies that the interstitial ad will be presented as a modal on top of the current view.
     *@param interstitialAd - Interstitial Ad instance.
     */
    override fun onAdOpened(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
    }

    /**
     * Callback method notifies that the interstitial ad has been animated off the screen.
     * @param interstitialAd - Interstitial Ad instance.
     */
    override fun onAdClosed(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onInterstitialAdClosed()
    }

    /**
     * Callback method notifies that the interstitial ad has been expired.
     * @param interstitialAd - Interstitial Ad instance.
     */
    override fun onAdExpired(interstitialAd: POBInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
    }
}
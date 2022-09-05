package com.ironsource.adapters.yahoo

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yahoo.ads.ErrorInfo
import com.yahoo.ads.interstitialplacement.InterstitialAd
import java.lang.ref.WeakReference

class YahooInterstitialAdListener(private val mIListener: InterstitialSmashListener?,
                                  private val mAdapter: WeakReference<YahooAdapter>?,
                                  private val mPlacementId: String) : InterstitialAd.InterstitialAdListener {

    // Called when the view for the InterstitialAd has been loaded.
    override fun onLoaded(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mAdapter?.get()?.setInterstitialAd(mPlacementId, interstitialAd)
        mAdapter?.get()?.setInterstitialAdAvailability(mPlacementId, true)
        mIListener?.onInterstitialAdReady()
    }

    // Called when there is an error requesting a InterstitialAd or loading a InterstitialAd from the cache.
    override fun onLoadFailed(interstitialAd: InterstitialAd?, errorInfo: ErrorInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load adUnitID = $mPlacementId with error: $errorInfo")
        mAdapter?.get()?.setInterstitialAdAvailability(mPlacementId, false)
        val interstitialError = mAdapter?.get()?.getLoadErrorAndCheckNoFill(errorInfo, IronSourceError.ERROR_IS_LOAD_NO_FILL)
        mIListener?.onInterstitialAdLoadFailed(interstitialError)
    }

    // Called when the InterstitialAd has been shown.
    override fun onShown(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mIListener?.onInterstitialAdOpened()
        mIListener?.onInterstitialAdShowSucceeded()
    }

    // Called when an error occurs during the InterstitialAd lifecycle.
    override fun onError(interstitialAd: InterstitialAd?, errorInfo: ErrorInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId, error = $errorInfo")
        val errorMsg = mAdapter?.get()?.generateShowFailErrorMessage(errorInfo, "interstitial show failed")
        val interstitialError: IronSourceError = ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, errorMsg)
        mIListener?.onInterstitialAdShowFailed(interstitialError)
    }

    // Called when the InterstitialAd has been clicked.
    override fun onClicked(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mIListener?.onInterstitialAdClicked()
    }

    // Called when the InterstitialAd causes the user to leave the application.
    override fun onAdLeftApplication(interstitialAd: InterstitialAd?) {
    }

    // Called when the InterstitialAd has been closed.
    override fun onClosed(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mIListener?.onInterstitialAdClosed()
    }

    // This callback is used to surface additional events to the publisher from the SDK.
    override fun onEvent(interstitialAd: InterstitialAd?, source: String?, eventId: String?, arguments: MutableMap<String, Any>?) {
    }
}
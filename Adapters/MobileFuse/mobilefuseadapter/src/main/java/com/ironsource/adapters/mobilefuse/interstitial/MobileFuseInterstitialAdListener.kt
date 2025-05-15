package com.ironsource.adapters.mobilefuse.interstitial

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseInterstitialAd

class MobileFuseInterstitialAdListener(
    private val mListener: InterstitialSmashListener,
) : MobileFuseInterstitialAd.Listener {

  override fun onAdLoaded() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onInterstitialAdReady()
  }

  override fun onAdNotFilled() {
    val error = IronSourceError(IronSourceError.ERROR_IS_LOAD_NO_FILL, "No Fill")
    mListener.onInterstitialAdLoadFailed(error)
  }

  override fun onAdRendered() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onInterstitialAdOpened()
    mListener.onInterstitialAdVisible()
  }

  override fun onAdClicked() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onInterstitialAdClicked()
  }

  override fun onAdExpired() {
    IronLog.ADAPTER_CALLBACK.verbose()
  }

  override fun onAdError(error: AdError?) {
    val code = error?.errorCode ?: 0
    val message = error?.errorMessage ?: ""
    IronLog.ADAPTER_CALLBACK.verbose("Failed to load/show, errorCode = ${code}, errorMessage = $message")

    val ironSourceError = IronSourceError(code, message)
    if ( error == AdError.AD_ALREADY_LOADED || error == AdError.AD_LOAD_ERROR ) {
      mListener.onInterstitialAdLoadFailed(ironSourceError)
    }
    else {
      mListener.onInterstitialAdShowFailed(ironSourceError)
    }
  }

  override fun onAdClosed() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onInterstitialAdClosed()
  }
}
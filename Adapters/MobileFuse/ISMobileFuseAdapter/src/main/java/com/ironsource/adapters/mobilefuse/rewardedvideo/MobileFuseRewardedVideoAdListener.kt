package com.ironsource.adapters.mobilefuse.rewardedvideo

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseRewardedAd

class MobileFuseRewardedVideoAdListener(
    private val mListener: RewardedVideoSmashListener,
) : MobileFuseRewardedAd.Listener {

  override fun onAdLoaded() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onRewardedVideoAvailabilityChanged(true)
  }

  override fun onAdNotFilled() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onRewardedVideoAvailabilityChanged(false)
    mListener.onRewardedVideoLoadFailed(IronSourceError(
      IronSourceError.ERROR_RV_LOAD_NO_FILL,
      "No Fill")
    )
  }

  override fun onAdRendered() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onRewardedVideoAdOpened()
    mListener.onRewardedVideoAdStarted()
  }

  override fun onAdClicked() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onRewardedVideoAdClicked()
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
      mListener.onRewardedVideoLoadFailed(ironSourceError)
    }
    else {
      mListener.onRewardedVideoAdShowFailed(ironSourceError)
    }
  }

  override fun onAdClosed() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onRewardedVideoAdEnded()
    mListener.onRewardedVideoAdClosed()
  }

  override fun onUserEarnedReward() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onRewardedVideoAdRewarded()
  }
}
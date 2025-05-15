package com.ironsource.adapters.mobilefuse.banner

import android.widget.FrameLayout
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.mobilefuse.sdk.AdError
import com.mobilefuse.sdk.MobileFuseBannerAd

class MobileFuseBannerAdListener(
  private val mListener: BannerSmashListener,
  private val mAdView: MobileFuseBannerAd?,
  private val mLayoutParams: FrameLayout.LayoutParams
) :  MobileFuseBannerAd.Listener{

  override fun onAdLoaded() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onBannerAdLoaded(
      mAdView,
      mLayoutParams
    )
  }

  override fun onAdNotFilled() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Banner ad not filled"))
  }

  override fun onAdRendered() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onBannerAdShown()
    mListener.onBannerAdScreenPresented()
  }

  override fun onAdClicked() {
    IronLog.ADAPTER_CALLBACK.verbose()
    mListener.onBannerAdClicked()
  }

  override fun onAdExpired() {
    IronLog.ADAPTER_CALLBACK.verbose()
  }

  override fun onAdError(error: AdError?) {
    val code = error?.errorCode ?: 0
    val message = error?.errorMessage ?: ""
    IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${code}, errorMessage = ${message}")
    mListener.onBannerAdLoadFailed(IronSourceError(code, message))
  }

  override fun onAdExpanded() {
    IronLog.ADAPTER_CALLBACK.verbose()
  }

  override fun onAdCollapsed() {
    IronLog.ADAPTER_CALLBACK.verbose()
  }

}
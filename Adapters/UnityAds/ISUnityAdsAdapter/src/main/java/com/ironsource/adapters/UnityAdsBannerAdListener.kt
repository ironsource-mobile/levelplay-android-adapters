package com.ironsource.adapters.unityads

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.BannerView.IListener
import java.lang.ref.WeakReference

class UnityAdsBannerListener(private val mListener: BannerSmashListener?,
                             private val mAdapter: WeakReference<UnityAdsAdapter>?,
                             private val mPlacementId: String) : IListener {

  override fun onBannerLoaded(bannerView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    if (bannerView != null) {
      mListener?.onBannerAdLoaded(bannerView, mAdapter?.get()?.createLayoutParams(bannerView.size))
    }
  }

  override fun onBannerFailedToLoad(
      bannerView: BannerView?,
      bannerErrorInfo: BannerErrorInfo?
  ) {
    val message = "${mAdapter?.get()?.providerName} banner, onAdLoadFailed placementId $mPlacementId with error: ${bannerErrorInfo?.errorMessage}"

    val ironSourceError = if (bannerErrorInfo?.errorCode == BannerErrorCode.NO_FILL) {
      IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, message)
    } else {
      ErrorBuilder.buildLoadFailedError(message)
    }

    IronLog.ADAPTER_CALLBACK.error("placementId = $mPlacementId ironSourceError = $ironSourceError")

    mListener?.onBannerAdLoadFailed(ironSourceError)
  }

  override fun onBannerShown(bannerAdView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    mListener?.onBannerAdShown()
  }

  override fun onBannerClick(bannerView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    mListener?.onBannerAdClicked()
  }

  override fun onBannerLeftApplication(bannerView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    mListener?.onBannerAdLeftApplication()
  }
}
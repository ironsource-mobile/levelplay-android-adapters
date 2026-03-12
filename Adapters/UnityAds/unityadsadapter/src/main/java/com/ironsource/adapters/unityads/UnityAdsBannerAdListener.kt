package com.ironsource.adapters.unityads

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.mediation.LevelPlay
import com.unity3d.services.banners.BannerView.IListener
import java.lang.ref.WeakReference

internal class UnityAdsBannerAdListener(private val mListener: BannerSmashListener?,
                             private val bridge: WeakReference<LegacyNetworkBridge>?,
                             private val mPlacementId: String,
                             private val mErrorReporter: UnityAdsErrorReporter?) : IListener {

  override fun onBannerLoaded(bannerView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    if (bannerView != null) {
      if (bridge?.get() == null || mListener == null) {
        mErrorReporter?.reportMissingCallback(LevelPlay.AdFormat.BANNER, bridge?.get() == null, mListener == null, "banner_onBannerLoaded")
      }
      mListener?.onBannerAdLoaded(bannerView, bridge?.get()?.createLayoutParams(bannerView.size))
    }
  }

  override fun onBannerFailedToLoad(
      bannerView: BannerView?,
      bannerErrorInfo: BannerErrorInfo?
  ) {
    val message = "${bridge?.get()?.providerName} banner, onAdLoadFailed placementId $mPlacementId with error: ${bannerErrorInfo?.errorMessage}"

    val ironSourceError = if (bannerErrorInfo?.errorCode == BannerErrorCode.NO_FILL) {
      IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, message)
    } else {
      ErrorBuilder.buildLoadFailedError(message)
    }

    IronLog.ADAPTER_CALLBACK.error("placementId = $mPlacementId ironSourceError = $ironSourceError")

    if (bridge?.get() == null || mListener == null) {
      mErrorReporter?.reportMissingCallback(LevelPlay.AdFormat.BANNER, bridge?.get() == null, mListener == null, "banner_onBannerFailedToLoad")
    }
    mListener?.onBannerAdLoadFailed(ironSourceError)
  }

  override fun onBannerShown(bannerAdView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    if (mListener == null) {
      mErrorReporter?.reportMissingCallback(LevelPlay.AdFormat.BANNER, false, true, "banner_onBannerShown")
    }
    mListener?.onBannerAdShown()
  }

  override fun onBannerClick(bannerView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    if (mListener == null) {
      mErrorReporter?.reportMissingCallback(LevelPlay.AdFormat.BANNER, false, true, "banner_onBannerClick")
    }
    mListener?.onBannerAdClicked()
  }

  override fun onBannerLeftApplication(bannerView: BannerView?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    if (mListener == null) {
      mErrorReporter?.reportMissingCallback(LevelPlay.AdFormat.BANNER, false, true, "banner_onBannerLeftApplication")
    }
    mListener?.onBannerAdLeftApplication()
  }

}
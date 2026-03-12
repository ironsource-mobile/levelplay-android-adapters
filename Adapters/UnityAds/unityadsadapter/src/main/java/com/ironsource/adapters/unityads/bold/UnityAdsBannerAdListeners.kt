package com.ironsource.adapters.unityads.bold

import android.widget.FrameLayout
import com.ironsource.adapters.unityads.BoldNetworkBridge.Companion.BOLD_NO_FILL_ERROR_CODE
import com.ironsource.adapters.unityads.UnityAdsErrorReporter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.BannerAd
import com.unity3d.ads.BannerShowListener
import com.unity3d.ads.LoadListener
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import com.unity3d.mediation.LevelPlay
import java.lang.ref.WeakReference

@OptIn(UnityAdsExperimental::class)
internal class UnityAdBannerAdLoadListener(
  private val providerName: String,
  private val placementId: String,
  private val listener: WeakReference<BannerSmashListener?>,
  private val getLayoutParam: () -> FrameLayout.LayoutParams,
  private val errorReporter: UnityAdsErrorReporter?,
  private val onAdAvailable: (BannerAd) -> Unit,
) : LoadListener<BannerAd> {
  override fun onAdLoaded(unityAd: BannerAd?, error: UnityAdsError?) {
    if (unityAd != null) {
      onAdAvailable(unityAd)

      IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
      listener.get()?.onBannerAdLoaded(
        unityAd.view, getLayoutParam()
      ) ?: reportMissingListener("banner_onAdLoad_success")
    } else {
      val message =
        "$providerName banner, onAdLoadFailed placementId $placementId with error: ${error?.message}"

      val ironSourceError: IronSourceError = if (error == null) {
        ErrorBuilder.buildLoadFailedError(
          IronSourceConstants.BANNER_AD_UNIT, providerName, message
        )
      } else if (error.code == BOLD_NO_FILL_ERROR_CODE) {
        IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, message)
      } else {
        IronSourceError(error.code, message)
      }

      IronLog.ADAPTER_CALLBACK.error("placementId = $placementId ironSourceError = $ironSourceError")

      listener.get()?.onBannerAdLoadFailed(ironSourceError)
        ?: reportMissingListener("banner_onAdLoad_fail")
    }
  }

  private fun reportMissingListener(tag: String) {
    errorReporter?.reportMissingCallback(
      adFormat = LevelPlay.AdFormat.BANNER, adapterNull = false, listenerNull = true, errorDetails = tag
    )
  }
}

@OptIn(UnityAdsExperimental::class)
internal class UnityAdBannerAdShowListener(
  private val placementId: String,
  private val listener: WeakReference<BannerSmashListener?>,
  private val errorReporter: UnityAdsErrorReporter?,
) : BannerShowListener {
  override fun onClicked(banner: BannerAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

    listener.get()?.onBannerAdClicked() ?: reportMissingListener("banner_onClicked")
  }

  override fun onFailedToShow(banner: BannerAd, error: UnityAdsError) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

    // Nothing to do
  }

  override fun onImpression(banner: BannerAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

    listener.get()?.onBannerAdShown() ?: reportMissingListener("banner_onImpression")
  }

  private fun reportMissingListener(tag: String) {
    errorReporter?.reportMissingCallback(
      adFormat = LevelPlay.AdFormat.BANNER, adapterNull = false, listenerNull = true, errorDetails = tag
    )
  }
}
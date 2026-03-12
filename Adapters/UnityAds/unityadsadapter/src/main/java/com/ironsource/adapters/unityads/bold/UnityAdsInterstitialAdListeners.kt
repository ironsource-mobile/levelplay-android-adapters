package com.ironsource.adapters.unityads.bold

import com.ironsource.adapters.unityads.BoldNetworkBridge.Companion.BOLD_NO_FILL_ERROR_CODE
import com.ironsource.adapters.unityads.UnityAdsErrorReporter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.InterstitialAd
import com.unity3d.ads.InterstitialShowListener
import com.unity3d.ads.LoadListener
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import com.unity3d.mediation.LevelPlay
import java.lang.ref.WeakReference

@OptIn(UnityAdsExperimental::class)
internal class UnityAdsInterstitialAdLoadListener(
  private val providerName: String,
  private val placementId: String,
  private val listener: WeakReference<InterstitialSmashListener?>,
  private val errorReporter: UnityAdsErrorReporter?,
  private val onAdAvailable: (InterstitialAd) -> Unit,
) : LoadListener<InterstitialAd> {
  override fun onAdLoaded(unityAd: InterstitialAd?, error: UnityAdsError?) {
    if (unityAd != null) {
      IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

      onAdAvailable(unityAd)
      listener.get()?.onInterstitialAdReady()
        ?: reportMissingListener("interstitial_onAdLoaded_success")
    } else {
      val ironSourceError: IronSourceError = if (error == null) {
        ErrorBuilder.buildLoadFailedError(
          IronSourceConstants.INTERSTITIAL_AD_UNIT, providerName, ""
        )
      } else if (error.code == BOLD_NO_FILL_ERROR_CODE) {
        IronSourceError(IronSourceError.ERROR_IS_LOAD_NO_FILL, error.message)
      } else {
        IronSourceError(error.code, error.message)
      }

      IronLog.ADAPTER_CALLBACK.error("placementId = $placementId ironSourceError = $ironSourceError")
      listener.get()?.onInterstitialAdLoadFailed(ironSourceError)
        ?: reportMissingListener("interstitial_onAdLoaded_fail")
    }
  }

  private fun reportMissingListener(tag: String) {
    errorReporter?.reportMissingCallback(
      adFormat = LevelPlay.AdFormat.INTERSTITIAL, adapterNull = false, listenerNull = true, errorDetails = tag
    )
  }
}

@OptIn(UnityAdsExperimental::class)
internal class UnityAdsInterstitialShowAdListener(
  private val placementId: String,
  private val listener: WeakReference<InterstitialSmashListener?>,
  private val errorReporter: UnityAdsErrorReporter?
) : InterstitialShowListener {

  override fun onClicked(unityAd: InterstitialAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
    listener.get()?.onInterstitialAdClicked() ?: reportMissingListener("interstitial_onClicked")
  }

  override fun onCompleted(unityAd: InterstitialAd, state: ShowFinishState) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId completionState = $state")

    when (state) {
      ShowFinishState.COMPLETED, ShowFinishState.SKIPPED -> {
        listener.get()?.onInterstitialAdClosed()
          ?: reportMissingListener("interstitial_onCompleted")
      }
    }
  }

  override fun onFailed(unityAd: InterstitialAd, error: UnityAdsError) {
    val ironSourceError = IronSourceError(error.code, error.message)

    IronLog.ADAPTER_CALLBACK.error("placementId = $placementId ironSourceError = $ironSourceError")
    listener.get()?.onInterstitialAdShowFailed(ironSourceError)
      ?: reportMissingListener("interstitial_onFailed")
  }

  override fun onStarted(unityAd: InterstitialAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

    listener.get()?.onInterstitialAdOpened() ?: reportMissingListener("interstitial_onStarted")
    listener.get()?.onInterstitialAdShowSucceeded()
  }

  private fun reportMissingListener(tag: String) {
    errorReporter?.reportMissingCallback(
      adFormat = LevelPlay.AdFormat.INTERSTITIAL, adapterNull = false, listenerNull = true, errorDetails = tag
    )
  }
}
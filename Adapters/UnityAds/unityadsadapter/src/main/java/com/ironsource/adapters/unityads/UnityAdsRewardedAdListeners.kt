package com.ironsource.adapters.unityads

import com.ironsource.adapters.unityads.UnityAdsNetworkBridge.Companion.BOLD_NO_FILL_ERROR_CODE
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.LoadListener
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.RewardedShowListener
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import com.unity3d.mediation.LevelPlay

@OptIn(UnityAdsExperimental::class)
internal class UnityAdsRewardedAdLoadListener(
  private val providerName: String,
  private val placementId: String,
  private val listener: RewardedVideoSmashListener?,
  private val errorReporter: UnityAdsErrorReporter?,
  private val onAdAvailable: (RewardedAd) -> Unit,
) : LoadListener<RewardedAd> {

  override fun onAdLoaded(unityAd: RewardedAd?, error: UnityAdsError?) {
    if (unityAd != null) {
      IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

      onAdAvailable(unityAd)
      listener?.onRewardedVideoAvailabilityChanged(true)
        ?: reportMissingListener("rewarded_onAdLoad_success")
    } else {
      val ironSourceError: IronSourceError = if (error == null) {
        ErrorBuilder.buildLoadFailedError(
          IronSourceConstants.REWARDED_VIDEO_AD_UNIT, providerName, ""
        )
      } else if (error.code == BOLD_NO_FILL_ERROR_CODE) {
        IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, error.message)
      } else {
        IronSourceError(error.code, error.message)
      }


      IronLog.ADAPTER_CALLBACK.error("placementId = $placementId ironSourceError = $ironSourceError")
      listener?.onRewardedVideoAvailabilityChanged(false)
      listener?.onRewardedVideoLoadFailed(ironSourceError)
        ?: reportMissingListener("rewarded_onAdLoad_fail")
    }
  }

  private fun reportMissingListener(tag: String) {
    errorReporter?.reportMissingCallback(
      adFormat = LevelPlay.AdFormat.REWARDED, adapterNull = false, listenerNull = true, errorDetails = tag
    )
  }
}

@OptIn(UnityAdsExperimental::class)
internal class UnityAdsRewardedAdShowListener(
  private val placementId: String,
  private val listener: RewardedVideoSmashListener?,
  private val errorReporter: UnityAdsErrorReporter?
) : RewardedShowListener {

  override fun onStarted(unityAd: RewardedAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")

    listener?.onRewardedVideoAdOpened() ?: reportMissingListener("rewarded_onStarted")
    listener?.onRewardedVideoAdStarted()
  }

  override fun onFailed(unityAd: RewardedAd, error: UnityAdsError) {
    val ironSourceError = IronSourceError(error.code, error.message)

    IronLog.ADAPTER_CALLBACK.error("placementId = $placementId ironSourceError = $ironSourceError")
    listener?.onRewardedVideoAdShowFailed(ironSourceError)
      ?: reportMissingListener("rewarded_onFailed")
  }

  override fun onClicked(unityAd: RewardedAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
    listener?.onRewardedVideoAdClicked() ?: reportMissingListener("rewarded_onClicked")
  }

  override fun onRewarded(rewardedAd: RewardedAd) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
    listener?.onRewardedVideoAdRewarded() ?: reportMissingListener("rewarded_onRewarded")
  }

  override fun onCompleted(unityAd: RewardedAd, state: ShowFinishState) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId completionState = $state")

    when (state) {
      ShowFinishState.SKIPPED -> listener?.onRewardedVideoAdClosed() ?: reportMissingListener(
        "rewarded_onCompleted"
      )

      ShowFinishState.COMPLETED -> {
        listener?.onRewardedVideoAdEnded() ?: reportMissingListener("rewarded_onCompleted")
        listener?.onRewardedVideoAdClosed()
      }
    }
  }

  private fun reportMissingListener(tag: String) {
    errorReporter?.reportMissingCallback(
      adFormat = LevelPlay.AdFormat.REWARDED, adapterNull = false, listenerNull = true, errorDetails = tag
    )
  }
}
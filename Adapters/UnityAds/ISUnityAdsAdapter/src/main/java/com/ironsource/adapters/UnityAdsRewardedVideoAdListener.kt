package com.ironsource.adapters.unityads

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState
import java.lang.ref.WeakReference

class UnityAdsRewardedVideoListener(private val mListener: RewardedVideoSmashListener?,
                                    private val mAdapter: WeakReference<UnityAdsAdapter>?,
                                    private val mPlacementId: String) : IUnityAdsLoadListener, IUnityAdsShowListener {

  //region IUnityAdsLoadListener Callbacks
  /**
   * Callback triggered when a load request has successfully filled the specified placementId with an ad that is ready to show.
   *
   * @param placementId placementId, as defined in Unity Ads admin tools
   */
  override fun onUnityAdsAdLoaded(placementId: String?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    mAdapter?.get()?.setRewardedVideoAdAvailability(mPlacementId, true)
    mListener?.onRewardedVideoAvailabilityChanged(true)
  }

  /**
   * Callback triggered when load request has failed to load an ad for a requested placementId.
   *
   * @param placementId placementId, as defined in Unity Ads admin tools
   * @param error Error code related to the error  See: [com.unity3d.ads.UnityAds.UnityAdsLoadError]
   * @param message Human-readable error message
   */
  override fun onUnityAdsFailedToLoad(
    placementId: String?,
    error: UnityAdsLoadError?,
    message: String?
  ) {
    mAdapter?.get()?.setRewardedVideoAdAvailability(mPlacementId, false)
    mListener?.onRewardedVideoAvailabilityChanged(false)

    // For Rewarded Videos, when an adapter receives a failure reason from the network, it will pass it to the Mediation.
    // This is done in addition to the load failure report of the adapter for further analysis
    if (error != null) {
      val ironSourceError: IronSourceError;

      val errorCode = if (error == UnityAdsLoadError.NO_FILL) {
        IronSourceError.ERROR_RV_LOAD_NO_FILL
      } else {
        mAdapter?.get()?.getUnityAdsLoadErrorCode(error) ?: IronSourceError.ERROR_CODE_GENERIC
      }
      ironSourceError = IronSourceError(errorCode, message)

      IronLog.ADAPTER_CALLBACK.error("placementId = $mPlacementId ironSourceError = $ironSourceError")
      mListener?.onRewardedVideoLoadFailed(ironSourceError)
    }
  }

  //endregion

  //region IUnityAdsShowListener Callbacks
  /**
   * Callback which notifies that UnityAds has started to show ad with a specific placementId.
   *
   * @param placementId placementId, as defined in Unity Ads admin tools
   */
  override fun onUnityAdsShowStart(placementId: String?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    mListener?.onRewardedVideoAdOpened()
    mListener?.onRewardedVideoAdStarted()
  }

  /**
   * Callback which notifies that UnityAds has failed to show a specific placementId with an error message and error category.
   *
   * @param placementId placementId, as defined in Unity Ads admin tools
   * @param error Error code related to the error  See: [com.unity3d.ads.UnityAds.UnityAdsShowError]
   * @param message Human-readable error message
   */
  override fun onUnityAdsShowFailure(
    placementId: String?,
    error: UnityAds.UnityAdsShowError?,
    message: String?
  ) {
    val ironSourceError = if (error != null) {
      val errorCode = mAdapter?.get()?.getUnityAdsShowErrorCode(error) ?: IronSourceError.ERROR_CODE_GENERIC
      IronSourceError(errorCode, message)
    } else {
      ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, message)
    }

    IronLog.ADAPTER_CALLBACK.error("placementId = $mPlacementId ironSourceError = $ironSourceError")
    mListener?.onRewardedVideoAdShowFailed(ironSourceError)
  }

  /**
   * Callback which notifies that UnityAds has received a click while showing ad for a specific placementId.
   *
   * @param placementId placementId, as defined in Unity Ads admin tools
   */
  override fun onUnityAdsShowClick(placementId: String?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

    mListener?.onRewardedVideoAdClicked()
  }

  /**
   * Callback triggered when the show operation completes successfully for a placementId.
   *
   * @param placementId placementId, as defined in Unity Ads admin tools
   * @param completionState If UnityAdsShowCompletionState.SKIPPED, the show operation completed after the user skipped the video playback
   *                        If UnityAdsShowCompletionState.COMPLETED, the show operation completed after the user allowed the video to play to completion before dismissing the ad
   */
  override fun onUnityAdsShowComplete(
    placementId: String?,
    completionState: UnityAdsShowCompletionState?
  ) {
    IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId completionState = $completionState")

    when (completionState) {
      UnityAdsShowCompletionState.SKIPPED -> mListener?.onRewardedVideoAdClosed()
      UnityAdsShowCompletionState.COMPLETED -> {
        mListener?.onRewardedVideoAdEnded()
        mListener?.onRewardedVideoAdRewarded()
        mListener?.onRewardedVideoAdClosed()
      }
      else -> {}
    }
  }
}
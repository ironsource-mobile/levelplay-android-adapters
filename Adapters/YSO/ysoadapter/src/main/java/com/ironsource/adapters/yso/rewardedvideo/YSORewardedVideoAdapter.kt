package com.ironsource.adapters.yso.rewardedvideo

import com.ironsource.adapters.yso.YSOAdapter
import com.ironsource.adapters.yso.YSOAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.ysocorp.ysonetwork.YsoNetwork
import org.json.JSONObject
import java.lang.ref.WeakReference

class YSORewardedVideoAdapter (adapter: YSOAdapter) :
  AbstractRewardedVideoAdapter<YSOAdapter>(adapter) {
  private var mSmashListener : RewardedVideoSmashListener? = null
  private var mYsoAdListener : YSORewardedVideoAdListener? = null
  private var mIsAdAvailable = false

  //Used for flows when the mediation needs to get a callback for init
  override fun initRewardedVideoWithCallback(
    appKey: String?,
    userId: String?,
    config: JSONObject,
    listener: RewardedVideoSmashListener
  ) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = getConfigStringValueFromKey(config, placementKeyId)

    if (placementKey.isNullOrEmpty()) {
      IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementKeyId))
      listener.onRewardedVideoInitFailed(
        ErrorBuilder.buildInitFailedError(
          getAdUnitIdMissingErrorString(placementKeyId),
          IronSourceConstants.REWARDED_VIDEO_AD_UNIT
        )
      )
      return
    }

    IronLog.ADAPTER_API.verbose("placementKey = $placementKey")

    //save rewardedVideo listener
    mSmashListener = listener

    when (adapter.getInitState()) {
      YSOAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
        listener.onRewardedVideoInitSuccess()
      }

      YSOAdapter.Companion.InitState.INIT_STATE_NONE,
      YSOAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
        adapter.initSdk(placementKey)
      }
      YSOAdapter.Companion.InitState.INIT_STATE_FAILED -> {
        listener.onRewardedVideoInitFailed(
          ErrorBuilder.buildInitFailedError(
            LOG_INIT_FAILED,
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT
          )
        )
      }
    }
  }

  override fun onNetworkInitCallbackSuccess() {
    mSmashListener?.onRewardedVideoInitSuccess()
  }

  override fun onNetworkInitCallbackFailed(error: String?) {
    mSmashListener?.onRewardedVideoInitFailed(
      ErrorBuilder.buildInitFailedError(
        error,
        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
      )
    )
  }

  override fun loadRewardedVideoForBidding(
    config: JSONObject,
    adData: JSONObject?,
    serverData: String?,
    listener: RewardedVideoSmashListener
  ) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = getConfigStringValueFromKey(config, placementKeyId)

    if (serverData.isNullOrEmpty()) {
      val error = "serverData is empty"
      IronLog.INTERNAL.error(error)
      listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
      listener.onRewardedVideoAvailabilityChanged(false)
      return
    }

    IronLog.ADAPTER_API.verbose("placementKey = $placementKey")

    setRewardedVideoAdAvailability(false)

    val rewardedVideoAdListener = YSORewardedVideoAdListener(listener, placementKey, WeakReference(this))
    mYsoAdListener = rewardedVideoAdListener

    YsoNetwork.rewardedLoad(placementKey, serverData, mYsoAdListener)
  }

  override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = getConfigStringValueFromKey(config, placementKeyId)
    IronLog.ADAPTER_API.verbose("placementKey = $placementKey")

    if (!isRewardedVideoAvailable(config)) {
      listener.onRewardedVideoAdShowFailed(
        ErrorBuilder.buildNoAdsToShowError(
          IronSourceConstants.REWARDED_VIDEO_AD_UNIT
        )
      )
    } else {
      setRewardedVideoAdAvailability(false)
      YsoNetwork.rewardedShow(placementKey, mYsoAdListener, ContextProvider.getInstance().currentActiveActivity)
    }
  }

  override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
   return mIsAdAvailable
  }

  override fun collectRewardedVideoBiddingData(
    config: JSONObject,
    adData: JSONObject?,
    biddingDataCallback: BiddingDataCallback
  ) {
    adapter.collectBiddingData(biddingDataCallback)
  }

  //region Helpers

  internal fun setRewardedVideoAdAvailability(isAvailable: Boolean) {
    mIsAdAvailable = isAvailable
  }

  override fun destroyRewardedVideoAd(config: JSONObject?) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = config?.let { getConfigStringValueFromKey(it, placementKeyId) }
    IronLog.ADAPTER_API.verbose("Destroy rewarded video ad of ${YSOAdapter.NETWORK_NAME}, placementKey = $placementKey")
    mYsoAdListener = null
    mSmashListener = null
  }

  //endregion

}
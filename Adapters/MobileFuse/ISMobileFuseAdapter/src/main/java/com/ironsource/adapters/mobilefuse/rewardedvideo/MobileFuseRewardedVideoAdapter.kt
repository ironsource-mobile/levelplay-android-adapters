package com.ironsource.adapters.mobilefuse.rewardedvideo

import com.ironsource.adapters.mobilefuse.MobileFuseAdapter
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.mobilefuse.sdk.MobileFuseRewardedAd
import org.json.JSONObject

class MobileFuseRewardedVideoAdapter(adapter: MobileFuseAdapter) :
    AbstractRewardedVideoAdapter<MobileFuseAdapter>(adapter) {

  private var mSmashListener : RewardedVideoSmashListener? = null
  private var mAdListener : MobileFuseRewardedVideoAdListener? = null
  private var mAd: MobileFuseRewardedAd? = null
  override fun initRewardedVideoWithCallback(
      appKey: String?,
      userId: String?,
      config: JSONObject,
      listener: RewardedVideoSmashListener
  ) {

    //save rewarded video listener
    mSmashListener = listener

    when (adapter.getInitState()) {
      MobileFuseAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
        listener.onRewardedVideoInitSuccess()
      }
      MobileFuseAdapter.Companion.InitState.INIT_STATE_FAILED -> {
        listener.onRewardedVideoInitFailed(
            ErrorBuilder.buildInitFailedError(
                LOG_INIT_FAILED,
                IronSourceConstants.REWARDED_VIDEO_AD_UNIT
            )
        )
      }
      MobileFuseAdapter.Companion.InitState.INIT_STATE_NONE,
      MobileFuseAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
        adapter.initSdk(config)
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
    IronLog.ADAPTER_API.verbose()

    if (serverData.isNullOrEmpty()) {
      val error = "serverData is empty"
      IronLog.INTERNAL.error(error)
      listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
      return
    }

    val rewardedVideoAdListener = MobileFuseRewardedVideoAdListener(listener)
    mAdListener = rewardedVideoAdListener

    val context = ContextProvider.getInstance().applicationContext
    val placementIdKey = MobileFuseAdapter.getPlacementIdKey()
    val placementId = getConfigStringValueFromKey(config, placementIdKey)
    mAd = MobileFuseRewardedAd(context, placementId)

    mAd?.let {
      mAd?.setListener(rewardedVideoAdListener)
      mAd?.loadAdFromBiddingToken(serverData)
    }?: run {
      listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
    }
  }

  override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
    IronLog.ADAPTER_API.verbose()
    mAd?.showAd()
  }

  override fun isRewardedVideoAvailable(config: JSONObject): Boolean = mAd?.isLoaded == true

  override fun collectRewardedVideoBiddingData(
      config: JSONObject,
      adData: JSONObject?,
      biddingDataCallback: BiddingDataCallback
  ) {
    adapter.collectBiddingData(biddingDataCallback)
  }

  override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
    IronLog.INTERNAL.verbose()
    destroyRewardedVideoAd()
    mAdListener = null
  }

  //endregion

  // region Helpers

  private fun destroyRewardedVideoAd() {
    mAd = null
    mSmashListener = null
  }

  //endregion
}
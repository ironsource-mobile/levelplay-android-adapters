package com.ironsource.adapters.yso.interstitial

import com.ironsource.adapters.yso.YSOAdapter
import com.ironsource.adapters.yso.YSOAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.ysocorp.ysonetwork.YsoNetwork
import org.json.JSONObject
import java.lang.ref.WeakReference

class YSOInterstitialAdapter (adapter:YSOAdapter) :
  AbstractInterstitialAdapter<YSOAdapter>(adapter) {
  private var mSmashListener : InterstitialSmashListener? = null
  private var mYsoAdListener : YSOInterstitialAdListener? = null
  private var mIsAdAvailable = false

  override fun initInterstitialForBidding(
    appKey: String?,
    userId: String?,
    config: JSONObject,
    listener: InterstitialSmashListener
  ) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = getConfigStringValueFromKey(config, placementKeyId)
    if (placementKey.isNullOrEmpty()) {
      IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementKeyId))
      listener.onInterstitialInitFailed(
        ErrorBuilder.buildInitFailedError(
          getAdUnitIdMissingErrorString(placementKeyId),
          IronSourceConstants.INTERSTITIAL_AD_UNIT
        )
      )
      return
    }

    IronLog.ADAPTER_API.verbose("placementKey = $placementKey")

    //save interstitial listener
    mSmashListener = listener

    when (adapter.getInitState()) {
      YSOAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
        listener.onInterstitialInitSuccess()
      }

      YSOAdapter.Companion.InitState.INIT_STATE_NONE,
      YSOAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
        adapter.initSdk(placementKey)
      }
      YSOAdapter.Companion.InitState.INIT_STATE_FAILED -> {
        listener.onInterstitialInitFailed(
          ErrorBuilder.buildInitFailedError(
            LOG_INIT_FAILED,
            IronSourceConstants.INTERSTITIAL_AD_UNIT
          )
        )
      }
    }
  }

  override fun onNetworkInitCallbackSuccess() {
    mSmashListener?.onInterstitialInitSuccess()
  }

  override fun onNetworkInitCallbackFailed(error: String?) {
    mSmashListener?.onInterstitialInitFailed(
      ErrorBuilder.buildInitFailedError(
        error,
        IronSourceConstants.INTERSTITIAL_AD_UNIT
      )
    )
  }

  override fun loadInterstitialForBidding(
    config: JSONObject,
    adData: JSONObject?,
    serverData: String?,
    listener: InterstitialSmashListener
  ) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = getConfigStringValueFromKey(config, placementKeyId)

    if (serverData.isNullOrEmpty()) {
      val error = "serverData is empty"
      IronLog.INTERNAL.error(error)
      listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
      return
    }

    IronLog.ADAPTER_API.verbose("placementKey = $placementKey")

    setInterstitialAdAvailability(false)

    val interstitialAdListener = YSOInterstitialAdListener(listener, placementKey, WeakReference(this))
    mYsoAdListener = interstitialAdListener

    YsoNetwork.interstitialLoad(placementKey, serverData, mYsoAdListener)
  }

  override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = getConfigStringValueFromKey(config, placementKeyId)
    IronLog.ADAPTER_API.verbose("placementKey= $placementKey")

    if (!isInterstitialReady(config)) {
      listener.onInterstitialAdShowFailed(
        ErrorBuilder.buildNoAdsToShowError(
          IronSourceConstants.INTERSTITIAL_AD_UNIT
        )
      )
    } else {
      setInterstitialAdAvailability(false)
      YsoNetwork.interstitialShow(placementKey, mYsoAdListener, ContextProvider.getInstance().currentActiveActivity)
    }
  }

  override fun isInterstitialReady(config: JSONObject): Boolean {
    return mIsAdAvailable
  }

  override fun collectInterstitialBiddingData(
    config: JSONObject,
    adData: JSONObject?,
    biddingDataCallback: BiddingDataCallback
  ) {
    adapter.collectBiddingData(biddingDataCallback)
  }

  //region Helpers
  internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
    mIsAdAvailable = isAvailable
  }

  override fun destroyInterstitialAd(config: JSONObject?) {
    val placementKeyId = YSOAdapter.getPlacementKeyId()
    val placementKey = config?.let { getConfigStringValueFromKey(it, placementKeyId) }
    IronLog.ADAPTER_API.verbose("Destroy interstitial ad of ${YSOAdapter.NETWORK_NAME}, placementKey = $placementKey")
    mYsoAdListener = null
    mSmashListener = null
  }

  //endregion

}
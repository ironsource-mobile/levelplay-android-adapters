package com.ironsource.adapters.mobilefuse.interstitial

import com.ironsource.adapters.mobilefuse.MobileFuseAdapter
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.mobilefuse.sdk.MobileFuseInterstitialAd
import org.json.JSONObject

class MobileFuseInterstitialAdapter (adapter: MobileFuseAdapter) :
    AbstractInterstitialAdapter<MobileFuseAdapter>(adapter) {

  private var mSmashListener : InterstitialSmashListener? = null
  private var mAdListener : MobileFuseInterstitialAdListener? = null
  private var mAd: MobileFuseInterstitialAd? = null

  //regin Interstitial API

  override fun initInterstitialForBidding(
      appKey: String?,
      userId: String?,
      config: JSONObject,
      listener: InterstitialSmashListener
  ) {

    IronLog.ADAPTER_API.verbose()

    //save interstitial listener
    mSmashListener = listener

    when (adapter.getInitState()) {
      MobileFuseAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
        listener.onInterstitialInitSuccess()
      }
      MobileFuseAdapter.Companion.InitState.INIT_STATE_FAILED -> {
        listener.onInterstitialInitFailed(
            ErrorBuilder.buildInitFailedError(
                LOG_INIT_FAILED,
                IronSourceConstants.INTERSTITIAL_AD_UNIT
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
    IronLog.ADAPTER_API.verbose()

    if (serverData.isNullOrEmpty()) {
      val error = "serverData is empty"
      IronLog.INTERNAL.error(error)
      listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
      return
    }

    val interstitialAdListener = MobileFuseInterstitialAdListener(listener)
    mAdListener = interstitialAdListener

    val context = ContextProvider.getInstance().applicationContext
    val placementIdKey = MobileFuseAdapter.getPlacementIdKey()
    val placementId = getConfigStringValueFromKey(config, placementIdKey)
    mAd = MobileFuseInterstitialAd(context, placementId)

    mAd?.let {
      mAd?.setListener(interstitialAdListener)
      mAd?.loadAdFromBiddingToken(serverData)
    }?: run {
      listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
    }
  }

  override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
    IronLog.ADAPTER_API.verbose()

    if (!isInterstitialReady(config)) {
      listener.onInterstitialAdShowFailed(
          ErrorBuilder.buildNoAdsToShowError(
              IronSourceConstants.INTERSTITIAL_AD_UNIT
          )
      )
      return
    }
    mAd?.showAd()
  }

  override fun isInterstitialReady(config: JSONObject): Boolean = mAd?.isLoaded == true

  override fun collectInterstitialBiddingData(
      config: JSONObject,
      adData: JSONObject?,
      biddingDataCallback: BiddingDataCallback
  ) {
    adapter.collectBiddingData(biddingDataCallback)
  }

  //region memory handling

  override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
    IronLog.INTERNAL.verbose()
    destroyInterstitialAd()
    mAdListener = null
    mSmashListener = null

  }

  //endregion

  // region Helpers

  private fun destroyInterstitialAd() {
    mAd = null
  }

  //endregion
}
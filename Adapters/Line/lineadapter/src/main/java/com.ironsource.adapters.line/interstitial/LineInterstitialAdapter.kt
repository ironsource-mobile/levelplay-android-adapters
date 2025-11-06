package com.ironsource.adapters.line.interstitial

import com.five_corp.ad.AdLoader
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdInterstitial
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.adapters.line.LineAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import java.lang.ref.WeakReference

class LineInterstitialAdapter(adapter: LineAdapter) : AbstractInterstitialAdapter<LineAdapter>(adapter) {
  private var mSmashListener : InterstitialSmashListener? = null
  private var mAdListener : LineInterstitialAdListener? = null
  private var mInterstitialAd: FiveAdInterstitial? = null
  private var mAdLoader: AdLoader? = null

  //region Interstitial API

  override fun initInterstitialForBidding(
          appKey: String?,
          userId: String?,
          config: JSONObject,
          listener: InterstitialSmashListener
  ) {

    val appIdKey = LineAdapter.getAppIdKey()
    val appId = config.optString(appIdKey)

    if (appId.isNullOrEmpty()) {
      IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appIdKey))
      listener.onInterstitialInitFailed(
        ErrorBuilder.buildInitFailedError(
          getAdUnitIdMissingErrorString(appIdKey),
          IronSourceConstants.INTERSTITIAL_AD_UNIT
        )
      )
      return
    }

    val slotIdKey = LineAdapter.getSlotIdKey()
    val slotId = config.optString(slotIdKey)

    if (slotId.isNullOrEmpty()) {
      IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(slotIdKey))
      listener.onInterstitialInitFailed(
        ErrorBuilder.buildInitFailedError(
          getAdUnitIdMissingErrorString(slotIdKey),
          IronSourceConstants.INTERSTITIAL_AD_UNIT
        )
      )
      return
    }

    IronLog.ADAPTER_API.verbose("appId = $appId, slotId = $slotId")

    //save interstitial listener
    mSmashListener = listener

    when (adapter.getInitState()) {
      LineAdapter.Companion.InitState.INIT_STATE_NONE -> {
        adapter.initSdk(config)
      }
      LineAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
          listener.onInterstitialInitSuccess()
      }
      LineAdapter.Companion.InitState.INIT_STATE_FAILED -> {
        listener.onInterstitialInitFailed(
          ErrorBuilder.buildInitFailedError(
            LOG_INIT_FAILED,
            IronSourceConstants.INTERSTITIAL_AD_UNIT
          )
        )}
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
  ){
    IronLog.ADAPTER_API.verbose()
    val appIdKey = LineAdapter.getAppIdKey()
    val appId = config.optString(appIdKey)

    if (serverData.isNullOrEmpty()) {
      val error = "serverData is empty"
      IronLog.INTERNAL.error(error)
      listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
      return
    }

    val interstitialAdListener = LineInterstitialAdListener(listener, WeakReference(this))
    mAdListener = interstitialAdListener

    val adLoader = LineAdapter.getAdLoader(appId)
    mAdLoader = adLoader
    val bidData = BidData(serverData, null)
    adLoader?.loadInterstitialAd(bidData, interstitialAdListener)
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
      } else {
        mAdListener?.let { mInterstitialAd?.setEventListener(it) }
        mInterstitialAd?.showAd()
      }
  }

  override fun isInterstitialReady(config: JSONObject): Boolean {
    return mInterstitialAd != null
  }

  override fun collectInterstitialBiddingData(
          config: JSONObject,
          adData: JSONObject?,
          biddingDataCallback: BiddingDataCallback
  ) {
    adapter.collectBiddingData(biddingDataCallback, config)
  }

  // region Helpers

  internal fun setInterstitialAd(interstitialAd: FiveAdInterstitial) {
    mInterstitialAd = interstitialAd
  }

  internal fun destroyInterstitialAd() {
    mInterstitialAd = null
    mAdLoader = null
  }

  //endregion

  }

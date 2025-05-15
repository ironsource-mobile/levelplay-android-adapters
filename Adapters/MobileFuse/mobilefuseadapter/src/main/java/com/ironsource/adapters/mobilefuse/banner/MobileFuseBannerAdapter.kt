package com.ironsource.adapters.mobilefuse.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.mobilefuse.sdk.MobileFuseBannerAd
import com.mobilefuse.sdk.MobileFuseBannerAd.AdSize
import org.json.JSONObject

class MobileFuseBannerAdapter(adapter: MobileFuseAdapter) :
    AbstractBannerAdapter<MobileFuseAdapter>(adapter) {
  private var mSmashListener : BannerSmashListener? = null
  private var mAdListener : MobileFuseBannerAdListener? = null
  private var mAdView: MobileFuseBannerAd? = null

  //region Banner API

  override fun initBannerForBidding(
      appKey: String?,
      userId: String?,
      config: JSONObject,
      listener: BannerSmashListener
  ) {

    IronLog.ADAPTER_API.verbose()

    //save banner listener
    mSmashListener = listener

    when (adapter.getInitState()) {
      MobileFuseAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
        listener.onBannerInitSuccess()
      }
      MobileFuseAdapter.Companion.InitState.INIT_STATE_FAILED -> {
        listener.onBannerInitFailed(
            ErrorBuilder.buildInitFailedError(
                LOG_INIT_FAILED,
                IronSourceConstants.BANNER_AD_UNIT
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
    mSmashListener?.onBannerInitSuccess()
  }

  override fun onNetworkInitCallbackFailed(error: String?) {
    mSmashListener?.onBannerInitFailed(
        ErrorBuilder.buildInitFailedError(
            error,
            IronSourceConstants.BANNER_AD_UNIT
        )
    )
  }

  override fun loadBannerForBidding(
      config: JSONObject,
      adData: JSONObject?,
      serverData: String?,
      banner: IronSourceBannerLayout,
      listener: BannerSmashListener
  ) {
    IronLog.ADAPTER_API.verbose()

    val bannerSize = getBannerSize(banner.size,
      AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext))

    if (bannerSize == null) {
      listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
      return
    }

    if (serverData.isNullOrEmpty()) {
      val error = "serverData is empty"
      IronLog.INTERNAL.error(error)
      listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
      return
    }

    val context = ContextProvider.getInstance().applicationContext
    val placementIdKey = MobileFuseAdapter.getPlacementIdKey()
    val placementId = getConfigStringValueFromKey(config, placementIdKey)
    val layoutParams = FrameLayout.LayoutParams(
        AdapterUtils.dpToPixels(context, bannerSize.width),
        AdapterUtils.dpToPixels(context, bannerSize.height),
        Gravity.CENTER
    )

    val bannerAd = MobileFuseBannerAd(
      context,
      placementId,
      bannerSize
    )
    mAdView = bannerAd

    val bannerAdListener = MobileFuseBannerAdListener(
      listener,
      bannerAd,
      layoutParams
    )

    mAdListener = bannerAdListener
    bannerAd.setListener(mAdListener)
    bannerAd.autorefreshEnabled = false
    bannerAd.setMuted(true)
    bannerAd.loadAdFromBiddingToken(serverData)
  }

  override fun collectBannerBiddingData(
      config: JSONObject,
      adData: JSONObject?,
      biddingDataCallback: BiddingDataCallback
  ) {
    adapter.collectBiddingData(biddingDataCallback)
  }

  //endregion

  //region memory handling

  override fun releaseMemory(
      adUnit: IronSource.AD_UNIT,
      config: JSONObject?
  ) {
    IronLog.INTERNAL.verbose()
    destroyBannerViewAd()
    mSmashListener = null
    mAdListener = null
  }

  //endregion

  // region Helpers

  override fun destroyBanner(config: JSONObject) {
    IronLog.ADAPTER_API.verbose()
    destroyBannerViewAd()
  }

  private fun destroyBannerViewAd() {
    postOnUIThread {
      mAdView?.setListener(null)
      mAdView?.destroy()
      mAdView = null
    }
  }

  private fun getBannerSize(size: ISBannerSize?, isLargeScreen: Boolean): AdSize? {
    return when (size?.description) {
      ISBannerSize.BANNER.description -> AdSize.BANNER_320x50
      ISBannerSize.RECTANGLE.description-> AdSize.BANNER_300x250
      ISBannerSize.SMART.description -> if (isLargeScreen) AdSize.BANNER_728x90 else AdSize.BANNER_320x50
      else -> null
    }
  }

  //endregion

}
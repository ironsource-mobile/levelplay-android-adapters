package com.ironsource.adapters.unityads

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_TRAITS
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_TRAITS_ENABLE_NEW_API
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.unity3d.ads.AdFormat
import com.unity3d.ads.TokenConfiguration
import com.unity3d.services.banners.UnityBannerSize
import org.json.JSONObject

interface NetworkBridge {

  fun initSdk(config: JSONObject?, debugEnabled: Boolean, listener: INetworkInitCallbackListener)

  /**********************
   * Rewarded apis
   **********************/
  fun loadRewardedAd(
    placementId: String, serverData: String?, listener: RewardedVideoSmashListener?
  )

  fun showRewardedAd(
    placementId: String, dynamicUserId: String?, listener: RewardedVideoSmashListener?
  )

  fun isRewardedAdAvailable(placementId: String): Boolean

  /**********************
   * Interstitial apis
   **********************/
  fun loadInterstitialAd(
    placementId: String, serverData: String?, listener: InterstitialSmashListener?
  )

  fun showInterstitialAd(placementId: String, listener: InterstitialSmashListener?)

  fun isInterstitialAdAvailable(placementId: String): Boolean

  fun isInterstitialAdReady(placementId: String): Boolean

  /**********************
   * Banner apis
   **********************/

  fun loadBanner(
    placementId: String,
    adData: JSONObject?,
    serverData: String?,
    bannerSize: UnityBannerSize?,
    listener: BannerSmashListener?
  )

  fun destroyBanner(placementId: String)

  /**********************
   * others
   **********************/
  fun getTokenConfig(
    adFormat: AdFormat,
    config: JSONObject? = null,
    adData: JSONObject? = null,
  ): TokenConfiguration

  fun createLayoutParams(size: UnityBannerSize): FrameLayout.LayoutParams {
    val widthPixel =
      AdapterUtils.dpToPixels(ContextProvider.getInstance().applicationContext, size.width)
    return FrameLayout.LayoutParams(
      widthPixel, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
    )
  }

  class Factory {
    fun getNetworkBridge(providerName: String, config: JSONObject?): NetworkBridge {
      if (config?.has(UADS_TRAITS) == true) {
        val traits = config.optJSONObject(UADS_TRAITS)
        if (traits?.optBoolean(UADS_TRAITS_ENABLE_NEW_API, false) == true) {
          return BoldNetworkBridge(providerName)
        }
      }
      return LegacyNetworkBridge(providerName)
    }
  }
}
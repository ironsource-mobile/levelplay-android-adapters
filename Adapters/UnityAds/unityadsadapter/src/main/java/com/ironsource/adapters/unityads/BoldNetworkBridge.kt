package com.ironsource.adapters.unityads

import com.ironsource.adapters.unityads.UnityAdsAdapter.Companion.errorReporter
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.BANNER_SIZE
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.GAME_ID
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.MEDIATION_AD_UNIT
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.MEDIATION_NAME
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.PLACEMENT_ID
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_AD_DATA_AD_UNIT_ID
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_INIT_BLOB
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_TRAITS
import com.ironsource.adapters.unityads.bold.UnityAdBannerAdLoadListener
import com.ironsource.adapters.unityads.bold.UnityAdBannerAdShowListener
import com.ironsource.adapters.unityads.bold.UnityAdsInterstitialAdLoadListener
import com.ironsource.adapters.unityads.bold.UnityAdsInterstitialShowAdListener
import com.ironsource.adapters.unityads.bold.UnityAdsRewardedAdLoadListener
import com.ironsource.adapters.unityads.bold.UnityAdsRewardedAdShowListener
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.AdFormat
import com.unity3d.ads.BannerAd
import com.unity3d.ads.BannerConfiguration
import com.unity3d.ads.BannerSize
import com.unity3d.ads.BuildConfig
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.InterstitialAd
import com.unity3d.ads.LoadConfiguration
import com.unity3d.ads.LogLevel
import com.unity3d.ads.MediationInfo
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.ShowConfiguration
import com.unity3d.ads.TokenConfiguration
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import com.unity3d.mediation.LevelPlay
import com.unity3d.services.banners.UnityBannerSize
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnityAdsExperimental::class)
class BoldNetworkBridge(private val providerName: String) : NetworkBridge {

  companion object {
    const val BOLD_NO_FILL_ERROR_CODE = 52100
  }

  private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

  private val rewardedAdPlacementIdToLoadedAdObject: ConcurrentHashMap<String, RewardedAd> =
    ConcurrentHashMap()
  private val interstitialAdPlacementIdToLoadedAdObject: ConcurrentHashMap<String, InterstitialAd> =
    ConcurrentHashMap()
  private val bannerAdPlacementIdToLoadedAdObject: ConcurrentHashMap<String, BannerAd> =
    ConcurrentHashMap()

  override fun initSdk(
    config: JSONObject?, debugEnabled: Boolean, listener: INetworkInitCallbackListener
  ) {
    val gameId = config?.optString(GAME_ID) ?: return

    if (!UnityAds.isInitialized) {
      initCallbackListeners.add(listener)
    }

    val extras = mutableMapOf<String, String>()
    // uads init blob
    if (config.has(UADS_INIT_BLOB)) {
      extras[UADS_INIT_BLOB] = config.optString(UADS_INIT_BLOB)
    }

    // uads traits
    if (config.has(UADS_TRAITS)) {
      val traits = config[UADS_TRAITS] as? JSONObject
      traits?.keys()?.forEach { key ->
        traits.optString(key).let { value ->
          if (!value.isNullOrEmpty()) {
            extras[key] = value
          }
        }
      }
    }

    val initConfig = InitializationConfiguration.Builder(gameId).withTestMode(false)
      .withMediationInfo(getMediationInfo()).apply {
        if (debugEnabled) withLogLevel(LogLevel.DEBUG)
        if (extras.isNotEmpty()) withExtras(extras)
      }.build()

    val initListener = InitializationListener { error ->
      if (error == null) {
        onInitSuccess()
      } else {
        onInitFail(error)
      }
    }

    UnityAds.initialize(initConfig, initListener)
  }

  private fun onInitSuccess() {
    IronLog.ADAPTER_CALLBACK.verbose()
    initCallbackListeners.forEach { it.onNetworkInitCallbackSuccess() }
    initCallbackListeners.clear()
  }

  private fun onInitFail(error: UnityAdsError) {
    val initError = "${error.code} ${error.message}"
    IronLog.ADAPTER_CALLBACK.verbose("initError = $initError")
    initCallbackListeners.forEach { it.onNetworkInitCallbackFailed(initError) }
    initCallbackListeners.clear()
  }

  /**********************
   * Rewarded apis
   **********************/

  override fun loadRewardedAd(
    placementId: String, serverData: String?, listener: RewardedVideoSmashListener?
  ) {
    val loadConfigurationBuilder =
      LoadConfiguration.Builder(placementId).withMediationInfo(getMediationInfo())
    if (!serverData.isNullOrEmpty()) {
      // add adMarkup for bidder instances
      loadConfigurationBuilder.withAdMarkup(serverData)
    }
    val loadConfiguration = loadConfigurationBuilder.build()

    RewardedAd.load(loadConfiguration, UnityAdsRewardedAdLoadListener(
      providerName,
      placementId,
      WeakReference(listener),
      errorReporter,
    ) { ad ->
      rewardedAdPlacementIdToLoadedAdObject[placementId] = ad
    })
  }

  override fun showRewardedAd(
    placementId: String, dynamicUserId: String?, listener: RewardedVideoSmashListener?
  ) {
    val rewardedAd = rewardedAdPlacementIdToLoadedAdObject.remove(placementId)

    if (rewardedAd == null) {
      listener?.onRewardedVideoAdShowFailed(
        ErrorBuilder.buildNoAdsToShowError(
          IronSourceConstants.REWARDED_VIDEO_AD_UNIT
        )
      )

      return
    }

    val showConfiguration = ShowConfiguration.Builder().apply {
      dynamicUserId?.let { withCustomRewardString(it) }
    }.build()

    rewardedAd.show(
      ContextProvider.getInstance().currentActiveActivity,
      showConfiguration,
      UnityAdsRewardedAdShowListener(placementId, WeakReference(listener), errorReporter)
    )
  }

  override fun isRewardedAdAvailable(placementId: String): Boolean {
    return (placementId.isNotEmpty() && rewardedAdPlacementIdToLoadedAdObject.containsKey(
      placementId
    ))
  }

  /**********************
   * Interstitial apis
   **********************/

  override fun loadInterstitialAd(
    placementId: String, serverData: String?, listener: InterstitialSmashListener?
  ) {
    val loadConfigurationBuilder =
      LoadConfiguration.Builder(placementId).withMediationInfo(getMediationInfo())
    if (!serverData.isNullOrEmpty()) {
      // add adMarkup for bidder instances
      loadConfigurationBuilder.withAdMarkup(serverData)
    }
    val loadConfiguration = loadConfigurationBuilder.build()

    InterstitialAd.load(loadConfiguration, UnityAdsInterstitialAdLoadListener(
      providerName,
      placementId,
      WeakReference(listener),
      errorReporter,
    ) { ad ->
      interstitialAdPlacementIdToLoadedAdObject[placementId] = ad
    })
  }

  override fun showInterstitialAd(placementId: String, listener: InterstitialSmashListener?) {
    val interstitialAd = interstitialAdPlacementIdToLoadedAdObject.remove(placementId)

    if (interstitialAd == null) {
      listener?.onInterstitialAdShowFailed(
        ErrorBuilder.buildNoAdsToShowError(
          IronSourceConstants.INTERSTITIAL_AD_UNIT
        )
      )
      return
    }

    val showConfiguration = ShowConfiguration.Builder().build()

    interstitialAd.show(
      ContextProvider.getInstance().currentActiveActivity,
      showConfiguration,
      UnityAdsInterstitialShowAdListener(placementId, WeakReference(listener), errorReporter)
    )
  }

  override fun isInterstitialAdAvailable(placementId: String): Boolean {
    return (placementId.isNotEmpty() && interstitialAdPlacementIdToLoadedAdObject.containsKey(
      placementId
    ))
  }

  override fun isInterstitialAdReady(placementId: String): Boolean {
    return (placementId.isNotEmpty() && interstitialAdPlacementIdToLoadedAdObject.containsKey(
      placementId
    ))
  }

  /**********************
   * Banner apis
   **********************/

  override fun loadBanner(
    placementId: String,
    adData: JSONObject?,
    serverData: String?,
    bannerSize: UnityBannerSize?,
    listener: BannerSmashListener?
  ) {
    if (bannerSize == null) return

    val bannerListener =
      UnityAdBannerAdShowListener(placementId, WeakReference(listener), errorReporter)
    val bannerLoadConfiguration = BannerConfiguration.Builder(
      placementId, BannerSize(bannerSize.width, bannerSize.height), bannerListener
    ).apply {
      if (!serverData.isNullOrEmpty()) {
        // add adMarkup for bidder instances
        withAdMarkup(serverData)
      }
    }.withMediationInfo(getMediationInfo()).apply {
      adData?.optString(UADS_AD_DATA_AD_UNIT_ID)?.let {
        if (it.isNotEmpty()) {
          withMediationAdUnitId(it)
        }
      }
    }.build()

    BannerAd.load(bannerLoadConfiguration, UnityAdBannerAdLoadListener(
      providerName,
      placementId,
      WeakReference(listener),
      { createLayoutParams(bannerSize) },
      errorReporter
    ) { ad ->
      bannerAdPlacementIdToLoadedAdObject[placementId] = ad
    })
  }

  override fun destroyBanner(placementId: String) {
    bannerAdPlacementIdToLoadedAdObject.remove(placementId)
  }

  /**********************
   * Others
   **********************/
  override fun getTokenConfig(
    adFormat: AdFormat,
    config: JSONObject?,
    adData: JSONObject?,
  ): TokenConfiguration {
    val builder = TokenConfiguration.Builder(adFormat)
      .withMediationInfo(getMediationInfo())

    val placement = config?.optString(PLACEMENT_ID)
    if(!placement.isNullOrBlank()) {
      builder.withPlacementId(placement)
    }

    val mediationAdUnit = adData?.optString(MEDIATION_AD_UNIT)
    if(!mediationAdUnit.isNullOrBlank()) {
      builder.withMediationAdUnitId(mediationAdUnit)
    }

    val bannerSize = adData?.opt(BANNER_SIZE) as? ISBannerSize
    if(adFormat == AdFormat.BANNER && bannerSize != null){
      builder.withBannerSize(BannerSize(width = bannerSize.width, height =  bannerSize.height))
    }

    return builder.build()
  }

  private fun getMediationInfo(): MediationInfo {
    return MediationInfo(
      MEDIATION_NAME, LevelPlay.getSdkVersion(), BuildConfig.VERSION_NAME
    )
  }

}
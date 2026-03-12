package com.ironsource.adapters.unityads

import com.ironsource.adapters.unityads.UnityAdsAdapter.Companion.errorReporter
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.ADAPTER_VERSION_KEY
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.GAME_ID
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.MEDIATION_NAME
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_INIT_BLOB
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UADS_TRAITS
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.AdFormat
import com.unity3d.ads.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.TokenConfiguration
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.ads.metadata.MediationMetaData
import com.unity3d.ads.metadata.PlayerMetaData
import com.unity3d.mediation.LevelPlay
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LegacyNetworkBridge(val providerName: String): NetworkBridge, IUnityAdsInitializationListener {

  // handle init callback for all adapter instances
  private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

  // Rewarded Ad collections
  private val placementIdToRewardedVideoAdListener: ConcurrentHashMap<String, UnityAdsRewardedVideoAdListener> = ConcurrentHashMap()
  private val rewardedVideoPlacementIdToLoadedAdObjectId: ConcurrentHashMap<String, String> = ConcurrentHashMap()
  private val placementIdToRewardedVideoAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

  // Interstitial maps
  private val placementIdToInterstitialAdListener: ConcurrentHashMap<String, UnityAdsInterstitialAdListener> = ConcurrentHashMap()
  private val interstitialPlacementIdToLoadedAdObjectId: ConcurrentHashMap<String, String> = ConcurrentHashMap()
  private val placementIdToInterstitialAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

  // Banner maps
  private val placementIdToBannerAdListener: ConcurrentHashMap<String, UnityAdsBannerAdListener> = ConcurrentHashMap()
  private val placementIdToBannerAd: ConcurrentHashMap<String, BannerView?> = ConcurrentHashMap()

  private val unityAdsStorageLock = Any()

  override fun initSdk(config: JSONObject?, debugEnabled: Boolean, listener: INetworkInitCallbackListener) {
    val gameId = config?.optString(GAME_ID) ?: return

    if (!UnityAds.isInitialized) {
      initCallbackListeners.add(listener)
    }

    synchronized(unityAdsStorageLock) {
      val mediationMetaData = MediationMetaData(ContextProvider.getInstance().applicationContext)
      mediationMetaData.setName(MEDIATION_NAME)
      // mediation version
      mediationMetaData.setVersion(LevelPlay.getSdkVersion())
      // adapter version
      mediationMetaData[ADAPTER_VERSION_KEY] = BuildConfig.VERSION_NAME

      // uads init blob
      if (config.has(UADS_INIT_BLOB)) {
        mediationMetaData.set(UADS_INIT_BLOB, config.optString(UADS_INIT_BLOB))
      }

      // uads traits
      if (config.has(UADS_TRAITS)) {
        mediationMetaData.set(UADS_TRAITS, config.optJSONObject(UADS_TRAITS))
      }

      mediationMetaData.commit()
    }

    UnityAds.debugMode = debugEnabled
    UnityAds.initialize(ContextProvider.getInstance().applicationContext, gameId, false, this)
  }


  override fun onInitializationComplete() {
    IronLog.ADAPTER_CALLBACK.verbose()
    initCallbackListeners.forEach { it.onNetworkInitCallbackSuccess() }
    initCallbackListeners.clear()
  }

  override fun onInitializationFailed(
    error: UnityAds.UnityAdsInitializationError?,
    message: String?
  ) {
    val initError = getUnityAdsInitializationErrorCode(error).toString() + message
    IronLog.ADAPTER_CALLBACK.verbose("initError = $initError")
    initCallbackListeners.forEach { it.onNetworkInitCallbackFailed(initError) }
    initCallbackListeners.clear()
  }

  // Region Rewarded Ads

  override fun loadRewardedAd(
    placementId: String, serverData: String?, listener: RewardedVideoSmashListener?
  ) {
    val rewardedAdListener = UnityAdsRewardedVideoAdListener(listener, WeakReference(this), placementId, errorReporter)
    placementIdToRewardedVideoAdListener[placementId] = rewardedAdListener

    val loadOptions = UnityAdsLoadOptions()

    // objectId is used to identify a loaded ad and to show it
    val mObjectId = UUID.randomUUID().toString()
    loadOptions.objectId = mObjectId

    if (!serverData.isNullOrEmpty()) {
      // add adMarkup for bidder instances
      loadOptions.setAdMarkup(serverData)
    }

    rewardedVideoPlacementIdToLoadedAdObjectId[placementId] = mObjectId
    UnityAds.load(placementId, loadOptions, rewardedAdListener)
  }

  override fun showRewardedAd(placementId: String, dynamicUserId: String?, listener: RewardedVideoSmashListener?) {
    if (isRewardedAdAvailable(placementId)) {
      if (!dynamicUserId.isNullOrEmpty()) {
        synchronized(unityAdsStorageLock) {
          val playerMetaData = PlayerMetaData(ContextProvider.getInstance().applicationContext)
          playerMetaData.setServerId(dynamicUserId)
          playerMetaData.commit()
        }
      }

      val rewardedVideoAdListener = placementIdToRewardedVideoAdListener[placementId]
      val objectId = rewardedVideoPlacementIdToLoadedAdObjectId[placementId]
      val showOptions = UnityAdsShowOptions()
      showOptions.objectId = objectId
      UnityAds.show(ContextProvider.getInstance().currentActiveActivity, placementId, showOptions, rewardedVideoAdListener)
    } else {
      listener?.onRewardedVideoAdShowFailed(
        ErrorBuilder.buildNoAdsToShowError(
          IronSourceConstants.REWARDED_VIDEO_AD_UNIT
        )
      )
    }

    setRewardedVideoAdAvailability(placementId, false)
  }

  override fun isRewardedAdAvailable(placementId: String): Boolean {
    return (placementId.isNotEmpty() &&
        placementIdToRewardedVideoAdAvailability.containsKey(placementId) &&
        (placementIdToRewardedVideoAdAvailability[placementId] == true))
  }

  fun setRewardedVideoAdAvailability(placementId: String, availability: Boolean) {
    placementIdToRewardedVideoAdAvailability[placementId] = availability
  }

  // End Region Rewarded ads

  // Region Interstitial ads

  override fun loadInterstitialAd(
    placementId: String,
    serverData: String?,
    listener: InterstitialSmashListener?
  ) {
    setInterstitialAdAvailability(placementId, false)

    val interstitialAdListener = UnityAdsInterstitialAdListener(listener, WeakReference(this), placementId, errorReporter)
    placementIdToInterstitialAdListener[placementId] = interstitialAdListener

    val loadOptions = UnityAdsLoadOptions()
    // objectId is used to identify a loaded ad and to show it
    val mObjectId = UUID.randomUUID().toString()
    loadOptions.objectId = mObjectId

    if (!serverData.isNullOrEmpty()) {
      // add adMarkup for bidder instances
      loadOptions.setAdMarkup(serverData)
    }

    interstitialPlacementIdToLoadedAdObjectId[placementId] = mObjectId
    UnityAds.load(placementId, loadOptions, interstitialAdListener)
  }

  override fun showInterstitialAd(placementId: String, listener: InterstitialSmashListener?) {
    if (isInterstitialAdReady(placementId)) {
      val interstitialAdListener = placementIdToInterstitialAdListener[placementId]
      val objectId = interstitialPlacementIdToLoadedAdObjectId[placementId]
      val showOptions = UnityAdsShowOptions()
      showOptions.objectId = objectId
      UnityAds.show(ContextProvider.getInstance().currentActiveActivity, placementId, showOptions, interstitialAdListener)
    } else {
      listener?.onInterstitialAdShowFailed(
        ErrorBuilder.buildNoAdsToShowError(
          IronSourceConstants.INTERSTITIAL_AD_UNIT
        )
      )
    }

    setInterstitialAdAvailability(placementId, false)
  }

  override fun isInterstitialAdReady(placementId: String): Boolean {
    return (placementId.isNotEmpty() &&
        placementIdToInterstitialAdAvailability.containsKey(placementId) &&
        (placementIdToInterstitialAdAvailability[placementId] == true))
  }

  override fun isInterstitialAdAvailable(placementId: String): Boolean {
    return (placementId.isNotEmpty() &&
        placementIdToInterstitialAdAvailability.containsKey(placementId) &&
        (placementIdToInterstitialAdAvailability[placementId] == true))
  }

  fun setInterstitialAdAvailability(placementId: String, availability: Boolean) {
    placementIdToInterstitialAdAvailability[placementId] = availability
  }

  // End Region Interstitial ads

  // Region Banner ads

  override fun loadBanner(
    placementId: String,
    adData: JSONObject?,
    serverData: String?,
    bannerSize: UnityBannerSize?,
    listener: BannerSmashListener?
  ) {
    if(bannerSize == null) return

    // create banner
    val bannerView = getBannerView(bannerSize, placementId, listener)
    val loadOptions = UnityAdsLoadOptions()

    // objectId is used to identify a loaded ad and to show it
    val mObjectId = UUID.randomUUID().toString()
    loadOptions.objectId = mObjectId

    if (!serverData.isNullOrEmpty()) {
      // add adMarkup for bidder instances
      loadOptions.setAdMarkup(serverData)
    }

    // load
    bannerView.load(loadOptions)
  }

  private fun getBannerView(
    bannerSize: UnityBannerSize,
    placementId: String,
    listener: BannerSmashListener?
  ): BannerView {
    // Remove previously created banner view
    if (placementIdToBannerAd[placementId] != null) {
      placementIdToBannerAd[placementId]?.destroy()
      placementIdToBannerAd.remove(placementId)
    }

    // create banner
    val bannerView = BannerView(ContextProvider.getInstance().applicationContext,
      placementId, bannerSize)

    // add listener
    val bannerAdListener = UnityAdsBannerAdListener(listener, WeakReference(this), placementId, errorReporter)
    placementIdToBannerAdListener[placementId] = bannerAdListener
    bannerView.listener = bannerAdListener

    // add to map
    placementIdToBannerAd[placementId] = bannerView
    return bannerView
  }

  override fun destroyBanner(placementId: String) {
    if (placementIdToBannerAd[placementId] != null) {
      placementIdToBannerAd[placementId]?.destroy()
      placementIdToBannerAd.remove(placementId)
    }
  }

  override fun getTokenConfig(
    adFormat: AdFormat,
    config: JSONObject?,
    adData: JSONObject?,
  ): TokenConfiguration {
    return TokenConfiguration(adFormat)
  }

  // End Region Banner ads

  private fun getUnityAdsInitializationErrorCode(error: UnityAds.UnityAdsInitializationError?): Int {
    if (error != null) {
      for (e in UnityAds.UnityAdsInitializationError.values()) {
        if (e.name.equals(error.toString(), ignoreCase = true)) {
          return UnityAds.UnityAdsInitializationError.valueOf(error.toString()).ordinal
        }
      }
    }

    return IronSourceError.ERROR_CODE_GENERIC
  }

  fun getUnityAdsShowErrorCode(error: UnityAds.UnityAdsShowError): Int {
    for (e in UnityAds.UnityAdsShowError.values()) {
      if (e.name.equals(error.toString(), ignoreCase = true)) {
        return UnityAds.UnityAdsShowError.valueOf(error.toString()).ordinal
      }
    }

    return IronSourceError.ERROR_CODE_GENERIC
  }

  fun getUnityAdsLoadErrorCode(error: UnityAds.UnityAdsLoadError): Int {
    for (e in UnityAds.UnityAdsLoadError.values()) {
      if (e.name.equals(error.toString(), ignoreCase = true)) {
        return UnityAds.UnityAdsLoadError.valueOf(error.toString()).ordinal
      }
    }

    return IronSourceError.ERROR_CODE_GENERIC
  }
}
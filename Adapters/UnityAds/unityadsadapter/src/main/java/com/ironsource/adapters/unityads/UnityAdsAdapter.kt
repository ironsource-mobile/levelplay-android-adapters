package com.ironsource.adapters.unityads

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource.AD_UNIT
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.ironsource.mediationsdk.utils.IronSourceUtils
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.ads.metadata.MediationMetaData
import com.unity3d.ads.metadata.MetaData
import com.unity3d.ads.metadata.PlayerMetaData
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UnityAdsAdapter(providerName: String) : AbstractAdapter(providerName),
    IUnityAdsInitializationListener, INetworkInitCallbackListener {

    // Rewarded video collections
    private val placementIdToRewardedVideoAdListener: ConcurrentHashMap<String, UnityAdsRewardedVideoListener> = ConcurrentHashMap()
    private val rewardedVideoPlacementIdToLoadedAdObjectId: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val placementIdToRewardedVideoAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    // Interstitial maps
    private val placementIdToInterstitialAdListener: ConcurrentHashMap<String, UnityAdsInterstitialListener> = ConcurrentHashMap()
    private val interstitialPlacementIdToLoadedAdObjectId: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val placementIdToInterstitialAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    // Banner maps
    private val placementIdToBannerAdListener: ConcurrentHashMap<String, UnityAdsBannerListener> = ConcurrentHashMap()
    private val placementIdToBannerAd: ConcurrentHashMap<String, BannerView?> = ConcurrentHashMap()

    // handle init callback for all adapter instances
    private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

    private val unityAdsStorageLock = Any()

    companion object {
        // UnityAds Mediation MetaData
        private const val MEDIATION_NAME = "ironSource"
        private const val ADAPTER_VERSION_KEY = "adapter_version"

        // UnityAds keys
        private const val GAME_ID = "sourceId"
        private const val PLACEMENT_ID = "zoneId"

        // Adapter version
        private const val VERSION = BuildConfig.VERSION_NAME
        private const val GitHash = BuildConfig.GitHash

        // Meta data flags
        private const val CONSENT_GDPR = "gdpr.consent"
        private const val CONSENT_CCPA = "privacy.consent"
        private const val UNITYADS_COPPA = "user.nonBehavioral"
        private const val UNITYADS_METADATA_COPPA_KEY = "unityads_coppa"
        private const val GAME_DESIGNATION = "mode"
        private const val MIXED_AUDIENCE = "mixed"
        private const val UADS_INIT_BLOB = "uads_init_blob"
        private const val UADS_TRAITS = "traits"

        // Feature flag key to disable the network's capability to load a Rewarded Video ad
        // while another Rewarded Video ad of that network is showing
        private const val LWS_SUPPORT_STATE = "isSupportedLWS"

        @JvmStatic
        fun startAdapter(providerName: String): UnityAdsAdapter {
            return UnityAdsAdapter(providerName)
        }

        // get the network and adapter integration data
        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData("UnityAds", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String = UnityAds.version
    }

    //region Adapter Methods

    // get adapter version
    override fun getVersion(): String = VERSION

    // get network sdk version
    override fun getCoreSDKVersion(): String = getAdapterSDKVersion()

    override fun isUsingActivityBeforeImpression(adUnit: AD_UNIT): Boolean = false

    //endregion

    //region Initializations methods and callbacks

    private fun initSDK(config: JSONObject) {
        val gameId = config.optString(GAME_ID)
        IronLog.ADAPTER_API.verbose("gameId = $gameId")

        if (!UnityAds.isInitialized) {
            initCallbackListeners.add(this)
        }

        synchronized(unityAdsStorageLock) {
            val mediationMetaData = MediationMetaData(ContextProvider.getInstance().applicationContext)
            mediationMetaData.setName(MEDIATION_NAME)
            // mediation version
            mediationMetaData.setVersion(IronSourceUtils.getSDKVersion())
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

        UnityAds.debugMode = isAdaptersDebugEnabled
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

    override fun onNetworkInitCallbackSuccess() = IronLog.ADAPTER_CALLBACK.verbose()

    override fun onNetworkInitCallbackFailed(error: String) = IronLog.ADAPTER_CALLBACK.verbose()

    //endregion

    //region Rewarded Video API

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: RewardedVideoSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val gameId = config?.optString(GAME_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $PLACEMENT_ID", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        if (gameId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $GAME_ID")
            listener?.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $GAME_ID", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!UnityAds.isInitialized) {
            initSDK(config)
        }

        listener?.onRewardedVideoInitSuccess()
    }

    // used for flows when the mediation doesn't need to get a callback for init
    override fun initAndLoadRewardedVideo(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val gameId = config?.optString(GAME_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onRewardedVideoAvailabilityChanged(false)
            return
        }

        if (gameId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $GAME_ID")
            listener?.onRewardedVideoAvailabilityChanged(false)
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!UnityAds.isInitialized) {
            initSDK(config)
        }

        loadRewardedVideoInternal(config, null, listener)
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject?,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener?
    ) {
        loadRewardedVideoInternal(config, serverData, listener)
    }

    override fun loadRewardedVideo(
        config: JSONObject?,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener?
    ) {
        loadRewardedVideoInternal(config, null, listener)
    }

    private fun loadRewardedVideoInternal(
        config: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, providerName, "Missing params - $PLACEMENT_ID"))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        setRewardedVideoAdAvailability(placementId, false)

        val rewardedVideoAdListener = UnityAdsRewardedVideoListener(listener, WeakReference(this), placementId)
        placementIdToRewardedVideoAdListener[placementId] = rewardedVideoAdListener
        val loadOptions = UnityAdsLoadOptions()

        // objectId is used to identify a loaded ad and to show it
        val mObjectId = UUID.randomUUID().toString()
        loadOptions.objectId = mObjectId

        if (!serverData.isNullOrEmpty()) {
            // add adMarkup for bidder instances
            loadOptions.setAdMarkup(serverData)
        }

        rewardedVideoPlacementIdToLoadedAdObjectId[placementId] = mObjectId
        UnityAds.load(placementId, loadOptions, rewardedVideoAdListener)
    }

    override fun showRewardedVideo(
        config: JSONObject?,
        listener: RewardedVideoSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, "Missing params - $PLACEMENT_ID"))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (isRewardedVideoAvailable(config)) {
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

    override fun isRewardedVideoAvailable(config: JSONObject?): Boolean {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        return (!placementId.isNullOrEmpty() &&
            placementIdToRewardedVideoAdAvailability.containsKey(placementId) &&
            (placementIdToRewardedVideoAdAvailability[placementId] == true))
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(config, biddingDataCallback)
    }

    // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
    override fun getLoadWhileShowSupportState(mAdUnitSettings: JSONObject): LoadWhileShowSupportState {
        var loadWhileShowSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
        val isSupportedLWS = mAdUnitSettings.optBoolean(LWS_SUPPORT_STATE, true)

        if (!isSupportedLWS) {
            loadWhileShowSupportState = LoadWhileShowSupportState.NONE
        }

        return loadWhileShowSupportState
    }

    //endregion

    //region Interstitial API

    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        initInterstitialInternal(config, listener)
    }

    override fun initInterstitial(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        initInterstitialInternal(config, listener)
    }

    private fun initInterstitialInternal(
        config: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val gameId = config?.optString(GAME_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $PLACEMENT_ID", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            return
        }

        if (gameId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $GAME_ID")
            listener?.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $GAME_ID", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!UnityAds.isInitialized) {
            initSDK(config)
        }

        listener?.onInterstitialInitSuccess()
    }

    override fun loadInterstitialForBidding(
        config: JSONObject?,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener?
    ) {
        loadInterstitialInternal(config, serverData, listener)
    }

    override fun loadInterstitial(
        config: JSONObject?,
        adData: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        loadInterstitialInternal(config, null, listener)
    }

    private fun loadInterstitialInternal(
        config: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, providerName, "Missing params - $PLACEMENT_ID"))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        setInterstitialAdAvailability(placementId, false)

        val interstitialAdListener = UnityAdsInterstitialListener(listener, WeakReference(this), placementId)
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

    override fun showInterstitial(
        config: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onInterstitialAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, "Missing params - $PLACEMENT_ID"))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (isInterstitialReady(config)) {
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

    override fun isInterstitialReady(config: JSONObject?): Boolean {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        return (!placementId.isNullOrEmpty() &&
            placementIdToInterstitialAdAvailability.containsKey(placementId) &&
            (placementIdToInterstitialAdAvailability[placementId] == true))
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(config, biddingDataCallback)
    }

    //endregion

    //region Banner API

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: BannerSmashListener?
    ) {
        initBannersInternal(config, listener)
    }

    override fun initBanners(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: BannerSmashListener?
    ) {
        initBannersInternal(config, listener)
    }

    private fun initBannersInternal(
        config: JSONObject?,
        listener: BannerSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val gameId = config?.optString(GAME_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $PLACEMENT_ID", IronSourceConstants.BANNER_AD_UNIT))
            return
        }

        if (gameId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $GAME_ID")
            listener?.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $GAME_ID", IronSourceConstants.BANNER_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!UnityAds.isInitialized) {
            initSDK(config)
        }

        listener?.onBannerInitSuccess()
    }

    override fun loadBannerForBidding(
        config: JSONObject?,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout?,
        listener: BannerSmashListener?
    ) {
        loadBannerInternal(config, banner, serverData, listener)
    }

    override fun loadBanner(
        config: JSONObject?,
        adData: JSONObject?,
        banner: IronSourceBannerLayout?,
        listener: BannerSmashListener?
    ) {
        loadBannerInternal(config, banner, null, listener)
    }

    private fun loadBannerInternal(
        config: JSONObject?,
        banner: IronSourceBannerLayout?,
        serverData: String?,
        listener: BannerSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.BANNER_AD_UNIT, providerName, "Missing params - $PLACEMENT_ID"))
            return
        }

        // check banner
        if (banner == null) {
            IronLog.ADAPTER_API.error("banner is null")
            listener?.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"))
            return
        }

        // check size
        if (!isBannerSizeSupported(banner.size)) {
            IronLog.ADAPTER_API.error("size not supported, size = " + banner.size.description)
            listener?.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(providerName))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // create banner
        val bannerView = getBannerView(banner, placementId, listener)
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

    override fun destroyBanner(config: JSONObject?) {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (placementIdToBannerAd[placementId] != null) {
            placementIdToBannerAd[placementId]?.destroy()
            placementIdToBannerAd.remove(placementId)
        }
    }

    override fun collectBannerBiddingData(
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(config, biddingDataCallback)
    }

    //endregion

    // region memory handling

    override fun releaseMemory(
        adUnit: AD_UNIT,
        config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

        when (adUnit) {
            AD_UNIT.REWARDED_VIDEO -> {
                placementIdToRewardedVideoAdListener.clear()
                rewardedVideoPlacementIdToLoadedAdObjectId.clear()
                placementIdToRewardedVideoAdAvailability.clear()
            }
            AD_UNIT.INTERSTITIAL -> {
                placementIdToInterstitialAdListener.clear()
                interstitialPlacementIdToLoadedAdObjectId.clear()
                placementIdToInterstitialAdAvailability.clear()
            }
            AD_UNIT.BANNER -> {
                for (adView in placementIdToBannerAd.values) {
                    adView?.destroy()
                }

                placementIdToBannerAdListener.clear()
                placementIdToBannerAd.clear()
            }
            else -> {}
        }
    }

    //endregion

    //region legal

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("setConsent = $consent")
        setUnityAdsMetaData(CONSENT_GDPR, consent)
    }

    override fun setMetaData(
        key: String,
        values: List<String>
    ) {
        if (values.isEmpty()) {
            return
        }

        // this is a list of 1 value
        val value = values[0]
        IronLog.ADAPTER_API.verbose("key = $key, value = $value")

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
        } else {
            val formattedValue = MetaDataUtils.formatValueForType(value, MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

            if (MetaDataUtils.isValidMetaData(key, UNITYADS_METADATA_COPPA_KEY, formattedValue)) {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setUnityAdsMetaData(
        key: String,
        value: Boolean
    ) {
        IronLog.INTERNAL.verbose("key = $key, value = $value")

        synchronized(unityAdsStorageLock) {
            val metaData = MetaData(ContextProvider.getInstance().applicationContext)
            metaData[key] = value

            // in case of COPPA we need to set an additional key
            if (key == UNITYADS_COPPA) {
                metaData[GAME_DESIGNATION] = MIXED_AUDIENCE // This is a mixed audience game.
            }

            metaData.commit()
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("value = $value")
        setUnityAdsMetaData(UNITYADS_COPPA, value)
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("value = $value")
        // The UnityAds CCPA API expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        val optIn = !value
        setUnityAdsMetaData(CONSENT_CCPA, optIn)
    }

    // endregion

    //region Adapter Helpers

    private fun collectBiddingData(
        config: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        UnityAds.getToken { bidToken ->
            if (!bidToken.isNullOrEmpty()) {
                IronLog.ADAPTER_API.verbose("token = $bidToken")
                mutableMapOf<String, Any>()
                    .apply { put("token", bidToken) }
                    .let { biddingDataCallback.onSuccess(it) }
            } else {
                biddingDataCallback.onFailure("Failed to receive token - UnityAds")
            }
        }
    }

    private fun isBannerSizeSupported(size: ISBannerSize): Boolean {
        return when (size.description) {
            "BANNER", "LARGE", "RECTANGLE", "SMART" -> true
            else -> false
        }
    }

    private fun getBannerSize(
        size: ISBannerSize,
        isLargeScreen: Boolean
    ): UnityBannerSize? {
        return when (size.description) {
            "BANNER", "LARGE" -> UnityBannerSize(320, 50)
            "RECTANGLE" -> UnityBannerSize(300, 250)
            "SMART" -> if (isLargeScreen) UnityBannerSize(728, 90) else UnityBannerSize(320, 50)
            else -> null
        }
    }

    private fun getBannerView(
        banner: IronSourceBannerLayout,
        placementId: String,
        listener: BannerSmashListener?
    ): BannerView {
        // Remove previously created banner view
        if (placementIdToBannerAd[placementId] != null) {
            placementIdToBannerAd[placementId]?.destroy()
            placementIdToBannerAd.remove(placementId)
        }

        // get size
        val unityBannerSize = getBannerSize(banner.size,
                AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext))

        // create banner
        val bannerView = BannerView(ContextProvider.getInstance().currentActiveActivity,
                placementId, unityBannerSize)

        // add listener
        val bannerAdListener = UnityAdsBannerListener(listener, WeakReference(this), placementId)
        placementIdToBannerAdListener[placementId] = bannerAdListener
        bannerView.listener = bannerAdListener

        // add to map
        placementIdToBannerAd[placementId] = bannerView
        return bannerView
    }

    fun createLayoutParams(size: UnityBannerSize): FrameLayout.LayoutParams {
        val widthPixel = AdapterUtils.dpToPixels(ContextProvider.getInstance().applicationContext, size.width)
        return FrameLayout.LayoutParams(widthPixel, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
    }

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

    fun getUnityAdsLoadErrorCode(error: UnityAds.UnityAdsLoadError): Int {
        for (e in UnityAds.UnityAdsLoadError.values()) {
            if (e.name.equals(error.toString(), ignoreCase = true)) {
                return UnityAds.UnityAdsLoadError.valueOf(error.toString()).ordinal
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

    internal fun setRewardedVideoAdAvailability(
        placementId: String,
        isAvailable: Boolean
    ) {
        placementIdToRewardedVideoAdAvailability[placementId] = isAvailable
    }

    internal fun setInterstitialAdAvailability(
        placementId: String,
        isAvailable: Boolean
    ) {
        placementIdToInterstitialAdAvailability[placementId] = isAvailable
    }

    // endregion
}
package com.ironsource.adapters.unityads

import android.content.Context
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.CONSENT_CCPA
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.CONSENT_GDPR
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.GAME_DESIGNATION
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.GAME_ID
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.LWS_SUPPORT_STATE
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.MIXED_AUDIENCE
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.PLACEMENT_ID
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UNITYADS_COPPA
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.UNITYADS_METADATA_COPPA_KEY
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.VERSION
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.ads.AdFormat
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsExperimental
import com.unity3d.ads.metadata.MetaData
import com.unity3d.mediation.LevelPlay
import com.unity3d.services.banners.UnityBannerSize
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

class UnityAdsAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    // By default its legacy
    // The reason it must default to something, is that getToken might proceed init request
    private val bridgeRef: AtomicReference<NetworkBridge> = AtomicReference(null)
    private val bridge: NetworkBridge?
        get() = bridgeRef.get()

    private val unityAdsStorageLock = Any()

    // Event sender injected by the SDK via config

    fun extractEventSender(config: JSONObject?) {
        if (errorReporter == null) {
            @Suppress("UNCHECKED_CAST")
            val sender = config?.opt("eventSender") as? ((LevelPlay.AdFormat?, Int, String) -> Unit)
            if (sender != null) {
                errorReporter = UnityAdsErrorReporter(sender)
            }
        }
    }

    companion object {
        internal var errorReporter: UnityAdsErrorReporter? = null

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

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    //endregion

    //region Initializations methods and callbacks

    private fun initSDK(config: JSONObject) {
        val gameId = config.optString(GAME_ID)
        IronLog.ADAPTER_API.verbose("gameId = $gameId")

        extractEventSender(config)

        initBridge(config)

        bridge?.initSdk(config, isAdaptersDebugEnabled, this)
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

    override fun loadRewardedVideoForBidding(
        config: JSONObject?,
        adData: JSONObject?,
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

        initBridge(config)

        bridge?.loadRewardedAd(placementId, serverData, listener)
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

        initBridge(config)

        bridge?.showRewardedAd(placementId, dynamicUserId, listener)
    }

    override fun isRewardedVideoAvailable(config: JSONObject?): Boolean {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        initBridge(config)

        return placementId?.let { bridge?.isRewardedAdAvailable(it) } ?: false
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(AdFormat.REWARDED, config, adData, biddingDataCallback)
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
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, providerName, "Missing params - $PLACEMENT_ID"))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        initBridge(config)

        bridge?.loadInterstitialAd(placementId, serverData, listener)
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

        initBridge(config)

        bridge?.showInterstitialAd(placementId, listener)
    }

    override fun isInterstitialReady(config: JSONObject?): Boolean {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        initBridge(config)

        return placementId?.let { bridge?.isInterstitialAdReady(it) } ?: false
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(AdFormat.INTERSTITIAL, config, adData, biddingDataCallback)
    }

    //endregion

    //region Banner API

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
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
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.ADAPTER_API.error("Missing param - $PLACEMENT_ID")
            listener?.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.BANNER_AD_UNIT, providerName, "Missing params - $PLACEMENT_ID"))
            return
        }

        // check size
        if (bannerSize == null || !isBannerSizeSupported(bannerSize)) {
            IronLog.ADAPTER_API.error("size not supported, size = " + bannerSize?.description)
            listener?.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(providerName))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        val unityBannerSize = getBannerSize(bannerSize,
            AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext))

        initBridge(config)

        bridge?.loadBanner(placementId, adData, serverData, unityBannerSize, listener)
    }

    override fun collectBannerBiddingData(
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(AdFormat.BANNER, config, adData, biddingDataCallback)
    }

    //endregion

    //region legal

    @OptIn(UnityAdsExperimental::class)
    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("setConsent = $consent")

        // both legacy and new apis to be called for privacy.

        // Legacy Api Call
        setUnityAdsMetaData(CONSENT_GDPR, consent)

        // Public Api Call
        UnityAds.userConsent = consent
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

    @OptIn(UnityAdsExperimental::class)
    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("value = $value")

        // both legacy and new apis to be called for privacy.

        // Legacy Api Call
        setUnityAdsMetaData(UNITYADS_COPPA, value)

        // Public Api Call
        UnityAds.nonBehavioral = value
    }

    @OptIn(UnityAdsExperimental::class)
    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("value = $value")

        // The UnityAds CCPA API expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        val optIn = !value

        // both legacy and new apis to be called for privacy.

        // Legacy Api Call
        setUnityAdsMetaData(CONSENT_CCPA, optIn)

        // Public Api Call
        UnityAds.userOptOut = value
    }

    private fun setUnityAdsMetaData(
        key: String,
        value: Boolean
    ) {
        IronLog.INTERNAL.verbose("key = $key, value = $value")

        synchronized(unityAdsStorageLock) {
            val metaData = MetaData(ContextProvider.getInstance().applicationContext)
            metaData[key] = value
            metaData.commit()
        }
    }

    // endregion

    //region Adapter Helpers

    private fun collectBiddingData(
        adFormat: AdFormat,
        config: JSONObject?,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        initBridge(config)
        val tokenConfiguration = bridge?.getTokenConfig(adFormat, config, adData) ?: return
        UnityAds.getToken(tokenConfiguration) { bidToken ->
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

    // endregion

    private fun initBridge(config: JSONObject?) {
        if(bridgeRef.get() == null) {
            bridgeRef.compareAndSet(
                null,
                NetworkBridge.Factory().getNetworkBridge(providerName, config))
        }
    }
}
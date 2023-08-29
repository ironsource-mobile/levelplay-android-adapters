package com.ironsource.adapters.vungle

import android.content.Context
import com.ironsource.adapters.vungle.VungleBannerAdapter.Companion.getBannerSize
import com.ironsource.adapters.vungle.VungleConsent.setCCPAValue
import com.ironsource.adapters.vungle.VungleConsent.setCOPPAValue
import com.ironsource.adapters.vungle.VungleConsent.setGDPRStatus
import com.ironsource.adapters.vungle.VungleInitializer.VungleInitializationListener
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource.AD_UNIT
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.AdConfig
import com.vungle.ads.VungleAds
import org.json.JSONObject

class VungleAdapter private constructor(providerName: String) :
    AbstractAdapter(providerName) {

    private var rewardedAdapter: VungleRewardedAdapter? = null
    private var interstitialAdapter: VungleInterstitialAdapter? = null
    private var bannerAdapter: VungleBannerAdapter? = null

    private var mAdOrientation: String? = null

    init {
        IronLog.INTERNAL.verbose("")
    }

    companion object {
        // Adapter version
        private const val VERSION = BuildConfig.VERSION_NAME
        private const val GitHash = BuildConfig.GitHash

        // Vungle keys
        private const val APP_ID = "AppID"
        private const val PLACEMENT_ID = "PlacementId"

        // Meta data flags
        private const val VUNGLE_COPPA_FLAG = "vungle_coppa"
        private const val ORIENTATION_FLAG = "vungle_adorientation"

        // Vungle Constants
        private const val CONSENT_MESSAGE_VERSION = "1.0.0"
        private const val ORIENTATION_PORTRAIT = "PORTRAIT"
        private const val ORIENTATION_LANDSCAPE = "LANDSCAPE"
        private const val ORIENTATION_AUTO_ROTATE = "AUTO_ROTATE"
        private const val LWS_SUPPORT_STATE = "isSupportedLWSByInstance"

        //region Adapter Methods
        @JvmStatic
        fun startAdapter(providerName: String): VungleAdapter {
            return VungleAdapter(providerName)
        }

        // Get the network and adapter integration data
        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData("Vungle", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return VungleAds.getSdkVersion()
        }
    }

    // get adapter version
    override fun getVersion(): String {
        return VERSION
    }

    //get network sdk version
    override fun getCoreSDKVersion(): String {
        return getAdapterSDKVersion()
    }

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
        val appId = config?.optString(APP_ID)

        // Configuration Validation
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID")
            listener?.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $PLACEMENT_ID",
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID")
            listener?.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $APP_ID",
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }
        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")
        VungleInitializer.instance.initialize(appId,
            ContextProvider.getInstance().applicationContext,
            object : VungleInitializationListener {
                override fun onInitializeSuccess() {
                    listener?.onRewardedVideoInitSuccess()
                }

                override fun onInitializeError(error: String?) {
                    IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                    listener?.onRewardedVideoInitFailed(
                        ErrorBuilder.buildInitFailedError(
                            error,
                            IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                        )
                    )
                }
            })
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
        val appId = config?.optString(APP_ID)
        loadRewardedVideoInternal(appId, placementId, listener, null)
    }

    override fun loadRewardedVideo(
        config: JSONObject?,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)
        loadRewardedVideoInternal(appId, placementId, listener, null)
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject?,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)
        loadRewardedVideoInternal(appId, placementId, listener, serverData)
    }

    private fun loadRewardedVideoInternal(
        appId: String?,
        placementId: String?,
        listener: RewardedVideoSmashListener?,
        serverData: String?
    ) {
        // Configuration Validation
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID")
            listener?.onRewardedVideoAvailabilityChanged(false)
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID")
            listener?.onRewardedVideoAvailabilityChanged(false)
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        VungleInitializer.instance.initialize(appId,
            ContextProvider.getInstance().applicationContext,
            object : VungleInitializationListener {
                override fun onInitializeSuccess() {
                    val adConfig = createAdConfig()
                    rewardedAdapter = VungleRewardedAdapter(placementId, adConfig, listener)
                    rewardedAdapter?.loadWithBid(serverData)
                }

                override fun onInitializeError(error: String?) {
                    IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                    listener?.onRewardedVideoAvailabilityChanged(false)
                }
            })
    }

    override fun showRewardedVideo(config: JSONObject?, listener: RewardedVideoSmashListener?) {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // if we can play
        if (rewardedAdapter != null) {
            // dynamic user id
            dynamicUserId?.let {
                rewardedAdapter?.setUserId(it)
            }
            rewardedAdapter?.play()
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = $placementId")
            listener?.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
        }
    }

    override fun isRewardedVideoAvailable(config: JSONObject?): Boolean {
        return rewardedAdapter != null && rewardedAdapter?.canPlayAd() == true
    }

    override fun getRewardedVideoBiddingData(config: JSONObject?, adData: JSONObject?):
            Map<String, Any> {
        return getBiddingData()
    }

    //endregion
    //region Interstitial API
    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        initInterstitial(appKey, userId, config, listener)
    }

    override fun initInterstitial(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)

        // Configuration Validation
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID")
            listener?.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $PLACEMENT_ID",
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID")
            listener?.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $APP_ID",
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")
        VungleInitializer.instance.initialize(appId,
            ContextProvider.getInstance().applicationContext,
            object : VungleInitializationListener {
                override fun onInitializeSuccess() {
                    listener?.onInterstitialInitSuccess()
                }

                override fun onInitializeError(error: String?) {
                    IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                    listener?.onInterstitialInitFailed(
                        ErrorBuilder.buildInitFailedError(
                            error,
                            IronSourceConstants.INTERSTITIAL_AD_UNIT
                        )
                    )
                }
            })
    }

    override fun loadInterstitialForBidding(
        config: JSONObject?,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)
        loadInterstitialInternal(appId, placementId, listener, serverData)
    }

    override fun loadInterstitial(
        config: JSONObject?,
        adData: JSONObject?,
        listener: InterstitialSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)
        loadInterstitialInternal(appId, placementId, listener, null)
    }

    private fun loadInterstitialInternal(
        appId: String?,
        placementId: String?,
        listener: InterstitialSmashListener?,
        serverData: String?
    ) {
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID")
            listener?.onInterstitialAdLoadFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $PLACEMENT_ID",
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID")
            listener?.onInterstitialAdLoadFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $APP_ID",
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        VungleInitializer.instance.initialize(appId,
            ContextProvider.getInstance().applicationContext,
            object : VungleInitializationListener {
                override fun onInitializeSuccess() {
                    val adConfig = createAdConfig()
                    interstitialAdapter = VungleInterstitialAdapter(placementId, adConfig, listener)
                    interstitialAdapter?.loadWithBid(serverData)
                }

                override fun onInitializeError(error: String?) {
                    IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                    listener?.onInterstitialAdLoadFailed(
                        ErrorBuilder.buildInitFailedError(
                            error,
                            IronSourceConstants.INTERSTITIAL_AD_UNIT
                        )
                    )
                }
            })
    }

    override fun showInterstitial(config: JSONObject?, listener: InterstitialSmashListener?) {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // if we can play
        if (interstitialAdapter != null) {
            interstitialAdapter?.play()
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = $placementId")
            listener?.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        }
    }

    override fun isInterstitialReady(config: JSONObject?): Boolean {
        return interstitialAdapter != null && interstitialAdapter?.canPlayAd() == true
    }

    override fun getInterstitialBiddingData(config: JSONObject?, adData: JSONObject?):
            Map<String, Any> {
        return getBiddingData()
    }

    //endregion
    //region Banner API
    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: BannerSmashListener?
    ) {
        initBanners(appKey, userId, config, listener)
    }

    override fun initBanners(
        appKey: String?,
        userId: String?,
        config: JSONObject?,
        listener: BannerSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)

        // Configuration Validation
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID")
            listener?.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $PLACEMENT_ID",
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID")
            listener?.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $APP_ID",
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }
        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")
        VungleInitializer.instance.initialize(appId,
            ContextProvider.getInstance().applicationContext,
            object : VungleInitializationListener {
                override fun onInitializeSuccess() {
                    listener?.onBannerInitSuccess()
                }

                override fun onInitializeError(error: String?) {
                    IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                    listener?.onBannerInitFailed(
                        ErrorBuilder.buildInitFailedError(
                            error,
                            IronSourceConstants.BANNER_AD_UNIT
                        )
                    )
                }
            })
    }

    override fun loadBannerForBidding(
        config: JSONObject?,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout?,
        listener: BannerSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)
        loadBannerInternal(appId, placementId, banner, listener, serverData)
    }

    override fun loadBanner(
        config: JSONObject?,
        adData: JSONObject?,
        banner: IronSourceBannerLayout?,
        listener: BannerSmashListener?
    ) {
        val placementId = config?.optString(PLACEMENT_ID)
        val appId = config?.optString(APP_ID)
        loadBannerInternal(appId, placementId, banner, listener, null)
    }

    private fun loadBannerInternal(
        appId: String?, placementId: String?, banner: IronSourceBannerLayout?,
        listener: BannerSmashListener?, serverData: String?
    ) {
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID")
            listener?.onBannerAdLoadFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $PLACEMENT_ID",
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID")
            listener?.onBannerAdLoadFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing param - $APP_ID",
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        // verify size
        val isBannerSize = banner?.size
        val loBannerSize = getBannerSize(isBannerSize)
        IronLog.ADAPTER_API.verbose("bannerSize = $loBannerSize")
        if (isBannerSize == null || loBannerSize == null) {
            IronLog.ADAPTER_API.verbose("size not supported, IS_size = ${isBannerSize?.description}, LO_size= $loBannerSize")
            listener?.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(providerName))
            return
        }
        VungleInitializer.instance.initialize(appId,
            ContextProvider.getInstance().applicationContext,
            object : VungleInitializationListener {
                override fun onInitializeSuccess() {
                    bannerAdapter =
                        VungleBannerAdapter(placementId, isBannerSize, loBannerSize, listener)
                    bannerAdapter?.loadWithBid(serverData)
                }

                override fun onInitializeError(error: String?) {
                    IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                    listener?.onBannerAdLoadFailed(
                        ErrorBuilder.buildInitFailedError(
                            error,
                            IronSourceConstants.BANNER_AD_UNIT
                        )
                    )
                }
            })
    }

    override fun destroyBanner(config: JSONObject?) {
        val placementId = config?.optString(PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // run on main thread
        postOnUIThread {
            if (bannerAdapter != null) {
                bannerAdapter?.destroy()
                bannerAdapter = null
            }
        }
    }

    override fun getBannerBiddingData(config: JSONObject?, adData: JSONObject?): Map<String, Any> {
        return getBiddingData()
    }

    //endregion
    // region memory handling
    override fun releaseMemory(adUnit: AD_UNIT, config: JSONObject?) {
        if (adUnit == AD_UNIT.REWARDED_VIDEO) {
            if (rewardedAdapter != null) {
                rewardedAdapter?.destroy()
                rewardedAdapter = null
            }
        } else if (adUnit == AD_UNIT.INTERSTITIAL) {
            if (interstitialAdapter != null) {
                interstitialAdapter?.destroy()
                interstitialAdapter = null
            }
        } else if (adUnit == AD_UNIT.BANNER) {
            if (bannerAdapter != null) {
                bannerAdapter?.destroy()
                bannerAdapter = null
            }
        }
    }

    //endregion

    // region progressive loading handling
    // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
    override fun getLoadWhileShowSupportState(mAdUnitSettings: JSONObject?): LoadWhileShowSupportState {
        var loadWhileShowSupportState = mLWSSupportState
        mAdUnitSettings?.let {
            val isSupportedLWSByInstance = it.optBoolean(LWS_SUPPORT_STATE)
            loadWhileShowSupportState = if (isSupportedLWSByInstance) {
                LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
            } else {
                LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK
            }
        }
        return loadWhileShowSupportState
    }

    //endregion
    //region legal
    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("consent = $consent")
        setGDPRStatus(consent, CONSENT_MESSAGE_VERSION)
    }

    override fun setMetaData(key: String?, values: List<String>?) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) {
            return
        }

        // this is a list of 1 value
        val value = values[0]
        IronLog.ADAPTER_API.verbose("key = $key, value = $value")
        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            val ccpa = MetaDataUtils.getMetaDataBooleanValue(value)
            setCCPAValue(ccpa)
        } else if (MetaDataUtils.isValidMetaData(key, ORIENTATION_FLAG, value)) {
            mAdOrientation = value
        } else {
            val formattedValue =
                MetaDataUtils.formatValueForType(value, MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)
            if (MetaDataUtils.isValidMetaData(key, VUNGLE_COPPA_FLAG, formattedValue)) {
                val isUserCoppa = MetaDataUtils.getMetaDataBooleanValue(formattedValue)
                setCOPPAValue(isUserCoppa)
            }
        }
    }

    private fun getBiddingData(): Map<String, Any> {
        val bidToken =
            VungleAds.getBiddingToken(ContextProvider.getInstance().applicationContext)
        val returnedToken = bidToken ?: ""
        val sdkVersion = coreSDKVersion
        IronLog.ADAPTER_API.verbose("sdkVersion = $sdkVersion")
        IronLog.ADAPTER_API.verbose("token = $returnedToken")
        val ret = mutableMapOf<String, Any>()
        ret["sdkVersion"] = sdkVersion
        ret["token"] = returnedToken
        return ret
    }

    private fun createAdConfig(): AdConfig {
        val adConfig = AdConfig()

        //set orientation configuration
        if (mAdOrientation != null) {
            when (mAdOrientation) {
                ORIENTATION_PORTRAIT -> adConfig.adOrientation = AdConfig.PORTRAIT
                ORIENTATION_LANDSCAPE -> adConfig.adOrientation = AdConfig.LANDSCAPE
                ORIENTATION_AUTO_ROTATE -> adConfig.adOrientation = AdConfig.AUTO_ROTATE
            }
            IronLog.INTERNAL.verbose("setAdOrientation to ${adConfig.adOrientation}")
        }
        return adConfig
    }
    //endregion

}

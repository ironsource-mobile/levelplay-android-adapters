package com.ironsource.adapters.yahoo

import android.app.Activity
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yahoo.ads.*
import com.yahoo.ads.inlineplacement.AdSize
import com.yahoo.ads.inlineplacement.InlineAdView
import com.yahoo.ads.inlineplacement.InlinePlacementConfig
import com.yahoo.ads.interstitialplacement.InterstitialAd
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean


class YahooAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    // Rewarded video maps
    private var mPlacementIdToRewardedVideoListener: ConcurrentHashMap<String, RewardedVideoSmashListener> = ConcurrentHashMap()
    private var mPlacementIdToRewardedVideoAdListener: ConcurrentHashMap<String, YahooRewardedVideoAdListener> = ConcurrentHashMap()
    private var mPlacementIdToRewardedVideoAd: ConcurrentHashMap<String, InterstitialAd> = ConcurrentHashMap()
    private val mPlacementIdToRewardedVideoAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    // Interstitial maps
    private var mPlacementIdToInterstitialListener: ConcurrentHashMap<String, InterstitialSmashListener> = ConcurrentHashMap()
    private var mPlacementIdToInterstitialAdListener: ConcurrentHashMap<String, YahooInterstitialAdListener> = ConcurrentHashMap()
    private var mPlacementIdToInterstitialAd: ConcurrentHashMap<String, InterstitialAd> = ConcurrentHashMap()
    private val mPlacementIdToInterstitialAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    // Banner maps
    private var mPlacementIdToBannerListener: ConcurrentHashMap<String, BannerSmashListener> = ConcurrentHashMap()
    private var mPlacementIdToBannerAdListener: ConcurrentHashMap<String, YahooBannerAdListener> = ConcurrentHashMap()
    private var mPlacementIdToBannerView: ConcurrentHashMap<String, InlineAdView> = ConcurrentHashMap()

    init {
        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    companion object {
        // Mediation info
        private const val MEDIATION_NAME: String = "IronSource"

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Yahoo keys
        private const val PLACEMENT_ID_KEY: String = "placementId"
        private const val SITE_ID_KEY: String = "siteId"

        // Meta data flags
        private const val META_DATA_YAHOO_COPPA: String = "yahoo_coppa"
        private const val META_DATA_YAHOO_GDPR: String = "yahoo_gdprconsent"
        private const val META_DATA_YAHOO_CCPA_NO_CONSENT_VALUE: String = "1YYN"
        private const val META_DATA_YAHOO_CCPA_CONSENT_VALUE: String = "1YNN"

        // Placement data
        private const val PLACEMENT_DATA_SERVER_DATA_KEY: String = "adContent"
        private const val PLACEMENT_DATA_WATERFALL_KEY: String = "overrideWaterfallProvider"
        private const val PLACEMENT_DATA_WATERFALL_VALUE: String = "waterfallprovider/sideloading"

        // Handle init callback for all adapter instances
        private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var mInitState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        // Init state possible values
        private enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        @JvmStatic
        fun startAdapter(providerName: String): YahooAdapter {
            return YahooAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(activity: Activity?): IntegrationData {
            return IntegrationData("Yahoo", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return YASAds.getSDKInfo().version
        }
    }

    //region Adapter Methods

    // Get adapter version
    override fun getVersion(): String {
        return VERSION
    }

    // Get network sdk version
    override fun getCoreSDKVersion(): String {
        return getAdapterSDKVersion()
    }

    //endregion

    //region Initializations methods and callbacks

    private fun initSdk(siteID: String) {
        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("siteID = $siteID")

            // Set log level
            if (isAdaptersDebugEnabled) {
                YASAds.setLogLevel(Logger.DEBUG)
            } else {
                YASAds.setLogLevel(Logger.INFO)
            }

            // Init Yahoo SDK
            if (YASAds.initialize(ContextProvider.getInstance().currentActiveActivity.application, siteID)) {
                initializationSuccess()
            } else {
                initializationFailure()
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("")

        mInitState = InitState.INIT_STATE_SUCCESS

        //iterate over all the adapter instances and report init success
        for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.verbose("")

        mInitState = InitState.INIT_STATE_FAILED

        //iterate over all the adapter instances and report init failed
        for (adapter in initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Yahoo SDK init failed")
        }

        initCallbackListeners.clear()
    }

    override fun onNetworkInitCallbackSuccess() {
        // Rewarded Video
        for ((_, value) in mPlacementIdToRewardedVideoListener.entries) {
            value.onRewardedVideoInitSuccess()
        }

        // Interstitial
        for ((_, value) in mPlacementIdToInterstitialListener.entries) {
            value.onInterstitialInitSuccess()
        }

        // Banner
        for ((_, value) in mPlacementIdToBannerListener.entries) {
            value.onBannerInitSuccess()
        }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        // Rewarded Video
        for ((_, value) in this.mPlacementIdToRewardedVideoListener.entries) {
            value.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
        }

        // Interstitial
        for ((_, value) in mPlacementIdToInterstitialListener.entries) {
            value.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        }

        // Banner
        for ((_, value) in mPlacementIdToBannerListener.entries) {
            value.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT
                )
            )
        }
    }

    override fun onNetworkInitCallbackLoadSuccess(placement: String?) {
    }

    //endregion

    //region Rewarded Video API

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(appKey: String?, userId: String?, config: JSONObject?, listener: RewardedVideoSmashListener) {
        val placementId = config?.optString(PLACEMENT_ID_KEY)
        val siteID = config?.optString(SITE_ID_KEY)

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID_KEY")
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $PLACEMENT_ID_KEY", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        if (siteID.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SITE_ID_KEY")
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $SITE_ID_KEY", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        val rewardedVideoAdListener = YahooRewardedVideoAdListener(listener, WeakReference(this), placementId)

        //add to rewarded video listener map
        mPlacementIdToRewardedVideoAdListener[placementId] = rewardedVideoAdListener
        mPlacementIdToRewardedVideoListener[placementId] = listener

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Yahoo SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            }
            else -> {
                initSdk(siteID)
            }
        }
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun initAndLoadRewardedVideo(appKey: String?, userId: String?, config: JSONObject?, listener: RewardedVideoSmashListener) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    override fun loadRewardedVideoForBidding(config: JSONObject, listener: RewardedVideoSmashListener?, serverData: String?) {
        val placementId = config.optString(PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        setRewardedVideoAdAvailability(placementId, false)

        val rewardedVideoAdListener = mPlacementIdToRewardedVideoAdListener[placementId]
        val rewardedVideoAd = InterstitialAd(ContextProvider.getInstance().applicationContext, placementId, rewardedVideoAdListener)
        val rewardedVideoPlacementConfig = InterstitialPlacementConfig(placementId, getLoadRequestMetaData(serverData))

        postOnUIThread {
            rewardedVideoAd.load(rewardedVideoPlacementConfig)
        }
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun fetchRewardedVideoForAutomaticLoad(config: JSONObject?, listener: RewardedVideoSmashListener?) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener?) {
        val placementId = config.optString(PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")
        listener?.onRewardedVideoAvailabilityChanged(false)

        if (isRewardedVideoAvailable(config)) {
            postOnUIThread {
                mPlacementIdToRewardedVideoAd[placementId].let {
                    it?.show(ContextProvider.getInstance().applicationContext)
                }
            }
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
        val placementId = config?.optString(PLACEMENT_ID_KEY)

        return (!placementId.isNullOrEmpty() &&
                mPlacementIdToRewardedVideoAd.containsKey(placementId) &&
                mPlacementIdToRewardedVideoAdAvailability.containsKey(placementId) &&
                (mPlacementIdToRewardedVideoAdAvailability[placementId] == true))
    }

    override fun getRewardedVideoBiddingData(config: JSONObject?): MutableMap<String, Any>? {
        return getBiddingData()
    }

    //endregion

    //region Interstitial API

    override fun initInterstitialForBidding(appKey: String?, userId: String?, config: JSONObject?, listener: InterstitialSmashListener) {
        val placementId = config?.optString(PLACEMENT_ID_KEY)
        val siteID = config?.optString(SITE_ID_KEY)

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID_KEY")
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $PLACEMENT_ID_KEY", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            return
        }

        if (siteID.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SITE_ID_KEY")
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $SITE_ID_KEY", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        val interstitialAdListener = YahooInterstitialAdListener(listener, WeakReference(this), placementId)

        //add to interstitial listener map
        mPlacementIdToInterstitialAdListener[placementId] = interstitialAdListener
        mPlacementIdToInterstitialListener[placementId] = listener

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Yahoo SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            }
            else -> {
                initSdk(siteID)
            }
        }
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun initInterstitial(appKey: String?, userId: String?, config: JSONObject?, listener: InterstitialSmashListener?) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    override fun loadInterstitialForBidding(config: JSONObject, listener: InterstitialSmashListener?, serverData: String?) {
        val placementId = config.optString(PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        setInterstitialAdAvailability(placementId, false)

        val interstitialAdListener = mPlacementIdToInterstitialAdListener[placementId]
        val interstitialAd = InterstitialAd(ContextProvider.getInstance().applicationContext, placementId, interstitialAdListener)
        val interstitialPlacementConfig = InterstitialPlacementConfig(placementId, getLoadRequestMetaData(serverData))

        postOnUIThread {
            interstitialAd.load(interstitialPlacementConfig)
        }
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun loadInterstitial(config: JSONObject?, listener: InterstitialSmashListener?) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener?) {
        val placementId = config.optString(PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (isInterstitialReady(config)) {
            postOnUIThread {
                mPlacementIdToInterstitialAd[placementId].let {
                    it?.show(ContextProvider.getInstance().applicationContext)
                }
            }
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
        val placementId = config?.optString(PLACEMENT_ID_KEY)

        return (!placementId.isNullOrEmpty() &&
                mPlacementIdToInterstitialAd.containsKey(placementId) &&
                mPlacementIdToInterstitialAdAvailability.containsKey(placementId) &&
                (mPlacementIdToInterstitialAdAvailability[placementId] == true))
    }

    override fun getInterstitialBiddingData(config: JSONObject?): MutableMap<String, Any>? {
        return getBiddingData()
    }

    //endregion

    //region Banners API

    override fun initBannerForBidding(appKey: String?, userId: String?, config: JSONObject?, listener: BannerSmashListener) {
        val placementId = config?.optString(PLACEMENT_ID_KEY)
        val siteID = config?.optString(SITE_ID_KEY)

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $PLACEMENT_ID_KEY")
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $PLACEMENT_ID_KEY", IronSourceConstants.BANNER_AD_UNIT))
            return
        }

        if (siteID.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SITE_ID_KEY")
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $SITE_ID_KEY", IronSourceConstants.BANNER_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // Add to banner listener map
        mPlacementIdToBannerListener[placementId] = listener

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - placementId = $placementId")
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Yahoo SDK init failed", IronSourceConstants.BANNER_AD_UNIT))
            }
            else -> {
                initSdk(siteID)
            }
        }
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun initBanners(appKey: String?, userId: String?, config: JSONObject?, listener: BannerSmashListener?) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    override fun loadBannerForBidding(banner: IronSourceBannerLayout?, config: JSONObject, listener: BannerSmashListener?, serverData: String?) {
        val placementId = config.optString(PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (banner == null) {
            IronLog.INTERNAL.error("banner is null")
            listener?.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"))
            return
        }

        val adSize : AdSize = getBannerSize(banner.size)
        val adSizes = ArrayList<AdSize>()
        adSizes.add(adSize)

        val layoutParams: FrameLayout.LayoutParams = getBannerLayoutParams(banner.size)
        val bannerAdListener = YahooBannerAdListener(listener, WeakReference(this), placementId, layoutParams)
        mPlacementIdToBannerAdListener[placementId] = bannerAdListener

        val bannerAdView = InlineAdView(ContextProvider.getInstance().applicationContext, placementId, bannerAdListener)
        val bannerPlacementConfig = InlinePlacementConfig(placementId, getLoadRequestMetaData(serverData), adSizes)

        postOnUIThread {
            bannerAdView.load(bannerPlacementConfig)
        }
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun loadBanner(banner: IronSourceBannerLayout?, config: JSONObject?, listener: BannerSmashListener?) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    // Yahoo only supports bidding flows, given that this method is for non bidding flow it is unused
    override fun reloadBanner(banner: IronSourceBannerLayout?, config: JSONObject?, listener: BannerSmashListener?) {
        IronLog.INTERNAL.warning("Unsupported method")
    }

    override fun destroyBanner(config: JSONObject?) {
        val placementId = config?.optString(PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!mPlacementIdToBannerView.containsKey(placementId)) {
            IronLog.ADAPTER_API.verbose("Banner is already destroyed")
            return
        }

        if (!placementId.isNullOrEmpty()) {
            postOnUIThread {
                // Destroy banner
                mPlacementIdToBannerView[placementId]?.destroy()
                // Remove banner view from map
                mPlacementIdToBannerView.remove(placementId)
            }
        }
    }

    override fun getBannerBiddingData(config: JSONObject?): MutableMap<String, Any>? {
        return getBiddingData()
    }

    //endregion

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT?, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

        when (adUnit) {
            IronSource.AD_UNIT.REWARDED_VIDEO -> {
                mPlacementIdToRewardedVideoListener.clear()
                mPlacementIdToRewardedVideoAdListener.clear()
                mPlacementIdToRewardedVideoAd.clear()
                mPlacementIdToRewardedVideoAdAvailability.clear()
            }
            IronSource.AD_UNIT.INTERSTITIAL -> {
                mPlacementIdToInterstitialListener.clear()
                mPlacementIdToInterstitialAdListener.clear()
                mPlacementIdToInterstitialAd.clear()
                mPlacementIdToInterstitialAdAvailability.clear()
            }
            IronSource.AD_UNIT.BANNER -> {
                for (bannerView in mPlacementIdToBannerView.values) {
                    postOnUIThread {
                        bannerView.destroy()
                    }
                }

                mPlacementIdToBannerListener.clear()
                mPlacementIdToBannerAdListener.clear()
                mPlacementIdToBannerView.clear()
            }
            else -> {}
        }
    }

    //endregion

    //region legal

    override fun setMetaData(key: String, values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0]
        IronLog.ADAPTER_API.verbose("key = $key, value = $value")
        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            isGDPRAMetaData(key, value) -> {
                setGDPRConsentString(value)
            }
            isCOPPAMetaData(key, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(value: Boolean) {
        val ccpaConsentString = if (value) {
            META_DATA_YAHOO_CCPA_NO_CONSENT_VALUE
        } else {
            META_DATA_YAHOO_CCPA_CONSENT_VALUE
        }

        IronLog.ADAPTER_API.verbose("value = $ccpaConsentString")
        YASAds.applyCcpa()
        val ccpaConsent = CcpaConsent(ccpaConsentString)
        YASAds.addConsent(ccpaConsent)
    }

    private fun isGDPRAMetaData(key: String, value: String): Boolean {
        return key.equals(META_DATA_YAHOO_GDPR, true) && value.isNotEmpty()
    }

    private fun setGDPRConsentString(consentString: String) {
        IronLog.ADAPTER_API.verbose("consentString = $consentString")
        YASAds.applyGdpr()
        val gdprConsent = GdprConsent(consentString)
        YASAds.addConsent(gdprConsent)
    }

    private fun isCOPPAMetaData(key: String, value: String): Boolean {
        return key.equals(META_DATA_YAHOO_COPPA, true) && value.isNotEmpty()
    }

    private fun setCOPPAValue(isCoppa: Boolean) {
        IronLog.ADAPTER_API.verbose("isCoppa = $isCoppa")

        if (isCoppa) {
            YASAds.applyCoppa()
        }
    }

    //endregion

    //region Helpers

    internal fun setRewardedVideoAd(placementId: String, rewardedVideoAd: InterstitialAd?) {
        if (rewardedVideoAd != null) {
            mPlacementIdToRewardedVideoAd[placementId] = rewardedVideoAd
        }
    }

    internal fun setRewardedVideoAdAvailability(placementId: String, isAvailable: Boolean) {
        mPlacementIdToRewardedVideoAdAvailability[placementId] = isAvailable
    }

    internal fun setInterstitialAd(placementId: String, interstitialAd: InterstitialAd?) {
        if (interstitialAd != null) {
            mPlacementIdToInterstitialAd[placementId] = interstitialAd
        }
    }

    internal fun setInterstitialAdAvailability(placementId: String, isAvailable: Boolean) {
        mPlacementIdToInterstitialAdAvailability[placementId] = isAvailable
    }

    internal fun setBannerView(placementId: String, inlineAdView: InlineAdView?) {
        if (inlineAdView != null) {
            mPlacementIdToBannerView[placementId] = inlineAdView
        }
    }

    private fun getBiddingData(): MutableMap<String, Any>? {
        if (mInitState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error("returning null as token since init is not successful")
            return null
        }

        val ret: MutableMap<String, Any> = HashMap()
        var bidderToken : String = YASAds.getBiddingToken(ContextProvider.getInstance().applicationContext)

        if (bidderToken.isNullOrEmpty()) {
            bidderToken = ""
        }
        IronLog.ADAPTER_API.verbose("token = $bidderToken")
        ret["token"] = bidderToken
        return ret
    }

    private fun getLoadRequestMetaData(serverData: String?): RequestMetadata {
        val requestMetadataBuilder = RequestMetadata.Builder(YASAds.getRequestMetadata())

        // Add ironSource mediation identifier
        requestMetadataBuilder.setMediator("$MEDIATION_NAME ${BuildConfig.VERSION_NAME}")

        val placementData: MutableMap<String, Any> = HashMap()

        serverData?.let {
            placementData[PLACEMENT_DATA_SERVER_DATA_KEY] = it
        }

        placementData[PLACEMENT_DATA_WATERFALL_KEY] = PLACEMENT_DATA_WATERFALL_VALUE
        requestMetadataBuilder.placementData = placementData
        return requestMetadataBuilder.build()
    }

    private fun getBannerSize(bannerSize: ISBannerSize): AdSize {
        val activity = ContextProvider.getInstance().currentActiveActivity

        return when (bannerSize.description) {
            "BANNER" -> AdSize(320, 50)
            "LARGE" -> AdSize(320, 90)
            "RECTANGLE" -> AdSize(300, 250)
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(activity)) {
                    AdSize(728, 90)
                } else {
                    AdSize(320, 50)
                })
            "CUSTOM" -> AdSize(bannerSize.width, bannerSize.height)
            else -> AdSize(bannerSize.width, bannerSize.height)
        }
    }

    private fun getBannerLayoutParams(size: ISBannerSize?): FrameLayout.LayoutParams {
        val activity = ContextProvider.getInstance().currentActiveActivity
        val layoutParams = when (size?.description) {
            "BANNER" -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(activity, 320),
                AdapterUtils.dpToPixels(activity, 50)
            )
            "LARGE" -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(activity, 320),
                AdapterUtils.dpToPixels(activity, 90)
            )
            "RECTANGLE" -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(activity, 300),
                AdapterUtils.dpToPixels(activity, 250)
            )
            "SMART" ->
                if (AdapterUtils.isLargeScreen(activity)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(activity, 728),
                        AdapterUtils.dpToPixels(activity, 90)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(activity, 320),
                        AdapterUtils.dpToPixels(activity, 50)
                    )
                }
            "CUSTOM" -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(activity, size.width),
                AdapterUtils.dpToPixels(activity, size.height)
            )
            else -> FrameLayout.LayoutParams(0, 0)
        }

        // Set gravity
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    fun getLoadErrorAndCheckNoFill(errorInfo: ErrorInfo?, isError: Int): IronSourceError {
        if (errorInfo == null) {
            return IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "internal failure")
        }

        if (errorInfo.errorCode == YASAds.ERROR_NO_FILL) {
            return IronSourceError(isError, errorInfo.description)
        }

        return IronSourceError(errorInfo.errorCode, errorInfo.description)
    }

    fun generateShowFailErrorMessage(errorInfo: ErrorInfo?, errorMsg: String): String {
        return if (errorInfo == null) {
            errorMsg
        } else {
            errorInfo.description
        }
    }

    //endregion
}

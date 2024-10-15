package com.ironsource.adapters.pangle

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGGDPRConsentType.*
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGDoNotSellType.*
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType.*
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.bytedance.sdk.openadsdk.api.init.PAGSdk.PAGInitCallback
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.IronSource.AD_UNIT
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean


class PangleAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    // Rewarded video collections
    private var mSlotIdToRewardedVideoListener: ConcurrentHashMap<String, RewardedVideoSmashListener> = ConcurrentHashMap()
    private var mSlotIdToRewardedVideoAdListener: ConcurrentHashMap<String, PangleRewardedVideoAdListener> = ConcurrentHashMap()
    private var mSlotIdToRewardedVideoAd: ConcurrentHashMap<String, PAGRewardedAd> = ConcurrentHashMap()
    private val mSlotIdToRewardedVideoAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()
    private val mRewardedVideoSlotIdsForInitCallbacks: CopyOnWriteArraySet<String> = CopyOnWriteArraySet()

    // Interstitial maps
    private var mSlotIdToInterstitialListener: ConcurrentHashMap<String, InterstitialSmashListener> = ConcurrentHashMap()
    private var mSlotIdToInterstitialAdListener: ConcurrentHashMap<String, PangleInterstitialAdListener> = ConcurrentHashMap()
    private var mSlotIdToInterstitialAd: ConcurrentHashMap<String, PAGInterstitialAd> = ConcurrentHashMap()
    private val mSlotIdToInterstitialAdAvailability: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    // Banner maps
    private var mSlotIdToBannerListener: ConcurrentHashMap<String, BannerSmashListener> = ConcurrentHashMap()
    private var mSlotIdToBannerAdListener: ConcurrentHashMap<String, PangleBannerAdListener> = ConcurrentHashMap()
    private var mSlotIdToBannerView: ConcurrentHashMap<String, PAGBannerAd> = ConcurrentHashMap()

    init {
        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    companion object {

        // Mediation Info
        private const val NAME_KEY = "name"
        private const val VALUE_KEY = "value"

        private const val MEDIATION_NAME_KEY = "mediation"
        private const val MEDIATION_NAME = "Ironsource"
        private const val ADAPTER_VERSION_KEY = "adapter_version"

        // Adapter version
        private const val VERSION = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Pangle keys
        private const val SLOT_ID_KEY = "slotID"
        private const val APP_ID_KEY = "appID"

        // Pangle errors
        const val PANGLE_NO_FILL_ERROR_CODE = 20001

        // Meta data flags
        private const val META_DATA_PANGLE_COPPA_KEY = "Pangle_COPPA"

        // Pangle Builder
        private val mPAGConfigBuilder = PAGConfig.Builder()

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
        fun startAdapter(providerName: String): PangleAdapter {
            return PangleAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData("Pangle", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return PAGSdk.getSDKVersion()
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

    override fun isUsingActivityBeforeImpression(adUnit: IronSource.AD_UNIT): Boolean {
        return false
    }

    //endregion

    //region Initializations methods and callbacks

    private fun initSdk(appId: String) {
        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("appId = $appId")

            val context = ContextProvider.getInstance().applicationContext
            val initConfig = mPAGConfigBuilder
                .appId(appId)
                .setUserData(getMediationInfo())
                .debugLog(isAdaptersDebugEnabled)
                //supportMultiProcess is an old API that will be deprecated in future versions, in the meantime set it to false
                .supportMultiProcess(false)
                .build()

            postOnUIThread {
                // Init Pangle SDK
                PAGSdk.init(context, initConfig, object : PAGInitCallback {
                    override fun success() {
                        initializationSuccess()
                    }

                    override fun fail(code: Int, message: String) {
                        initializationFailure(code, message)
                    }
                })
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        mInitState = InitState.INIT_STATE_SUCCESS

        //iterate over all the adapter instances and report init success
        for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun initializationFailure(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.verbose("error code = $code, message = $message")

        mInitState = InitState.INIT_STATE_FAILED

        //iterate over all the adapter instances and report init failed
        for (adapter in initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed(message)
        }

        initCallbackListeners.clear()
    }

    override fun onNetworkInitCallbackSuccess() {
        // Rewarded Video
        mSlotIdToRewardedVideoListener.forEach { (slotId, rewardVideoListener) ->

            if (mRewardedVideoSlotIdsForInitCallbacks.contains(slotId)) {
                rewardVideoListener.onRewardedVideoInitSuccess()
            } else {
                loadRewardedVideoInternal(slotId, null, rewardVideoListener)
            }
        }

        // Interstitial
        mSlotIdToInterstitialListener.forEach { (_, interstitialListener) -> interstitialListener.onInterstitialInitSuccess() }

        // Banner
        mSlotIdToBannerListener.forEach { (_, bannerListener) ->  bannerListener.onBannerInitSuccess()}
    }

    override fun onNetworkInitCallbackFailed(error: String) {
        // Rewarded Video
        mSlotIdToRewardedVideoListener.forEach { (slotId, rewardVideoListener) ->

            if (mRewardedVideoSlotIdsForInitCallbacks.contains(slotId)) {
                rewardVideoListener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
            } else {
                rewardVideoListener.onRewardedVideoAvailabilityChanged(false)
            }
        }

        // Interstitial
        mSlotIdToInterstitialListener.forEach { (_, interstitialListener) ->
            interstitialListener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        }

        // Banner
        mSlotIdToBannerListener.forEach { (_, bannerListener) ->
            bannerListener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT
                )
            )
        }
    }

    //endregion

    //region Rewarded Video API

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(appKey: String?, userId: String?, config: JSONObject?, listener: RewardedVideoSmashListener) {
        val slotId = config?.optString(SLOT_ID_KEY)
        val appId = config?.optString(APP_ID_KEY)

        if (slotId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SLOT_ID_KEY")
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $SLOT_ID_KEY", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID_KEY")
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $APP_ID_KEY", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        //add to rewarded video listener map
        mSlotIdToRewardedVideoListener[slotId] = listener

        // add slotId to init callback map
        mRewardedVideoSlotIdsForInitCallbacks.add(slotId)

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - slotId = $slotId")
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Pangle SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            }
            else -> {
                initSdk(appId)
            }
        }
    }

    // used for flows when the mediation doesn't need to get a callback for init
    override fun initAndLoadRewardedVideo(appKey: String?, userId: String?, config: JSONObject?, adData: JSONObject?, listener: RewardedVideoSmashListener) {
        val slotId = config?.optString(SLOT_ID_KEY)
        val appId = config?.optString(APP_ID_KEY)

        if (slotId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SLOT_ID_KEY")
            listener.onRewardedVideoAvailabilityChanged(false)
            return
        }

        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID_KEY")
            listener.onRewardedVideoAvailabilityChanged(false)
            return
        }

        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        //add to rewarded video listener map
        mSlotIdToRewardedVideoListener[slotId] = listener

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                loadRewardedVideoInternal(slotId, null, listener)
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - slotId = $slotId")
                listener.onRewardedVideoAvailabilityChanged(false)
            }
            else -> {
                initSdk(appId)
            }
        }
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        val slotId = config.optString(SLOT_ID_KEY)
        loadRewardedVideoInternal(slotId, serverData, listener)
    }

    override fun loadRewardedVideo(config: JSONObject, adData: JSONObject?, listener: RewardedVideoSmashListener) {
        val slotId = config.optString(SLOT_ID_KEY)
        loadRewardedVideoInternal(slotId, null, listener)
    }

    private fun loadRewardedVideoInternal(slotId: String, serverData: String?, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose("slotId = $slotId")
        setRewardedVideoAdAvailability(slotId, false)

        val rewardedVideoAdListener = PangleRewardedVideoAdListener(listener, WeakReference(this), slotId)
        mSlotIdToRewardedVideoAdListener[slotId] = rewardedVideoAdListener
        val request = PAGRewardedRequest()

        if (serverData != null) {
            request.adString = serverData
        }

        postOnUIThread {
            PAGRewardedAd.loadAd(slotId, request, rewardedVideoAdListener)
        }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        val slotId = config.optString(SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        if (isRewardedVideoAvailable(config)) {
            val activity = ContextProvider.getInstance().currentActiveActivity

            mSlotIdToRewardedVideoAd[slotId]?.let { rewardedVideoAd ->

                val rewardedVideoAdListener = mSlotIdToRewardedVideoAdListener[slotId]
                rewardedVideoAd.setAdInteractionListener(rewardedVideoAdListener)

                postOnUIThread {
                    rewardedVideoAd.show(activity)
                }
            }
        } else {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
        }

        setRewardedVideoAdAvailability(slotId, false)
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        val slotId = config.optString(SLOT_ID_KEY)

        return (!slotId.isNullOrEmpty() &&
                mSlotIdToRewardedVideoAd.containsKey(slotId) &&
                mSlotIdToRewardedVideoAdAvailability.containsKey(slotId) &&
                (mSlotIdToRewardedVideoAdAvailability[slotId] == true))
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(biddingDataCallback, config)
    }

    //endregion

    //region Interstitial API

    override fun initInterstitialForBidding(appKey: String?, userId: String?, config: JSONObject?, listener: InterstitialSmashListener) {
        IronLog.INTERNAL.verbose()
        initInterstitial(appKey, userId, config, listener)
    }

    override fun initInterstitial(appKey: String?, userId: String?, config: JSONObject?, listener: InterstitialSmashListener) {
        val slotId = config?.optString(SLOT_ID_KEY)
        val appId = config?.optString(APP_ID_KEY)

        if (slotId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SLOT_ID_KEY")
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $SLOT_ID_KEY", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            return
        }

        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID_KEY")
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $APP_ID_KEY", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        //add to interstitial listener map
        mSlotIdToInterstitialListener[slotId] = listener

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - slotId = $slotId")
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Pangle SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT))
            }
            else -> {
                initSdk(appId)
            }
        }
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        val slotId = config.optString(SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose("slotId = $slotId")
        loadInterstitialInternal(slotId, serverData, listener)
    }

    override fun loadInterstitial(config: JSONObject, adData: JSONObject?, listener: InterstitialSmashListener) {
        val slotId = config.optString(SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose("slotId = $slotId")
        loadInterstitialInternal(slotId, null, listener)
    }

    private fun loadInterstitialInternal(slotId: String, serverData: String?, listener: InterstitialSmashListener) {
        setInterstitialAdAvailability(slotId, false)

        val interstitialAdListener = PangleInterstitialAdListener(listener, WeakReference(this), slotId)
        mSlotIdToInterstitialAdListener[slotId] = interstitialAdListener
        val request = PAGInterstitialRequest()

        if (serverData != null) {
            request.adString = serverData
        }

        postOnUIThread {
            PAGInterstitialAd.loadAd(slotId, request, interstitialAdListener)
        }
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        val slotId = config.optString(SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        if (isInterstitialReady(config)) {
            val activity = ContextProvider.getInstance().currentActiveActivity
            mSlotIdToInterstitialAd[slotId]?.let { interstitialAd ->

                val interstitialAdListener = mSlotIdToInterstitialAdListener[slotId]
                interstitialAd.setAdInteractionListener(interstitialAdListener)

                postOnUIThread {
                    interstitialAd.show(activity)
                }
            }
        } else {
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        }

        setInterstitialAdAvailability(slotId, false)
    }

    override fun isInterstitialReady(config: JSONObject): Boolean {
        val slotId = config.optString(SLOT_ID_KEY)

        return (!slotId.isNullOrEmpty() &&
                mSlotIdToInterstitialAd.containsKey(slotId) &&
                mSlotIdToInterstitialAdAvailability.containsKey(slotId) &&
                (mSlotIdToInterstitialAdAvailability[slotId] == true))
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(biddingDataCallback, config)
    }

    //endregion

    //region Banners API

    override fun initBannerForBidding(appKey: String?, userId: String?, config: JSONObject?, listener: BannerSmashListener) {
        val slotId = config?.optString(SLOT_ID_KEY)
        val appId = config?.optString(APP_ID_KEY)

        if (slotId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $SLOT_ID_KEY")
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $SLOT_ID_KEY", IronSourceConstants.BANNER_AD_UNIT))
            return
        }

        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error("Missing param - $APP_ID_KEY")
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - $APP_ID_KEY", IronSourceConstants.BANNER_AD_UNIT))
            return
        }

        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        // add to banner listener map
        mSlotIdToBannerListener[slotId] = listener

        when (mInitState) {
            InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            InitState.INIT_STATE_FAILED -> {
                IronLog.INTERNAL.verbose("init failed - slotId = $slotId")
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Pangle SDK init failed", IronSourceConstants.BANNER_AD_UNIT))
            }
            else -> {
                initSdk(appId)
            }
        }
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout?,
        listener: BannerSmashListener
    ) {
        val slotId = config.optString(SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        if (banner == null) {
            IronLog.INTERNAL.error("banner is null")
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"))
            return
        }

        val layoutParams: FrameLayout.LayoutParams = getBannerLayoutParams(banner.size)
        val bannerAdListener = PangleBannerAdListener(listener, WeakReference(this), slotId, layoutParams)
        mSlotIdToBannerAdListener[slotId] = bannerAdListener

        val adSize = getBannerSize(banner.size)
        val bannerRequest = PAGBannerRequest(adSize)
        bannerRequest.adString = serverData

        postOnUIThread {
            if(banner == null){
                IronLog.INTERNAL.error("banner is null")
                listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"))
                return@postOnUIThread
            }
            PAGBannerAd.loadAd(slotId, bannerRequest, bannerAdListener)
        }
    }

    override fun destroyBanner(config: JSONObject?) {
        val slotId = config?.optString(SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose("slotId = $slotId")

        if (!mSlotIdToBannerView.containsKey(slotId)) {
            IronLog.ADAPTER_API.verbose("Banner is already destroyed")
            return
        }

        if (!slotId.isNullOrEmpty()) {
            postOnUIThread {
                // The listener needs to be set to null prior to the destroying of the banner to prevent a memory leak
                mSlotIdToBannerView[slotId]?.setAdInteractionListener(null)
                // Destroy banner
                mSlotIdToBannerView[slotId]?.destroy()
                // Remove banner view from map
                mSlotIdToBannerView.remove(slotId)
            }
        }
    }

    override fun collectBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        collectBiddingData(biddingDataCallback, config)
    }

    //endregion

    //region memory handling

    override fun releaseMemory(adUnit: AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

        when (adUnit) {
            AD_UNIT.REWARDED_VIDEO -> {
                mSlotIdToRewardedVideoAd.forEach { (_, rewardVideoAd) -> rewardVideoAd.setAdInteractionListener(null) }
                mSlotIdToRewardedVideoListener.clear()
                mSlotIdToRewardedVideoAdListener.clear()
                mSlotIdToRewardedVideoAd.clear()
                mSlotIdToRewardedVideoAdAvailability.clear()
                mRewardedVideoSlotIdsForInitCallbacks.clear()
            }
            AD_UNIT.INTERSTITIAL -> {
                mSlotIdToInterstitialAd.forEach { (_, interstitialAd) -> interstitialAd.setAdInteractionListener(null) }
                mSlotIdToInterstitialListener.clear()
                mSlotIdToInterstitialAdListener.clear()
                mSlotIdToInterstitialAd.clear()
                mSlotIdToInterstitialAdAvailability.clear()
            }
            AD_UNIT.BANNER -> {
                mSlotIdToBannerView.forEach { (_, bannerAd) ->
                    postOnUIThread {
                        bannerAd.setAdInteractionListener(null)
                        bannerAd.destroy()
                        mSlotIdToBannerListener.clear()
                        mSlotIdToBannerAdListener.clear()
                        mSlotIdToBannerView.clear()
                    }
                }
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

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, META_DATA_PANGLE_COPPA_KEY, value) -> {
                setCOPPAValue(value)
            }
        }
    }

    private fun setCCPAValue(doNotSell: Boolean) {

        val ccpaValue: Int
        val ccpaValueString : String

        if (doNotSell) {
            ccpaValue = PAG_DO_NOT_SELL_TYPE_NOT_SELL
            ccpaValueString = "PAG_DO_NOT_SELL_TYPE_NOT_SELL"
        }
        else {
            ccpaValue = PAG_DO_NOT_SELL_TYPE_SELL
            ccpaValueString = "PAG_DO_NOT_SELL_TYPE_SELL"
        }

        IronLog.ADAPTER_API.verbose("ccpaValue = $ccpaValueString")
        mPAGConfigBuilder.setDoNotSell(ccpaValue)
    }

    private fun setCOPPAValue(value: String) {
        val coppaValue: Int
        val coppaValueString : String

        when (value.toIntOrNull()) {
            PAG_CHILD_DIRECTED_TYPE_CHILD -> {
                coppaValue = PAG_CHILD_DIRECTED_TYPE_CHILD
                coppaValueString = "PAG_CHILD_DIRECTED_TYPE_CHILD"
            }
            PAG_CHILD_DIRECTED_TYPE_NON_CHILD -> {
                coppaValue = PAG_CHILD_DIRECTED_TYPE_NON_CHILD
                coppaValueString = "PAG_CHILD_DIRECTED_TYPE_NON_CHILD"
            }
            else -> {
                coppaValue = PAG_CHILD_DIRECTED_TYPE_DEFAULT
                coppaValueString = "PAG_CHILD_DIRECTED_TYPE_DEFAULT"
            }
        }

        IronLog.ADAPTER_API.verbose("coppaValue = $coppaValueString")
        mPAGConfigBuilder.setChildDirected(coppaValue)
    }

    override fun setConsent(consent: Boolean) {
        val gdprValue: Int
        val gdprValueString : String

        if (consent) {
            gdprValue = PAG_GDPR_CONSENT_TYPE_CONSENT
            gdprValueString = "PAG_GDPR_CONSENT_TYPE_CONSENT"
        } else {
            gdprValue = PAG_GDPR_CONSENT_TYPE_NO_CONSENT
            gdprValueString = "PAG_GDPR_CONSENT_TYPE_NO_CONSENT"
        }

        IronLog.ADAPTER_API.verbose("consent = $gdprValueString")
        mPAGConfigBuilder.setGDPRConsent(gdprValue)
    }

    //endregion

    //region Helpers

    private fun getBannerSize(bannerSize: ISBannerSize): PAGBannerSize {
        return when (bannerSize.description) {
            "BANNER" -> PAGBannerSize.BANNER_W_320_H_50
            "RECTANGLE" -> PAGBannerSize.BANNER_W_300_H_250
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                    PAGBannerSize.BANNER_W_728_H_90
                } else {
                    PAGBannerSize.BANNER_W_320_H_50
                })
            else -> PAGBannerSize(0, 0)
        }
    }

    private fun getBannerLayoutParams(size: ISBannerSize): FrameLayout.LayoutParams {
        val context = ContextProvider.getInstance().applicationContext

        val layoutParams = when (size.description) {
            "BANNER" -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, 320),
                AdapterUtils.dpToPixels(context, 50)
            )
            "RECTANGLE" -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, 300),
                AdapterUtils.dpToPixels(context, 250)
            )
            "SMART" ->
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, 728),
                        AdapterUtils.dpToPixels(context, 90)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, 320),
                        AdapterUtils.dpToPixels(context, 50)
                    )
                }
            else -> FrameLayout.LayoutParams(0, 0)
        }

        // Set gravity
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // Get the mediation info
    private fun getMediationInfo(): String {
        val mediationInfo = JSONArray()
        try {
            val mediationObject = JSONObject()
            mediationObject.put(NAME_KEY, MEDIATION_NAME_KEY)
            mediationObject.put(VALUE_KEY, MEDIATION_NAME)
            mediationInfo.put(mediationObject)

            val adapterObject = JSONObject()
            adapterObject.put(NAME_KEY, ADAPTER_VERSION_KEY)
            adapterObject.put(VALUE_KEY, VERSION)
            mediationInfo.put(adapterObject)

            IronLog.INTERNAL.verbose("mediationInfo = $mediationInfo")
        } catch (exception: JSONException) {
            IronLog.INTERNAL.error("Error while creating mediation info object - $exception")
        }

        return mediationInfo.toString()
    }

    internal fun setRewardedVideoAd(slotId: String, rewardedVideoAd: PAGRewardedAd?) {
        if (rewardedVideoAd != null) {
            mSlotIdToRewardedVideoAd[slotId] = rewardedVideoAd
        }
    }

    internal fun setRewardedVideoAdAvailability(slotId: String, isAvailable: Boolean) {
        mSlotIdToRewardedVideoAdAvailability[slotId] = isAvailable
    }

    internal fun setInterstitialAd(slotId: String, interstitialAd: PAGInterstitialAd?) {
        if (interstitialAd != null) {
            mSlotIdToInterstitialAd[slotId] = interstitialAd
        }
    }

    internal fun setInterstitialAdAvailability(slotId: String, isAvailable: Boolean) {
        mSlotIdToInterstitialAdAvailability[slotId] = isAvailable
    }

    internal fun setBannerAd(slotId: String, bannerAd: PAGBannerAd?) {
        if (bannerAd != null) {
            mSlotIdToBannerView[slotId] = bannerAd
            val bannerAdListener = mSlotIdToBannerAdListener[slotId]
            bannerAd.setAdInteractionListener(bannerAdListener)
        }
    }

    private fun collectBiddingData(biddingDataCallback: BiddingDataCallback, config: JSONObject) {
        val slotId = config.optString(SLOT_ID_KEY)
        if (mInitState == InitState.INIT_STATE_FAILED) {
            val error = "returning null as token since init is not successful"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - Pangle")
            return
        }
        PAGSdk.getBiddingToken(slotId) { bidToken ->
            if (!bidToken.isNullOrEmpty()) {
                IronLog.ADAPTER_API.verbose("token = $bidToken")
                mutableMapOf<String, Any>()
                    .apply { put("token", bidToken) }
                    .let { biddingDataCallback.onSuccess(it) }
            } else {
                biddingDataCallback.onFailure("Failed to receive token - Pangle")
            }
        }
    }

    //endregion

}
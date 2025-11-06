package com.ironsource.adapters.line

import android.content.Context
import com.five_corp.ad.AdLoader
import com.five_corp.ad.FiveAd
import com.five_corp.ad.FiveAdConfig
import com.ironsource.adapters.line.interstitial.LineInterstitialAdapter
import com.ironsource.adapters.line.rewardedvideo.LineRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.mediation.LevelPlay
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import com.five_corp.ad.AdLoader.*
import com.five_corp.ad.FiveAdErrorCode

class LineAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(LineRewardedVideoAdapter(this))
        setInterstitialAdapter(LineInterstitialAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Verve Keys
        private const val NETWORK_NAME: String = "Line"
        private const val APP_ID_KEY: String = "appId"
        private const val SLOT_ID_KEY: String = "slotId"

        const val LOG_INIT_FAILED = "$NETWORK_NAME sdk init failed"

        // Handle init callback for all adapter instances
        private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var mInitState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        private var fiveAdConfig: FiveAdConfig? = null

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        @JvmStatic
        fun startAdapter(providerName: String): LineAdapter {
            return LineAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData(NETWORK_NAME, VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return FiveAd.getSdkSemanticVersion()
        }

        fun getAppIdKey(): String {
            return APP_ID_KEY
        }

        fun getSlotIdKey(): String {
            return SLOT_ID_KEY
        }

        fun getLoadError(errorCode: FiveAdErrorCode): IronSourceError {
            return IronSourceError(errorCode.value, errorCode.name)
        }

        fun getFiveAdConfig(appId: String): FiveAdConfig {
            return fiveAdConfig ?: FiveAdConfig(appId).also {
                fiveAdConfig = it }
        }

        fun getAdLoader(appId: String): AdLoader? {
            val context = ContextProvider.getInstance().applicationContext
            val config = getFiveAdConfig(appId)
            val adLoader = forConfig(context, config)
            return adLoader
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

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean {
        return false
    }

    //endregion

    //region Initializations methods and callbacks

    fun initSdk(config: JSONObject) {
        val appIdKey = getAppIdKey()
        val appId = config.optString(appIdKey)

        IronLog.ADAPTER_API.verbose("appId = $appId")

        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE) {
            initCallbackListeners.add(this)
        }

        if (FiveAd.isInitialized()) {
            IronLog.ADAPTER_API.verbose("Initialization success")
            initializationSuccess()
            return
        }

        IronLog.ADAPTER_API.verbose("$appId: $appId")
        val context = ContextProvider.getInstance().applicationContext
        val lineConfig = getFiveAdConfig(appId)

        try {
            FiveAd.initialize(context, lineConfig)
            initializationSuccess()
        } catch (e: IllegalArgumentException) {
            IronLog.ADAPTER_API.verbose("Initialization failed: ${e.message}")
            initializationFailure()
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

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.verbose()

        mInitState = InitState.INIT_STATE_FAILED

        //iterate over all the adapter instances and report init failed
        for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Line sdk init failed")
        }
        initCallbackListeners.clear()
    }

    fun getInitState(): InitState {
        return mInitState
    }

    //endregion

    // region Helpers

    fun collectBiddingData(biddingDataCallback: BiddingDataCallback,config: JSONObject){
        val appId = config.optString(getAppIdKey())
        val adLoader = getAdLoader(appId)
        if(adLoader == null){
            biddingDataCallback.onFailure("failed to receive token adLoader is null - Line")
            return
        }
        val slotId = config.optString(getSlotIdKey())
        adLoader.collectSignal(slotId, object : CollectSignalCallback {
            override fun onCollect(token: String) {
                val ret: MutableMap<String?, Any?> = HashMap()
                IronLog.ADAPTER_API.verbose("token = $token")
                ret["token"] = token
                biddingDataCallback.onSuccess(ret)
            }

            override fun onError(fiveAdErrorCode: FiveAdErrorCode) {
                biddingDataCallback.onFailure("failed to receive token - Line, error = $fiveAdErrorCode")
            }
        })
    }

    //endregion

}
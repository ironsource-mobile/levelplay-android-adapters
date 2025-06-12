package com.ironsource.adapters.verve

import android.app.Application
import android.content.Context
import com.ironsource.adapters.verve.banner.VerveBannerAdapter
import com.ironsource.adapters.verve.interstitial.VerveInterstitialAdapter
import com.ironsource.adapters.verve.rewardedvideo.VerveRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import net.pubnative.lite.sdk.HyBid
import net.pubnative.lite.sdk.HyBidError
import net.pubnative.lite.sdk.utils.Logger
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean


class VerveAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener, HyBid.InitialisationListener {
    init {
        setRewardedVideoAdapter(VerveRewardedVideoAdapter(this))
        setInterstitialAdapter(VerveInterstitialAdapter(this))
        setBannerAdapter(VerveBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK
    }
    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Verve Keys
        private const val NETWORK_NAME: String = "Verve"
        private const val ZONE_ID: String = "zoneId"
        private const val APP_TOKEN: String = "appToken"
        private const val MEDIATION_NAME: String = "lp"

        // Meta data flags
        private const val META_DATA_VERVE_COPPA_KEY = "LevelPlay_ChildDirected"
        private const val META_DATA_VERVE_CCPA_YES_VALUE: String = "1YY-"
        private const val META_DATA_VERVE_CCPA_NO_VALUE: String = "1YN-"

        public const val LOG_INIT_FAILED = "Verve sdk init failed"

        // Handle init callback for all adapter instances
        private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var mInitState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        @JvmStatic
        fun startAdapter(providerName: String): VerveAdapter {
            return VerveAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData(NETWORK_NAME, VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return HyBid.getSDKVersionInfo()
        }

        fun getZoneIdKey(): String {
            return ZONE_ID
        }

        fun getAppTokenKey(): String {
            return APP_TOKEN
        }

        fun getLoadError(error: Throwable?): IronSourceError {
            return IronSourceError(
                (error as HyBidError).errorCode.code,
                error.errorCode.message)
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

    fun initSdk(config: JSONObject) {

        val appTokenKey= getAppTokenKey()
        val appToken = config.optString(appTokenKey)

        IronLog.ADAPTER_API.verbose("appToken = $appToken")


        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("$APP_TOKEN: $appToken")

            // Set log level
            if (isAdaptersDebugEnabled) {
                HyBid.setLogLevel(Logger.Level.debug)
            }

            val context = ContextProvider.getInstance().currentActiveActivity.applicationContext

            // Init Verve SDK
            HyBid.initialize(
                appToken,
                context as Application,
                this
            )
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
            adapter.onNetworkInitCallbackFailed("Verve sdk init failed")
        }
        initCallbackListeners.clear()
    }

    fun getInitState(): InitState {
        return mInitState
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
            MetaDataUtils.isValidMetaData(key, META_DATA_VERVE_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(value: Boolean) {
        val ccpaConsentString = if (value) {
            META_DATA_VERVE_CCPA_YES_VALUE
        } else {
            META_DATA_VERVE_CCPA_NO_VALUE
        }
        IronLog.ADAPTER_API.verbose("ccpa value = $ccpaConsentString")
        HyBid.getUserDataManager().iabusPrivacyString = ccpaConsentString
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("isCoppa = $value")
        HyBid.setCoppaEnabled(value)
    }

    // region Helpers

    fun collectBiddingData(biddingDataCallback: BiddingDataCallback): MutableMap<String?, Any?> {
        val ret: MutableMap<String?, Any?> = HashMap()
        val bidToken = HyBid.getCustomRequestSignalData(MEDIATION_NAME)
        IronLog.ADAPTER_API.verbose("token = $bidToken")
        ret["token"] = bidToken
        biddingDataCallback.onSuccess(ret)
        return ret
    }

    override fun onInitialisationFinished(success: Boolean) {
        IronLog.ADAPTER_API.verbose("Verve initialization " + if (success) "succeeded" else "failed")
        if (success) {
            initializationSuccess()
        }
        else {
            initializationFailure()
        }
    }
}
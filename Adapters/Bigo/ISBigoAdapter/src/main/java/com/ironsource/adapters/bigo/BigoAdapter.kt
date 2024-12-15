package com.ironsource.adapters.bigo

import android.content.Context
import com.ironsource.adapters.bigo.banner.BigoBannerAdapter
import com.ironsource.adapters.bigo.interstitial.BigoInterstitialAdapter
import com.ironsource.adapters.bigo.rewardedvideo.BigoRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.mediationsdk.utils.IronSourceUtils
import org.json.JSONObject
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions
import sg.bigo.ads.api.AdConfig
import sg.bigo.ads.api.AdError
import java.util.concurrent.atomic.AtomicBoolean

class BigoAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener, BigoAdSdk.InitListener {

    init {
        setRewardedVideoAdapter(BigoRewardedVideoAdapter(this))
        setInterstitialAdapter(BigoInterstitialAdapter(this))
        setBannerAdapter(BigoBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Bigo keys
        const val SLOT_ID = "slotId"
        private const val APP_ID = "appId"
        private const val NETWORK_NAME: String = "Bigo"

        val MEDIATION_INFO: String
        init {
            val mediationInfoJSON = JSONObject()
            try {
                mediationInfoJSON.putOpt("mediationName", "LevelPlay")
                mediationInfoJSON.putOpt("mediationVersion", IronSourceUtils.getSDKVersion())
                mediationInfoJSON.putOpt("adapterVersion", VERSION)
            } catch (th: Throwable) {
                IronLog.INTERNAL.error("Error creating mediation info JSON in BigoAdapter $th")
            }
            MEDIATION_INFO = mediationInfoJSON.toString()
        }

        // Handle init callback for all adapter instances
        private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var mInitState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS
        }

        @JvmStatic
        fun startAdapter(providerName: String): BigoAdapter {
            return BigoAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData(NETWORK_NAME, VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return BigoAdSdk.getSDKVersionName()
        }

        fun getAppIdKey(): String {
            return APP_ID
        }

        fun getSlotIdKey(): String {
            return SLOT_ID
        }

        fun getLoadError(error: AdError): IronSourceError {
            return IronSourceError(
                error.code,
                error.message)
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

    fun initSdk(appId: String) {

        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("appId = $appId")
        }

        val context = ContextProvider.getInstance().applicationContext

        val config = AdConfig.Builder()
            .setDebug(isAdaptersDebugEnabled)
            .setAppId(appId)
            .build()

        BigoAdSdk.initialize(context, config, this)
    }

    override fun onInitialized() {
        IronLog.ADAPTER_API.verbose("BIGO SDK Initialized")
        initializationSuccess()
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
        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("consent = $consent")
        BigoAdSdk.setUserConsent(
            ContextProvider.getInstance().applicationContext, ConsentOptions.GDPR, consent)
    }

    private fun setCCPAValue(doNotSell: Boolean) {
        IronLog.ADAPTER_API.verbose("ccpa = $doNotSell")
        BigoAdSdk.setUserConsent(
            ContextProvider.getInstance().applicationContext, ConsentOptions.CCPA, !doNotSell)
    }

    //endregion

    // region Helpers

    internal fun getBiddingData(): MutableMap<String?, Any?>? {
        if (mInitState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error("returning nil as token since init isn't completed")
            return null
        }

        val ret: MutableMap<String?, Any?> = HashMap()
        val token = BigoAdSdk.getBidderToken()

        IronLog.ADAPTER_API.verbose("token = $token")
        ret["token"] = token
        return ret
    }

    //endregion

    }

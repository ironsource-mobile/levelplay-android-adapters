package com.ironsource.adapters.bidmachine

import android.content.Context
import com.ironsource.adapters.bidmachine.interstitial.BidMachineInterstitialAdapter
import com.ironsource.adapters.bidmachine.rewardedvideo.BidMachineRewardedVideoAdapter
import com.ironsource.adapters.bidmachine.banner.BidMachineBannerAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import io.bidmachine.AdsFormat
import io.bidmachine.BidMachine
import io.bidmachine.utils.BMError
import java.util.concurrent.atomic.AtomicBoolean

class BidMachineAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(BidMachineRewardedVideoAdapter(this))
        setInterstitialAdapter(BidMachineInterstitialAdapter(this))
        setBannerAdapter(BidMachineBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // BidMachine keys
        private const val SOURCE_ID_KEY: String = "sourceId"

        // Meta data flags
        private const val META_DATA_BIDMACHINE_COPPA_KEY = "BidMachine_COPPA"
        private const val META_DATA_BIDMACHINE_CCPA_NO_CONSENT_VALUE: String = "1YY-"
        private const val META_DATA_BIDMACHINE_CCPA_CONSENT_VALUE: String = "1YN-"

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
        fun startAdapter(providerName: String): BidMachineAdapter {
            return BidMachineAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData("BidMachine", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return BidMachine.VERSION
        }

        fun getSourceIdKey(): String {
            return SOURCE_ID_KEY
        }

        fun getLoadErrorAndCheckNoFill(error: BMError, noFillError : Int): IronSourceError {
            return when (error.code) {
                BMError.NO_CONTENT -> IronSourceError(
                    noFillError,
                    error.message
                )
                else -> IronSourceError(error.code, error.message)
            }
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

    fun initSdk(sourceId: String) {

        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("sourceId = $sourceId")

            // Set log level
            BidMachine.setLoggingEnabled(isAdaptersDebugEnabled)

            // Init BidMachine SDK
            BidMachine.initialize(
                ContextProvider.getInstance().applicationContext,
                sourceId
            ) {
                // BidMachine's initialization callback currently doesn't give any indication to initialization failure.
                // Once this callback is called we will treat the initialization as successful
                initializationSuccess()
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
            MetaDataUtils.isValidMetaData(key, META_DATA_BIDMACHINE_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("consent = $consent")
        BidMachine.setSubjectToGDPR(true)
        BidMachine.setConsentConfig(consent, null)
    }

    private fun setCCPAValue(value: Boolean) {
        val ccpaConsentString = if (value) {
            META_DATA_BIDMACHINE_CCPA_NO_CONSENT_VALUE
        } else {
            META_DATA_BIDMACHINE_CCPA_CONSENT_VALUE
        }
        IronLog.ADAPTER_API.verbose("value = $ccpaConsentString")
        BidMachine.setUSPrivacyString(ccpaConsentString)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("isCoppa = $value")
        BidMachine.setCoppa(value)
    }

    //endregion

    // region Helpers

    internal fun getBiddingData(adsFormat: AdsFormat): MutableMap<String?, Any?>? {
        if (mInitState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error("returning nil as token since init isn't completed")
            return null
        }

        val ret: MutableMap<String?, Any?> = HashMap()
        var bidToken = BidMachine.getBidToken(
            ContextProvider.getInstance().applicationContext,
            adsFormat
        )
        IronLog.ADAPTER_API.verbose("token = $bidToken")
        ret["token"] = bidToken
        return ret
    }

    //endregion

    }

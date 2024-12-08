package com.ironsource.adapters.moloco

import android.content.Context
import com.ironsource.adapters.moloco.banner.MolocoBannerAdapter
import com.ironsource.adapters.moloco.interstitial.MolocoInterstitialAdapter
import com.ironsource.adapters.moloco.rewardedvideo.MolocoRewardedVideoAdapter
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
import com.moloco.sdk.internal.MolocoLogger
import com.moloco.sdk.publisher.Initialization
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.MolocoBidTokenListener
import com.moloco.sdk.publisher.init.MolocoInitParams
import com.moloco.sdk.publisher.privacy.MolocoPrivacy
import java.util.concurrent.atomic.AtomicBoolean


class MolocoAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(MolocoRewardedVideoAdapter(this))
        setBannerAdapter(MolocoBannerAdapter(this))
        setInterstitialAdapter(MolocoInterstitialAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Moloco Keys
        private const val APP_KEY: String = "appKey"
        private const val AD_UNIT_ID: String = "adUnitId"

        // Meta data flags
        private const val META_DATA_MOLOCO_COPPA_KEY = "Moloco_COPPA"

        const val INVALID_CONFIGURATION = "invalid configuration"

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
        fun startAdapter(providerName: String): MolocoAdapter {
            return MolocoAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData("Moloco", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return com.moloco.sdk.BuildConfig.SDK_VERSION_NAME
        }

        fun getAppKey(): String {
            return APP_KEY
        }

        fun getAdUnitIdKey(): String {
            return AD_UNIT_ID
        }

        fun getLoadErrorAndCheckNoFill(error: MolocoAdError, noFillError : Int): IronSourceError {
            return when (error.errorType) {
                MolocoAdError.ErrorType.AD_LOAD_FAILED-> IronSourceError(
                    noFillError,
                    error.description
                )
                else -> IronSourceError(MolocoAdError.ErrorType.AD_LOAD_FAILED.errorCode, error.description)
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

    fun initSdk(appKey: String) {

        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("appKey: $appKey")

            // Set log leve
            MolocoLogger.logEnabled = isAdaptersDebugEnabled

            val mediationInfo = MediationInfo("LevelPlay")
            val context = ContextProvider.getInstance().currentActiveActivity.applicationContext
            // Init Moloco SDK
            Moloco.initialize(MolocoInitParams(context, appKey, mediationInfo)) { molocoInitStatus ->
                val description = molocoInitStatus.description
                if (molocoInitStatus.initialization == Initialization.SUCCESS) {
                    IronLog.ADAPTER_API.verbose("Initialization success $description")
                    initializationSuccess()
                } else {
                    IronLog.ADAPTER_API.verbose("Initialization failed $description")
                    initializationFailure()
                }
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
    
    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.verbose()

        mInitState = InitState.INIT_STATE_FAILED

        //iterate over all the adapter instances and report init failed
        for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Moloco sdk init failed")
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
            MetaDataUtils.isValidMetaData(key, META_DATA_MOLOCO_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("consent = $consent")
        val privacy = MolocoPrivacy.PrivacySettings(isUserConsent = consent, isAgeRestrictedUser = null, isDoNotSell = null)
        MolocoPrivacy.setPrivacy(privacy)
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("value = $value")
        val privacy = MolocoPrivacy.PrivacySettings(isUserConsent = null, isAgeRestrictedUser = null, isDoNotSell = value)
        MolocoPrivacy.setPrivacy(privacy)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("isCoppa = $value")
        val privacy = MolocoPrivacy.PrivacySettings(isUserConsent = null, isAgeRestrictedUser = value, isDoNotSell = null)
        MolocoPrivacy.setPrivacy(privacy)
    }

    //endregion

    // region Helpers
    fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (mInitState != InitState.INIT_STATE_SUCCESS) {
            val error = "returning null as token since init isn't completed"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - Moloco")
            return
        }
        Moloco.bidRequestEndpoint
        Moloco.getBidToken(ContextProvider.getInstance().applicationContext) { bidToken, error ->            if (error == null) {
                val biddingDataMap: MutableMap<String?, Any?> = HashMap()
                IronLog.ADAPTER_API.verbose("token = $bidToken")
                biddingDataMap["token"] = bidToken
                biddingDataCallback.onSuccess(biddingDataMap)
            } else {
                biddingDataCallback.onFailure("failed to receive token - Moloco, errorCode = ${error.errorCode}, error = ${error.description}")
            }
        }
    }

    //endregion

}
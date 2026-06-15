package com.ironsource.adapters.fyber

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.fyber.inneractive.sdk.external.BidTokenProvider
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener
import com.ironsource.mediationsdk.AdapterNetworkData
import com.ironsource.mediationsdk.adunit.adapter.internal.AdapterNetworkDataInterface
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class FyberAdapter : LevelPlayBaseAdapter(), AdapterNetworkDataInterface {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private const val GitHash: String = BuildConfig.GitHash

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        // Meta data flags applied once the SDK is initialized
        private var consent: Boolean? = null
        private var ccpa: Boolean? = null
        private var isChild: Boolean = false
        private var coppa: Boolean? = null

        @JvmStatic
        fun getLoadErrorType(errorCode: InneractiveErrorCode?): AdapterErrorType {
            return if (errorCode == InneractiveErrorCode.NO_FILL) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun getAdapterVersion(): String = FyberConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = InneractiveAdManager.getVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate appId first before any other checks
        val appId = adData.getString(FyberConstants.APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
            val errorMessage = FyberConstants.Logs.MISSING_PARAM.format(FyberConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Init previously failed - report failure immediately
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(FyberConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                FyberConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(FyberConstants.Logs.APP_ID.format(appId))

            mainHandler.post {
                setIsChildValue(isChild)
                InneractiveAdManager.initialize(
                    context.applicationContext,
                    appId
                ) { fyberInitStatus ->
                    if (fyberInitStatus == OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY) {
                        initializationSuccess()
                    } else {
                        initializationFailure()
                    }
                }
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(FyberConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        consent?.let { setConsent(it) }
        ccpa?.let { setCCPAValue(it) }
        coppa?.let { setCOPPAValue(it) }

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.error(FyberConstants.Logs.INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, FyberConstants.Logs.SDK_INIT_FAILED)
        }

        initCallbackListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        mainHandler.post {
            if (wasInitCalled.get()) {
                IronLog.ADAPTER_API.verbose(FyberConstants.Logs.CONSENT.format(consent))
                InneractiveAdManager.setGdprConsent(consent)
            } else {
                FyberAdapter.consent = consent
            }
        }
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(FyberConstants.Logs.KEY_VALUE.format(key ?: "", value))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            return
        }

        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)
        when {
            MetaDataUtils.isValidMetaData(key, FyberConstants.META_DATA_DT_IS_CHILD_KEY, formattedValue) -> {
                setIsChildValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }

            MetaDataUtils.isValidMetaData(key, FyberConstants.META_DATA_DT_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setNetworkData(networkData: AdapterNetworkData) {
        networkData.dataByKeyIgnoreCase(FyberConstants.NETWORK_DATA_IS_CHILD_KEY, Boolean::class.javaObjectType)?.let { networkDataIsChild ->
            IronLog.ADAPTER_API.verbose(FyberConstants.Logs.KEY_VALUE.format(FyberConstants.NETWORK_DATA_IS_CHILD_KEY, networkDataIsChild))
            setIsChildValue(networkDataIsChild)
        }
    }

    private fun setCCPAValue(value: Boolean) {
        mainHandler.post {
            if (wasInitCalled.get()) {
                val ccpaString = if (value) FyberConstants.CCPA_OPT_IN_STRING else FyberConstants.CCPA_OPT_OUT_STRING
                IronLog.ADAPTER_API.verbose(FyberConstants.Logs.CCPA.format(ccpaString))
                InneractiveAdManager.setUSPrivacyString(ccpaString)
            } else {
                ccpa = value
            }
        }
    }

    private fun setIsChildValue(value: Boolean) {
        if (initState == InitState.INIT_STATE_NONE) {
            isChild = value
        } else if (initState == InitState.INIT_STATE_IN_PROGRESS && value) {
            IronLog.ADAPTER_API.verbose(FyberConstants.Logs.IS_CHILD)
            InneractiveAdManager.currentAudienceIsAChild()
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        if (!value) {
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            IronLog.ADAPTER_API.verbose(FyberConstants.Logs.COPPA)
            InneractiveAdManager.currentAudienceAppliesToCoppa()
        } else {
            coppa = value
        }
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(FyberConstants.Logs.TOKEN_NOT_READY)
            biddingDataCallback.onFailure(FyberConstants.Logs.TOKEN_NOT_READY)
            return
        }

        val bidderToken = BidTokenProvider.getBidderToken().orEmpty()
        IronLog.ADAPTER_API.verbose(FyberConstants.Logs.TOKEN.format(bidderToken))
        val ret: MutableMap<String?, Any?> = mutableMapOf(FyberConstants.TOKEN_KEY to bidderToken)
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}

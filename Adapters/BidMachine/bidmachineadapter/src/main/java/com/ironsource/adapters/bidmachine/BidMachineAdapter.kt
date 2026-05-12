package com.ironsource.adapters.bidmachine

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import io.bidmachine.AdPlacementConfig
import io.bidmachine.BidMachine
import io.bidmachine.utils.BMError
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class BidMachineAdapter : LevelPlayBaseAdapter() {

    companion object {
        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS
        }

        private const val GitHash: String = BuildConfig.GitHash

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        @JvmStatic
        fun getLoadErrorType(error: BMError): AdapterErrorType {
            return when (error.code) {
                BMError.NO_CONTENT -> AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
                else -> AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = BidMachineConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = BidMachine.VERSION

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate sourceId first before any other checks
        val sourceId = adData.getString(BidMachineConstants.SOURCE_ID_KEY)
        if (sourceId.isNullOrEmpty()) {
            val errorMessage = BidMachineConstants.Logs.MISSING_PARAM.format(BidMachineConstants.SOURCE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(BMError.NO_CONTENT, errorMessage)
            return
        }

        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Add listener to list if initialization is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        // Start initialization only once using atomic boolean
        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.SOURCE_ID.format(sourceId))

            // Set log level
            BidMachine.setLoggingEnabled(isAdaptersDebugEnabled())

            // Initialize BidMachine SDK
            BidMachine.initialize(context.applicationContext, sourceId) {
                // BidMachine's initialization callback currently doesn't give any indication to initialization failure.
                // Once this callback is called we will treat the initialization as successful
                initializationSuccess()
            }
        }
    }

    // endregion

    // region BidMachine SDK Init Callbacks

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.META_DATA.format(key ?: "", value))

        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, BidMachineConstants.META_DATA_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.CONSENT.format(consent))
        BidMachine.setSubjectToGDPR(true)
        BidMachine.setConsentConfig(consent, null)
    }

    private fun setCCPAValue(value: Boolean) {
        val ccpaConsentString = if (value) {
            BidMachineConstants.CCPA_NO_CONSENT_VALUE
        } else {
            BidMachineConstants.CCPA_CONSENT_VALUE
        }
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.CCPA.format(ccpaConsentString))
        BidMachine.setUSPrivacyString(ccpaConsentString)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.COPPA.format(value))
        BidMachine.setCoppa(value)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(
        context: Context,
        biddingDataCallback: BiddingDataCallback,
        adPlacementConfig: AdPlacementConfig
    ) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error(BidMachineConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(BidMachineConstants.Logs.TOKEN_ERROR))
            return
        }

        BidMachine.getBidToken(context.applicationContext, adPlacementConfig) { token ->
            if (token.isNullOrEmpty()) {
                IronLog.INTERNAL.error(BidMachineConstants.TOKEN_EMPTY)
                biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(BidMachineConstants.TOKEN_EMPTY))
                return@getBidToken
            }

            IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.TOKEN.format(token))
            val result: MutableMap<String, Any> = HashMap()
            result[BidMachineConstants.TOKEN_KEY] = token
            biddingDataCallback.onSuccess(result)
        }
    }

    // endregion
}

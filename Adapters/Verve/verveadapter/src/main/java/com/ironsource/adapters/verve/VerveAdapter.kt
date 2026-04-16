package com.ironsource.adapters.verve

import android.app.Application
import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import net.pubnative.lite.sdk.HyBid
import net.pubnative.lite.sdk.utils.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class VerveAdapter() : LevelPlayBaseAdapter() {

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
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = VerveConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = HyBid.getSDKVersionInfo()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate appToken first before any other checks
        val appToken = adData.getString(VerveConstants.APP_TOKEN_KEY)
        if (appToken.isNullOrEmpty()) {
            val errorMessage = VerveConstants.Logs.MISSING_PARAM.format(VerveConstants.APP_TOKEN_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized (most common case after first init)
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Check if init failed previously
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(VerveConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, VerveConstants.Logs.SDK_INIT_FAILED)
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(VerveConstants.Logs.APP_TOKEN.format(appToken))

            // Set log level
            if (isAdaptersDebugEnabled()) {
                HyBid.setLogLevel(Logger.Level.debug)
            }

            // Init Verve SDK
            HyBid.initialize(
                appToken,
                context.applicationContext as Application
            ) { success ->
                if (success) {
                    onInitializationSuccess()
                } else {
                    onInitializationFailure()
                }
            }
        }
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(VerveConstants.Logs.META_DATA_SET.format(key ?: "", value))
        val formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, VerveConstants.META_DATA_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(value: Boolean) {
        val ccpaConsentString = if (value) {
            VerveConstants.META_DATA_CCPA_YES_VALUE
        } else {
            VerveConstants.META_DATA_CCPA_NO_VALUE
        }
        IronLog.ADAPTER_API.verbose(VerveConstants.Logs.CCPA_VALUE.format(ccpaConsentString))
        HyBid.getUserDataManager().iabusPrivacyString = ccpaConsentString
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(VerveConstants.Logs.COPPA_VALUE.format(value))
        HyBid.setCoppaEnabled(value)
    }

    // endregion

    // region Helper Methods

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun onInitializationFailure() {
        IronLog.ADAPTER_CALLBACK.error(VerveConstants.Logs.INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, VerveConstants.Logs.INIT_FAILED)
        }

        initCallbackListeners.clear()
    }

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(VerveConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(VerveConstants.Logs.TOKEN_ERROR)
            return
        }

        val bidToken = HyBid.getCustomRequestSignalData(VerveConstants.MEDIATION_NAME)
        val ret: MutableMap<String?, Any?> = HashMap()
        IronLog.ADAPTER_API.verbose(VerveConstants.Logs.TOKEN.format(bidToken))
        ret[VerveConstants.TOKEN_KEY] = bidToken
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}

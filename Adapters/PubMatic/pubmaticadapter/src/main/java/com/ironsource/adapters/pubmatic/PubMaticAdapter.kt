package com.ironsource.adapters.pubmatic

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.pubmatic.sdk.common.OpenWrapSDK
import com.pubmatic.sdk.common.OpenWrapSDKConfig
import com.pubmatic.sdk.common.OpenWrapSDKInitializer
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig
import com.pubmatic.sdk.openwrap.core.signal.POBSignalGenerator
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class PubMaticAdapter : LevelPlayBaseAdapter() {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private const val GitHash: String = BuildConfig.GitHash

        // Bidding host used when generating signals and loading bidding ads
        val BIDDING_HOST: POBBiddingHost = POBBiddingHost.UNITYLEVELPLAY

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        @JvmStatic
        fun getLoadError(error: POBError): AdapterErrorType {
            return when (error.errorCode) {
                POBError.NO_ADS_AVAILABLE -> AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
                else -> AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = PubMaticConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = OpenWrapSDK.getVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate publisherId and profileId first before any other checks
        val publisherId = adData.getString(PubMaticConstants.PUBLISHER_ID_KEY)
        if (publisherId.isNullOrEmpty()) {
            val errorMessage = PubMaticConstants.Logs.MISSING_PARAM.format(PubMaticConstants.PUBLISHER_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        val profileId = adData.getString(PubMaticConstants.PROFILE_ID_KEY)?.toIntOrNull()
        if (profileId == null) {
            val errorMessage = PubMaticConstants.Logs.MISSING_PARAM.format(PubMaticConstants.PROFILE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized successfully
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Check if init failed previously
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(PubMaticConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                PubMaticConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(PubMaticConstants.Logs.PUBLISHER_ID_AND_PROFILE_ID.format(publisherId, profileId))

            // Set log level
            if (isAdaptersDebugEnabled()) {
                OpenWrapSDK.setLogLevel(OpenWrapSDK.LogLevel.Debug)
            }

            val sdkConfig: OpenWrapSDKConfig = OpenWrapSDKConfig.Builder(publisherId, listOf(profileId)).build()

            OpenWrapSDK.initialize(
                context.applicationContext,
                sdkConfig,
                object : OpenWrapSDKInitializer.Listener {
                    override fun onSuccess() {
                        onInitializationSuccess()
                    }

                    override fun onFailure(error: POBError) {
                        onInitializationFailure(error)
                    }
                })
        }
    }

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(PubMaticConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun onInitializationFailure(error: POBError) {
        IronLog.ADAPTER_CALLBACK.error(PubMaticConstants.Logs.INIT_FAILED.format(error.errorMessage, error.errorCode))

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitFailed(error.errorCode, error.errorMessage)
        }

        initCallbackListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(PubMaticConstants.Logs.META_DATA_SET.format(key ?: "", value))
        val formattedValue: String = MetaDataUtils.formatValueForType(
            value,
            MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
        )

        when {
            MetaDataUtils.isValidMetaData(key, PubMaticConstants.META_DATA_PUBMATIC_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(PubMaticConstants.Logs.COPPA.format(value))
        OpenWrapSDK.setCoppa(value)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(
        context: Context,
        biddingDataCallback: BiddingDataCallback,
        adFormat: POBAdFormat
    ) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(PubMaticConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure("${PubMaticConstants.Logs.TOKEN_ERROR} - ${PubMaticConstants.NETWORK_NAME}")
            return
        }

        val signalConfig: POBSignalConfig = POBSignalConfig.Builder(adFormat).build()
        val signal: String = POBSignalGenerator.generateSignal(context.applicationContext, BIDDING_HOST, signalConfig)
        IronLog.ADAPTER_API.verbose(PubMaticConstants.Logs.TOKEN.format(signal))

        val ret: MutableMap<String?, Any?> = HashMap()
        ret[PubMaticConstants.TOKEN_KEY] = signal
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}
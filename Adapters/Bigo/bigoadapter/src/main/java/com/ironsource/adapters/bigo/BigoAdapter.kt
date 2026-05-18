package com.ironsource.adapters.bigo

import android.content.Context
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import org.json.JSONObject
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions
import sg.bigo.ads.api.AdConfig
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class BigoAdapter : LevelPlayBaseAdapter() {

    companion object {

        private const val GitHash: String = BuildConfig.GitHash

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS
        }

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        fun getMediationInfo(): String = JSONObject().apply {
            put(BigoConstants.MEDIATION_INFO_MEDIATION_NAME, BigoConstants.MEDIATION_NAME)
            put(BigoConstants.MEDIATION_INFO_MEDIATION_VERSION, LevelPlay.getSdkVersion())
            put(BigoConstants.MEDIATION_INFO_ADAPTER_VERSION, BigoConstants.ADAPTER_VERSION)
        }.toString()
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = BigoConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = BigoAdSdk.getSDKVersionName()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Extract and validate appId first
        val appId = adData.getString(BigoConstants.APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
            val errorMessage = BigoConstants.Logs.MISSING_PARAM.format(BigoConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized successfully
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Add listener to list if initialization is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        // Start initialization if not called yet
        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(BigoConstants.Logs.APP_ID.format(appId))

            val config = AdConfig.Builder()
                .setDebug(isAdaptersDebugEnabled())
                .setAppId(appId)
                .build()

            BigoAdSdk.initialize(context.applicationContext, config) {
                onInitializationSuccess()
            }
        }
    }

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(BigoConstants.Logs.SDK_INITIALIZED)

        initState = InitState.INIT_STATE_SUCCESS

        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
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
        val value = values[0]
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.META_DATA_SET.format(key ?: "", value ?: ""))
        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, BigoConstants.META_DATA_BIGO_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.CONSENT.format(consent))
        BigoAdSdk.setUserConsent(
            ContextProvider.getInstance().applicationContext, ConsentOptions.GDPR, consent
        )
    }

    private fun setCCPAValue(doNotSell: Boolean) {
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.CCPA.format(doNotSell))
        BigoAdSdk.setUserConsent(
            ContextProvider.getInstance().applicationContext, ConsentOptions.CCPA, !doNotSell
        )
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.COPPA.format(value))
        BigoAdSdk.setUserConsent(
            ContextProvider.getInstance().applicationContext, ConsentOptions.COPPA, !value
        )
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error(BigoConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(BigoConstants.Logs.TOKEN_ERROR)
            return
        }

        val token = BigoAdSdk.getBidderToken()
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.TOKEN.format(token))

        val ret: MutableMap<String?, Any?> = HashMap()
        ret[BigoConstants.TOKEN_KEY] = token
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}

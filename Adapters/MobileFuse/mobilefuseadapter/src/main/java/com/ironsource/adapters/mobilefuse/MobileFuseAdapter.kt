package com.ironsource.adapters.mobilefuse

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.mobilefuse.sdk.MobileFuse
import com.mobilefuse.sdk.MobileFuseSettings
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenProvider
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenRequest
import com.mobilefuse.sdk.internal.TokenGeneratorListener
import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MobileFuseAdapter() : LevelPlayBaseAdapter(), com.mobilefuse.sdk.SdkInitListener {

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
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        // Privacy values
        private var coppaValue: Boolean = false
        private var doNotSellValue: String = MobileFuseConstants.DEFAULT_DO_NOT_SELL_VALUE
        private var doNotTrackValue: Boolean = false
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = MobileFuseConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = MobileFuse.getSdkVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Add listener to list if initialization is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        // Start initialization if not called yet
        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose()

            MobileFuseSettings.setSdkAdapter(MobileFuseConstants.MEDIATION_NAME, adapterVersion)

            // Init MobileFuse SDK
            MobileFuse.init(this)
        }
    }

    // endregion

    // region MobileFuse SDK Init Callbacks

    override fun onInitSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(MobileFuseConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    override fun onInitError() {
        IronLog.ADAPTER_CALLBACK.error(MobileFuseConstants.Logs.INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, MobileFuseConstants.Logs.INIT_FAILED)
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
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.META_DATA.format(key, value))

        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(
                key,
                MobileFuseConstants.META_DATA_COPPA_KEY,
                formattedValue
            ) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.CONSENT.format(consent))
        doNotTrackValue = !consent // true means user agreed to tracking
    }

    private fun setCCPAValue(doNotSell: Boolean) {
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.CCPA.format(doNotSell))
        doNotSellValue = if (doNotSell) {
            MobileFuseConstants.DO_NOT_SELL_YES_VALUE
        } else {
            MobileFuseConstants.DO_NOT_SELL_NO_VALUE
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.COPPA.format(value))
        coppaValue = value
    }

    // endregion

    // region Helper Methods

    private fun getPrivacyData(): MobileFusePrivacyPreferences {
        val builder: MobileFusePrivacyPreferences.Builder = MobileFusePrivacyPreferences.Builder()

        builder.setUsPrivacyConsentString(doNotSellValue)
        builder.setSubjectToCoppa(coppaValue)
        builder.setDoNotTrack(doNotTrackValue)

        return builder.build()
    }

    internal fun collectBiddingData(
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(MobileFuseConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(MobileFuseConstants.Logs.TOKEN_ERROR)
            return
        }

        val tokenRequest = MobileFuseBiddingTokenRequest(
            getPrivacyData(),
            MobileFuseConstants.TEST_MODE
        )

        MobileFuseBiddingTokenProvider.getToken(
            tokenRequest,
            context.applicationContext,
            object : TokenGeneratorListener {

                override fun onTokenGenerated(token: String) {
                    IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.TOKEN.format(token))
                    val result: MutableMap<String, Any> = HashMap()
                    result[MobileFuseConstants.TOKEN_KEY] = token
                    biddingDataCallback.onSuccess(result)
                }

                override fun onTokenGenerationFailed(error: String) {
                    IronLog.ADAPTER_CALLBACK.error(MobileFuseConstants.Logs.TOKEN_GENERATION_FAILED.format(error))
                    biddingDataCallback.onFailure(error)
                }
            }
        )
    }

    // endregion
}

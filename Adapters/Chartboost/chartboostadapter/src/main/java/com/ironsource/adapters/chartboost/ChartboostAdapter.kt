package com.ironsource.adapters.chartboost

import android.content.Context
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.LoggingLevel
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.privacy.model.CCPA
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.privacy.model.DataUseConsent
import com.chartboost.sdk.privacy.model.GDPR
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class ChartboostAdapter : LevelPlayBaseAdapter() {

    companion object {
        private const val GitHash: String = BuildConfig.GitHash

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        // Mediation info
        internal val mediation = Mediation(
            ChartboostConstants.MEDIATION_NAME,
            LevelPlay.getSdkVersion(),
            ChartboostConstants.ADAPTER_VERSION
        )

        // Meta data flags applied once the SDK is initialized
        private var consentCollectingUserData: Boolean? = null
        private var doNotSellCollectingUserData: Boolean? = null
        private var coppaUserData: Boolean? = null

        @JvmStatic
        fun networkAdapterVersion(): String = ChartboostConstants.ADAPTER_VERSION
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = ChartboostConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = Chartboost.getSDKVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val appId = adData.getString(ChartboostConstants.APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
            val errorMessage = ChartboostConstants.Logs.MISSING_PARAM.format(ChartboostConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        val appSignature = adData.getString(ChartboostConstants.APP_SIGNATURE_KEY)
        if (appSignature.isNullOrEmpty()) {
            val errorMessage = ChartboostConstants.Logs.MISSING_PARAM.format(ChartboostConstants.APP_SIGNATURE_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(ChartboostConstants.Logs.INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                ChartboostConstants.Logs.INIT_FAILED
            )
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(ChartboostConstants.Logs.APP_ID_AND_SIGNATURE.format(appId, appSignature))

            consentCollectingUserData?.let { setConsent(it) }
            doNotSellCollectingUserData?.let { setCCPAValue(it) }
            coppaUserData?.let { setCOPPAValue(it) }

            Chartboost.setLoggingLevel(if (isAdaptersDebugEnabled()) LoggingLevel.ALL else LoggingLevel.NONE)
            Chartboost.startWithAppId(context.applicationContext, appId, appSignature) { startError ->
                if (startError == null) {
                    onInitializationSuccess()
                } else {
                    onInitializationFailure()
                }
            }
        }
    }

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun onInitializationFailure() {
        IronLog.ADAPTER_CALLBACK.error(ChartboostConstants.Logs.INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, ChartboostConstants.Logs.INIT_FAILED)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        if (wasInitCalled.get()) {
            IronLog.ADAPTER_API.verbose(
                ChartboostConstants.Logs.CONSENT.format(
                    if (consent) ChartboostConstants.CONSENT_BEHAVIORAL else ChartboostConstants.CONSENT_NON_BEHAVIORAL
                )
            )
            val chartboostConsent =
                if (consent) GDPR.GDPR_CONSENT.BEHAVIORAL else GDPR.GDPR_CONSENT.NON_BEHAVIORAL
            Chartboost.addDataUseConsent(
                ContextProvider.getInstance().applicationContext,
                GDPR(chartboostConsent)
            )
        } else {
            consentCollectingUserData = consent
        }
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(ChartboostConstants.Logs.KEY_VALUE.format(key ?: "", value))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            doNotSellCollectingUserData = MetaDataUtils.getMetaDataBooleanValue(value)
            return
        }

        val formattedValue: String =
            MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)
        if (MetaDataUtils.isValidMetaData(key, ChartboostConstants.META_DATA_COPPA_KEY, formattedValue)) {
            coppaUserData = MetaDataUtils.getMetaDataBooleanValue(formattedValue)
        }
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(
            ChartboostConstants.Logs.CCPA.format(
                if (value) ChartboostConstants.CCPA_OPT_OUT_SALE else ChartboostConstants.CCPA_OPT_IN_SALE
            )
        )
        val dataUseConsent: DataUseConsent =
            CCPA(if (value) CCPA.CCPA_CONSENT.OPT_OUT_SALE else CCPA.CCPA_CONSENT.OPT_IN_SALE)
        Chartboost.addDataUseConsent(ContextProvider.getInstance().applicationContext, dataUseConsent)
    }

    private fun setCOPPAValue(isUserCoppa: Boolean) {
        IronLog.ADAPTER_API.verbose(ChartboostConstants.Logs.COPPA.format(isUserCoppa))
        val dataUseConsent: DataUseConsent = COPPA(isUserCoppa)
        Chartboost.addDataUseConsent(ContextProvider.getInstance().applicationContext, dataUseConsent)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error(ChartboostConstants.Logs.INIT_NOT_COMPLETED)
            biddingDataCallback.onFailure(ChartboostConstants.Logs.INIT_NOT_COMPLETED)
            return
        }

        val returnedToken = Chartboost.getBidderToken().orEmpty()
        IronLog.ADAPTER_API.verbose(ChartboostConstants.Logs.TOKEN.format(returnedToken))
        val biddingDataMap: MutableMap<String?, Any?> = HashMap()
        biddingDataMap[ChartboostConstants.TOKEN_KEY] = returnedToken
        biddingDataCallback.onSuccess(biddingDataMap)
    }

    // endregion
}

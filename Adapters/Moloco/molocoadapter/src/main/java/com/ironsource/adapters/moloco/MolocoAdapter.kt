package com.ironsource.adapters.moloco

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.moloco.sdk.internal.MolocoLogger
import com.moloco.sdk.publisher.Initialization
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.init.MolocoInitParams
import com.moloco.sdk.publisher.privacy.MolocoPrivacy
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MolocoAdapter : LevelPlayBaseAdapter() {

    companion object {
        private const val GitHash: String = BuildConfig.GitHash

        internal val mediationInfo = MediationInfo(MolocoConstants.MEDIATION_NAME)

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
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = MolocoConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = com.moloco.sdk.BuildConfig.SDK_VERSION_NAME

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val appKey = adData.getString(MolocoConstants.APP_KEY)
        val adUnitId = adData.getString(MolocoConstants.AD_UNIT_ID_KEY)

        if (appKey.isNullOrEmpty()) {
            val errorMessage = MolocoConstants.Logs.MISSING_PARAM.format(MolocoConstants.APP_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = MolocoConstants.Logs.MISSING_PARAM.format(MolocoConstants.AD_UNIT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Check if initialization failed
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, MolocoConstants.Logs.SDK_INIT_FAILED)
            return
        }

        // Add listener to list if initialization is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        // Start initialization if not called yet
        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.APP_KEY_AND_AD_UNIT_ID.format(appKey, adUnitId))

            // Set log level
            MolocoLogger.logEnabled = isAdaptersDebugEnabled()

            // Init Moloco SDK
            Moloco.initialize(MolocoInitParams(context.applicationContext, appKey, mediationInfo)) { molocoInitStatus ->
                val description = molocoInitStatus.description
                if (molocoInitStatus.initialization == Initialization.SUCCESS) {
                    onInitializationSuccess(description)
                } else {
                    onInitializationFailure(description)
                }
            }
        }
    }

    private fun onInitializationSuccess(description: String) {
        IronLog.ADAPTER_CALLBACK.verbose(MolocoConstants.Logs.INIT_SUCCESS.format(description))

        initState = InitState.INIT_STATE_SUCCESS

        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun onInitializationFailure(errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(MolocoConstants.Logs.INIT_ERROR.format(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage))

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.CONSENT.format(consent))
        val privacy = MolocoPrivacy.PrivacySettings(
            isUserConsent = consent,
            isAgeRestrictedUser = null,
            isDoNotSell = null
        )
        MolocoPrivacy.setPrivacy(privacy)
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.KEY_VALUE.format(key, value))
        val formattedValue: String = MetaDataUtils.formatValueForType(
            value,
            MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
        )

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, MolocoConstants.META_DATA_MOLOCO_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.VALUE.format(value))
        val privacy = MolocoPrivacy.PrivacySettings(
            isUserConsent = null,
            isAgeRestrictedUser = null,
            isDoNotSell = value
        )
        MolocoPrivacy.setPrivacy(privacy)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.IS_COPPA.format(value))
        val privacy = MolocoPrivacy.PrivacySettings(
            isUserConsent = null,
            isAgeRestrictedUser = value,
            isDoNotSell = null
        )
        MolocoPrivacy.setPrivacy(privacy)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(MolocoConstants.Logs.INIT_NOT_COMPLETED)
            biddingDataCallback.onFailure(MolocoConstants.Logs.INIT_NOT_COMPLETED_TOKEN)
            return
        }
        Moloco.getBidToken(mediationInfo, context.applicationContext) { bidToken, error ->
            if (error == null) {
                val biddingDataMap: MutableMap<String?, Any?> = HashMap()
                IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.TOKEN.format(bidToken))
                biddingDataMap[MolocoConstants.TOKEN_KEY] = bidToken
                biddingDataCallback.onSuccess(biddingDataMap)
            } else {
                val errorMessage = MolocoConstants.Logs.FAILED_TO_RECEIVE_TOKEN.format(error.errorCode, error.description)
                IronLog.ADAPTER_API.error(errorMessage)
                biddingDataCallback.onFailure(errorMessage)
            }
        }
    }

    // endregion
}

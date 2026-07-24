package com.ironsource.adapters.hyprmx

import android.content.Context
import com.hyprmx.android.sdk.consent.ConsentStatus
import com.hyprmx.android.sdk.core.HyprMX
import com.hyprmx.android.sdk.utility.HyprMXLog
import com.hyprmx.android.sdk.utility.HyprMXProperties
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

class HyprMXAdapter : LevelPlayBaseAdapter() {

    companion object {
        private const val GitHash: String = BuildConfig.GitHash

        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = HyprMXConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = HyprMXProperties.version

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val distributorId = adData.getString(HyprMXConstants.DISTRIBUTOR_ID_KEY)
        val propertyId = adData.getString(HyprMXConstants.PROPERTY_ID_KEY)

        if (distributorId.isNullOrEmpty()) {
            val errorMessage = HyprMXConstants.Logs.MISSING_PARAM.format(HyprMXConstants.DISTRIBUTOR_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (propertyId.isNullOrEmpty()) {
            val errorMessage = HyprMXConstants.Logs.MISSING_PARAM.format(HyprMXConstants.PROPERTY_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(HyprMXConstants.Logs.INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, HyprMXConstants.Logs.INIT_FAILED)
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.DISTRIBUTOR_ID.format(distributorId))

            HyprMXLog.enableDebugLogs(isAdaptersDebugEnabled())
            HyprMX.setMediationProvider(HyprMXConstants.MEDIATION_NAME, LevelPlay.getSdkVersion(), HyprMXConstants.ADAPTER_VERSION)
            HyprMX.initialize(context.applicationContext, distributorId) { result ->
                if (result.success) {
                    onInitializationSuccess()
                } else {
                    onInitializationFailure(result.message)
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

    private fun onInitializationFailure(errorMessage: String?) {
        val message = errorMessage ?: HyprMXConstants.Logs.INIT_FAILED
        IronLog.ADAPTER_CALLBACK.error(message)

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, message)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.CONSENT.format(consent))
        HyprMX.setConsentStatus(if (consent) ConsentStatus.CONSENT_GIVEN else ConsentStatus.CONSENT_DECLINED)
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) {
            return
        }

        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.KEY_VALUE.format(key, value))

        val formattedValue = MetaDataUtils.formatValueForType(
            value,
            MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
        )

        if (MetaDataUtils.isValidMetaData(key, HyprMXConstants.META_DATA_AGE_RESTRICTION_KEY, formattedValue)) {
            setAgeRestrictionValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
        }
    }

    private fun setAgeRestrictionValue(ageRestricted: Boolean) {
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.AGE_RESTRICTED.format(ageRestricted))
        HyprMX.setAgeRestrictedUser(ageRestricted)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(HyprMXConstants.Logs.INIT_NOT_COMPLETED)
            biddingDataCallback.onFailure(HyprMXConstants.Logs.INIT_NOT_COMPLETED_TOKEN)
            return
        }

        val returnedToken = HyprMX.sessionToken()
        if (returnedToken.isNullOrEmpty()) {
            IronLog.INTERNAL.verbose(HyprMXConstants.Logs.TOKEN_FAILED)
            biddingDataCallback.onFailure(HyprMXConstants.Logs.TOKEN_FAILED_TOKEN)
            return
        }

        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.TOKEN.format(returnedToken))
        val biddingDataMap: MutableMap<String?, Any?> = HashMap()
        biddingDataMap[HyprMXConstants.TOKEN_KEY] = returnedToken
        biddingDataCallback.onSuccess(biddingDataMap)
    }

    // endregion
}

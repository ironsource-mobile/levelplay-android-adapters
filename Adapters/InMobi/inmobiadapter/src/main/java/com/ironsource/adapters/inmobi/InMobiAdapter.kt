package com.ironsource.adapters.inmobi

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class InMobiAdapter : LevelPlayBaseAdapter() {

    companion object {
        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private const val GitHash = BuildConfig.GitHash

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        // Indicates whether setAgeRestriction was called once
        private var isAgeRestrictionCalled: AtomicBoolean = AtomicBoolean(false)

        // MetaData
        private var consentCollectingUserData: String? = null
        var ageRestrictionCollectingUserData: Boolean? = null
        private var doNotSellCollectingUserData: Boolean? = null

        // Main thread handler
        private val mainHandler = Handler(Looper.getMainLooper())
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = InMobiConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = InMobiSdk.getVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate accountId and placementId first before any other checks
        val accountId = adData.getString(InMobiConstants.ACCOUNT_ID_KEY)
        val placementId = adData.getString(InMobiConstants.PLACEMENT_ID_KEY)

        if (accountId.isNullOrEmpty()) {
            val errorMessage = InMobiConstants.Logs.MISSING_PARAM.format(InMobiConstants.ACCOUNT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (placementId?.toLongOrNull() == null) {
            val errorMessage = InMobiConstants.Logs.MISSING_PARAM.format(InMobiConstants.PLACEMENT_ID_KEY)
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
            IronLog.INTERNAL.error(InMobiConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, InMobiConstants.Logs.SDK_INIT_FAILED)
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.ACCOUNT_ID_PLACEMENT_ID.format(accountId, placementId))

            // Set log level
            InMobiSdk.setLogLevel(if (isAdaptersDebugEnabled()) InMobiSdk.LogLevel.DEBUG else InMobiSdk.LogLevel.NONE)

            // Init SDK on main thread
            mainHandler.post {
                InMobiSdk.init(context, accountId, getConsentObject(), object : SdkInitializationListener {
                    override fun onInitializationComplete(error: Error?) {
                        if (error != null) {
                            onInitializationFailure(error.message ?: InMobiConstants.Logs.SDK_INIT_FAILED)
                        } else {
                            onInitializationSuccess()
                        }
                    }
                })
            }
        }
    }

    // endregion

    // region Initialization Callbacks

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(InMobiConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        // Set age restriction if it was set before init (only once)
        if (isAgeRestrictionCalled.compareAndSet(false, true)) {
            ageRestrictionCollectingUserData?.let {
                setAgeRestricted(it)
            }
        }

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun onInitializationFailure(errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(InMobiConstants.Logs.INIT_FAILED.format(errorMessage))

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
        }

        initCallbackListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.CONSENT.format(consent))
        consentCollectingUserData = consent.toString()

        if (initState == InitState.INIT_STATE_SUCCESS) {
            InMobiSdk.updateGDPRConsent(getConsentObject())
        }
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.META_DATA_KEY_VALUE.format(key, value))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            doNotSellCollectingUserData = MetaDataUtils.getMetaDataBooleanValue(value)
            return
        }

        val formattedValue = MetaDataUtils.formatValueForType(value, MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        if (MetaDataUtils.isValidMetaData(key, InMobiConstants.META_DATA_AGE_RESTRICTED_KEY, formattedValue) ||
            MetaDataUtils.isValidMetaData(key, InMobiConstants.META_DATA_CHILD_DIRECTED_KEY, formattedValue)
        ) {
            setAgeRestricted(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
        }
    }

    // endregion

    // region Helper Methods

    internal fun setAgeRestricted(isAgeRestricted: Boolean) {
        if (initState == InitState.INIT_STATE_SUCCESS) {
            IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.AGE_RESTRICTED.format(isAgeRestricted))
            InMobiSdk.setIsAgeRestricted(isAgeRestricted)
        } else {
            ageRestrictionCollectingUserData = isAgeRestricted
        }
    }

    private fun getConsentObject(): JSONObject {
        val obj = JSONObject()
        try {
            if (!consentCollectingUserData.isNullOrEmpty()) {
                obj.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, consentCollectingUserData)
            }
        } catch (e: JSONException) {
            IronLog.INTERNAL.error(e.toString())
        }
        return obj
    }

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            val errorMessage = InMobiConstants.Logs.TOKEN_NULL
            IronLog.INTERNAL.verbose(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        val bidderToken = InMobiSdk.getToken(getExtrasMap(), InMobiConstants.EMPTY_STRING)
        val returnedToken = bidderToken.orEmpty()
        IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.TOKEN.format(returnedToken))

        val ret: MutableMap<String, Any> = HashMap()
        ret[InMobiConstants.TOKEN_KEY] = returnedToken
        biddingDataCallback.onSuccess(ret)
    }

    internal fun getExtrasMap(): Map<String, String> {
        val map = HashMap<String, String>()
        map[InMobiConstants.EXTRAS_TP_KEY] = InMobiConstants.EXTRAS_TP_VALUE
        map[InMobiConstants.EXTRAS_TP_VER_KEY] = getAdapterVersion()

        doNotSellCollectingUserData?.let {
            map[InMobiConstants.INMOBI_DO_NOT_SELL_KEY] = if (it) {
                InMobiConstants.INMOBI_DO_NOT_SELL_VALUE_TRUE
            } else {
                InMobiConstants.INMOBI_DO_NOT_SELL_VALUE_FALSE
            }
        }
        return map
    }

    // endregion
}

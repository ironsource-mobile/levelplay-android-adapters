package com.ironsource.adapters.pangle

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGPAConsentType.*
import com.bytedance.sdk.openadsdk.api.bidding.PAGBiddingRequest
import com.bytedance.sdk.openadsdk.api.init.PAGBidCallback
import com.bytedance.sdk.openadsdk.api.init.PAGBidError
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.bytedance.sdk.openadsdk.api.init.PAGSdk.PAGInitCallback
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class PangleAdapter() : LevelPlayBaseAdapter() {

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

        // Pangle Builder
        private val pagConfigBuilder = PAGConfig.Builder()

        // Pangle defaultChildDirected
        private var childDirected = PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_DEFAULT

        private val mainHandler = Handler(Looper.getMainLooper())

        @JvmStatic
        fun networkAdapterVersion(): String = PangleConstants.ADAPTER_VERSION
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = PangleConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = PAGSdk.getSDKVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val appId = adData.getString(PangleConstants.APP_ID_KEY)
        val slotId = adData.getString(PangleConstants.SLOT_ID_KEY)

        if (appId.isNullOrEmpty()) {
            val errorMessage = PangleConstants.Logs.MISSING_PARAM.format(PangleConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (slotId.isNullOrEmpty()) {
            val errorMessage = PangleConstants.Logs.MISSING_PARAM.format(PangleConstants.SLOT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if SDK is already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Check if init failed previously
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(PangleConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, PangleConstants.Logs.SDK_INIT_FAILED)
            return
        }

        // Add listener to list if initialization is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        // Start initialization if not called yet
        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(PangleConstants.Logs.APP_ID_AND_SLOT_ID.format(appId, slotId))

            // Check if user is a child
            if (isCoppaChildUser()) {
                onInitializationFailure(PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_CODE, PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_MSG)
                return
            }

            val initConfig = pagConfigBuilder
                .appId(appId)
                .setAdxId(PangleConstants.LEVELPLAY_ADXID)
                .setUserData(getMediationInfo())
                .debugLog(isAdaptersDebugEnabled())
                // supportMultiProcess is an old API that will be deprecated in future versions, in the meantime set it to false
                .supportMultiProcess(false)
                .build()

            mainHandler.post {
                // Init Pangle SDK
                PAGSdk.init(context.applicationContext, initConfig, object : PAGInitCallback {
                    override fun success() {
                        onInitializationSuccess()
                    }

                    override fun fail(code: Int, message: String) {
                        onInitializationFailure(code, message)
                    }
                })
            }
        }
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.META_DATA_VALUE.format(key, value))

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, PangleConstants.META_DATA_PANGLE_COPPA_KEY, value) -> {
                setCOPPAValue(value)
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(PangleConstants.PANGLE_GDPR_CONSENT_MSG)
    }

    private fun setCCPAValue(doNotSell: Boolean) {
        val ccpaValue: Int
        val ccpaValueString: String

        if (doNotSell) {
            ccpaValue = PAG_PA_CONSENT_TYPE_NO_CONSENT
            ccpaValueString = PangleConstants.CONSENT_TYPE_NO_CONSENT_STRING
        } else {
            ccpaValue = PAG_PA_CONSENT_TYPE_CONSENT
            ccpaValueString = PangleConstants.CONSENT_TYPE_CONSENT_STRING
        }

        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.CCPA_VALUE.format(ccpaValueString))
        pagConfigBuilder.setPAConsent(ccpaValue)
    }

    private fun setCOPPAValue(value: String) {
        val coppaValueString: String
        when (value.toIntOrNull()) {
            PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_CHILD -> {
                childDirected = PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_CHILD
                coppaValueString = PangleConstants.CHILD_DIRECTED_TYPE_CHILD_STRING
            }

            PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_NON_CHILD -> {
                childDirected = PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_NON_CHILD
                coppaValueString = PangleConstants.CHILD_DIRECTED_TYPE_NON_CHILD_STRING
            }

            else -> {
                childDirected = PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_DEFAULT
                coppaValueString = PangleConstants.CHILD_DIRECTED_TYPE_DEFAULT_STRING
            }
        }
        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.COPPA_VALUE.format(coppaValueString))
    }

    // endregion

    // region Helper Methods

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun onInitializationFailure(errorCode: Int, errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.INIT_FAILED.format(errorCode, errorMessage))

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener in initListeners) {
            listener.onInitFailed(errorCode, errorMessage)
        }

        initListeners.clear()
    }

    internal fun isCoppaChildUser(): Boolean {
        return childDirected == PangleConstants.PANGLE_CHILD_DIRECTED_TYPE_CHILD
    }

    private fun getMediationInfo(): String {
        val mediationInfo = JSONArray()
        try {
            val mediationObject = JSONObject()
            mediationObject.put(PangleConstants.NAME_KEY, PangleConstants.MEDIATION_NAME_KEY)
            mediationObject.put(PangleConstants.VALUE_KEY, PangleConstants.MEDIATION_NAME)
            mediationInfo.put(mediationObject)

            val adapterObject = JSONObject()
            adapterObject.put(PangleConstants.NAME_KEY, PangleConstants.ADAPTER_VERSION_KEY)
            adapterObject.put(PangleConstants.VALUE_KEY, PangleConstants.ADAPTER_VERSION)
            mediationInfo.put(adapterObject)

            IronLog.INTERNAL.verbose(PangleConstants.Logs.MEDIATION_INFO.format(mediationInfo))
        } catch (exception: JSONException) {
            IronLog.INTERNAL.error(PangleConstants.Logs.MEDIATION_INFO_ERROR.format(exception))
        }

        return mediationInfo.toString()
    }

    internal fun collectBiddingData(context: Context, slotId: String?, biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(PangleConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(PangleConstants.Logs.TOKEN_ERROR)
            return
        }

        if (isCoppaChildUser()) {
            IronLog.INTERNAL.verbose(PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_MSG)
            biddingDataCallback.onFailure(PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_MSG)
            return
        }

        val pagBiddingRequest = PAGBiddingRequest().apply {
            this.slotId = slotId
            adxId = PangleConstants.LEVELPLAY_ADXID
        }

        PAGSdk.getBiddingToken(context.applicationContext, pagBiddingRequest, object :
            PAGBidCallback {
            override fun onBiddingTokenCollected(bidToken: String?) {
                if (!bidToken.isNullOrEmpty()) {
                    IronLog.ADAPTER_API.verbose(PangleConstants.Logs.TOKEN_COLLECTED.format(bidToken))
                    val data = mutableMapOf<String, Any>()
                    data[PangleConstants.TOKEN_KEY] = bidToken
                    biddingDataCallback.onSuccess(data)
                } else {
                    IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.TOKEN_FAILED)
                    biddingDataCallback.onFailure(PangleConstants.Logs.TOKEN_FAILED)
                }
            }

            override fun onBiddingTokenFailed(pagBidError: PAGBidError?) {
                IronLog.ADAPTER_CALLBACK.error(PangleConstants.Logs.TOKEN_FAILED.format(pagBidError?.message))
                biddingDataCallback.onFailure(PangleConstants.Logs.TOKEN_FAILED.format(pagBidError?.message))
            }
        })
    }

    // endregion
}

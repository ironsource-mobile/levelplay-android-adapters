package com.ironsource.adapters.mintegral

import android.content.Context
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.MBridgeSDK
import com.mbridge.msdk.foundation.same.net.Aa
import com.mbridge.msdk.mbbid.out.BidConstants
import com.mbridge.msdk.mbbid.out.BidManager
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.mbridge.msdk.out.SDKInitStatusListener
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MintegralAdapter : LevelPlayBaseAdapter(), SDKInitStatusListener {

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

        private var consentCollectingUserData: Boolean? = null
        private var doNotSellCollectingUserData: Boolean? = null
        private var coppaUserData: Boolean? = null
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = MintegralConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = com.mbridge.msdk.out.MBConfiguration.SDK_VERSION

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val appId = adData.getString(MintegralConstants.APP_ID_KEY)
        val appKey = adData.getString(MintegralConstants.APP_KEY)
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)

        if (appId.isNullOrEmpty()) {
            val errorMessage = MintegralConstants.Logs.MISSING_PARAM.format(MintegralConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (appKey.isNullOrEmpty()) {
            val errorMessage = MintegralConstants.Logs.MISSING_PARAM.format(MintegralConstants.APP_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (placementId.isNullOrEmpty()) {
            val errorMessage = MintegralConstants.Logs.MISSING_PARAM.format(MintegralConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(MintegralConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, MintegralConstants.Logs.SDK_INIT_FAILED)
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.APP_ID_AND_APP_KEY.format(appId, appKey))

            if (isAdaptersDebugEnabled()) {
                MBridgeConstans.DEBUG = true
            }

            val sdk: MBridgeSDK = MBridgeSDKFactory.getMBridgeSDK()
            val map = sdk.getMBConfigurationMap(appId, appKey)

            setChannelCode()

            consentCollectingUserData?.let {
                setConsent(it)
            }

            doNotSellCollectingUserData?.let {
                setCCPAValue(it)
            }

            sdk.init(map, context.applicationContext, this)
        }
    }

    override fun onInitSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        coppaUserData?.let {
            setCOPPAValue(it)
        }

        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    override fun onInitFail(errorMsg: String) {
        IronLog.ADAPTER_CALLBACK.error(MintegralConstants.Logs.INIT_FAILED.format(errorMsg))

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMsg)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.CONSENT.format(consent))
        when (initState) {
            InitState.INIT_STATE_NONE -> consentCollectingUserData = consent
            InitState.INIT_STATE_IN_PROGRESS -> {
                val sdk: MBridgeSDK = MBridgeSDKFactory.getMBridgeSDK()
                val consentStatus: Int = if (consent) MBridgeConstans.IS_SWITCH_ON else MBridgeConstans.IS_SWITCH_OFF
                IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.CONSENT_STATUS.format(consentStatus))
                sdk.setConsentStatus(ContextProvider.getInstance().applicationContext, consentStatus)
            }
            else -> {}
        }
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) {
            return
        }

        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.KEY_VALUE.format(key, value))

        val formattedValue = MetaDataUtils.formatValueForType(
            value,
            MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
        )

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }
            MetaDataUtils.isValidMetaData(key, MintegralConstants.META_DATA_MINTEGRAL_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(ccpa: Boolean) {
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.DO_NOT_TRACK_STATUS.format(ccpa))
        when (initState) {
            InitState.INIT_STATE_NONE -> doNotSellCollectingUserData = ccpa
            InitState.INIT_STATE_IN_PROGRESS -> {
                val sdk: MBridgeSDK = MBridgeSDKFactory.getMBridgeSDK()
                sdk.setDoNotTrackStatus(ContextProvider.getInstance().applicationContext, ccpa)
            }
            else -> {}
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.COPPA_VALUE.format(value))
        when (initState) {
            InitState.INIT_STATE_SUCCESS -> {
                val sdk: MBridgeSDK = MBridgeSDKFactory.getMBridgeSDK()
                sdk.setCoppaStatus(ContextProvider.getInstance().applicationContext, value)
            }
            else -> coppaUserData = value
        }
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(
        context: Context,
        adType: String,
        placementId: String?,
        unitId: String?,
        biddingDataCallback: BiddingDataCallback
    ) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(MintegralConstants.Logs.INIT_NOT_COMPLETED)
            biddingDataCallback.onFailure(MintegralConstants.Logs.INIT_NOT_COMPLETED_TOKEN)
            return
        }

        val adConfig = mutableMapOf<String, String>()
        placementId?.let { adConfig[BidConstants.BID_FILTER_KEY_PLACEMENT_ID] = it }
        unitId?.let { adConfig[BidConstants.BID_FILTER_KEY_UNIT_ID] = it }
        adConfig[BidConstants.BID_FILTER_KEY_AD_TYPE] = adType

        val bidderToken = BidManager.getBuyerUid(context.applicationContext, adConfig)
        val returnedToken = bidderToken ?: ""
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.TOKEN.format(returnedToken))

        val biddingDataMap: MutableMap<String?, Any?> = HashMap()
        biddingDataMap[MintegralConstants.TOKEN_KEY] = returnedToken
        biddingDataCallback.onSuccess(biddingDataMap)
    }

    private fun setChannelCode() {
        try {
            val mintegralSdkClass = Aa::class.java
            val method = mintegralSdkClass.getDeclaredMethod(MintegralConstants.CHANNEL_CODE_METHOD, String::class.java)
            method.isAccessible = true
            method.invoke(mintegralSdkClass, MintegralConstants.CHANNEL_CODE_VALUE)
        } catch (th: Throwable) {
            th.printStackTrace()
            IronLog.INTERNAL.error(MintegralConstants.Logs.CHANNEL_CODE_ERROR.format(th))
        }
    }

    // endregion
}

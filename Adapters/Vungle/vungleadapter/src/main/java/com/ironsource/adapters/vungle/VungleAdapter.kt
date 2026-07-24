package com.ironsource.adapters.vungle

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import com.vungle.ads.BidTokenCallback
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleError
import com.vungle.ads.VunglePrivacySettings.setCCPAStatus
import com.vungle.ads.VunglePrivacySettings.setCOPPAStatus
import com.vungle.ads.VunglePrivacySettings.setGDPRStatus
import com.vungle.ads.internal.protos.Sdk.SDKError
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class VungleAdapter : LevelPlayBaseAdapter() {

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

        @JvmStatic
        fun getLoadErrorType(adError: VungleError): AdapterErrorType {
            return if (adError.code == SDKError.Reason.AD_NO_FILL_VALUE) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }

        @JvmStatic
        fun networkAdapterVersion(): String = VungleConstants.ADAPTER_VERSION
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = VungleConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = VungleAds.getSdkVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate appId first before any other checks
        val appId = adData.getString(VungleConstants.APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
            val errorMessage = VungleConstants.Logs.MISSING_PARAM.format(VungleConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Init previously failed - report failure immediately
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(VungleConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                VungleConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(VungleConstants.Logs.APP_ID.format(appId))

            VungleAds.setIntegrationName(VungleConstants.MEDIATION_NAME, VungleConstants.ADAPTER_VERSION)

            VungleAds.init(context.applicationContext, appId, object : InitializationListener {
                override fun onSuccess() {
                    initializationSuccess()
                }

                override fun onError(vungleError: VungleError) {
                    initializationFailure(vungleError.code, vungleError.errorMessage)
                }
            })
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
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.META_DATA_SET.format(key ?: "", value))
        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }

            MetaDataUtils.isValidMetaData(key, VungleConstants.META_DATA_VUNGLE_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.CONSENT.format(consent))
        setGDPRStatus(consent, VungleConstants.META_DATA_VUNGLE_CONSENT_MESSAGE_VERSION)
    }

    private fun setCCPAValue(value: Boolean) {
        // The Vungle CCPA API expects an indication if the user opts in to targeted advertising,
        // opposite to the ironSource Mediation CCPA flag of do_not_sell
        val optIn = !value
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.CCPA.format(optIn))
        setCCPAStatus(optIn)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.COPPA.format(value))
        setCOPPAStatus(value)
    }

    // endregion

    // region Helper Methods

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(VungleConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun initializationFailure(errorCode: Int, errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(VungleConstants.Logs.INIT_FAILED.format(errorCode, errorMessage))

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitFailed(errorCode, errorMessage)
        }

        initCallbackListeners.clear()
    }

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback) {
        VungleAds.getBiddingToken(context.applicationContext, object : BidTokenCallback {
            override fun onBidTokenCollected(bidToken: String) {
                val ret: MutableMap<String?, Any?> = HashMap()
                val sdkVersion = getNetworkSDKVersion()
                IronLog.ADAPTER_API.verbose(VungleConstants.Logs.TOKEN.format(sdkVersion, bidToken))
                ret[VungleConstants.SDK_VERSION_KEY] = sdkVersion
                ret[VungleConstants.TOKEN_KEY] = bidToken
                biddingDataCallback.onSuccess(ret)
            }

            override fun onBidTokenError(errorMessage: String) {
                biddingDataCallback.onFailure(VungleConstants.Logs.TOKEN_FAILURE.format(errorMessage))
            }
        })
    }

    // endregion
}

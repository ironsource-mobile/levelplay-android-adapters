package com.ironsource.adapters.unityads

import android.content.Context
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.ads.AdFormat
import com.unity3d.ads.BannerSize
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.LogLevel
import com.unity3d.ads.MediationInfo
import com.unity3d.ads.TokenConfiguration
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.metadata.MetaData
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class UnityAdsAdapter : LevelPlayBaseAdapter() {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private const val GitHash: String = BuildConfig.GitHash

        internal val mediationInfo = MediationInfo(
            UnityAdsConstants.MEDIATION_NAME,
            LevelPlay.getSdkVersion(),
            UnityAdsConstants.ADAPTER_VERSION
        )

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        @JvmStatic
        fun getLoadErrorType(error: UnityAdsError?): AdapterErrorType {
            return if (error?.code == UnityAdsConstants.UNITYADS_NO_FILL_ERROR_CODE) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }
    }

    private val unityAdsStorageLock = Any()

    // region Adapter Methods

    override fun getAdapterVersion(): String = UnityAdsConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = UnityAds.version

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate sourceId first before any other checks
        val sourceId = adData.getString(UnityAdsConstants.SOURCE_ID_KEY)
        if (sourceId.isNullOrEmpty()) {
            val errorMessage = UnityAdsConstants.Logs.MISSING_PARAM.format(UnityAdsConstants.SOURCE_ID_KEY)
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
            IronLog.INTERNAL.error(UnityAdsConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                UnityAdsConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.SOURCE_ID.format(sourceId))

            val initConfig = InitializationConfiguration.Builder(sourceId)
                .withTestMode(false)
                .withMediationInfo(mediationInfo)
                .apply {
                    if (isAdaptersDebugEnabled()) {
                        withLogLevel(LogLevel.DEBUG)
                    }
                    val extras = buildInitExtras(adData)
                    if (extras.isNotEmpty()) {
                        withExtras(extras)
                    }
                }
                .build()

            UnityAds.initialize(initConfig, InitializationListener { error ->
                if (error == null) {
                    initializationSuccess()
                } else {
                    initializationFailure(error.code, error.message.orEmpty())
                }
            })
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    private fun initializationFailure(errorCode: Int, errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.INIT_FAILED.format(errorCode, errorMessage))

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitFailed(errorCode, errorMessage)
        }

        initCallbackListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.CONSENT.format(consent))

        // Both legacy and public privacy apis are called

        // Legacy api call
        setUnityAdsMetaData(UnityAdsConstants.CONSENT_GDPR, consent)

        // Public api call
        UnityAds.userConsent = consent
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.META_DATA_SET.format(key ?: "", value))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
        } else {
            val formattedValue = MetaDataUtils.formatValueForType(value, MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)
            if (MetaDataUtils.isValidMetaData(key, UnityAdsConstants.UNITYADS_METADATA_COPPA_KEY, formattedValue)) {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.CCPA.format(value))

        // The UnityAds CCPA api expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        val optIn = !value

        // Both legacy and public privacy apis are called

        // Legacy api call
        setUnityAdsMetaData(UnityAdsConstants.CONSENT_CCPA, optIn)

        // Public api call
        UnityAds.userOptOut = value
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.COPPA.format(value))

        // Both legacy and public privacy apis are called

        // Legacy api call
        setUnityAdsMetaData(UnityAdsConstants.UNITYADS_COPPA, value)

        // Public api call
        UnityAds.nonBehavioral = value
    }

    // endregion

    // region Helper Methods

    private fun buildInitExtras(adData: AdData): Map<String, String> {
        val extras = mutableMapOf<String, String>()

        adData.getString(UnityAdsConstants.UADS_INIT_BLOB)?.let { initBlob ->
            if (initBlob.isNotEmpty()) {
                extras[UnityAdsConstants.UADS_INIT_BLOB] = initBlob
            }
        }

        (adData.configuration[UnityAdsConstants.UADS_TRAITS] as? Map<*, *>)?.forEach { (key, value) ->
            val keyString = key as? String
            val valueString = value as? String
            if (!keyString.isNullOrEmpty() && !valueString.isNullOrEmpty()) {
                extras[keyString] = valueString
            }
        }

        return extras
    }

    private fun setUnityAdsMetaData(key: String, value: Boolean) {
        IronLog.INTERNAL.verbose(UnityAdsConstants.Logs.META_DATA_SET.format(key, value))

        synchronized(unityAdsStorageLock) {
            val metaData = MetaData(ContextProvider.getInstance().applicationContext)
            metaData[key] = value
            metaData.commit()
        }
    }

    internal fun collectBiddingData(
        adData: AdData?,
        biddingDataCallback: BiddingDataCallback,
        adFormat: AdFormat,
        bannerSize: BannerSize? = null
    ) {
        val builder = TokenConfiguration.Builder(adFormat)
            .withMediationInfo(mediationInfo)

        adData?.getString(UnityAdsConstants.ZONE_ID_KEY)?.let { zoneId ->
            if (zoneId.isNotBlank()) {
                builder.withPlacementId(zoneId)
            }
        }

        (adData?.adUnitData?.get(UnityAdsConstants.AD_UNIT_ID_KEY) as? String)?.let { mediationAdUnitId ->
            if (mediationAdUnitId.isNotBlank()) {
                builder.withMediationAdUnitId(mediationAdUnitId)
            }
        }

        bannerSize?.let { builder.withBannerSize(it) }

        UnityAds.getToken(builder.build()) { bidToken ->
            if (!bidToken.isNullOrEmpty()) {
                IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.TOKEN.format(bidToken))
                biddingDataCallback.onSuccess(mutableMapOf<String, Any>().apply {
                    put(UnityAdsConstants.TOKEN_KEY, bidToken)
                })
            } else {
                IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.TOKEN_FAILURE)
                biddingDataCallback.onFailure(UnityAdsConstants.Logs.TOKEN_FAILURE)
            }
        }
    }

    // endregion
}

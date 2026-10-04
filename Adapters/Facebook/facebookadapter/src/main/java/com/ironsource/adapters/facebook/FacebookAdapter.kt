package com.ironsource.adapters.facebook

import android.content.Context
import com.facebook.ads.AdError
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.BidderTokenProvider
import com.facebook.ads.CacheFlag
import com.ironsource.environment.StringUtils
import com.ironsource.mediationsdk.AdapterNetworkData
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
import org.json.JSONArray
import java.util.EnumSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class FacebookAdapter : LevelPlayBaseAdapter(), AudienceNetworkAds.InitListener {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private const val GitHash: String = BuildConfig.GitHash

        @Suppress("ACCIDENTAL_OVERRIDE")
        @JvmStatic
        fun networkAdapterVersion(): String = FacebookConstants.ADAPTER_VERSION

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        // Collected interstitial cache flags
        internal var interstitialFacebookCacheFlags: EnumSet<CacheFlag> = EnumSet.allOf(CacheFlag::class.java)

        fun getLoadErrorType(adError: AdError): AdapterErrorType =
            if (adError.errorCode == AdError.NO_FILL_ERROR_CODE) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = FacebookConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = com.facebook.ads.BuildConfig.VERSION_NAME

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val placementId = adData.getString(FacebookConstants.PLACEMENT_ID_KEY)
        if (placementId.isNullOrEmpty()) {
            val errorMessage = FacebookConstants.Logs.MISSING_PARAM.format(FacebookConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        val allPlacementIds = adData.getString(FacebookConstants.ALL_PLACEMENT_IDS_KEY)
        if (allPlacementIds.isNullOrEmpty()) {
            val errorMessage = FacebookConstants.Logs.MISSING_PARAM.format(FacebookConstants.ALL_PLACEMENT_IDS_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                FacebookConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            val placementIds = allPlacementIds.split(FacebookConstants.PLACEMENT_IDS_SEPARATOR)
            IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.INIT.format(placementIds))
            AudienceNetworkAds.buildInitSettings(context.applicationContext)
                .withInitListener(this)
                .withMediationService(getMediationServiceInfo())
                .withPlacementIds(placementIds)
                .initialize()
        }
    }

    /**
     * Called by the Meta SDK once the initialization is completed.
     * @param result the init result holding the success status and message
     */
    override fun onInitialized(result: AudienceNetworkAds.InitResult) {
        if (result.isSuccess) {
            onInitializationSuccess()
        } else {
            onInitializationFailure(result.message)
        }
    }

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(FacebookConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun onInitializationFailure(errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(FacebookConstants.Logs.INIT_FAILED.format(errorMessage))

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (key == null || values.isNullOrEmpty()) {
            return
        }

        when (StringUtils.toLowerCase(key)) {
            FacebookConstants.FACEBOOK_INTERSTITIAL_CACHE_FLAG,
            FacebookConstants.META_INTERSTITIAL_CACHE_FLAG -> processCacheFlags(key, values)

            FacebookConstants.META_MIXED_AUDIENCE -> {
                val value = values[0] ?: return
                IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.META_DATA.format(key, value))
                val formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)
                if (formattedValue.isNotEmpty()) {
                    setMixedAudience(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
                }
            }
        }
    }

    override fun setNetworkData(networkData: AdapterNetworkData) {
        val cacheFlags = networkData.dataByKeyIgnoreCase(FacebookConstants.NETWORK_DATA_CACHE_FLAG, JSONArray::class.java)
        if (cacheFlags != null) {
            val values = (0 until cacheFlags.length()).map { cacheFlags.optString(it) }
            processCacheFlags(FacebookConstants.NETWORK_DATA_CACHE_FLAG, values)
        }
    }

    override fun setTestMode(enabled: Boolean) {
        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.TEST_MODE.format(enabled))
        AdSettings.setTestMode(enabled)
    }

    // endregion

    // region Helper Methods

    private fun processCacheFlags(key: String?, values: List<String?>) {
        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.CACHE_FLAGS.format(key, values))

        interstitialFacebookCacheFlags = try {
            val flags = EnumSet.noneOf(CacheFlag::class.java)
            for (value in values) {
                val flag = CacheFlag.valueOf(StringUtils.toUpperCase(value.orEmpty()))
                IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.CACHE_FLAG_VALUE.format(value, flag.name))
                flags.add(flag)
            }
            flags
        } catch (e: Exception) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.CACHE_FLAG_UNKNOWN)
            EnumSet.allOf(CacheFlag::class.java)
        }

        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.CACHE_FLAGS_ALL.format(interstitialFacebookCacheFlags))
    }

    private fun setMixedAudience(isMixedAudience: Boolean) {
        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.MIXED_AUDIENCE.format(isMixedAudience))
        AdSettings.setMixedAudience(isMixedAudience)
    }

    private fun getMediationServiceInfo(): String {
        val mediationServiceInfo = String.format(
            "%s_%s:%s",
            FacebookConstants.MEDIATION_NAME,
            LevelPlay.getSdkVersion(),
            FacebookConstants.ADAPTER_VERSION
        )
        IronLog.INTERNAL.verbose(FacebookConstants.Logs.MEDIATION_SERVICE_INFO.format(mediationServiceInfo))
        return mediationServiceInfo
    }

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback) {
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose(FacebookConstants.Logs.TOKEN_INIT_FAILED)
            biddingDataCallback.onFailure(FacebookConstants.Logs.SDK_INIT_FAILED)
            return
        }

        val bidderToken = BidderTokenProvider.getBidderToken(context.applicationContext)
        val returnedToken = if (!bidderToken.isNullOrEmpty()) bidderToken else ""
        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.TOKEN.format(returnedToken))
        val ret: MutableMap<String, Any> = HashMap()
        ret[FacebookConstants.TOKEN_KEY] = returnedToken
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}

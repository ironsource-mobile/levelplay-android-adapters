package com.ironsource.adapters.applovin

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.applovin.sdk.AppLovinErrorCodes
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class AppLovinAdapter : LevelPlayBaseAdapter() {

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

        // AppLovin sdk instance
        internal var appLovinSdk: AppLovinSdk? = null
            private set

        @JvmStatic
        fun getLoadErrorType(errorCode: Int): AdapterErrorType {
            return if (errorCode == AppLovinErrorCodes.NO_FILL) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }

        @JvmStatic
        fun getErrorString(errorCode: Int): String {
            return when (errorCode) {
                AppLovinErrorCodes.SDK_DISABLED -> AppLovinConstants.Logs.ERROR_SDK_DISABLED
                AppLovinErrorCodes.FETCH_AD_TIMEOUT -> AppLovinConstants.Logs.ERROR_FETCH_AD_TIMEOUT
                AppLovinErrorCodes.NO_NETWORK -> AppLovinConstants.Logs.ERROR_NO_NETWORK
                AppLovinErrorCodes.NO_FILL -> AppLovinConstants.Logs.ERROR_NO_FILL
                AppLovinErrorCodes.UNABLE_TO_RENDER_AD -> AppLovinConstants.Logs.ERROR_UNABLE_TO_RENDER_AD
                AppLovinErrorCodes.INVALID_ZONE -> AppLovinConstants.Logs.ERROR_INVALID_ZONE
                AppLovinErrorCodes.INVALID_AD_TOKEN -> AppLovinConstants.Logs.ERROR_INVALID_AD_TOKEN
                AppLovinErrorCodes.UNSPECIFIED_ERROR -> AppLovinConstants.Logs.ERROR_UNSPECIFIED
                AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED -> AppLovinConstants.Logs.ERROR_INCENTIVIZED_NO_AD_PRELOADED
                AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR -> AppLovinConstants.Logs.ERROR_INCENTIVIZED_UNKNOWN_SERVER_ERROR
                AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT -> AppLovinConstants.Logs.ERROR_INCENTIVIZED_SERVER_TIMEOUT
                AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO -> AppLovinConstants.Logs.ERROR_INCENTIVIZED_USER_CLOSED_VIDEO
                AppLovinErrorCodes.INVALID_RESPONSE -> AppLovinConstants.Logs.ERROR_INVALID_RESPONSE
                AppLovinErrorCodes.INVALID_URL -> AppLovinConstants.Logs.ERROR_INVALID_URL
                AppLovinErrorCodes.UNABLE_TO_PRECACHE_RESOURCES -> AppLovinConstants.Logs.ERROR_UNABLE_TO_PRECACHE_RESOURCES
                AppLovinErrorCodes.UNABLE_TO_PRECACHE_IMAGE_RESOURCES -> AppLovinConstants.Logs.ERROR_UNABLE_TO_PRECACHE_IMAGE_RESOURCES
                AppLovinErrorCodes.UNABLE_TO_PRECACHE_VIDEO_RESOURCES -> AppLovinConstants.Logs.ERROR_UNABLE_TO_PRECACHE_VIDEO_RESOURCES
                AppLovinErrorCodes.INVALID_BODY -> AppLovinConstants.Logs.ERROR_INVALID_BODY
                AppLovinErrorCodes.UNABLE_TO_PRECACHE_HTML_RESOURCES -> AppLovinConstants.Logs.ERROR_UNABLE_TO_PRECACHE_HTML_RESOURCES
                else -> AppLovinConstants.Logs.ERROR_UNKNOWN
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun getAdapterVersion(): String = AppLovinConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = AppLovinSdk.VERSION

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate sdkKey and zoneId first before any other checks
        val sdkKey = adData.getString(AppLovinConstants.SDK_KEY)
        if (sdkKey.isNullOrEmpty()) {
            val errorMessage = AppLovinConstants.Logs.MISSING_PARAM.format(AppLovinConstants.SDK_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        val zoneId = adData.getString(AppLovinConstants.ZONE_ID_KEY)
        if (zoneId.isNullOrEmpty()) {
            val errorMessage = AppLovinConstants.Logs.MISSING_PARAM.format(AppLovinConstants.ZONE_ID_KEY)
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
            IronLog.INTERNAL.error(AppLovinConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AppLovinConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.SDK_KEY.format(sdkKey))

            mainHandler.post {
                val initConfig = try {
                    AppLovinSdkInitializationConfiguration.builder(sdkKey)
                        .setMediationProvider(AppLovinMediationProvider.IRONSOURCE)
                        .build()
                } catch (t: Throwable) {
                    initializationFailure(t.message.orEmpty())
                    return@post
                }

                appLovinSdk = AppLovinSdk.getInstance(context.applicationContext).apply {
                    settings.setVerboseLogging(isAdaptersDebugEnabled())
                }

                // AppLovin's initialization callback currently doesn't give any indication to
                // initialization failure. Once this callback is called we treat the initialization
                // as successful
                appLovinSdk?.initialize(initConfig) { initializationSuccess() }
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun initializationFailure(message: String) {
        IronLog.ADAPTER_CALLBACK.error(AppLovinConstants.Logs.INIT_FAILED.format(message))

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, AppLovinConstants.Logs.SDK_INIT_FAILED)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.CONSENT.format(consent))
        AppLovinPrivacySettings.setHasUserConsent(consent)
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.KEY_VALUE.format(key ?: "", value))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
        }
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.CCPA.format(value))
        AppLovinPrivacySettings.setDoNotSell(value)
    }

    // endregion

}

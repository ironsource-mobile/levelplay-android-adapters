package com.ironsource.adapters.admob

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.banner.BannerSignalRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration.MaxAdContentRating
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration.TagForChildDirectedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration.TagForUnderAgeOfConsent
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialSignalRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeSignalRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedSignalRequest
import com.google.android.libraries.ads.mobile.sdk.signal.Signal
import com.google.android.libraries.ads.mobile.sdk.signal.SignalError
import com.google.android.libraries.ads.mobile.sdk.signal.SignalGenerationCallback
import com.google.android.libraries.ads.mobile.sdk.signal.SignalRequest
import com.ironsource.mediationsdk.AdapterNetworkData
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ironsource.environment.ContextProvider
import com.ironsource.environment.StringUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import org.json.JSONArray
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class AdMobAdapter : LevelPlayBaseAdapter() {

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

        // Shared metadata values between instances
        private var consent: Boolean? = null
        private var ccpaValue: Boolean? = null
        private var coppaValue: TagForChildDirectedTreatment? = null
        private var euValue: TagForUnderAgeOfConsent? = null
        private var ratingValue: MaxAdContentRating? = null
        private var contentMappingUrl: String? = null
        private var neighboringContentMappingUrls: MutableSet<String> = HashSet()

        @Suppress("ACCIDENTAL_OVERRIDE")
        @JvmStatic
        fun networkAdapterVersion(): String = AdMobConstants.ADAPTER_VERSION

        internal fun getLoadErrorType(errorCode: LoadAdError.ErrorCode): AdapterErrorType =
            if (errorCode == LoadAdError.ErrorCode.NO_FILL) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = AdMobConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = MobileAds.getVersion().toString()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun getAdaptiveHeight(width: Int): Int {
        val height = AdSize.getLargeAnchoredAdaptiveBannerAdSize(
            ContextProvider.getInstance().applicationContext, width
        ).height
        IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.ADAPTIVE_HEIGHT.format(height, width))
        return height
    }

    @SuppressLint("MissingPermission")
    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val appId = adData.getString(AdMobConstants.APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
            val errorMessage = AdMobConstants.Logs.MISSING_PARAM.format(AdMobConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized successfully
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Check if init failed previously
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(AdMobConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, AdMobConstants.Logs.SDK_INIT_FAILED)
            return
        }

        // Add listener to list if initialization is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        // Start initialization if not called yet
        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.APP_ID.format(appId))

            val networkOnlyInit = readBoolean(adData.configuration, AdMobConstants.NETWORK_ONLY_INIT_KEY, true)
            val shouldWaitForInitCallback = readBoolean(adData.configuration, AdMobConstants.INIT_RESPONSE_REQUIRED_KEY, false)

            val initConfigBuilder = InitializationConfig.Builder(appId)
            if (networkOnlyInit) {
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.DISABLE_MEDIATION_ADAPTER_INIT)
                // Limit the AdMob initialization to its network
                initConfigBuilder.disableMediationAdapterInitialization()
            }
            val initConfig = initConfigBuilder.build()
            val applicationContext = context.applicationContext

            Thread {
                if (shouldWaitForInitCallback) {
                    IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.INIT_AND_WAIT)
                    MobileAds.initialize(applicationContext, initConfig) { initializationStatus ->
                        val adMobStatus = initializationStatus.adapterStatusMap[AdMobConstants.ADMOB_ADAPTER_CLASS_NAME]
                        if (adMobStatus != null) {
                            IronLog.ADAPTER_API.verbose(
                                AdMobConstants.Logs.INIT_STATE.format(adMobStatus.initializationState, adMobStatus.description)
                            )
                        }

                        if (adMobStatus != null && adMobStatus.initializationState == AdapterStatus.InitializationState.COMPLETE) {
                            onInitializationSuccess()
                        } else {
                            val error = adMobStatus?.description ?: AdMobConstants.Logs.ADAPTER_STATUS_NOT_FOUND
                            onInitializationFailure(error)
                        }
                    }
                } else {
                    IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.INIT_WITHOUT_CALLBACK)
                    MobileAds.initialize(applicationContext, initConfig)
                    onInitializationSuccess()
                }
            }.start()
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

    private fun onInitializationFailure(errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.INIT_FAILED.format(errorMessage))

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, AdMobConstants.Logs.SDK_INIT_FAILED)
        }
        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONSENT.format(consent))
        AdMobAdapter.consent = consent
    }

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) {
            return
        }

        if (values.size > 1 && key.equals(AdMobConstants.ADMOB_CONTENT_MAPPING_KEY, ignoreCase = true)) {
            // Multiple URLs
            neighboringContentMappingUrls = HashSet(values.filterNotNull())
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.META_DATA_VALUES.format(key, values))
            return
        }

        // This is a list of 1 value
        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.META_DATA_VALUE.format(key, value))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
        } else {
            setAdMobMetaDataValue(StringUtils.toLowerCase(key), StringUtils.toLowerCase(value))
        }
    }

    override fun setNetworkData(networkData: AdapterNetworkData) {
        // If the contentMapping key maps to a string
        networkData.dataByKeyIgnoreCase(AdMobConstants.NETWORK_DATA_CONTENT_MAPPING, String::class.java)?.let {
            contentMappingUrl = it
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONTENT_MAPPING_VALUE.format(AdMobConstants.NETWORK_DATA_CONTENT_MAPPING, it))
        }

        // If the contentMapping key maps to an array
        networkData.dataByKeyIgnoreCase(AdMobConstants.NETWORK_DATA_CONTENT_MAPPING, JSONArray::class.java)?.let { array ->
            neighboringContentMappingUrls.clear()
            for (i in 0 until array.length()) {
                neighboringContentMappingUrls.add(array.optString(i))
            }
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONTENT_MAPPING_VALUES.format(AdMobConstants.NETWORK_DATA_CONTENT_MAPPING, neighboringContentMappingUrls))
        }

        networkData.dataByKeyIgnoreCase(AdMobConstants.NETWORK_DATA_CONTENT_RATING, String::class.java)?.let {
            ratingValue = getAdMobRatingValue(StringUtils.toLowerCase(it))
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.RATING_VALUE.format(AdMobConstants.NETWORK_DATA_CONTENT_RATING, ratingValue))
            setRequestConfiguration()
        }
    }

    private fun setCCPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CCPA_VALUE.format(value))
        ccpaValue = value
    }

    private fun setAdMobMetaDataValue(key: String, value: String) {
        var formattedValue = value

        if (key == AdMobConstants.ADMOB_TFCD_KEY || key == AdMobConstants.ADMOB_TFUA_KEY) {
            // AdMob MetaData keys accept only boolean values
            formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN)
            if (formattedValue.isEmpty()) {
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.INVALID_META_DATA_VALUE.format(key, value))
                return
            }
        }

        when (key) {
            AdMobConstants.ADMOB_TFCD_KEY -> {
                coppaValue = getAdMobCoppaValue(formattedValue)
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.COPPA_VALUE.format(key, coppaValue))
            }
            AdMobConstants.ADMOB_TFUA_KEY -> {
                euValue = getAdMobEuValue(formattedValue)
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.EU_VALUE.format(key, euValue))
            }
            AdMobConstants.ADMOB_MAX_RATING_KEY -> {
                ratingValue = getAdMobRatingValue(formattedValue)
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.RATING_VALUE.format(key, ratingValue))
            }
            AdMobConstants.ADMOB_CONTENT_MAPPING_KEY -> {
                contentMappingUrl = value
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONTENT_MAPPING_VALUE.format(key, contentMappingUrl))
            }
        }

        setRequestConfiguration()
    }

    private fun getAdMobCoppaValue(value: String): TagForChildDirectedTreatment =
        if (MetaDataUtils.getMetaDataBooleanValue(value)) {
            TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        } else {
            TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        }

    private fun getAdMobEuValue(value: String): TagForUnderAgeOfConsent =
        if (MetaDataUtils.getMetaDataBooleanValue(value)) {
            TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        } else {
            TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        }

    private fun getAdMobRatingValue(value: String): MaxAdContentRating? {
        if (value.isEmpty()) {
            IronLog.INTERNAL.error(AdMobConstants.Logs.NULL_RATING_VALUE)
            return null
        }

        return when (value) {
            AdMobConstants.ADMOB_MAX_AD_CONTENT_RATING_G -> MaxAdContentRating.MAX_AD_CONTENT_RATING_G
            AdMobConstants.ADMOB_MAX_AD_CONTENT_RATING_PG -> MaxAdContentRating.MAX_AD_CONTENT_RATING_PG
            AdMobConstants.ADMOB_MAX_AD_CONTENT_RATING_T -> MaxAdContentRating.MAX_AD_CONTENT_RATING_T
            AdMobConstants.ADMOB_MAX_AD_CONTENT_RATING_MA -> MaxAdContentRating.MAX_AD_CONTENT_RATING_MA
            else -> {
                IronLog.INTERNAL.error(AdMobConstants.Logs.UNDEFINED_RATING_VALUE.format(value))
                null
            }
        }
    }

    private fun setRequestConfiguration() {
        val builder = RequestConfiguration.Builder()
        var requestConfiguration: RequestConfiguration? = null

        coppaValue?.let { requestConfiguration = builder.setTagForChildDirectedTreatment(it).build() }
        euValue?.let { requestConfiguration = builder.setTagForUnderAgeOfConsent(it).build() }
        ratingValue?.let { requestConfiguration = builder.setMaxAdContentRating(it).build() }

        requestConfiguration?.let { MobileAds.setRequestConfiguration(it) }
    }

    // endregion

    // region Helper Methods

    internal fun createAdRequest(adUnitId: String, adData: AdData?): AdRequest {
        val builder = AdRequest.Builder(adUnitId)
        builder.setRequestAgent(AdMobConstants.REQUEST_AGENT)
        applyContentMapping(builder)
        setRequestConfiguration()
        builder.setGoogleExtrasBundle(createExtrasBundle(adData))
        return builder.build()
    }

    internal fun createBannerAdRequest(adUnitId: String, adSize: AdSize, adData: AdData?): BannerAdRequest {
        val builder = BannerAdRequest.Builder(adUnitId, adSize)
        builder.setRequestAgent(AdMobConstants.REQUEST_AGENT)
        applyContentMapping(builder)
        setRequestConfiguration()
        builder.setGoogleExtrasBundle(createExtrasBundle(adData))
        return builder.build()
    }

    internal fun createNativeAdRequest(adUnitId: String, adChoicesPlacement: AdChoicesPlacement, adData: AdData?): NativeAdRequest {
        val builder = NativeAdRequest.Builder(adUnitId, Collections.singletonList(NativeAd.NativeAdType.NATIVE))
        builder.setRequestAgent(AdMobConstants.REQUEST_AGENT)
        builder.setAdChoicesPlacement(adChoicesPlacement)
        applyContentMapping(builder)
        setRequestConfiguration()
        builder.setGoogleExtrasBundle(createExtrasBundle(adData))
        return builder.build()
    }

    private fun applyContentMapping(builder: AdRequest.Builder) {
        contentMappingUrl?.takeIf { it.isNotEmpty() }?.let {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONTENT_MAPPING_URL.format(it))
            builder.setContentUrl(it)
        }
        if (neighboringContentMappingUrls.isNotEmpty()) {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.NEIGHBORING_CONTENT_MAPPING_URLS.format(neighboringContentMappingUrls))
            builder.setNeighboringContentUrls(neighboringContentMappingUrls)
        }
    }

    private fun applyContentMapping(builder: BannerAdRequest.Builder) {
        contentMappingUrl?.takeIf { it.isNotEmpty() }?.let {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONTENT_MAPPING_URL.format(it))
            builder.setContentUrl(it)
        }
        if (neighboringContentMappingUrls.isNotEmpty()) {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.NEIGHBORING_CONTENT_MAPPING_URLS.format(neighboringContentMappingUrls))
            builder.setNeighboringContentUrls(neighboringContentMappingUrls)
        }
    }

    private fun applyContentMapping(builder: NativeAdRequest.Builder) {
        contentMappingUrl?.takeIf { it.isNotEmpty() }?.let {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.CONTENT_MAPPING_URL.format(it))
            builder.setContentUrl(it)
        }
        if (neighboringContentMappingUrls.isNotEmpty()) {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.NEIGHBORING_CONTENT_MAPPING_URLS.format(neighboringContentMappingUrls))
            builder.setNeighboringContentUrls(neighboringContentMappingUrls)
        }
    }

    private fun readBoolean(source: Map<String, Any>?, key: String, default: Boolean): Boolean =
        when (val value = source?.get(key)) {
            is Boolean -> value
            is String -> when {
                value.equals("true", ignoreCase = true) -> true
                value.equals("false", ignoreCase = true) -> false
                else -> default
            }
            else -> default
        }

    private fun createExtrasBundle(adData: AdData?): Bundle {
        val extras = Bundle()
        extras.putString(AdMobConstants.EXTRA_PLATFORM_NAME, AdMobConstants.PLATFORM_NAME)

        val requestId = adData?.adUnitData?.get(AdMobConstants.REQUEST_ID_KEY)?.toString()
        val hybridMode = readBoolean(adData?.adUnitData, AdMobConstants.IS_HYBRID_KEY, false)

        if (!requestId.isNullOrEmpty()) {
            extras.putString(AdMobConstants.EXTRA_PLACEMENT_REQ_ID, requestId)
        }
        extras.putString(AdMobConstants.EXTRA_IS_HYBRID_SETUP, hybridMode.toString())

        // Handle consent for ad request
        consent?.let { if (!it) extras.putString(AdMobConstants.EXTRA_NPA, AdMobConstants.NPA_NO_CONSENT_VALUE) }

        // Handle CCPA for ad request
        ccpaValue?.let { extras.putInt(AdMobConstants.EXTRA_RDP, if (it) 1 else 0) }

        return extras
    }

    internal fun collectBiddingData(
        biddingDataCallback: BiddingDataCallback,
        adFormat: AdFormat,
        additionalExtras: Bundle?
    ) {
        if (initState == InitState.INIT_STATE_NONE) {
            IronLog.INTERNAL.verbose(AdMobConstants.Logs.TOKEN_INIT_NOT_STARTED)
            biddingDataCallback.onFailure(AdMobConstants.Logs.TOKEN_INIT_NOT_STARTED)
            return
        }

        IronLog.ADAPTER_API.verbose(adFormat.toString())

        val signalRequest = createSignalRequest(adFormat, additionalExtras)
        if (signalRequest == null) {
            val error = AdMobConstants.Logs.UNSUPPORTED_AD_FORMAT.format(adFormat)
            IronLog.INTERNAL.error(error)
            biddingDataCallback.onFailure(error)
            return
        }

        MobileAds.generateSignal(signalRequest, object : SignalGenerationCallback {
            override fun onSuccess(signal: Signal) {
                val token = signal.signalString ?: ""
                val sdkVersion = getNetworkSDKVersion()
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.TOKEN_COLLECTED.format(token, sdkVersion))
                val biddingDataMap = mutableMapOf<String, Any>()
                biddingDataMap[AdMobConstants.TOKEN_KEY] = token
                biddingDataMap[AdMobConstants.SDK_VERSION_KEY] = sdkVersion
                biddingDataCallback.onSuccess(biddingDataMap)
            }

            override fun onFailure(error: SignalError) {
                biddingDataCallback.onFailure(AdMobConstants.Logs.TOKEN_FAILED.format(error.message))
            }
        })
    }

    private fun createSignalRequest(adFormat: AdFormat, additionalExtras: Bundle?): SignalRequest? =
        when (adFormat) {
            AdFormat.INTERSTITIAL -> InterstitialSignalRequest.Builder(AdMobConstants.REQUESTER_TYPE).build()
            AdFormat.REWARDED -> RewardedSignalRequest.Builder(AdMobConstants.REQUESTER_TYPE).build()
            AdFormat.BANNER -> BannerSignalRequest.Builder(AdMobConstants.REQUESTER_TYPE)
                .setGoogleExtrasBundle(additionalExtras ?: Bundle())
                .build()
            AdFormat.NATIVE -> NativeSignalRequest.Builder(AdMobConstants.REQUESTER_TYPE).build()
            else -> null
        }

    // endregion
}

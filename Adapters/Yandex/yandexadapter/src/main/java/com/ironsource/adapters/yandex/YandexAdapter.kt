package com.ironsource.adapters.yandex

import android.content.Context
import android.text.TextUtils
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.BidderTokenLoadListener
import com.yandex.mobile.ads.common.BidderTokenLoader
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration
import com.yandex.mobile.ads.common.MobileAds
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class YandexAdapter() : LevelPlayBaseAdapter() {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS
        }

        private const val GitHash: String = BuildConfig.GitHash

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        @JvmStatic
        fun getLoadError(error: AdRequestError): AdapterErrorType {
            return when (error.code) {
                AdRequestError.Code.NO_FILL -> AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
                else -> AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }

        internal fun buildCreativeIdString(creativeIds: List<String?>): String {
            return creativeIds
                .filter { !TextUtils.isEmpty(it) }
                .joinToString(",")
        }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = YandexConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = MobileAds.libraryVersion

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initCallbackListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            val appId = adData.getString(YandexConstants.APP_ID_KEY)
            IronLog.ADAPTER_API.verbose(YandexConstants.Logs.APP_ID.format(appId ?: ""))

            // Set log level
            MobileAds.enableLogging(isAdaptersDebugEnabled())

            // Initialize the SDK
            MobileAds.initialize(context.applicationContext) {
                // Yandex's initialization callback currently doesn't give any indication to initialization failure.
                // Once this callback is called we will treat the initialization as successful
                initializationSuccess()
            }
        } else if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
        }
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0]
        IronLog.ADAPTER_API.verbose(YandexConstants.Logs.META_DATA_SET.format(key ?: "", value ?: ""))
        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidMetaData(key, YandexConstants.META_DATA_YANDEX_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(YandexConstants.Logs.CONSENT.format(consent))
        MobileAds.setUserConsent(consent)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(YandexConstants.Logs.COPPA.format(value))
        MobileAds.setAgeRestrictedUser(value)
    }

    // endregion

    // region Helper Methods

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initCallbackListeners) {
            listener.onInitSuccess()
        }

        initCallbackListeners.clear()
    }

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback, bidderTokenRequest: BidderTokenRequestConfiguration) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(YandexConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(YandexConstants.Logs.TOKEN_ERROR)
            return
        }

        BidderTokenLoader.loadBidderToken(
            context.applicationContext,
            bidderTokenRequest,
            object : BidderTokenLoadListener {
                override fun onBidderTokenLoaded(bidderToken: String) {
                    val ret: MutableMap<String?, Any?> = HashMap()
                    IronLog.ADAPTER_API.verbose(YandexConstants.Logs.TOKEN.format(bidderToken))
                    ret[YandexConstants.TOKEN_KEY] = bidderToken
                    biddingDataCallback.onSuccess(ret)
                }

                override fun onBidderTokenFailedToLoad(failureReason: String) {
                    biddingDataCallback.onFailure(YandexConstants.Logs.TOKEN_FAILURE.format(failureReason))
                }
            })
    }

    internal fun getConfigParams(): Map<String, String> {
        return mapOf(
            YandexConstants.ADAPTER_VERSION_KEY to YandexConstants.ADAPTER_VERSION,
            YandexConstants.ADAPTER_NETWORK_NAME_KEY to YandexConstants.MEDIATION_NAME,
            YandexConstants.ADAPTER_NETWORK_SDK_VERSION_KEY to LevelPlay.getSdkVersion()
        )
    }

    // endregion
}

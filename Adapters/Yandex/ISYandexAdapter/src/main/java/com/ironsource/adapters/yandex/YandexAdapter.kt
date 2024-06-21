package com.ironsource.adapters.yandex

import android.content.Context
import com.ironsource.adapters.yandex.banner.YandexBannerAdapter
import com.ironsource.adapters.yandex.interstitial.YandexInterstitialAdapter
import com.ironsource.adapters.yandex.rewardedvideo.YandexRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.utils.IronSourceUtils
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.BidderTokenLoadListener
import com.yandex.mobile.ads.common.BidderTokenLoader
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration
import com.yandex.mobile.ads.common.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

class YandexAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    // Init state possible values
    enum class InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS
    }

    init {
        setRewardedVideoAdapter(YandexRewardedVideoAdapter(this))
        setInterstitialAdapter(YandexInterstitialAdapter(this))
        setBannerAdapter(YandexBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Yandex keys
        private const val MEDIATION_NAME: String = "ironSource" // We recommend using only lowercase like "ironsource"
        private const val APP_ID_KEY: String = "appId"
        private const val AD_UNIT_ID_KEY: String = "adUnitId"

        // Handle init callback for all adapter instances
        private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var mInitState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        @JvmStatic
        fun startAdapter(providerName: String): YandexAdapter {
            return YandexAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData("Yandex", VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return MobileAds.libraryVersion
        }

        fun getAppIdKey(): String {
            return APP_ID_KEY
        }

        fun getAdUnitIdKey(): String {
            return AD_UNIT_ID_KEY
        }

        fun getLoadErrorAndCheckNoFill(error: AdRequestError, noFillError : Int): IronSourceError {
            return when (error.code) {
                AdRequestError.Code.NO_FILL -> IronSourceError(
                    noFillError,
                    error.description
                )
                else -> IronSourceError(error.code, error.description)
            }
        }
    }
    //region Adapter Methods

    // Get adapter version
    override fun getVersion(): String {
        return VERSION
    }

    // Get network sdk version
    override fun getCoreSDKVersion(): String {
        return getAdapterSDKVersion()
    }

    override fun isUsingActivityBeforeImpression(adUnit: IronSource.AD_UNIT): Boolean {
        return false
    }

    //endregion

    //region Initializations methods and callbacks

    fun initSdk(appId: String) {
        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("appId = $appId")

            // Set log level
            MobileAds.enableLogging(isAdaptersDebugEnabled)

            // Initialize the SDK
            MobileAds.initialize(ContextProvider.getInstance().applicationContext) {
                // Yandex's initialization callback currently doesn't give any indication to initialization failure.
                // Once this callback is called we will treat the initialization as successful
                initializationSuccess()
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        mInitState = InitState.INIT_STATE_SUCCESS

        //iterate over all the adapter instances and report init success
        for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess()
        }

        initCallbackListeners.clear()
    }

    fun getInitState(): InitState {
        return mInitState
    }

    //endregion

    //region legal

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("consent = $consent")
        MobileAds.setUserConsent(consent)
    }

    override fun setMetaData(p0: String?, p1: MutableList<String>?) {
        super.setMetaData(p0, p1)
        // Yandex SDK supports COPPA using MobileAds::setAgeRestrictedUser
        // Please use it if you have that information
    }

    //endregion

    // region Helpers
    fun collectBiddingData(biddingDataCallback: BiddingDataCallback, bidderTokenRequest: BidderTokenRequestConfiguration) {

        // This check is not necessary, the SDK will be initialized automatically if it has not already been
        if (mInitState != InitState.INIT_STATE_SUCCESS) {
            val error = "returning null as token since init isn't completed"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - Yandex")
            return
        }

        BidderTokenLoader.loadBidderToken(
            ContextProvider.getInstance().applicationContext,
            bidderTokenRequest,
            object : BidderTokenLoadListener {
                override fun onBidderTokenLoaded(bidderToken: String) {
                    val ret: MutableMap<String?, Any?> = HashMap()
                    IronLog.ADAPTER_API.verbose("token = $bidderToken")
                    ret["token"] = bidderToken
                    biddingDataCallback.onSuccess(ret)
                }

                override fun onBidderTokenFailedToLoad(failureReason: String) {
                    biddingDataCallback.onFailure("failed to receive token - Yandex $failureReason")
                }
            })
    }

    fun getConfigParams(): Map<String, String> {
        return mapOf(
            "adapter_version" to VERSION,
            "adapter_network" to MEDIATION_NAME,
            "adapter_network_sdk_version" to IronSourceUtils.getSDKVersion()
        )
    }

    //endregion

}

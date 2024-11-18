package com.ironsource.adapters.vungle

import android.content.Context
import com.ironsource.adapters.vungle.banner.VungleBannerAdapter
import com.ironsource.adapters.vungle.interstitial.VungleInterstitialAdapter
import com.ironsource.adapters.vungle.rewardedvideo.VungleRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.vungle.ads.BidTokenCallback
import com.vungle.ads.VungleAds
import com.vungle.ads.VunglePrivacySettings.setCCPAStatus
import com.vungle.ads.VunglePrivacySettings.setCOPPAStatus
import com.vungle.ads.VunglePrivacySettings.setGDPRStatus
import java.util.concurrent.atomic.AtomicBoolean


class VungleAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(VungleRewardedVideoAdapter(this))
        setInterstitialAdapter(VungleInterstitialAdapter(this))
        setBannerAdapter(VungleBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad
        // of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    companion object {
        // Adapter version
        private const val VERSION = BuildConfig.VERSION_NAME
        private const val GitHash = BuildConfig.GitHash

        // Vungle keys
        const val APP_ID = "AppID"
        const val PLACEMENT_ID = "PlacementId"

        // Integration data
        private const val VUNGLE_KEYWORD = "Vungle"

        // Meta data flags
        private const val META_DATA_VUNGLE_COPPA_KEY = "Vungle_COPPA"

        // Publisher-controlled consent policy version
        private const val META_DATA_VUNGLE_CONSENT_MESSAGE_VERSION = "1.0.0"

        // Indicates whether init was called once- prevents from perform additional init
        private var isInitiated = AtomicBoolean(false)

        internal var mInitState = InitState.INIT_STATE_NONE

        // Handle init callback for all adapter instances for each init that was called
        internal val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        @JvmStatic
        fun startAdapter(providerName: String): VungleAdapter = VungleAdapter(providerName)

        @JvmStatic
        fun getIntegrationData(context: Context): IntegrationData =
            IntegrationData(VUNGLE_KEYWORD, VERSION)

        @JvmStatic
        fun getAdapterSDKVersion(): String = VungleAds.getSdkVersion()

    }

    //region Adapter Methods

    // Get adapter version
    override fun getVersion(): String = VERSION

    // Get network sdk version
    override fun getCoreSDKVersion(): String = getAdapterSDKVersion()

    override fun isUsingActivityBeforeImpression(adUnit: IronSource.AD_UNIT): Boolean = false

    //endregion

    //region Initializations methods and callbacks

    fun initSDK(context: Context, appID: String) {
        // add self to init delegates only when init not finished yet
        if (mInitState == InitState.INIT_STATE_NONE ||
            mInitState == InitState.INIT_STATE_IN_PROGRESS
        ) {
            initCallbackListeners.add(this)
        }

        if (isInitiated.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("appId = $appID")
            // set init in progress
            mInitState = InitState.INIT_STATE_IN_PROGRESS

            VungleAds.setIntegrationName(
                VungleAds.WrapperFramework.ironsource, BuildConfig.VERSION_NAME
            )

            // init SDK
            val initListener = VungleInitListener()
            VungleAds.init(context, appID, initListener)
        }
    }

    fun getInitState(): InitState {
        return mInitState
    }

    //endregion

    //region legal

    override fun setMetaData(key: String, values: List<String>) {
        if (values.isEmpty()) {
            return
        }
        // this is a list of 1 value.
        val value = values[0]
        IronLog.ADAPTER_API.verbose("key = $key, value = $value")
        val formattedValue: String = MetaDataUtils.formatValueForType(
            value,
            MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
        )
        when {
            MetaDataUtils.isValidCCPAMetaData(key, value) -> {
                setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
            }

            MetaDataUtils.isValidMetaData(key, META_DATA_VUNGLE_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCCPAValue(value: Boolean) {
        // The Vungle CCPA API expects an indication if the user opts in to targeted advertising.
        // opposite to the ironSource Mediation CCPA flag of do_not_sell
        val optIn = !value
        IronLog.ADAPTER_API.verbose("ccpa = $optIn")
        setCCPAStatus(optIn)
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("coppa = $value")
        setCOPPAStatus(value)
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose("gdpr = $consent")
        setGDPRStatus(consent, META_DATA_VUNGLE_CONSENT_MESSAGE_VERSION)
    }

    //endregion

    //region Helpers

    fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        VungleAds.getBiddingToken(ContextProvider.getInstance().applicationContext, object : BidTokenCallback {
            override fun onBidTokenCollected(bidToken: String) {
                val ret: MutableMap<String?, Any?> = HashMap()
                val sdkVersion = coreSDKVersion
                IronLog.ADAPTER_API.verbose("sdkVersion = $sdkVersion, token = $bidToken")
                ret["sdkVersion"] = sdkVersion
                ret["token"] = bidToken
                biddingDataCallback.onSuccess(ret)
            }

            override fun onBidTokenError(errorMessage: String) {
                biddingDataCallback.onFailure("failed to receive token - Vungle , error = $errorMessage")
            }
        })
    }

    //endregion
}
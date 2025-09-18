package com.ironsource.adapters.ogury

import android.content.Context
import com.ironsource.adapters.ogury.banner.OguryBannerAdapter
import com.ironsource.adapters.ogury.interstitial.OguryInterstitialAdapter
import com.ironsource.adapters.ogury.rewardedvideo.OguryRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ogury.ad.OguryBidTokenListener
import com.ogury.ad.OguryBidTokenProvider
import com.ogury.core.OguryError
import com.ogury.core.OguryLog
import com.ogury.sdk.Ogury
import com.ogury.sdk.OguryChildPrivacyTreatment
import com.ogury.sdk.OguryOnStartListener
import com.unity3d.mediation.LevelPlay
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean


class OguryAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(OguryRewardedVideoAdapter(this))
        setInterstitialAdapter(OguryInterstitialAdapter(this))
        setBannerAdapter(OguryBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Verve Keys
        private const val NETWORK_NAME: String = "Ogury"
        private const val ASSET_KEY: String = "assetKey"
        private const val AD_UNIT_ID: String = "adUnitId"
        const val MEDIATION_NAME: String = "Unity LevelPlay"

        // Meta data flags
        private const val META_DATA_OGURY_COPPA_KEY = "LevelPlay_ChildDirected"

        const val LOG_INIT_FAILED = "$NETWORK_NAME sdk init failed"


        // Handle init callback for all adapter instances
        private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var mInitState: InitState = InitState.INIT_STATE_NONE
        private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        @JvmStatic
        fun startAdapter(providerName: String): OguryAdapter {
            return OguryAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData(NETWORK_NAME, VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return Ogury.getSdkVersion()
        }

        fun getAssetKey(): String {
            return ASSET_KEY
        }

        fun getAdUnitIdKey(): String {
            return AD_UNIT_ID
        }

        fun getLoadError(error: Throwable?): IronSourceError {
            return IronSourceError(
                (error as OguryError).code,
                error.message)
        }
    }

    //region Adapter Methods

    // Get adapter version

    override fun getVersion(): String {
        return VERSION
    }

    override fun getCoreSDKVersion(): String {
        return getAdapterSDKVersion()
    }

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean {
        return false
    }

    //endregion

    //region Initializations methods and callbacks

    fun initSdk(config: JSONObject) {
        val assetIdKey = getAssetKey()
        val assetKey = config.optString(assetIdKey)

        IronLog.ADAPTER_API.verbose("assetKey = $assetKey")

        // Add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this)
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose("$assetKey: $assetKey")

            if (isAdaptersDebugEnabled) {
                OguryLog.enable(OguryLog.Level.DEBUG)
            }

            val context = ContextProvider.getInstance().applicationContext

            // Init Ogury SDK
            Ogury.start(context,assetKey, object : OguryOnStartListener {
                override fun onStarted() {
                    IronLog.ADAPTER_API.verbose("Initialization Success")
                    initializationSuccess()
                }

                override fun onFailed(error: OguryError) {
                    IronLog.ADAPTER_API.verbose("Initialization Failed")
                    initializationFailure()
                }
            })
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

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.verbose()

        mInitState = InitState.INIT_STATE_FAILED

        //iterate over all the adapter instances and report init failed
        for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Ogury sdk init failed")
        }
        initCallbackListeners.clear()
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

        // This is a list of 1 value
        val value = values[0]
        IronLog.ADAPTER_API.verbose("key = $key, value = $value")
        val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        when {
            MetaDataUtils.isValidMetaData(key, META_DATA_OGURY_COPPA_KEY, formattedValue) -> {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
            }
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose("isCoppa = $value")
        Ogury.applyChildPrivacy(
            when (value) {
                true -> OguryChildPrivacyTreatment.CHILD_UNDER_COPPA_TREATMENT_TRUE
                false -> OguryChildPrivacyTreatment.CHILD_UNDER_COPPA_TREATMENT_FALSE
            }
        )
    }

    fun collectBiddingData(biddingDataCallback: BiddingDataCallback){
        val context = ContextProvider.getInstance().applicationContext
        OguryBidTokenProvider.getBidToken(context, object : OguryBidTokenListener {

            override fun onBidTokenGenerated(bidToken: String) {
                IronLog.ADAPTER_API.verbose("token = $bidToken")
                val result: MutableMap<String?, Any?> = HashMap()
                result["token"] = bidToken
                biddingDataCallback.onSuccess(result)
            }

            override fun onBidTokenGenerationFailed(error: OguryError) {
                biddingDataCallback.onFailure("failed to receive token - Ogury , errorCode = ${error.code}, error = ${error.message}")
            }
        })
    }

}
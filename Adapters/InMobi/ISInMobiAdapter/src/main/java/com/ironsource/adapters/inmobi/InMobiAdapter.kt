package com.ironsource.adapters.inmobi

import android.content.Context
import com.inmobi.sdk.InMobiSdk
import com.ironsource.adapters.inmobi.banner.InMobiBannerAdapter
import com.ironsource.adapters.inmobi.interstitial.InMobiInterstitialAdapter
import com.ironsource.adapters.inmobi.rewardedvideo.InMobiRewardedVideoAdapter
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean


class InMobiAdapter(providerName: String) : AbstractAdapter(providerName),
    INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(InMobiRewardedVideoAdapter(this))
        setInterstitialAdapter(InMobiInterstitialAdapter(this))
        setBannerAdapter(InMobiBannerAdapter(this))

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad
        // of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE
    }

    enum class InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_ERROR
    }

    companion object {
        // Adapter version
        private const val VERSION = BuildConfig.VERSION_NAME
        private const val GitHash = BuildConfig.GitHash

        // Meta data flags
        const val META_DATA_INMOBI_AGE_RESTRICTED = "inMobi_AgeRestricted"
        const val META_DATA_INMOBI_CHILD_DIRECTED = "LevelPlay_Child_Directed"
        const val INMOBI_DO_NOT_SELL_KEY = "do_not_sell"

        // InMobi keys
        const val ACCOUNT_ID = "accountId"
        const val PLACEMENT_ID = "placementId"

        // Integration data
        private const val INMOBI_KEYWORD = "InMobi"

        const val EMPTY_STRING = ""

        // Indicates whether init was called once- prevents from perform additional init
        private var isInitiated = AtomicBoolean(false)


        // Indicates whether setAgeRestriction was called once
        private var isAgeRestrictionCalled = AtomicBoolean(false)

        internal var initState = InitState.INIT_STATE_NONE

        // Handle init callback for all adapter instances for each init that was called
        internal val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

        // Meta Data
        private var consentCollectingUserData: String? = null
        var ageRestrictionCollectingUserData: Boolean? = null
        private var doNotSellCollectingUserData: Boolean? = null

        @JvmStatic
        fun startAdapter(providerName: String): InMobiAdapter = InMobiAdapter(providerName)

        @JvmStatic
        fun getIntegrationData(context: Context): IntegrationData =
            IntegrationData(INMOBI_KEYWORD, VERSION)

        @JvmStatic
        fun getAdapterSDKVersion(): String = InMobiSdk.getVersion()


    }

    //region Adapter Methods

    // Get adapter version
    override fun getVersion(): String = VERSION

    // Get network sdk version
    override fun getCoreSDKVersion(): String = getAdapterSDKVersion()

    override fun isUsingActivityBeforeImpression(adUnit: IronSource.AD_UNIT): Boolean {
        return false
    }

    //endregion

    //region Initializations methods and callbacks

    fun initSDK(context: Context, accountId: String) {
        // add self to init delegates only when init not finished yet
        if (initState == InitState.INIT_STATE_NONE ||
            initState == InitState.INIT_STATE_IN_PROGRESS
        ) {
            initCallbackListeners.add(this)
        }

        if (isInitiated.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("$ACCOUNT_ID = $accountId")
            // set init in progress
            initState = InitState.INIT_STATE_IN_PROGRESS

            val sdkLogLevel = if (isAdaptersDebugEnabled) InMobiSdk.LogLevel.DEBUG
            else InMobiSdk.LogLevel.NONE

            // set debug mode
            InMobiSdk.setLogLevel(sdkLogLevel)

            // init SDK
            val initListener = InMobiInitListener()
            postOnUIThread {
                InMobiSdk.init(context, accountId, getConsentObject(), initListener)
            }
        }
    }

    //endregion

    //region legal

    override fun setConsent(consent: Boolean) {
        consentCollectingUserData = consent.toString()

        if (initState == InitState.INIT_STATE_SUCCESS) {
            IronLog.ADAPTER_API.verbose("$providerName consent = $consent")

            InMobiSdk.updateGDPRConsent(getConsentObject())
        }
    }

    override fun setMetaData(key: String, values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        // this is a list of 1 value.
        val value = values[0]
        IronLog.ADAPTER_API.verbose("key = $key, value = $value")

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            doNotSellCollectingUserData = MetaDataUtils.getMetaDataBooleanValue(value)
            return
        }

        val formattedValue =
            MetaDataUtils.formatValueForType(value, MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        if (MetaDataUtils.isValidMetaData(key, META_DATA_INMOBI_AGE_RESTRICTED, formattedValue) ||
            MetaDataUtils.isValidMetaData(key, META_DATA_INMOBI_CHILD_DIRECTED, formattedValue)
        ) {
            setAgeRestricted(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
        }
    }

    fun shouldSetAgeRestrictedOnInitSuccess(): Boolean {
        if (isAgeRestrictionCalled.compareAndSet(false, true)) {
            return ageRestrictionCollectingUserData != null
        }
        return false
    }

    fun setAgeRestricted(isAgeRestricted: Boolean) {
        if (initState == InitState.INIT_STATE_SUCCESS) {
            IronLog.ADAPTER_API.verbose("isAgeRestricted = $isAgeRestricted")
            InMobiSdk.setIsAgeRestricted(isAgeRestricted)
        } else {
            ageRestrictionCollectingUserData = isAgeRestricted
        }
    }

    private fun getConsentObject(): JSONObject {
        val obj = JSONObject()
        try {
            if (!consentCollectingUserData.isNullOrEmpty()) {
                obj.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, consentCollectingUserData)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return obj
    }

    //endregion

    //region Helpers

    fun getBiddingData(): MutableMap<String?, Any?>? {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("returning null as token since init did not finish")
            return null
        }

        val bidderToken = InMobiSdk.getToken(getExtrasMap(), EMPTY_STRING)
        val returnedToken: String = if (!bidderToken.isNullOrEmpty()) bidderToken else EMPTY_STRING
        IronLog.ADAPTER_API.verbose("token = $returnedToken")
        val ret: MutableMap<String?, Any?> = HashMap()
        ret["token"] = returnedToken
        return ret
    }

    fun getExtrasMap(): Map<String, String> {
        val map = HashMap<String, String>()
        map["tp"] = "c_supersonic"
        map["tp-ver"] = version

        doNotSellCollectingUserData?.let {
            map[INMOBI_DO_NOT_SELL_KEY] = if (it) "1" else "0"
        }
        return map
    }

    //endregion
}
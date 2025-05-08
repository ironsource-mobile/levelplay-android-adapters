package com.ironsource.adapters.inmobi.interstitial

import com.inmobi.ads.InMobiInterstitial
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.util.concurrent.ConcurrentHashMap

class InMobiInterstitialAdapter (adapter: InMobiAdapter) :
AbstractInterstitialAdapter<InMobiAdapter>(adapter){

    private val placementToInterstitialAd: ConcurrentHashMap<String, InMobiInterstitial> = ConcurrentHashMap()
    private val interstitialPlacementToListenerMap:
            ConcurrentHashMap<String, InterstitialSmashListener> = ConcurrentHashMap()

    override fun initInterstitial(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()
        initInterstitialInternal(config, listener)
    }

    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()
        initInterstitialInternal(config, listener)
    }

    private fun initInterstitialInternal(config: JSONObject, listener: InterstitialSmashListener) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        val accountId = config.optString(InMobiAdapter.ACCOUNT_ID)

        if (!isValidPlacementId(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.PLACEMENT_ID))

            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Invalid ${InMobiAdapter.PLACEMENT_ID}",
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        if (accountId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.ACCOUNT_ID))

            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Empty ${InMobiAdapter.ACCOUNT_ID}",
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

        // add listener to map
        interstitialPlacementToListenerMap[placementId] = listener

        // notify listener about init state
        when (InMobiAdapter.initState) {
            InMobiAdapter.InitState.INIT_STATE_SUCCESS -> {
                IronLog.ADAPTER_API.verbose(
                    "onInterstitialInitSuccess with ${InMobiAdapter.PLACEMENT_ID}: $placementId"
                )

                // call listener init success
                listener.onInterstitialInitSuccess()
            }
            InMobiAdapter.InitState.INIT_STATE_ERROR -> {
                IronLog.ADAPTER_API.verbose(
                    "onInterstitialInitFailed with ${InMobiAdapter.PLACEMENT_ID}: $placementId"
                )
                // call listener init failed
                listener.onInterstitialInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Init Failed",
                        IronSourceConstants.INTERSTITIAL_AD_UNIT
                    )
                )
            }
            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, accountId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        if(adapter.shouldSetAgeRestrictedOnInitSuccess()){
            InMobiAdapter.ageRestrictionCollectingUserData?.let {
                adapter.setAgeRestricted(it)
            }
        }
        interstitialPlacementToListenerMap.values.forEach { interstitialListener ->
            interstitialListener.onInterstitialInitSuccess()
        }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        val message = "init failed: $error"
        interstitialPlacementToListenerMap.values.forEach { interstitialListener ->
            interstitialListener.onInterstitialInitFailed(
                IronSourceError(IronSourceError.ERROR_CODE_INIT_FAILED, message)
            )
        }
    }

    override fun loadInterstitial(
        config: JSONObject,
        adData: JSONObject?,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose(" <" + config.optString(InMobiAdapter.PLACEMENT_ID) + ">")
        loadInterstitialInternal(config, listener, null)
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose( " <" + config.optString(InMobiAdapter.PLACEMENT_ID) + ">")
        loadInterstitialInternal(config, listener, serverData)
    }

    private fun loadInterstitialInternal(
        config: JSONObject,
        listener: InterstitialSmashListener,
        serverData: String?
    ) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)

        parseToLong(placementId)?.let { placement ->
            IronLog.ADAPTER_API.verbose(
                "create InMobi ad with ${InMobiAdapter.PLACEMENT_ID}: <$placementId>"
            )

            // create InMobi interstitial listener
            val interstitialListener = InMobiInterstitialListener(listener, placementId)

            // post on ui thread
            postOnUIThread {
            // create InMobi interstitial obj
            val inMobiInterstitial = InMobiInterstitial(
                ContextProvider.getInstance().applicationContext,
                placement,
                interstitialListener
            )
            // add InMobi interstitial obj to map
            placementToInterstitialAd[placementId] = inMobiInterstitial
            IronLog.ADAPTER_API.verbose(
                "loadInterstitial InMobi ad with placement:<$placement>"
            )
                serverData?.let {
                    // Load InMobi interstitial bidding
                    try {
                        val bytes = it.toByteArray(Charsets.UTF_8)
                        inMobiInterstitial.load(bytes)
                    } catch (e: UnsupportedEncodingException) {
                        val error = ErrorBuilder.buildLoadFailedError(
                            IronSourceConstants.INTERSTITIAL_AD_UNIT,
                            "InMobi",
                            "Couldn't parse server data for ${InMobiAdapter.PLACEMENT_ID} = $placement"
                        )
                        listener.onInterstitialAdLoadFailed(error)
                    }
                } ?: run {
                    // Load InMobi interstitial
                    val map = adapter.getExtrasMap()
                    inMobiInterstitial.setExtras(map)
                    inMobiInterstitial.load()
                }
        }
    }
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")
        // check if InMobi Interstitial is ready or not
        if (!isInterstitialReady(config)) {
            IronLog.INTERNAL.error(
                "failed: inMobiInterstitial isn't ready <$placementId>"
            )
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildGenericError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        placementToInterstitialAd[placementId]?.let { inMobiInterstitial ->
            IronLog.ADAPTER_API.verbose("showInterstitial InMobi ad <$placementId")
            postOnUIThread {
                inMobiInterstitial.show()
            }
        }
    }

    override fun isInterstitialReady(config: JSONObject): Boolean {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

        // Get InMobi Interstitial
        val inMobiInterstitial = placementToInterstitialAd[placementId]
        return inMobiInterstitial?.isReady() == true
    }

    override fun getInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? = adapter.getBiddingData()

    private fun isValidPlacementId(placementId: String): Boolean {
        parseToLong(placementId)?.let {
            return true
        }
        return false
    }

    private fun parseToLong(placementId: String): Long? {
        var placementIdLong: Long? = null
        try {
            placementIdLong = placementId.toLong()
        } catch (e: Exception) {
            IronLog.INTERNAL.error("parseToLong threw error ${e.message}")
        }
        return placementIdLong
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

    }

    //endregion

}
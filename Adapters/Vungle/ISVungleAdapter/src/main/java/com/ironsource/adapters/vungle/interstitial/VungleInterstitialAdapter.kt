package com.ironsource.adapters.vungle.interstitial

import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.AdConfig
import com.vungle.ads.InterstitialAd
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap


class VungleInterstitialAdapter(adapter: VungleAdapter) :
    AbstractInterstitialAdapter<VungleAdapter>(adapter) {
    private val mPlacementToInterstitialAd: ConcurrentHashMap<String, InterstitialAd> =
        ConcurrentHashMap()
    private val mInterstitialPlacementToListenerMap:
            ConcurrentHashMap<String, InterstitialSmashListener> = ConcurrentHashMap()
    private val mPlacementIdToAdAvailability: ConcurrentHashMap<String, Boolean> =
        ConcurrentHashMap()

    override fun initInterstitial(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        initInterstitialInternal(config, listener)
    }

    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        initInterstitialInternal(config, listener)
    }

    private fun initInterstitialInternal(
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        val appId = config.optString(VungleAdapter.APP_ID)

        if (placementId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(VungleAdapter.PLACEMENT_ID))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(placementId),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        if (appId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(VungleAdapter.APP_ID))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appId),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        //add to interstitial listener map
        mInterstitialPlacementToListenerMap[placementId] = listener

        when (adapter.getInitState()) {
            VungleAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }

            VungleAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                // call listener init failed
                listener.onInterstitialInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Vungle SDK Init Failed",
                        IronSourceConstants.INTERSTITIAL_AD_UNIT
                    )
                )
            }

            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, appId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mInterstitialPlacementToListenerMap.values.forEach { interstitialListener ->
            interstitialListener.onInterstitialInitSuccess()
        }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mInterstitialPlacementToListenerMap.values.forEach { interstitialListener ->
            interstitialListener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    error,
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        }
    }

    override fun loadInterstitial(
        config: JSONObject,
        adData: JSONObject?,
        listener: InterstitialSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("loadInterstitial Vungle ad with placementId = $placementId")
        loadInterstitialInternal(config, listener, null)
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("loadInterstitial Vungle ad with placementId = $placementId")
        loadInterstitialInternal(config, listener, serverData)
    }

    private fun loadInterstitialInternal(
        config: JSONObject,
        listener: InterstitialSmashListener,
        serverData: String?
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("loadInterstitial Vungle ad with placementId = $placementId")
        setInterstitialAdAvailability(placementId, false)

        val vungleInterstitialAdListener = VungleInterstitialAdListener(WeakReference(this), listener, placementId)
        val vungleInterstitial = InterstitialAd(
            ContextProvider.getInstance().applicationContext,
            placementId,
            AdConfig()
        ).apply {

            adListener = vungleInterstitialAdListener
        }
        mPlacementToInterstitialAd[placementId] = vungleInterstitial
        vungleInterstitial.load(serverData)
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")
        // Check if Vungle Interstitial Ad is ready
        if (!isInterstitialReady(config)) {
            IronLog.INTERNAL.error("There is no ad available for placementId = $placementId")
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        val vungleInterstitial = mPlacementToInterstitialAd[placementId]
        IronLog.ADAPTER_API.verbose("showInterstitial vungle ad <$placementId")
        postOnUIThread{
            vungleInterstitial?.play()
        }
        setInterstitialAdAvailability(placementId, false)
    }

    override fun isInterstitialReady(config: JSONObject): Boolean {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")
        if (placementId.isEmpty()) {
            return false
        }

        val isAvailable = mPlacementIdToAdAvailability[placementId] ?: false
        if (!isAvailable) {
            return false
        }

        // Get Vungle Interstitial
        val vungleInterstitial = mPlacementToInterstitialAd[placementId]
        return vungleInterstitial?.canPlayAd() ?: false
    }

    internal fun setInterstitialAdAvailability(placementId: String, isAvailable: Boolean) {
        mPlacementIdToAdAvailability[placementId] = isAvailable
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            mInterstitialPlacementToListenerMap.clear()
            mPlacementToInterstitialAd.clear()
            mPlacementIdToAdAvailability.clear()
        }
    }

    //endregion

}
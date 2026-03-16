package com.ironsource.adapters.voodoo.interstitial

import com.ironsource.adapters.voodoo.VoodooAdapter
import com.ironsource.adapters.voodoo.VoodooAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.adn.sdk.publisher.AdnAdPlacement
import io.adn.sdk.publisher.AdnAdRequest
import io.adn.sdk.publisher.AdnFullscreenAd
import io.adn.sdk.publisher.AdnFullscreenAdListener
import io.adn.sdk.publisher.AdnLoadTimeout
import io.adn.sdk.publisher.AdnSdk
import org.json.JSONObject

class VoodooInterstitialAdapter(adapter: VoodooAdapter) :
    AbstractInterstitialAdapter<VoodooAdapter>(adapter) {

    private var mSmashListener:  InterstitialSmashListener? = null
    private var mAdListener: AdnFullscreenAdListener? = null
    private var mAd: AdnFullscreenAd? = null

    override fun initInterstitialForBidding(
        appKey: String?, userId: String?, config: JSONObject,
        listener: InterstitialSmashListener
    ) {

        val placementIdKey = VoodooAdapter.getPlacementIdKey()
        val placementId = getConfigStringValueFromKey(config, placementIdKey)
        if (placementId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementIdKey))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(placementIdKey),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            VoodooAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            VoodooAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onInterstitialInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.INTERSTITIAL_AD_UNIT
                    )
                )
            }
            VoodooAdapter.Companion.InitState.INIT_STATE_NONE,
            VoodooAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(config)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onInterstitialInitSuccess()
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mSmashListener?.onInterstitialInitFailed(
            ErrorBuilder.buildInitFailedError(
                error,
                IronSourceConstants.INTERSTITIAL_AD_UNIT
            )
        )
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        val placementId = getConfigStringValueFromKey(config, VoodooAdapter.getPlacementIdKey())
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val interstitialAdListener = VoodooInterstitialAdListener(placementId, listener)
        mAdListener = interstitialAdListener
        mAd = AdnSdk.getInterstitialAdInstance(ContextProvider.getInstance().currentActiveActivity, interstitialAdListener)
        mAd?.load(AdnAdRequest.AdBidRequest(AdnAdPlacement.INTERSTITIAL, serverData, AdnLoadTimeout.MEDIATION)) ?: run {
                listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
            }
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        val placementId = getConfigStringValueFromKey(config, VoodooAdapter.getPlacementIdKey())
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!isInterstitialReady(config)) {
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        mAd?.show() ?: run {
            listener.onInterstitialAdShowFailed(
                IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Ad is null")
            )
        }
    }

    override fun isInterstitialReady(config: JSONObject): Boolean = mAd?.isReady() == true


    override fun destroyInterstitialAd(config: JSONObject?) {
        val placementIdKey = VoodooAdapter.getPlacementIdKey()
        val placementId = config?.let { getConfigStringValueFromKey(it, placementIdKey) }
        IronLog.ADAPTER_API.verbose("Destroy interstitial ad of ${VoodooAdapter.NETWORK_NAME}, placementId = $placementId")
        mAd?.destroy()
        mAd = null
        mAdListener = null
        mSmashListener = null
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback, AdnAdPlacement.INTERSTITIAL)
    }
}
package com.ironsource.adapters.bidmachine.interstitial

import android.text.TextUtils
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.bidmachine.AdPlacementConfig
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialRequest
import org.json.JSONObject
import java.lang.ref.WeakReference


class BidMachineInterstitialAdapter(adapter: BidMachineAdapter) :
    AbstractInterstitialAdapter<BidMachineAdapter>(adapter) {

    private var mInterstitialListener : InterstitialSmashListener? = null
    private var mInterstitialAdListener : BidMachineInterstitialAdListener? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var isInterstitialAdAvailable = false
    private var mInterstitialRequest: InterstitialRequest? = null

    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()
        val sourceId = config.optString(BidMachineAdapter.getSourceIdKey())
        if (TextUtils.isEmpty(sourceId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(sourceId))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(sourceId),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        //save interstitial listener
        mInterstitialListener = listener

        when (adapter.getInitState()) {
            BidMachineAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            BidMachineAdapter.Companion.InitState.INIT_STATE_NONE,
            BidMachineAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(sourceId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mInterstitialListener?.onInterstitialInitSuccess()
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        setInterstitialAdAvailability(false)

        val interstitial = InterstitialAd(ContextProvider.getInstance().applicationContext)
        val interstitialAdListener = BidMachineInterstitialAdListener(WeakReference(this), listener)
        interstitial.setListener(interstitialAdListener)
        mInterstitialAdListener = interstitialAdListener

        val adPlacementConfig = createInterstitialPlacementConfig(config)
        val interstitialRequestBuilder = InterstitialRequest.Builder(adPlacementConfig)
            .setBidPayload(serverData)

        mInterstitialRequest = interstitialRequestBuilder.build()
        interstitial.load(mInterstitialRequest)
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isInterstitialReady(config)) {
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        } else {
                mInterstitialAd?.show()
        }

        setInterstitialAdAvailability(false)
    }

    override fun isInterstitialReady(config: JSONObject): Boolean {
        return isInterstitialAdAvailable &&
            mInterstitialAd?.let { interstitialAd ->
                interstitialAd.canShow() && !interstitialAd.isExpired
            } ?: false

    }

    override fun collectInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        val adPlacementConfig = createInterstitialPlacementConfig(config)
        adapter.collectBiddingData(biddingDataCallback, adPlacementConfig)
    }

    //region Helpers

    internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
        isInterstitialAdAvailable = isAvailable
    }

    internal fun setInterstitialAd(interstitialAd: InterstitialAd) {
        mInterstitialAd = interstitialAd
    }

    internal fun destroyInterstitialAd() {
        mInterstitialAd?.setListener(null)
        mInterstitialAd?.destroy()
        mInterstitialAd = null
    }


    private fun createInterstitialPlacementConfig(config: JSONObject): AdPlacementConfig {
        val placementId = config.optString(BidMachineAdapter.getPlacementIdKey())
        val adPlacementConfigBuilder = AdPlacementConfig.interstitialBuilder()
        if(!placementId.isNullOrEmpty()) {
            adPlacementConfigBuilder.withPlacementId(placementId)
        }
        return adPlacementConfigBuilder.build()
    }


    //end region
}
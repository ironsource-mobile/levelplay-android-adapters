package com.ironsource.adapters.verve.interstitial

import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.adapters.verve.VerveAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd
import org.json.JSONObject

class VerveInterstitialAdapter(adapter: VerveAdapter) :
        AbstractInterstitialAdapter<VerveAdapter>(adapter) {

    private var mSmashListener : InterstitialSmashListener? = null
    private var mAdListener : VerveInterstitialAdListener? = null
    private var mAd: HyBidInterstitialAd? = null

    //regin Interstitial API

    override fun initInterstitialForBidding(
            appKey: String?,
            userId: String?,
            config: JSONObject,
            listener: InterstitialSmashListener
    ) {

        IronLog.ADAPTER_API.verbose()

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            VerveAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            VerveAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onInterstitialInitFailed(
                        ErrorBuilder.buildInitFailedError(
                                LOG_INIT_FAILED,
                                IronSourceConstants.INTERSTITIAL_AD_UNIT
                        )
                )
            }
            VerveAdapter.Companion.InitState.INIT_STATE_NONE,
            VerveAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
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
    ){
        IronLog.ADAPTER_API.verbose()

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val interstitialAdListener = VerveInterstitialAdListener(listener)
        mAdListener = interstitialAdListener

        val context = ContextProvider.getInstance().applicationContext
        val zoneIdKey= VerveAdapter.getZoneIdKey()
        val zoneId = getConfigStringValueFromKey(config, zoneIdKey)

        mAd = HyBidInterstitialAd(
            context,
            zoneId,
            mAdListener
        )
        mAd?.prepareAd(serverData)
        ?: run {
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
        }
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        IronLog.ADAPTER_API.verbose()

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
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT)
            )
        }
    }

    override fun isInterstitialReady(config: JSONObject): Boolean = mAd?.isReady == true

    override fun collectInterstitialBiddingData(
            config: JSONObject,
            adData: JSONObject?,
            biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose()
        destroyInterstitialAd()
        mAdListener = null
        mSmashListener = null

    }

    //endregion

    // region Helpers

    private fun destroyInterstitialAd() {
        mAd?.destroy()
        mAd = null
    }

    //endregion

}

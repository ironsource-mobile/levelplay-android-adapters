package com.ironsource.adapters.ogury.interstitial

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.adapters.ogury.OguryAdapter.Companion.MEDIATION_NAME
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.ogury.ad.OguryInterstitialAd
import com.ogury.ad.common.OguryMediation
import com.unity3d.mediation.LevelPlay
import org.json.JSONObject

class OguryInterstitialAdapter(adapter: OguryAdapter) :
        AbstractInterstitialAdapter<OguryAdapter>(adapter) {

    private var mSmashListener : InterstitialSmashListener? = null
    private var mAdListener : OguryInterstitialAdListener? = null
    private var mAd: OguryInterstitialAd? = null

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
            OguryAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            OguryAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onInterstitialInitFailed(
                        ErrorBuilder.buildInitFailedError(
                            LOG_INIT_FAILED,
                            IronSourceConstants.INTERSTITIAL_AD_UNIT
                        )
                )
            }
            OguryAdapter.Companion.InitState.INIT_STATE_NONE,
            OguryAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(config)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onInterstitialInitSuccess()
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

        val interstitialAdListener = OguryInterstitialAdListener(listener)
        mAdListener = interstitialAdListener

        val context = ContextProvider.getInstance().applicationContext
        val adUnitIdKey = OguryAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        mAd = OguryInterstitialAd(
            context,
            adUnitId,
            OguryMediation(MEDIATION_NAME, LevelPlay.getSdkVersion())
        )
        mAd?.setListener(mAdListener)
        mAd?.load(serverData)?: run {
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

    override fun isInterstitialReady(config: JSONObject): Boolean {
        return mAd?.isLoaded() ?: false
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
        IronLog.INTERNAL.verbose()
        mAd = null
        mAdListener = null
        mSmashListener = null

    }

    //endregion

}
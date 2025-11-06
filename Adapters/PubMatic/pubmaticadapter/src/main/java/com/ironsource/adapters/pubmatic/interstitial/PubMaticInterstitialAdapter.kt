package com.ironsource.adapters.pubmatic.interstitial

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial
import org.json.JSONObject

class PubMaticInterstitialAdapter(adapter: PubMaticAdapter) :
        AbstractInterstitialAdapter<PubMaticAdapter>(adapter) {

    private var mSmashListener : InterstitialSmashListener? = null
    private var mAdListener : PubMaticInterstitialAdListener? = null
    private var mAd: POBInterstitial? = null

    //region Interstitial API

    override fun initInterstitialForBidding(
            appKey: String?,
            userId: String?,
            config: JSONObject,
            listener: InterstitialSmashListener
    ) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitIdKey),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            PubMaticAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            PubMaticAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onInterstitialInitFailed(
                        ErrorBuilder.buildInitFailedError(
                            LOG_INIT_FAILED,
                            IronSourceConstants.INTERSTITIAL_AD_UNIT
                        )
                )
            }
            PubMaticAdapter.Companion.InitState.INIT_STATE_NONE,
            PubMaticAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
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
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }
        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        mAdListener = PubMaticInterstitialAdListener(listener, adUnitId)

        val context = ContextProvider.getInstance().applicationContext

        mAd = POBInterstitial(context)
        mAd?.setListener(mAdListener)
        postOnUIThread {
            mAd?.loadAd(serverData, PubMaticAdapter.BiddingHost) ?: run {
                listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
            }
        }
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        if (!isInterstitialReady(config)) {
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }
        postOnUIThread {
            mAd?.show() ?: run {
                listener.onInterstitialAdShowFailed(IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Ad is null"))
            }
        }
    }

    override fun isInterstitialReady(config: JSONObject): Boolean = mAd?.isReady == true

    override fun collectInterstitialBiddingData(
            config: JSONObject,
            adData: JSONObject?,
            biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback, POBAdFormat.INTERSTITIAL)
    }

    //region Helpers

    override fun destroyInterstitialAd(config: JSONObject?) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = config?.let { getConfigStringValueFromKey(it, adUnitIdKey) }
        IronLog.ADAPTER_API.verbose("Destroy interstitial ad of ${PubMaticAdapter.NETWORK_NAME}, adUnitId = $adUnitId")
        postOnUIThread {
            mAd?.destroy()
            mAd = null
            mAdListener = null
            mSmashListener = null
        }
    }

    //endregion

}
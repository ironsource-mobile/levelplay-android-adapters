package com.ironsource.adapters.moloco.interstitial

import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.moloco.sdk.publisher.InterstitialAd
import com.moloco.sdk.publisher.Moloco
import org.json.JSONObject
import java.lang.ref.WeakReference

class MolocoInterstitialAdapter(adapter: MolocoAdapter) :
    AbstractInterstitialAdapter<MolocoAdapter>(adapter) {

    private var mListener : InterstitialSmashListener? = null
    private var mAdListener : MolocoInterstitialAdListener? = null
    private var mAd: InterstitialAd? = null

    //regin Interstitial API

    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {

        val adUnitIdKey = MolocoAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        val appKey = getConfigStringValueFromKey(config, MolocoAdapter.getAppKey())

        if (adUnitId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitId))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitId),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        if (appKey.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appKey))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appKey),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId, appKey = $appKey")

        //save interstitial listener
        mListener = listener

        when (adapter.getInitState()) {
            MolocoAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            MolocoAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onInterstitialInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Moloco sdk init failed",
                        IronSourceConstants.INTERSTITIAL_AD_UNIT
                    )
                )
            }
            MolocoAdapter.Companion.InitState.INIT_STATE_NONE,
            MolocoAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appKey)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mListener?.onInterstitialInitSuccess()
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mListener?.onInterstitialInitFailed(
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

        val interstitialAdListener = MolocoInterstitialAdListener(listener, WeakReference(this))
        mAdListener = interstitialAdListener

        val adUnitIdKey = MolocoAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        mAd = Moloco.createInterstitial(adUnitId)
        mAd?.load(serverData, mAdListener)
    }

    override fun showInterstitial(
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isInterstitialReady(config)) {
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        } else {
            mAd?.show(mAdListener)
        }
    }

    override fun isInterstitialReady(config: JSONObject): Boolean {
        return mAd != null && mAd?.isLoaded == true
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //end region

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose()
            destroyInterstitialAd()
        mAdListener = null
        mListener = null
    }

    //end region

    //region Helpers

    internal fun destroyInterstitialAd() {
        mAd?.destroy()
        mAd = null
    }

    //end region

}
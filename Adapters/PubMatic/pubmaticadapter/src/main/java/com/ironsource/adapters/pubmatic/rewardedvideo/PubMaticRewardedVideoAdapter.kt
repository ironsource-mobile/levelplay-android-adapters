package com.ironsource.adapters.pubmatic.rewardedvideo

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.rewardedad.POBRewardedAd
import org.json.JSONObject

class PubMaticRewardedVideoAdapter(adapter: PubMaticAdapter) :
    AbstractRewardedVideoAdapter<PubMaticAdapter>(adapter) {

    private var mSmashListener : RewardedVideoSmashListener? = null
    private var mAdListener : PubMaticRewardedVideoAdListener? = null
    private var mAd: POBRewardedAd? = null

    //region Rewarded Video API

    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitIdKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        //save rewarded video listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            PubMaticAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            PubMaticAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
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
        mSmashListener?.onRewardedVideoInitSuccess()
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mSmashListener?.onRewardedVideoInitFailed(
            ErrorBuilder.buildInitFailedError(
                error,
                IronSourceConstants.REWARDED_VIDEO_AD_UNIT
            )
        )
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoAvailabilityChanged(false)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        mAdListener = PubMaticRewardedVideoAdListener(listener, adUnitId)

        val context = ContextProvider.getInstance().applicationContext
        mAd = POBRewardedAd.getRewardedAd(context)
        mAd?.setListener(mAdListener)
        postOnUIThread {
            mAd?.loadAd(serverData, PubMaticAdapter.BiddingHost) ?: run {
                listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
            }
        }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }
        postOnUIThread {
            mAd?.show() ?: run {
                listener.onRewardedVideoAdShowFailed(IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Ad is null"))
            }
        }
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean = mAd?.isReady == true

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback, POBAdFormat.REWARDEDAD)
    }

    //endregion

    //region Helpers

    override fun destroyRewardedVideoAd(config: JSONObject?) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = config?.let { getConfigStringValueFromKey(it, adUnitIdKey) }
        IronLog.ADAPTER_API.verbose("Destroy rewarded video ad of ${PubMaticAdapter.NETWORK_NAME}, adUnitId = $adUnitId")
        postOnUIThread {
            mAd?.destroy()
            mAd = null
            mSmashListener = null
            mAdListener = null
        }
    }

    //endregion

}
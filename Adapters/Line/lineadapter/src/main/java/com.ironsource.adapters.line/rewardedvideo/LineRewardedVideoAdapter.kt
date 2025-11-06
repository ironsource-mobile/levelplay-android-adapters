package com.ironsource.adapters.line.rewardedvideo

import com.five_corp.ad.AdLoader
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdVideoReward
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.adapters.line.LineAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import java.lang.ref.WeakReference

class LineRewardedVideoAdapter(adapter: LineAdapter) :
    AbstractRewardedVideoAdapter<LineAdapter>(adapter) {

    private var mSmashListener : RewardedVideoSmashListener? = null
    private var mAdListener : LineRewardedVideoAdListener? = null
    private var mRewardedVideoAd: FiveAdVideoReward? = null
    private var mAdLoader: AdLoader? = null
    private var mIsAdAvailable = false

    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val appIdKey = LineAdapter.getAppIdKey()
        val appId = config.optString(appIdKey)

        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appIdKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appIdKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        val slotIdKey = LineAdapter.getSlotIdKey()
        val slotId = config.optString(slotIdKey)

        if (slotId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(slotIdKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(slotIdKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("appId = $appId, slotId = $slotId")

        //save rewarded video listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            LineAdapter.Companion.InitState.INIT_STATE_NONE -> {
                adapter.initSdk(config)
            }
            LineAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            LineAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
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
        IronLog.ADAPTER_API.verbose()
        val appIdKey = LineAdapter.getAppIdKey()
        val appId = config.optString(appIdKey)

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        setRewardedVideoAdAvailability(false)

        val rewardedVideoAdListener = LineRewardedVideoAdListener(listener, WeakReference(this))
        mAdListener = rewardedVideoAdListener

        val adLoader = LineAdapter.getAdLoader(appId)
        mAdLoader = adLoader
        val bidData = BidData(serverData, null)
        adLoader?.loadRewardAd(bidData, rewardedVideoAdListener)
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        } else {
            mAdListener?.let { mRewardedVideoAd?.setEventListener(it) }
            mRewardedVideoAd?.showAd()
        }
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        return mRewardedVideoAd != null && mIsAdAvailable
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback, config)
    }

    //region Helpers

    internal fun setRewardedVideoAdAvailability(isAvailable: Boolean) {
        mIsAdAvailable = isAvailable
    }

    internal fun setRewardedVideoAd(rewardedAd: FiveAdVideoReward) {
        mRewardedVideoAd = rewardedAd
    }

    internal fun destroyRewardedVideoAd() {
        mRewardedVideoAd = null
        mAdLoader = null
    }

    //endregion
}
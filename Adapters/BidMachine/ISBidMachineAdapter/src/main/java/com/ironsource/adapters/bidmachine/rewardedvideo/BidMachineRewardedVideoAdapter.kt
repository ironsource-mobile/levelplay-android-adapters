package com.ironsource.adapters.bidmachine.rewardedvideo

import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.bidmachine.AdsFormat
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedRequest
import org.json.JSONObject
import java.lang.ref.WeakReference

class BidMachineRewardedVideoAdapter(adapter: BidMachineAdapter) :
    AbstractRewardedVideoAdapter<BidMachineAdapter>(adapter) {

    private var mRewardedVideoListener : RewardedVideoSmashListener? = null
    private var mRewardedVideoAdListener : BidMachineRewardedVideoAdListener? = null
    private var mRewardedVideoAd: RewardedAd? = null
    private var isRewardedVideoAdAvailable = false
    private var mRewardedRequest: RewardedRequest? = null

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val sourceKey = BidMachineAdapter.getSourceIdKey()
        val sourceId = config.optString(sourceKey)
        if (sourceId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(sourceKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(sourceKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        //save rewarded video listener
        mRewardedVideoListener = listener

        when (adapter.getInitState()) {
            BidMachineAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            BidMachineAdapter.Companion.InitState.INIT_STATE_NONE,
            BidMachineAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(sourceId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mRewardedVideoListener?.onRewardedVideoInitSuccess()
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        setRewardedVideoAdAvailability(false)

        val rewardedVideo = RewardedAd(ContextProvider.getInstance().applicationContext)
        val rewardedVideoAdListener = BidMachineRewardedVideoAdListener(listener, WeakReference(this))
        rewardedVideo.setListener(rewardedVideoAdListener)

        mRewardedVideoAdListener = rewardedVideoAdListener

        mRewardedRequest = RewardedRequest.Builder()
            .setBidPayload(serverData)
            .build()

        rewardedVideo.load(mRewardedRequest)
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
        } else {
            mRewardedVideoAd?.show()
        }

        setRewardedVideoAdAvailability(false)
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        return isRewardedVideoAdAvailable && (mRewardedVideoAd?.let { rewardedVideoAd ->
            return rewardedVideoAd.canShow() && !rewardedVideoAd.isExpired
        } ?: false)
    }

    override fun getRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? {
        return adapter.getBiddingData(AdsFormat.RewardedVideo)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            destroyRewardedVideoAd()
            mRewardedVideoListener = null
            mRewardedVideoAdListener = null
        }
    }

    //endregion

    // region Helpers

    internal fun setRewardedVideoAd(rewardedVideoAd: RewardedAd) {
        mRewardedVideoAd = rewardedVideoAd
    }

    internal fun setRewardedVideoAdAvailability(isAvailable: Boolean) {
        isRewardedVideoAdAvailable = isAvailable
    }

    internal fun destroyRewardedVideoAd() {
        mRewardedVideoAd?.setListener(null)
        mRewardedVideoAd?.destroy()
        mRewardedVideoAd = null
    }

    //endregion
}

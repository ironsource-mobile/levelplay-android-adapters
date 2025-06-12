package com.ironsource.adapters.verve.rewardedvideo

import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.adapters.verve.VerveAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd
import org.json.JSONObject
class VerveRewardedVideoAdapter(adapter: VerveAdapter) :
        AbstractRewardedVideoAdapter<VerveAdapter>(adapter) {
    private var mSmashListener : RewardedVideoSmashListener? = null
    private var mAdListener : VerveRewardedVideoAdListener? = null
    private var mAd: HyBidRewardedAd? = null

    override fun initRewardedVideoWithCallback(
            appKey: String?,
            userId: String?,
            config: JSONObject,
            listener: RewardedVideoSmashListener
    ) {

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            VerveAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            VerveAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onRewardedVideoInitFailed(
                        ErrorBuilder.buildInitFailedError(
                                LOG_INIT_FAILED,
                                IronSourceConstants.REWARDED_VIDEO_AD_UNIT
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

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val zoneIdKey= VerveAdapter.getZoneIdKey()
        val zoneId = getConfigStringValueFromKey(config, zoneIdKey)

        val rewardedVideoAdListener = VerveRewardedVideoAdListener(listener)
        mAdListener = rewardedVideoAdListener

        val context = ContextProvider.getInstance().applicationContext
        mAd = HyBidRewardedAd(
            context,
            zoneId,
            mAdListener
        )
        mAd?.prepareAd(serverData)
            ?: run {
                listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
            }

    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                    ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        val rewardedAdShowListener = VerveRewardedVideoAdListener(listener)
        mAdListener = rewardedAdShowListener
        mAd?.show() ?: run {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT)
            )
        }

    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean = mAd?.isReady == true

    override fun collectRewardedVideoBiddingData(
            config: JSONObject,
            adData: JSONObject?,
            biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //region memory handling
    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose()
        destroyRewardedVideoAd()
        mAdListener = null

    }

    //endregion

    // region Helpers

    private fun destroyRewardedVideoAd() {
        mAd?.destroy()
        mAd = null
        mSmashListener = null
    }

    //endregion

}
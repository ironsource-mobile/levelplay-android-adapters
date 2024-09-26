package com.ironsource.adapters.ogury.rewardedvideo

import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.ogury.ed.OguryOptinVideoAd
import org.json.JSONObject

class OguryRewardedVideoAdapter(adapter: OguryAdapter) :
    AbstractRewardedVideoAdapter<OguryAdapter>(adapter) {

    private var mSmashListener : RewardedVideoSmashListener? = null
    private var mAdListener : OguryRewardedVideoAdListener? = null
    private var mAd: OguryOptinVideoAd? = null
    private var mAdState: AdState = AdState.STATE_NONE

    // ad state possible values
    enum class AdState {
        STATE_NONE,
        STATE_LOAD,
        STATE_SHOW
    }

    //regin Rewarded Video API
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {

        //save rewarded video listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            OguryAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            OguryAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
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
        mSmashListener?.onRewardedVideoInitSuccess()
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        setAdState(AdState.STATE_LOAD)
        IronLog.ADAPTER_API.verbose()

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }
        val adUnitIdKey = OguryAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        val rewardedVideoAdListener = OguryRewardedVideoAdListener(listener, this)
        mAdListener = rewardedVideoAdListener

        val context = ContextProvider.getInstance().applicationContext
        mAd = OguryOptinVideoAd(context,adUnitId)
        mAd?.setListener(mAdListener)
        mAd?.setAdMarkup(serverData)
        mAd?.load()?: run {
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
        }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        setAdState(AdState.STATE_SHOW)
        IronLog.ADAPTER_API.verbose()

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }

        val rewardedAdShowListener = OguryRewardedVideoAdListener(listener, this)
        mAdListener = rewardedAdShowListener
        mAd?.show() ?: run {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT)
            )
        }
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        return mAd?.isLoaded() ?: false
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose()
        mAd = null
        mSmashListener = null
        mAdListener = null
    }

    //endregion

    //region helper methods
    fun getAdState(): AdState {
        return mAdState
    }

    private fun setAdState(newState: AdState) {
        mAdState = newState
    }

    //endregion

}

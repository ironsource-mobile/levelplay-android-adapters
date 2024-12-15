package com.ironsource.adapters.bigo.rewardedvideo

import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import sg.bigo.ads.api.RewardVideoAd
import sg.bigo.ads.api.RewardVideoAdLoader
import sg.bigo.ads.api.RewardVideoAdRequest
import java.lang.ref.WeakReference

class BigoRewardedVideoAdapter(adapter: BigoAdapter) :
    AbstractRewardedVideoAdapter<BigoAdapter>(adapter) {

    private var mSmashListener : RewardedVideoSmashListener? = null
    private var mAdListener : BigoRewardedVideoAdListener? = null
    private var mRewardedAd: RewardVideoAd? = null
    private var mAdLoader: RewardVideoAdLoader? = null
    private var isRewardedVideoAdAvailable = false

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val appIdKey = BigoAdapter.getAppIdKey()
        val appId= getConfigStringValueFromKey(config, appIdKey)
        if (appId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appId))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appId),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("appId = $appId")

        //save rewarded video listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            BigoAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            BigoAdapter.Companion.InitState.INIT_STATE_NONE,
            BigoAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appId)
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
        IronLog.ADAPTER_API.verbose()

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        setRewardedVideoAdAvailability(false)

        val slotIdKey= BigoAdapter.getSlotIdKey()
        val slotId = getConfigStringValueFromKey(config, slotIdKey)

        val rewardedAdListener = BigoRewardedVideoAdListener(WeakReference(this), listener)
        mAdListener = rewardedAdListener

        val rewardedAdLoader = RewardVideoAdLoader.Builder()
            .withAdLoadListener(mAdListener)
            .withExt(BigoAdapter.MEDIATION_INFO)
            .build()
        mAdLoader = rewardedAdLoader

        val rewardedAdRequest =
            RewardVideoAdRequest.Builder()
                .withBid(serverData)
                .withSlotId(slotId)
                .build()

        rewardedAdLoader.loadAd(rewardedAdRequest)

    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose()
        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT))
            return
        }
        mRewardedAd?.setAdInteractionListener(mAdListener)
        mRewardedAd?.show()
        setRewardedVideoAdAvailability(false)
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        return mRewardedAd?.isExpired == false
    }

    override fun getRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? {
        return adapter.getBiddingData()
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        destroyRewardedVideoAd()
        mAdListener = null
        mSmashListener = null
    }

    //endregion

    // region Helpers

    internal fun setRewardedVideoAd(ad: RewardVideoAd) {
        mRewardedAd = ad
    }

    internal fun setRewardedVideoAdAvailability(isAvailable: Boolean) {
        isRewardedVideoAdAvailable = isAvailable
    }

    internal fun destroyRewardedVideoAd() {
        mRewardedAd?.setAdInteractionListener(null)
        mRewardedAd?.destroy()
        mAdLoader = null
        mRewardedAd = null
    }

    //endregion
}

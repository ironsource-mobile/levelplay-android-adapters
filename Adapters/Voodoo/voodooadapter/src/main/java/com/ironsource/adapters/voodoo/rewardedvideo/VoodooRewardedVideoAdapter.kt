package com.ironsource.adapters.voodoo.rewardedvideo

import com.ironsource.adapters.voodoo.VoodooAdapter
import com.ironsource.adapters.voodoo.VoodooAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.adn.sdk.publisher.AdnAdPlacement
import io.adn.sdk.publisher.AdnAdRequest
import io.adn.sdk.publisher.AdnFullscreenAd
import io.adn.sdk.publisher.AdnFullscreenAdListener
import io.adn.sdk.publisher.AdnLoadTimeout
import io.adn.sdk.publisher.AdnSdk
import org.json.JSONObject

class VoodooRewardedVideoAdapter(adapter: VoodooAdapter) :
    AbstractRewardedVideoAdapter<VoodooAdapter>(adapter) {

    private var mSmashListener: RewardedVideoSmashListener? = null
    private var mAdListener: AdnFullscreenAdListener? = null
    private var mAd: AdnFullscreenAd? = null

    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val placementIdKey = VoodooAdapter.getPlacementIdKey()
        val placementId = getConfigStringValueFromKey(config, placementIdKey)
        if (placementId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementIdKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(placementIdKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        //save rewarded video listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            VoodooAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            VoodooAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
            }
            VoodooAdapter.Companion.InitState.INIT_STATE_NONE,
            VoodooAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
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
        val placementId = getConfigStringValueFromKey(config, VoodooAdapter.getPlacementIdKey())
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoAvailabilityChanged(false)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val rewardedVideoAdListener = VoodooRewardedVideoAdListener(placementId, listener)
        mAdListener = rewardedVideoAdListener
        mAd = AdnSdk.getRewardedAdInstance(ContextProvider.getInstance().currentActiveActivity, rewardedVideoAdListener)
        mAd?.load(AdnAdRequest.AdBidRequest(AdnAdPlacement.REWARDED, serverData, AdnLoadTimeout.MEDIATION)) ?: run {
            listener.onRewardedVideoAvailabilityChanged(false)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
        }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        val placementId = getConfigStringValueFromKey(config, VoodooAdapter.getPlacementIdKey())
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }
        mAd?.show() ?: run {
            listener.onRewardedVideoAdShowFailed(
                IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Ad is null")
            )
        }
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback, AdnAdPlacement.REWARDED)
    }

    // region Helpers

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean = mAd?.isReady() == true

    override fun destroyRewardedVideoAd(config: JSONObject?) {
        val placementIdKey = VoodooAdapter.getPlacementIdKey()
        val placementId = config?.let { getConfigStringValueFromKey(it, placementIdKey) }
        IronLog.ADAPTER_API.verbose("Destroy rewarded video ad of ${VoodooAdapter.NETWORK_NAME}, placementId = $placementId")
        mAd?.destroy()
        mAd = null
        mAdListener = null
        mSmashListener = null
    }

    //endregion
}
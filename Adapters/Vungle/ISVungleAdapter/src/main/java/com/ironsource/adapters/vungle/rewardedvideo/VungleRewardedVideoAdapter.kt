package com.ironsource.adapters.vungle.rewardedvideo

import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.AdConfig
import com.vungle.ads.RewardedAd
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class VungleRewardedVideoAdapter(adapter: VungleAdapter) :
    AbstractRewardedVideoAdapter<VungleAdapter>(adapter) {

    // Rewarded video collections
    private val mPlacementIdToListener: ConcurrentHashMap<String, RewardedVideoSmashListener> =
        ConcurrentHashMap()
    private val mPlacementToRewardedVideoAd: ConcurrentHashMap<String, RewardedAd> =
        ConcurrentHashMap()
    private val mRewardedVideoPlacementToListenerMap:
            ConcurrentHashMap<String, RewardedVideoSmashListener> = ConcurrentHashMap()
    private val mRewardedVideoPlacementIdsForInitCallbacks: CopyOnWriteArraySet<String> =
        CopyOnWriteArraySet<String>()
    private val mPlacementIdToAdAvailability: ConcurrentHashMap<String, Boolean> =
        ConcurrentHashMap()

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        val appId = config.optString(VungleAdapter.APP_ID)

        // Configuration Validation
        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementId))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(placementId),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appId))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appId),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        //add to rewarded video listener map
        mPlacementIdToListener[placementId] = listener

        // add placement to init callback map
        mRewardedVideoPlacementIdsForInitCallbacks.add(placementId)

        when (adapter.getInitState()) {
            VungleAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }

            VungleAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                // call listener init failed
                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Vungle SDK Init Failed",
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
            }

            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, appId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mRewardedVideoPlacementToListenerMap.entries.forEach { (rewardedVideoPlacement, rewardedVideoListener) ->
            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(rewardedVideoPlacement)) {
                rewardedVideoListener.onRewardedVideoInitSuccess()
            } else {
                loadRewardedVideoInternal(rewardedVideoPlacement, rewardedVideoListener, null)
            }
        }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mRewardedVideoPlacementToListenerMap.forEach { (rewardedVideoPlacement, rewardVideoListener) ->
            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(rewardedVideoPlacement)) {
                rewardVideoListener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        error,
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
            } else {
                rewardVideoListener.onRewardedVideoAvailabilityChanged(false)
            }
        }
    }

    // used for flows when the mediation doesn't need to get a callback for init
    override fun initAndLoadRewardedVideo(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        val appId = config.optString(VungleAdapter.APP_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(VungleAdapter.PLACEMENT_ID))
            listener.onRewardedVideoAvailabilityChanged(false)
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(VungleAdapter.APP_ID))
            listener.onRewardedVideoAvailabilityChanged(false)
            return
        }
        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        // add listener to map
        mRewardedVideoPlacementToListenerMap[placementId] = listener

        // notify listener about init state
        when (VungleAdapter.mInitState) {
            VungleAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                IronLog.ADAPTER_API.verbose("initRewardedVideo: load rv $placementId")
                // call load rewarded video
                loadRewardedVideoInternal(placementId, listener, null)
            }

            VungleAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                IronLog.ADAPTER_API.verbose("initRewardedVideo - onRewardedVideoAvailabilityChanged(false)")

                // set availability false
                listener.onRewardedVideoAvailabilityChanged(false)
            }

            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, appId)
            }
        }
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")
        loadRewardedVideoInternal(placementId, listener, serverData)
    }

    override fun loadRewardedVideo(
        config: JSONObject,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")
        loadRewardedVideoInternal(placementId, listener, null)
    }

    private fun loadRewardedVideoInternal(
        placementId: String,
        listener: RewardedVideoSmashListener,
        serverData: String?
    ) {
        IronLog.ADAPTER_API.verbose("loadRewardedVideo Vungle ad with placementId = $placementId")
        setRewardedVideoAdAvailability(placementId, false)

        val vungleRewardedVideoAdListener = VungleRewardedVideoAdListener(WeakReference(this), listener, placementId)
        val vungleRewardedVideo = RewardedAd(
            ContextProvider.getInstance().applicationContext,
            placementId,
            AdConfig()
        ).apply {

            adListener = vungleRewardedVideoAdListener
        }
        mPlacementToRewardedVideoAd[placementId] = vungleRewardedVideo
        vungleRewardedVideo.load(serverData)
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // if we can play
        if (!isRewardedVideoAvailable(config)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(VungleAdapter.PLACEMENT_ID))
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }
        val vungleRewardedVideo = mPlacementToRewardedVideoAd[placementId]
        adapter.dynamicUserId?.let {
            vungleRewardedVideo?.setUserId(it)
        }
        IronLog.ADAPTER_API.verbose("showRewardedVideo vungle ad $placementId")
        postOnUIThread{
            vungleRewardedVideo?.play()
        }
        setRewardedVideoAdAvailability(placementId, false)
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")
        if (placementId.isEmpty()) {
            return false
        }

        val isAvailable = mPlacementIdToAdAvailability[placementId] ?: false
        if (!isAvailable) {
            return false
        }

        // Get Vungle Rewarded video
        val vungleRewardedVideo = mPlacementToRewardedVideoAd[placementId]
        return vungleRewardedVideo?.canPlayAd() ?: false
    }

    internal fun setRewardedVideoAdAvailability(placementId: String, isAvailable: Boolean) {
        mPlacementIdToAdAvailability[placementId] = isAvailable
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            mPlacementIdToListener.clear()
            mPlacementToRewardedVideoAd.clear()
            mRewardedVideoPlacementToListenerMap.clear()
            mRewardedVideoPlacementIdsForInitCallbacks.clear()
            mPlacementIdToAdAvailability.clear()
        }
    }

    //endregion

}
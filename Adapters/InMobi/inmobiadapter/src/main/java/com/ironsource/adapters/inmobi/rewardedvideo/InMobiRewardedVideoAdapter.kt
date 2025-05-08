package com.ironsource.adapters.inmobi.rewardedvideo

import com.inmobi.ads.InMobiInterstitial
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource.AD_UNIT
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class InMobiRewardedVideoAdapter (adapter: InMobiAdapter) :
    AbstractRewardedVideoAdapter<InMobiAdapter>(adapter) {

    private val mPlacementIdToListener: ConcurrentHashMap<String, RewardedVideoSmashListener> =
        ConcurrentHashMap()
    private val placementToRewardedVideoAd: ConcurrentHashMap<String, InMobiInterstitial> = ConcurrentHashMap()
    private val rewardedVideoPlacementToListenerMap:
            ConcurrentHashMap<String, RewardedVideoSmashListener> =ConcurrentHashMap()
    private val rewardedVideoPlacementsForInitCallbacks: CopyOnWriteArraySet<String> = CopyOnWriteArraySet<String>()

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        val accountId = config.optString(InMobiAdapter.ACCOUNT_ID)

        // verify placementId
        if (!isValidPlacementId(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.PLACEMENT_ID))

            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing ${InMobiAdapter.PLACEMENT_ID}",
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        // verify accountId
        if (accountId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.ACCOUNT_ID))

            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "empty ${InMobiAdapter.ACCOUNT_ID} for $${InMobiAdapter.PLACEMENT_ID} - $placementId",
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

        //add to RewardedVideo listener map
        mPlacementIdToListener[placementId] = listener

        // add placement to init callback map
        rewardedVideoPlacementsForInitCallbacks.add(placementId)

        // notify listener about init state
        when (InMobiAdapter.initState) {
            InMobiAdapter.InitState.INIT_STATE_SUCCESS -> {
                IronLog.ADAPTER_API.verbose("initRewardedVideo: init rv $placementId")

                listener.onRewardedVideoInitSuccess()
            }

            InMobiAdapter.InitState.INIT_STATE_ERROR -> {
                IronLog.ADAPTER_API.verbose("initRewardedVideo - onRewardedVideoInitFailed")

                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "InMobi Sdk failed to initiate",
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
            }

            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, accountId)
            }
        }
    }
    override fun onNetworkInitCallbackSuccess() {
        if(adapter.shouldSetAgeRestrictedOnInitSuccess()){
            InMobiAdapter.ageRestrictionCollectingUserData?.let {
                adapter.setAgeRestricted(it)
            }
        }
        rewardedVideoPlacementToListenerMap.entries.forEach { (rewardedVideoPlacement, rewardVideoListener) ->
            if (rewardedVideoPlacementsForInitCallbacks.contains(rewardedVideoPlacement)) {
                rewardVideoListener.onRewardedVideoInitSuccess()
            } else {
                loadRewardedVideoInternal(rewardedVideoPlacement, null, rewardVideoListener)
            }
        }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        rewardedVideoPlacementToListenerMap.forEach { (rewardedVideoPlacement, rewardVideoListener) ->
            if (rewardedVideoPlacementsForInitCallbacks.contains(rewardedVideoPlacement)) {
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

    override fun initAndLoadRewardedVideo(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        val accountId = config.optString(InMobiAdapter.ACCOUNT_ID)

        // verified placementId
        if (!isValidPlacementId(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.PLACEMENT_ID))

            listener.onRewardedVideoAvailabilityChanged(false)
            return
        }

        // verified accountId
        if (accountId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.ACCOUNT_ID))

            listener.onRewardedVideoAvailabilityChanged(false)
            return
        }

        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

        // add listener to map
        rewardedVideoPlacementToListenerMap[placementId] = listener

        // notify listener about init state
        when (InMobiAdapter.initState) {
            InMobiAdapter.InitState.INIT_STATE_SUCCESS -> {
                IronLog.ADAPTER_API.verbose("initRewardedVideo: load rv $placementId")

                // call load rewarded video
                loadRewardedVideoInternal(placementId, null, listener)
            }
            InMobiAdapter.InitState.INIT_STATE_ERROR -> {
                IronLog.ADAPTER_API.verbose("initRewardedVideo - onRewardedVideoAvailabilityChanged(false)")

                // set availability false
                listener.onRewardedVideoAvailabilityChanged(false)
            }
            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, accountId)
            }
        }
    }

    override fun loadRewardedVideo(
        config: JSONObject,
        adData: JSONObject?,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

        // call load rewarded video
        loadRewardedVideoInternal(placementId, null, listener)
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

            // call load rewarded video
            loadRewardedVideoInternal(placementId, serverData, listener)
    }

    private fun loadRewardedVideoInternal(
        placementId: String,
        serverData: String?,
        smashListener: RewardedVideoSmashListener
    ) {
        IronLog.ADAPTER_API.verbose("loadRewardedVideo with ${InMobiAdapter.PLACEMENT_ID}: $placementId")

        IronLog.ADAPTER_API.verbose("create InMobi ad with ${InMobiAdapter.PLACEMENT_ID}: $placementId")

            parseToLong(placementId)?.let { placement ->
                val rewardedVideoListener =
                    InMobiRewardedVideoAdListener(smashListener, placementId)

                // post on ui thread
                postOnUIThread {
                    val inMobiRewardedVideo = InMobiInterstitial(
                        ContextProvider.getInstance().applicationContext,
                        placement,
                        rewardedVideoListener
                    )

                    // add InMobi rewarded video obj to map
                    placementToRewardedVideoAd[placementId] = inMobiRewardedVideo
                    IronLog.ADAPTER_API.verbose(
                        "loadRewardedVideo InMobi ad with ${InMobiAdapter.PLACEMENT_ID}:" +
                                " $placementId"
                    )

                    serverData?.let {
                        // Load InMobi rewarded video with bidding
                        try {
                            val bytes = it.toByteArray(Charsets.UTF_8)
                            inMobiRewardedVideo.load(bytes)
                        } catch (e: UnsupportedEncodingException) {
                            smashListener.onRewardedVideoAvailabilityChanged(false)
                        }
                    } ?: run {
                        // Load InMobi rewarded video
                        val map = adapter.getExtrasMap()
                        inMobiRewardedVideo.setExtras(map)
                        inMobiRewardedVideo.load()
                    }
                }
            }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)

        // check if inMobiRewardedVideo is not ready
        if (!isRewardedVideoAvailable(config)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.PLACEMENT_ID))

            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildGenericError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        placementToRewardedVideoAd[placementId]?.let { inMobiRewarded ->
            IronLog.ADAPTER_API.verbose("show InMobi ad with ${InMobiAdapter.PLACEMENT_ID}: $placementId")

            postOnUIThread {
                // call show rewarded video
                inMobiRewarded.show()
            }
        }
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

        // Get InMobi Rewarded video
        val inMobiRewardedVideo = placementToRewardedVideoAd[placementId]
        return inMobiRewardedVideo?.isReady() == true
    }
    override fun getRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? {
        return adapter.getBiddingData()
    }

    //endregion

    //region memory handling

    override fun releaseMemory(adUnit: AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
    }

    //endregion

    private fun isValidPlacementId(placementId: String): Boolean {
        parseToLong(placementId)?.let {
            return true
        }
        return false
    }

    private fun parseToLong(placementId: String): Long? {
        var placementIdLong: Long? = null
        try {
            placementIdLong = placementId.toLong()
        } catch (e: Exception) {
            IronLog.INTERNAL.error("parseToLong threw error ${e.message}")
        }
        return placementIdLong
    }
}
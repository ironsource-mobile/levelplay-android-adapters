package com.ironsource.adapters.pangle.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest
import com.ironsource.adapters.pangle.PangleAdapter
import com.ironsource.adapters.pangle.PangleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class PangleRewardedAdapter(networkSettings: NetworkSettings) : LevelPlayBaseRewardedVideo<PangleAdapter>(networkSettings) {

    private var rewardedAd: PAGRewardedAd? = null
    private var rewardedAdListener: PangleRewardedListener? = null
    private var isAdAvailableFlag = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val slotId = adData.getString(PangleConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.SLOT_ID.format(slotId))

        if (slotId.isNullOrEmpty()) {
            val errorMessage = PangleConstants.Logs.MISSING_PARAM.format(PangleConstants.SLOT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL
            )
            return
        }

        if (networkAdapter.isCoppaChildUser()) {
            val errorMessage = PangleConstants.Logs.CHILD_USER_ERROR.format(PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_MSG)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_CODE,
                errorMessage
            )
            return
        }

        setRewardedAdAvailability(false)
        rewardedAdListener = PangleRewardedListener(listener, WeakReference(this))
        val request = PAGRewardedRequest().apply { adString = adData.serverData }

        mainHandler.post {
            PAGRewardedAd.loadAd(slotId, request, rewardedAdListener!!)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL
            )
            return
        }

        if (networkAdapter.isCoppaChildUser()) {
            val errorMessage = PangleConstants.Logs.CHILD_USER_ERROR.format(PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_MSG)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdShowFailed(
                PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_CODE,
                errorMessage
            )
            return
        }

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, PangleConstants.NO_AD_TO_SHOW)
            return
        }

        mainHandler.post {
            rewardedAd?.apply {
                setAdInteractionListener(rewardedAdListener)
                show(activity)
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedAd != null && isAdAvailableFlag
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
        rewardedAdListener = null
        isAdAvailableFlag = false
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        val slotId = adData?.getString(PangleConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, slotId, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAd(rewardedAd: PAGRewardedAd?) {
        this.rewardedAd = rewardedAd
    }

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        this.isAdAvailableFlag = isAvailable
    }


    // endregion
}

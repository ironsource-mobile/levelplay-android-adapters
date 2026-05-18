package com.ironsource.adapters.bigo.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.adapters.bigo.BigoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import sg.bigo.ads.api.RewardVideoAd
import sg.bigo.ads.api.RewardVideoAdLoader
import sg.bigo.ads.api.RewardVideoAdRequest
import java.lang.ref.WeakReference

class BigoRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<BigoAdapter>(networkSettings) {

    private var rewardedListener: BigoRewardedListener? = null
    private var rewardedAd: RewardVideoAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val slotId = adData.getString(BigoConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = BigoConstants.Logs.SERVER_DATA_EMPTY
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        rewardedListener = BigoRewardedListener(listener, WeakReference(this))

        val rewardedAdLoader = RewardVideoAdLoader.Builder()
            .withAdLoadListener(rewardedListener)
            .withExt(BigoAdapter.getMediationInfo())
            .build()

        val rewardedAdRequest = RewardVideoAdRequest.Builder()
            .withBid(serverData)
            .withSlotId(slotId)
            .build()

        rewardedAdLoader.loadAd(rewardedAdRequest)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                BigoConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.setAdInteractionListener(rewardedListener)
        rewardedAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedAd != null && rewardedAd?.isExpired == false
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd?.setAdInteractionListener(null)
        rewardedAd?.destroy()
        rewardedAd = null
        rewardedListener = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            val errorMessage = BigoConstants.Logs.ADAPTER_UNAVAILABLE
            IronLog.INTERNAL.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAd(ad: RewardVideoAd) {
        rewardedAd = ad
    }

    // endregion
}

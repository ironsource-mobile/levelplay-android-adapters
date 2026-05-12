package com.ironsource.adapters.bidmachine.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import io.bidmachine.AdPlacementConfig
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedRequest

class BidMachineRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<BidMachineAdapter>(networkSettings) {

    private var rewardedAd: RewardedAd? = null

    // region LevelPlay Rewarded Video API

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val placementId = adData.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        rewardedAd = RewardedAd(context.applicationContext).apply {
            setListener(BidMachineRewardedListener(listener))
        }

        val adPlacementConfig = createRewardedPlacementConfig(placementId)
        val rewardedRequest = RewardedRequest.Builder(adPlacementConfig)
            .setBidPayload(adData.serverData)
            .build()

        rewardedAd?.load(rewardedRequest)
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: RewardedVideoAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.ADAPTER_API.error(BidMachineConstants.AD_NOT_READY)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                BidMachineConstants.AD_NOT_READY
            )
            return
        }

        rewardedAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedAd?.let { ad ->
            ad.canShow() && !ad.isExpired
        } ?: false
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd?.setListener(null)
        rewardedAd?.destroy()
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val placementId = adData?.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        val adPlacementConfig = createRewardedPlacementConfig(placementId)
        networkAdapter.collectBiddingData(context, biddingDataCallback, adPlacementConfig)
    }

    // endregion

    // region Helper Methods

    private fun createRewardedPlacementConfig(placementId: String?): AdPlacementConfig {
        val adPlacementConfigBuilder = AdPlacementConfig.rewardedBuilder()
        if (!placementId.isNullOrEmpty()) {
            adPlacementConfigBuilder.withPlacementId(placementId)
        }
        return adPlacementConfigBuilder.build()
    }

    // endregion
}

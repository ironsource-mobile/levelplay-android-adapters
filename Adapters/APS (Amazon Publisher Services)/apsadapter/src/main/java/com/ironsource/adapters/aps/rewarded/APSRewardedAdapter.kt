package com.ironsource.adapters.aps.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.amazon.aps.ads.ApsAdController
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.SDKUtilities
import com.ironsource.adapters.aps.APSAdapter
import com.ironsource.adapters.aps.APSConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class APSRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<APSAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: ApsAdController? = null
    private var isAdAvailableFlag = false
    private var adResponse: DTBAdResponse? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val uuid = adData.getString(APSConstants.UUID)
        IronLog.ADAPTER_API.verbose(APSConstants.Logs.UUID_LOG.format(uuid ?: ""))

        val adResponse = this.adResponse
        if (adResponse == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.AD_RESPONSE_MISSING)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                APSConstants.Logs.AD_RESPONSE_MISSING
            )
            return
        }

        isAdAvailableFlag = false

        val apsRewardedListener = APSRewardedListener(listener, WeakReference(this))
        val bidInfo = SDKUtilities.getBidInfo(adResponse)

        mainHandler.post {
            val rewardedVideoAdController = ApsAdController(context, apsRewardedListener)
            rewardedAd = rewardedVideoAdController
            rewardedVideoAdController.fetchRewardedAd(bidInfo)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                APSConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean = isAdAvailableFlag && rewardedAd != null

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
        adResponse = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!AdRegistration.isInitialized()) {
            IronLog.ADAPTER_API.error(APSConstants.Logs.APS_NOT_INITIALIZED)
            biddingDataCallback.onFailure(APSConstants.Logs.APS_NOT_INITIALIZED)
            return
        }

        if (adData == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.MISSING_AD_DATA)
            biddingDataCallback.onFailure(APSConstants.Logs.MISSING_AD_DATA)
            return
        }

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(APSConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val uuid = adData.getString(APSConstants.UUID)
        if (uuid.isNullOrEmpty()) {
            val errorMessage = APSConstants.Logs.MISSING_APS_CONFIGURATION.format(APSConstants.UUID)
            IronLog.ADAPTER_API.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        val adSize = networkAdapter.getVideoSize(context, uuid)
        networkAdapter.collectBiddingData(adSize, biddingDataCallback) { this.adResponse = it }
    }

    // endregion

    // region Helper Methods

    internal fun setAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}

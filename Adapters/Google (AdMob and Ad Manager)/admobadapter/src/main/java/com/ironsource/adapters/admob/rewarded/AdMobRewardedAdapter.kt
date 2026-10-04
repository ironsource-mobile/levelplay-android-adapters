package com.ironsource.adapters.admob.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class AdMobRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<AdMobAdapter>(networkSettings) {

    private var rewardedAd: RewardedAd? = null
    private var rewardedAdListener: AdMobRewardedListener? = null
    private var isAdAvailableFlag = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val adUnitId = adData.getString(AdMobConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.AD_UNIT_ID.format(adUnitId))

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = AdMobConstants.Logs.MISSING_PARAM.format(AdMobConstants.AD_UNIT_ID_KEY)
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
            IronLog.INTERNAL.error(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL
            )
            return
        }

        val serverData = adData.serverData
        setRewardedAdAvailability(false)
        val adListener = AdMobRewardedListener(listener, WeakReference(this))
        rewardedAdListener = adListener

        mainHandler.post {
            if (serverData.isNullOrEmpty()) {
                val adRequest = networkAdapter.createAdRequest(adUnitId, adData)
                RewardedAd.load(adRequest, adListener)
            } else {
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.LOAD_WITH_SERVER_DATA)
                RewardedAd.loadFromAdResponse(serverData, adListener)
            }
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.ADAPTER_API.error(AdMobConstants.Logs.AD_NOT_READY)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, AdMobConstants.Logs.AD_NOT_READY)
            return
        }

        mainHandler.post {
            rewardedAd?.let { ad ->
                val rewardedListener = rewardedAdListener ?: return@let
                ad.adEventCallback = rewardedListener
                ad.show(activity, rewardedListener)
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd != null && isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
        rewardedAdListener = null
        isAdAvailableFlag = false
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback, AdFormat.REWARDED, null)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAd(rewardedAd: RewardedAd?) {
        this.rewardedAd = rewardedAd
    }

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        this.isAdAvailableFlag = isAvailable
    }

    // endregion
}

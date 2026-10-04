package com.ironsource.adapters.facebook.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.facebook.ads.RewardData
import com.facebook.ads.RewardedVideoAd
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class FacebookRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<FacebookAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: RewardedVideoAd? = null
    private var rewardedAdListener: FacebookRewardedListener? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val placementId = adData.getString(FacebookConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            val errorMessage = FacebookConstants.Logs.MISSING_PARAM.format(FacebookConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        val dynamicUserId = getDynamicUserId()
        val appContext = context.applicationContext

        setRewardedAdAvailability(false)

        mainHandler.post {
            try {
                this.rewardedAd?.destroy()
                val rewardedAd = RewardedVideoAd(appContext, placementId)
                val adListener = FacebookRewardedListener(listener, WeakReference(this))
                rewardedAdListener = adListener
                val configBuilder = rewardedAd.buildLoadAdConfig()
                    .withAdListener(adListener)
                if (!serverData.isNullOrEmpty()) {
                    configBuilder.withBid(serverData)
                }
                if (!dynamicUserId.isNullOrEmpty()) {
                    configBuilder.withRewardData(RewardData(dynamicUserId, ""))
                }

                this.rewardedAd = rewardedAd
                rewardedAd.loadAd(configBuilder.build())
            } catch (e: Exception) {
                val errorMessage = FacebookConstants.Logs.LOAD_EXCEPTION.format(e.message)
                IronLog.INTERNAL.error(errorMessage)
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                    AdapterErrors.ADAPTER_ERROR_INTERNAL,
                    errorMessage
                )
            }
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()
        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, FacebookConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        mainHandler.post {
            try {
                val ad = rewardedAd
                if (ad == null || !ad.isAdLoaded || ad.isAdInvalidated) {
                    IronLog.INTERNAL.error(FacebookConstants.Logs.AD_NOT_AVAILABLE)
                    listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, FacebookConstants.Logs.AD_NOT_AVAILABLE)
                    return@post
                }
                setRewardedAdAvailability(false)
                rewardedAdListener?.showAttempted = true
                ad.show()
            } catch (e: Exception) {
                val errorMessage = FacebookConstants.Logs.SHOW_EXCEPTION.format(e.message)
                IronLog.INTERNAL.error(errorMessage)
                listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        setRewardedAdAvailability(false)
        mainHandler.post {
            rewardedAd?.destroy()
            rewardedAd = null
            rewardedAdListener = null
        }
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        IronLog.ADAPTER_API.verbose()
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }
        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}

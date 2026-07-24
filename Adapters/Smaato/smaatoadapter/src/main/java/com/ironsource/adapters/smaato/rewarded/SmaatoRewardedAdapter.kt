package com.ironsource.adapters.smaato.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.smaato.SmaatoAdapter
import com.ironsource.adapters.smaato.SmaatoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.smaato.sdk.rewarded.RewardedInterstitial
import com.smaato.sdk.rewarded.RewardedInterstitialAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class SmaatoRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<SmaatoAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: RewardedInterstitialAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val adSpaceId = adData.getString(SmaatoConstants.AD_SPACE_ID_KEY)
        IronLog.ADAPTER_API.verbose(SmaatoConstants.Logs.AD_SPACE_ID.format(adSpaceId ?: ""))

        if (adSpaceId.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.AD_SPACE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val adRequestParams = SmaatoAdapter.getAdRequest(serverData)
        if (adRequestParams == null) {
            IronLog.INTERNAL.error(SmaatoConstants.Logs.INVALID_AD_REQUEST)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                SmaatoConstants.Logs.INVALID_AD_REQUEST
            )
            return
        }

        val rewardedListener = SmaatoRewardedListener(listener, WeakReference(this))

        mainHandler.post {
            RewardedInterstitial.loadAd(adSpaceId, rewardedListener, adRequestParams)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                SmaatoConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        mainHandler.post {
            rewardedAd?.showAd()
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        rewardedAd?.isAvailableForPresentation == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(SmaatoConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(SmaatoConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAd(ad: RewardedInterstitialAd) {
        rewardedAd = ad
    }

    // endregion
}

package com.ironsource.adapters.yandex.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.adapters.yandex.YandexConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdType
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import java.lang.ref.WeakReference

class YandexRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<YandexAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAdListener: YandexRewardedListener? = null
    private var adLoader: RewardedAdLoader? = null
    private var ad: RewardedAd? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val adUnitId = adData.getString(YandexConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(YandexConstants.Logs.AD_UNIT_ID.format(adUnitId ?: ""))

        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(YandexConstants.Logs.AD_UNIT_ID_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                YandexConstants.Logs.AD_UNIT_ID_EMPTY
            )
            return
        }

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(YandexConstants.Logs.ADAPTER_UNAVAILABLE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                YandexConstants.Logs.ADAPTER_UNAVAILABLE
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(YandexConstants.Logs.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                YandexConstants.Logs.SERVER_DATA_EMPTY
            )
            return
        }

        setRewardedAdAvailability(false)

        val rewardedListener = YandexRewardedListener(listener, WeakReference(this))
        rewardedAdListener = rewardedListener

        val rewardedLoader = RewardedAdLoader(context.applicationContext).apply {
            setAdLoadListener(rewardedAdListener)
        }

        adLoader = rewardedLoader

        val adRequest: AdRequestConfiguration = AdRequestConfiguration.Builder(adUnitId)
            .setBiddingData(serverData)
            .setParameters(networkAdapter.getConfigParams())
            .build()

        mainHandler.post {
            rewardedLoader.loadAd(adRequest)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                YandexConstants.Logs.AD_NOT_AVAILABLE
            )
        } else {
            mainHandler.post {
                ad?.apply {
                    setAdEventListener(rewardedAdListener)
                    show(activity)
                }
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return ad != null && isAdAvailableFlag
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        destroyRewardedAd()
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            biddingDataCallback.onFailure(YandexConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val bidderTokenRequest = BidderTokenRequestConfiguration.Builder(AdType.REWARDED)
            .setParameters(networkAdapter.getConfigParams())
            .build()

        networkAdapter.collectBiddingData(context, biddingDataCallback, bidderTokenRequest)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    internal fun setRewardedAd(rewardedAd: RewardedAd) {
        ad = rewardedAd
    }

    internal fun destroyRewardedAd() {
        adLoader?.setAdLoadListener(null)
        adLoader = null
        ad?.setAdEventListener(null)
        ad = null
    }

    // endregion
}

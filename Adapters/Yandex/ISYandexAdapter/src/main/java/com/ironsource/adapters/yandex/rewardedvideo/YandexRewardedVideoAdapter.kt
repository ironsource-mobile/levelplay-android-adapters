package com.ironsource.adapters.yandex.rewardedvideo

import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdType
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import org.json.JSONObject
import java.lang.ref.WeakReference

class YandexRewardedVideoAdapter(adapter: YandexAdapter) :
    AbstractRewardedVideoAdapter<YandexAdapter>(adapter) {

    private var mSmashListener : RewardedVideoSmashListener? = null
    private var mYandexAdListener : YandexRewardedVideoAdListener? = null
    private var mAdLoader: RewardedAdLoader? = null
    private var mAd: RewardedAd? = null
    private var mIsAdAvailable = false

    // Used for flows when the mediation needs to get a callback for init
    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val appIdKey = YandexAdapter.getAppIdKey()
        val appId = config.optString(appIdKey)
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appIdKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appIdKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        val adUnitIdKey = YandexAdapter.getAdUnitIdKey()
        val adUnitId = config.optString(adUnitIdKey)
        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitIdKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("appId = $appId, adUnitId = $adUnitId")

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            YandexAdapter.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }

            YandexAdapter.InitState.INIT_STATE_NONE,
            YandexAdapter.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onRewardedVideoInitSuccess()
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        IronLog.ADAPTER_API.verbose()

        setRewardedVideoAdAvailability(false)

        val rewardedVideoAdListener = YandexRewardedVideoAdListener(listener, WeakReference(this))
        mYandexAdListener = rewardedVideoAdListener

        val adUnitIdKey = YandexAdapter.getAdUnitIdKey()
        val adUnitId = config.optString(adUnitIdKey)
        val rewardedVideoAdLoader = RewardedAdLoader(ContextProvider.getInstance().applicationContext).apply {
            setAdLoadListener(mYandexAdListener)
        }

        mAdLoader = rewardedVideoAdLoader

        val adRequest: AdRequestConfiguration = AdRequestConfiguration.Builder(adUnitId)
            .setBiddingData(serverData)
            .setParameters(adapter.getConfigParams())
            .build()

        postOnUIThread {
            rewardedVideoAdLoader.loadAd(adRequest)
        }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        } else {
            postOnUIThread {
                mAd?.apply {
                    setAdEventListener(mYandexAdListener)
                    show(ContextProvider.getInstance().currentActiveActivity)
                }
            }
        }
    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        return mAd != null && mIsAdAvailable
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        val bidderTokenRequest = BidderTokenRequestConfiguration.Builder(AdType.REWARDED)
            .setParameters(adapter.getConfigParams())
            .build()

        adapter.collectBiddingData(biddingDataCallback, bidderTokenRequest)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
            destroyRewardedVideoAd()
            mYandexAdListener = null
            mSmashListener = null
    }

    //endregion

    //region Helpers

    internal fun setRewardedVideoAdAvailability(isAvailable: Boolean) {
        mIsAdAvailable = isAvailable
    }

    internal fun setRewardedVideoAd(rewardedAd: RewardedAd) {
        mAd = rewardedAd
    }

    internal fun destroyRewardedVideoAd() {
        mAdLoader?.setAdLoadListener(null)
        mAdLoader = null
        mAd?.setAdEventListener(null)
        mAd = null
    }
    //endregion


}
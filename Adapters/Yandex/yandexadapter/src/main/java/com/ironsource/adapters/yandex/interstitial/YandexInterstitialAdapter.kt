package com.ironsource.adapters.yandex.interstitial

import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdType
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import org.json.JSONObject
import java.lang.ref.WeakReference


class YandexInterstitialAdapter(adapter: YandexAdapter) :
AbstractInterstitialAdapter<YandexAdapter>(adapter) {

    private var mSmashListener : InterstitialSmashListener? = null
    private var mYandexAdListener : YandexInterstitialAdListener? = null
    private var mAdLoader: InterstitialAdLoader? = null
    private var mAd: InterstitialAd? = null
    private var mIsAdAvailable = false

    override fun initInterstitialForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        val appIdKey = YandexAdapter.getAppIdKey()
        val appId = getConfigStringValueFromKey(config, appIdKey)
        if (appId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appIdKey))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appIdKey),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        val adUnitIdKey = YandexAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        if (adUnitId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitIdKey),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("appId = $appId, adUnitId = $adUnitId")

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            YandexAdapter.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            YandexAdapter.InitState.INIT_STATE_NONE,
            YandexAdapter.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onInterstitialInitSuccess()
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        IronLog.ADAPTER_API.verbose()

        setInterstitialAdAvailability(false)

        val interstitialAdListener = YandexInterstitialAdListener(listener, WeakReference(this))
        mYandexAdListener = interstitialAdListener

        val adUnitIdKey = YandexAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        val interstitialAdLoader = InterstitialAdLoader(ContextProvider.getInstance().applicationContext).apply {
            setAdLoadListener(mYandexAdListener)
        }

        mAdLoader = interstitialAdLoader

        val adRequest: AdRequestConfiguration = AdRequestConfiguration.Builder(adUnitId)
            .setBiddingData(serverData)
            .setParameters(adapter.getConfigParams())
            .build()

        postOnUIThread {
            interstitialAdLoader.loadAd(adRequest)
        }
    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isInterstitialReady(config)) {
            listener.onInterstitialAdShowFailed(
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

    override fun isInterstitialReady(config: JSONObject): Boolean {
        return mAd != null && mIsAdAvailable
    }

    override fun collectInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        val bidderTokenRequest = BidderTokenRequestConfiguration.Builder(AdType.INTERSTITIAL)
            .setParameters(adapter.getConfigParams())
            .build()

        adapter.collectBiddingData(biddingDataCallback, bidderTokenRequest)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        destroyInterstitialAd()
        mYandexAdListener = null
        mSmashListener = null
    }

    //endregion

    //region Helpers

    internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
        mIsAdAvailable = isAvailable
    }

    internal fun setInterstitialAd(interstitialAd: InterstitialAd) {
        mAd = interstitialAd
    }

    internal fun destroyInterstitialAd() {
        mAdLoader?.setAdLoadListener(null)
        mAdLoader = null
        mAd?.setAdEventListener(null)
        mAd = null
    }
    //endregion


}
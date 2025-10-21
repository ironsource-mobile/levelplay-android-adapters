package com.ironsource.adapters.yandex.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdType
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration
import org.json.JSONObject
import java.lang.ref.WeakReference

class YandexBannerAdapter (adapter: YandexAdapter) :
    AbstractBannerAdapter<YandexAdapter>(adapter) {

    private var mSmashListener : BannerSmashListener? = null
    private var mYandexAdListener : YandexBannerAdListener? = null
    private var mAdView: BannerAdView? = null

    companion object {
        private const val BANNER_SIZE_IS_NULL_ERROR_MSG = "banner size is null, banner has been destroyed"
    }

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        val appIdKey = YandexAdapter.getAppIdKey()
        val appId = getConfigStringValueFromKey(config, appIdKey)
        if (appId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appIdKey))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appIdKey),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        val adUnitIdKey = YandexAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        if (adUnitId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitIdKey),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("appId = $appId, adUnitId = $adUnitId")

        //save banner listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            YandexAdapter.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            YandexAdapter.InitState.INIT_STATE_NONE,
            YandexAdapter.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onBannerInitSuccess()
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (bannerSize == null) {
            IronLog.INTERNAL.error(BANNER_SIZE_IS_NULL_ERROR_MSG)
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(BANNER_SIZE_IS_NULL_ERROR_MSG))
            return
        }

        val yandexBannerSize = getBannerSize(bannerSize)
        if (yandexBannerSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, yandexBannerSize.width),
            AdapterUtils.dpToPixels(context, yandexBannerSize.height),
            Gravity.CENTER
        )

        val adUnitIdKey = YandexAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        val bannerAdView = BannerAdView(ContextProvider.getInstance().applicationContext)
        bannerAdView.setAdUnitId(adUnitId)
        bannerAdView.setAdSize(yandexBannerSize)

        val bannerAdListener = YandexBannerAdListener(listener, WeakReference(this), bannerAdView, layoutParams)
        mYandexAdListener = bannerAdListener

        val adRequest: AdRequest = AdRequest.Builder()
            .setBiddingData(serverData)
            .setParameters(adapter.getConfigParams())
            .build()
        bannerAdView.setBannerAdEventListener(mYandexAdListener)

        postOnUIThread {
            if (bannerSize == null) {
                IronLog.INTERNAL.error(BANNER_SIZE_IS_NULL_ERROR_MSG)
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(BANNER_SIZE_IS_NULL_ERROR_MSG))
                return@postOnUIThread
            }
            bannerAdView.loadAd(adRequest)
        }
    }

    override fun destroyBanner(config: JSONObject) {
        IronLog.ADAPTER_API.verbose()
        destroyBannerViewAd()
    }

    override fun collectBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        val bidderTokenRequest: BidderTokenRequestConfiguration.Builder =
            BidderTokenRequestConfiguration.Builder(AdType.BANNER)

        adData?.let {
            val bannerSize =
                adData.opt(IronSourceConstants.BANNER_SIZE) as ISBannerSize?
            bannerSize.let {
                getBannerSize(bannerSize)
                    ?.let {
                        bidderTokenRequest.setBannerAdSize(it)
                    }
            }
        }

        bidderTokenRequest.setParameters(adapter.getConfigParams())
        adapter.collectBiddingData(biddingDataCallback, bidderTokenRequest.build())
    }

    // region Helpers

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mAdView?.setBannerAdEventListener(null)
            mAdView?.destroy()
            mAdView = null
        }
    }

    internal fun setBannerView(bannerAdView: BannerAdView) {
        mAdView = bannerAdView
    }

    private fun getBannerSize(bannerSize: ISBannerSize?): BannerAdSize? {
        if(bannerSize == null) {
            IronLog.INTERNAL.verbose("Banner size is null")
            return null
        }

        val context = ContextProvider.getInstance().applicationContext;
        return when (bannerSize.description) {
            "BANNER" -> BannerAdSize.fixedSize(context, 320, 50)
            "LARGE" -> BannerAdSize.fixedSize(context, 320, 90)
            "RECTANGLE" -> BannerAdSize.fixedSize(context, 300, 250)
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(context)) {
                    BannerAdSize.fixedSize(context, 728, 90)
                } else {
                    BannerAdSize.fixedSize(context, 320, 50)
                })
            "CUSTOM" -> {
                val width = bannerSize.width
                val height = bannerSize.height
                BannerAdSize.fixedSize(context, width, height)
            }
            else -> null
        }
    }

    //endregion
}
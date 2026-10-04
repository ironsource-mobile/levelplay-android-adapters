package com.ironsource.adapters.admob.banner

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class AdMobBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<AdMobAdapter>(networkSettings) {

    private var bannerAdView: AdView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun loadAd(adData: AdData, activity: Activity, bannerSize: ISBannerSize, listener: BannerAdListener) {
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
        val appContext = activity.applicationContext

        mainHandler.post {
            try {
                loadBanner(networkAdapter, adData, appContext, bannerSize, adUnitId, serverData, listener)
            } catch (e: Exception) {
                IronLog.ADAPTER_API.error(AdMobConstants.Logs.LOAD_EXCEPTION.format(e.message))
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                    AdapterErrors.ADAPTER_ERROR_INTERNAL,
                    AdMobConstants.Logs.LOAD_EXCEPTION.format(e.message)
                )
            }
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            try {
                bannerAdView?.destroy()
                bannerAdView = null
            } catch (e: Exception) {
                IronLog.ADAPTER_API.error(AdMobConstants.Logs.DESTROY_EXCEPTION.format(e.message))
            }
        }
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        val extras = Bundle()
        val bannerSize = adData?.adUnitData?.get(IronSourceConstants.BANNER_SIZE) as? ISBannerSize
        if (bannerSize != null && bannerSize.isAdaptive) {
            getAdSize(context, bannerSize)?.let { adMobBannerSize ->
                extras.putInt(AdMobConstants.EXTRA_ADAPTIVE_BANNER_WIDTH, adMobBannerSize.width)
                extras.putInt(AdMobConstants.EXTRA_ADAPTIVE_BANNER_HEIGHT, adMobBannerSize.height)
                IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.ADAPTIVE_BANNER_SIZE.format(adMobBannerSize.width, adMobBannerSize.height))
            }
        }

        networkAdapter.collectBiddingData(biddingDataCallback, AdFormat.BANNER, extras)
    }

    // endregion

    // region Helper Methods

    private fun loadBanner(
        networkAdapter: AdMobAdapter,
        adData: AdData,
        context: Context,
        bannerSize: ISBannerSize,
        adUnitId: String,
        serverData: String?,
        listener: BannerAdListener
    ) {
        val adMobBannerSize = getAdSize(context, bannerSize)
        if (adMobBannerSize == null) {
            val errorMessage = AdMobConstants.Logs.BANNER_SIZE_NOT_SUPPORTED.format(bannerSize.description)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        val adView = AdView(context)
        adView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        bannerAdView = adView

        val bannerListener = AdMobBannerListener(listener, adView)
        if (serverData.isNullOrEmpty()) {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.LOAD_AD)
            adView.loadAd(networkAdapter.createBannerAdRequest(adUnitId, adMobBannerSize, adData), bannerListener)
        } else {
            IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.LOAD_WITH_SERVER_DATA)
            adView.loadFromAdResponse(serverData, bannerListener)
        }
    }

    private fun getAdSize(context: Context, selectedBannerSize: ISBannerSize): AdSize? {
        var adSize: AdSize? = when (selectedBannerSize.description) {
            AdMobConstants.BANNER_SIZE_BANNER -> AdSize.BANNER
            AdMobConstants.BANNER_SIZE_LARGE -> AdSize.LARGE_BANNER
            AdMobConstants.BANNER_SIZE_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
            AdMobConstants.BANNER_SIZE_SMART -> if (AdapterUtils.isLargeScreen(context)) AdSize.LEADERBOARD else AdSize.BANNER
            AdMobConstants.BANNER_SIZE_CUSTOM -> AdSize(selectedBannerSize.width, selectedBannerSize.height)
            else -> null
        }

        if (selectedBannerSize.isAdaptive && adSize != null) {
            val levelPlayAdaptiveSize = selectedBannerSize.toLevelPlayAdSize(context)
            val adMobAdaptiveSize = getAdaptiveBannerSize(context, levelPlayAdaptiveSize.getWidth())
            if (adMobAdaptiveSize != null) {
                adSize = adMobAdaptiveSize
            }
        }

        return adSize
    }

    private fun getAdaptiveBannerSize(context: Context, width: Int): AdSize =
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, width)

    // endregion
}

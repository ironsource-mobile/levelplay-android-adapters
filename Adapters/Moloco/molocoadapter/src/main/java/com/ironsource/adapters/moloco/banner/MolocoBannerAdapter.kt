package com.ironsource.adapters.moloco.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.adapters.moloco.MolocoConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class MolocoBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<MolocoAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: Banner? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val adUnitId = adData.getString(MolocoConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.AD_UNIT_ID_LOG.format(adUnitId ?: ""))

        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.MISSING_PARAM.format(MolocoConstants.AD_UNIT_ID_KEY))
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MolocoConstants.Logs.MISSING_PARAM.format(MolocoConstants.AD_UNIT_ID_KEY)
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MolocoConstants.Logs.SERVER_DATA_EMPTY
            )
            return
        }

        val appContext = activity.applicationContext
        val layoutParams = createBannerLayoutParams(appContext, bannerSize)

        createBannerWithSize(bannerSize, adUnitId, appContext) { adView, error ->
            if (error != null) {
                IronLog.ADAPTER_CALLBACK.error(MolocoConstants.Logs.CREATE_AD_ERROR.format(error.errorCode, error.description))
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                    error.errorCode,
                    error.description
                )
            } else {
                adView?.let { ad ->
                    bannerAdView = ad
                    bannerAdView?.apply {
                        adShowListener = MolocoBannerShowListener(listener)
                        load(serverData, MolocoBannerLoadListener(listener, layoutParams, ad))
                    }
                } ?: run {
                    listener.onAdLoadFailed(
                        AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                        AdapterErrors.ADAPTER_ERROR_INTERNAL,
                        MolocoConstants.INVALID_CONFIGURATION
                    )
                }
            }
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAdView?.destroy()
            bannerAdView = null
        }
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(MolocoConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun createBannerLayoutParams(context: Context, size: ISBannerSize): FrameLayout.LayoutParams {
        val layoutParams = when (size.description) {
            MolocoConstants.BANNER_SIZE_BANNER -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MolocoConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, MolocoConstants.BANNER_HEIGHT)
            )
            MolocoConstants.BANNER_SIZE_LARGE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MolocoConstants.LEADERBOARD_WIDTH),
                AdapterUtils.dpToPixels(context, MolocoConstants.LEADERBOARD_HEIGHT)
            )
            MolocoConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MolocoConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, MolocoConstants.RECTANGLE_HEIGHT)
            )
            MolocoConstants.BANNER_SIZE_SMART -> {
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, MolocoConstants.LEADERBOARD_WIDTH),
                        AdapterUtils.dpToPixels(context, MolocoConstants.LEADERBOARD_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, MolocoConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, MolocoConstants.BANNER_HEIGHT)
                    )
                }
            }
            else -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, size.width),
                AdapterUtils.dpToPixels(context, size.height)
            )
        }
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    private fun createBannerWithSize(
        size: ISBannerSize,
        adUnitId: String,
        context: Context,
        createCallback: (Banner?, MolocoAdError.AdCreateError?) -> Unit
    ) {
        val mediationInfo = MolocoAdapter.mediationInfo
        when (size.description) {
            MolocoConstants.BANNER_SIZE_BANNER -> Moloco.createBanner(mediationInfo, adUnitId, null, createCallback)
            MolocoConstants.BANNER_SIZE_LARGE -> Moloco.createBannerTablet(mediationInfo, adUnitId, null, createCallback)
            MolocoConstants.BANNER_SIZE_RECTANGLE -> Moloco.createMREC(mediationInfo, adUnitId, null, createCallback)
            MolocoConstants.BANNER_SIZE_SMART -> {
                if (AdapterUtils.isLargeScreen(context)) {
                    Moloco.createBannerTablet(mediationInfo, adUnitId, null, createCallback)
                } else {
                    Moloco.createBanner(mediationInfo, adUnitId, null, createCallback)
                }
            }
            else -> Moloco.createBanner(mediationInfo, adUnitId, null, createCallback)
        }
    }

    // endregion
}

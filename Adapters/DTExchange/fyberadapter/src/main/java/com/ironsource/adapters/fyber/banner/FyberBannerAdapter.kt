package com.ironsource.adapters.fyber.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.ironsource.adapters.fyber.FyberAdapter
import com.ironsource.adapters.fyber.FyberConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class FyberBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<FyberAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: InneractiveAdSpot? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val spotId = adData.getString(FyberConstants.SPOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(FyberConstants.Logs.SPOT_ID.format(spotId ?: ""))

        if (spotId.isNullOrEmpty()) {
            val errorMessage = FyberConstants.Logs.MISSING_PARAM.format(FyberConstants.SPOT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val appContext = activity.applicationContext
        val layoutParams = getBannerLayoutParams(appContext, bannerSize)
        if (layoutParams == null) {
            IronLog.INTERNAL.error(FyberConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                FyberConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val serverData = adData.serverData
        mainHandler.post {
            val adViewController = InneractiveAdViewUnitController()
            val bannerListener = FyberBannerListener(listener, adViewController, layoutParams, appContext)
            adViewController.setEventsListener(bannerListener)

            bannerAdView = InneractiveAdSpotManager.get().createSpot().apply {
                setMediationName(FyberConstants.MEDIATION_NAME)
                setMediationVersion(FyberConstants.ADAPTER_VERSION)
                addUnitController(adViewController)
                setRequestListener(bannerListener)
            }

            if (serverData.isNullOrEmpty()) {
                bannerAdView?.requestAd(InneractiveAdRequest(spotId))
            } else {
                bannerAdView?.loadAd(serverData)
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
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(FyberConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(FyberConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerLayoutParams(context: Context, bannerSize: ISBannerSize): FrameLayout.LayoutParams? {
        val layoutParams = when (bannerSize.description) {
            FyberConstants.BANNER_SIZE_BANNER -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, FyberConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, FyberConstants.BANNER_HEIGHT)
            )

            FyberConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, FyberConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, FyberConstants.RECTANGLE_HEIGHT)
            )

            FyberConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, FyberConstants.LARGE_WIDTH),
                        AdapterUtils.dpToPixels(context, FyberConstants.LARGE_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, FyberConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, FyberConstants.BANNER_HEIGHT)
                    )
                }

            else -> return null
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}

package com.ironsource.adapters.mintegral.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.mintegral.MintegralAdapter
import com.ironsource.adapters.mintegral.MintegralConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.mbridge.msdk.mbbid.out.BidConstants
import com.mbridge.msdk.out.BannerSize
import com.mbridge.msdk.out.MBBannerView
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class MintegralBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<MintegralAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerView: MBBannerView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)
        val unitId = adData.getString(MintegralConstants.UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.PLACEMENT_ID_AND_UNIT_ID.format(placementId ?: "", unitId ?: ""))

        if (unitId.isNullOrEmpty()) {
            val errorMessage = MintegralConstants.Logs.MISSING_PARAM.format(MintegralConstants.UNIT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val context = activity.applicationContext
        val layoutParams = createBannerLayoutParams(context, bannerSize)
        val mintegralBannerSize = createBannerSize(context, bannerSize)

        bannerView = MBBannerView(context).apply {
            init(mintegralBannerSize, placementId, unitId)
            setRefreshTime(0)
            setAllowShowCloseBtn(false)
            setBannerAdListener(MintegralBannerListener(listener, this, layoutParams))
        }

        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.LOAD_BANNER.format(bannerSize.width, bannerSize.height, placementId, unitId, adData.serverData))
        bannerView?.loadFromBid(adData.serverData)
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()

        mainHandler.post {
            bannerView?.release()
            bannerView = null
        }
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MintegralConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(MintegralConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val placementId = adData?.getString(MintegralConstants.PLACEMENT_ID_KEY)
        val unitId = adData?.getString(MintegralConstants.UNIT_ID_KEY)
        networkAdapter.collectBiddingData(context, BidConstants.BID_FILTER_VALUE_AD_TYPE_BANNER, placementId, unitId, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun createBannerSize(context: Context, bannerSize: ISBannerSize): BannerSize {
        return when (bannerSize.description) {
            MintegralConstants.BANNER_SIZE_BANNER -> BannerSize(BannerSize.STANDARD_TYPE, MintegralConstants.BANNER_WIDTH, MintegralConstants.BANNER_HEIGHT)
            MintegralConstants.BANNER_SIZE_LARGE -> BannerSize(BannerSize.LARGE_TYPE, MintegralConstants.LARGE_WIDTH, MintegralConstants.LARGE_HEIGHT)
            MintegralConstants.BANNER_SIZE_RECTANGLE -> BannerSize(BannerSize.MEDIUM_TYPE, MintegralConstants.RECTANGLE_WIDTH, MintegralConstants.RECTANGLE_HEIGHT)
            MintegralConstants.BANNER_SIZE_SMART -> {
                if (AdapterUtils.isLargeScreen(context)) {
                    BannerSize(BannerSize.SMART_TYPE, MintegralConstants.LEADERBOARD_WIDTH, MintegralConstants.LEADERBOARD_HEIGHT)
                } else {
                    BannerSize(BannerSize.STANDARD_TYPE, MintegralConstants.BANNER_WIDTH, MintegralConstants.BANNER_HEIGHT)
                }
            }
            else -> BannerSize(BannerSize.DEV_SET_TYPE, bannerSize.width, bannerSize.height)
        }
    }

    private fun createBannerLayoutParams(context: Context, size: ISBannerSize): FrameLayout.LayoutParams {
        var layoutParams = FrameLayout.LayoutParams(0, 0)

        when (size.description) {
            MintegralConstants.BANNER_SIZE_BANNER -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MintegralConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, MintegralConstants.BANNER_HEIGHT)
            )
            MintegralConstants.BANNER_SIZE_LARGE -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MintegralConstants.LARGE_WIDTH),
                AdapterUtils.dpToPixels(context, MintegralConstants.LARGE_HEIGHT)
            )
            MintegralConstants.BANNER_SIZE_RECTANGLE -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MintegralConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, MintegralConstants.RECTANGLE_HEIGHT)
            )
            MintegralConstants.BANNER_SIZE_SMART -> layoutParams = if (AdapterUtils.isLargeScreen(context)) {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, MintegralConstants.LEADERBOARD_WIDTH),
                    AdapterUtils.dpToPixels(context, MintegralConstants.LEADERBOARD_HEIGHT)
                )
            } else {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, MintegralConstants.BANNER_WIDTH),
                    AdapterUtils.dpToPixels(context, MintegralConstants.BANNER_HEIGHT)
                )
            }
            MintegralConstants.BANNER_SIZE_CUSTOM -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, size.width),
                AdapterUtils.dpToPixels(context, size.height)
            )
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}

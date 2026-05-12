package com.ironsource.adapters.bidmachine.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
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
import io.bidmachine.AdPlacementConfig
import io.bidmachine.BannerAdSize
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView

class BidMachineBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<BidMachineAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: BannerView? = null

    // region LevelPlay Banner API

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val placementId = adData.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val appContext = activity.applicationContext
        val bidMachineBannerSize = getBannerSize(bannerSize, appContext)
        if (bidMachineBannerSize == null) {
            IronLog.INTERNAL.error(BidMachineConstants.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                BidMachineConstants.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(appContext, bidMachineBannerSize.width),
            AdapterUtils.dpToPixels(appContext, bidMachineBannerSize.height),
            Gravity.CENTER
        )

        bannerAdView = BannerView(appContext).apply {
            setListener(BidMachineBannerListener(listener, layoutParams))
        }

        val adPlacementConfig = createBannerPlacementConfig(placementId, bidMachineBannerSize)
        val bannerRequest = BannerRequest.Builder(adPlacementConfig)
            .setBidPayload(adData.serverData)
            .build()

        mainHandler.post {
            bannerAdView?.load(bannerRequest)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAdView?.setListener(null)
            bannerAdView?.destroy()
            bannerAdView = null
        }
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val placementId = adData?.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        val bannerSize = adData?.adUnitData?.get(BidMachineConstants.BANNER_SIZE_KEY)
        if (bannerSize !is ISBannerSize) {
            IronLog.INTERNAL.error(BidMachineConstants.BANNER_SIZE_INVALID)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(BidMachineConstants.BANNER_SIZE_INVALID))
            return
        }

        val bidMachineBannerSize = getBannerSize(bannerSize, context.applicationContext)
        if (bidMachineBannerSize == null) {
            IronLog.INTERNAL.error(BidMachineConstants.UNSUPPORTED_BANNER_SIZE)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(BidMachineConstants.UNSUPPORTED_BANNER_SIZE))
            return
        }

        val adPlacementConfig = createBannerPlacementConfig(placementId, bidMachineBannerSize)
        networkAdapter.collectBiddingData(context, biddingDataCallback, adPlacementConfig)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(bannerSize: ISBannerSize, context: Context): BannerAdSize? {
        return when (bannerSize.description) {
            BidMachineConstants.BANNER -> BannerAdSize.Banner
            BidMachineConstants.LEADERBOARD -> BannerAdSize.Leaderboard
            BidMachineConstants.RECTANGLE -> BannerAdSize.MediumRectangle
            BidMachineConstants.SMART -> if (AdapterUtils.isLargeScreen(context)) BannerAdSize.Leaderboard else BannerAdSize.Banner
            else -> null
        }
    }

    private fun createBannerPlacementConfig(placementId: String?, bannerSize: BannerAdSize): AdPlacementConfig {
        val adPlacementConfigBuilder = AdPlacementConfig.bannerBuilder(bannerSize)
        if (!placementId.isNullOrEmpty()) {
            adPlacementConfigBuilder.withPlacementId(placementId)
        }
        return adPlacementConfigBuilder.build()
    }

    // endregion
}

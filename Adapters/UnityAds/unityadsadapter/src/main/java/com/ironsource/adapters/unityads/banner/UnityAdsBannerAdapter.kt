package com.ironsource.adapters.unityads.banner

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.unityads.UnityAdsAdapter
import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.ads.AdFormat
import com.unity3d.ads.BannerAd
import com.unity3d.ads.BannerConfiguration
import com.unity3d.ads.BannerSize
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner
import com.unity3d.services.banners.UnityBannerSize
import java.lang.ref.WeakReference

class UnityAdsBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<UnityAdsAdapter>(networkSettings) {

    private var bannerAdView: BannerAd? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val zoneId = adData.getString(UnityAdsConstants.ZONE_ID_KEY)
        IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.ZONE_ID.format(zoneId ?: ""))

        if (zoneId.isNullOrEmpty()) {
            val errorMessage = UnityAdsConstants.Logs.MISSING_PARAM.format(UnityAdsConstants.ZONE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val unityBannerSize = getBannerSize(bannerSize, AdapterUtils.isLargeScreen(activity.applicationContext))
        if (unityBannerSize == null) {
            IronLog.INTERNAL.error(UnityAdsConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                UnityAdsConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(activity.applicationContext, unityBannerSize.width),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )

        val mediationAdUnitId = adData.adUnitData?.get(UnityAdsConstants.AD_UNIT_ID_KEY) as? String

        val bannerConfiguration = BannerConfiguration.Builder(
            zoneId,
            BannerSize(unityBannerSize.width, unityBannerSize.height),
            UnityAdsBannerShowListener(listener)
        )
            .withMediationInfo(UnityAdsAdapter.mediationInfo)
            .apply {
                adData.serverData?.let { serverData ->
                    if (serverData.isNotEmpty()) {
                        withAdMarkup(serverData)
                    }
                }
                if (!mediationAdUnitId.isNullOrEmpty()) {
                    withMediationAdUnitId(mediationAdUnitId)
                }
            }
            .build()

        BannerAd.load(bannerConfiguration, UnityAdsBannerLoadListener(listener, layoutParams, WeakReference(this)))
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        bannerAdView = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(UnityAdsConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(UnityAdsConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        val isBannerSize = adData?.adUnitData?.get(UnityAdsConstants.BANNER_SIZE_KEY) as? ISBannerSize
        val bannerSize = isBannerSize?.let { BannerSize(it.width, it.height) }

        networkAdapter.collectBiddingData(adData, biddingDataCallback, AdFormat.BANNER, bannerSize)
    }

    // endregion

    // region Helper Methods

    internal fun setBannerAdView(ad: BannerAd) {
        bannerAdView = ad
    }

    private fun getBannerSize(bannerSize: ISBannerSize, isLargeScreen: Boolean): UnityBannerSize? {
        return when (bannerSize.description) {
            UnityAdsConstants.BANNER_SIZE_BANNER, UnityAdsConstants.BANNER_SIZE_LARGE ->
                UnityBannerSize(UnityAdsConstants.BANNER_WIDTH, UnityAdsConstants.BANNER_HEIGHT)

            UnityAdsConstants.BANNER_SIZE_RECTANGLE ->
                UnityBannerSize(UnityAdsConstants.RECTANGLE_WIDTH, UnityAdsConstants.RECTANGLE_HEIGHT)

            UnityAdsConstants.BANNER_SIZE_SMART ->
                if (isLargeScreen) {
                    UnityBannerSize(UnityAdsConstants.LEADERBOARD_WIDTH, UnityAdsConstants.LEADERBOARD_HEIGHT)
                } else {
                    UnityBannerSize(UnityAdsConstants.BANNER_WIDTH, UnityAdsConstants.BANNER_HEIGHT)
                }

            else -> null
        }
    }

    // endregion
}

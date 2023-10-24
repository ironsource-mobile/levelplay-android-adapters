package com.ironsource.adapters.vungle.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdSize
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class VungleBannerAdapter(adapter: VungleAdapter) :
    AbstractBannerAdapter<VungleAdapter>(adapter) {
    private val mBannerPlacementToListenerMap: ConcurrentHashMap<String, BannerSmashListener> =
        ConcurrentHashMap()
    private val mPlacementToBannerAd: ConcurrentHashMap<String, BannerAd> =
        ConcurrentHashMap()

    //endregion

    //region Banner API
    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()
        initBannersInternal(config, listener)
    }

    override fun initBanners(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()
        initBannersInternal(config, listener)
    }

    private fun initBannersInternal(
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        val appId = config.optString(VungleAdapter.APP_ID)

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementId))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(placementId),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }
        if (appId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appId))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appId),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }
        IronLog.ADAPTER_API.verbose("placementId = $placementId, appId = $appId")

        //add to banner listener map
        mBannerPlacementToListenerMap[placementId] = listener

        when (adapter.getInitState()) {
            VungleAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }

            VungleAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onBannerInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Vungle SDK init failed",
                        IronSourceConstants.BANNER_AD_UNIT
                    )
                )
            }

            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, appId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mBannerPlacementToListenerMap.values.forEach { bannerListener ->
            bannerListener.onBannerInitSuccess()
        }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mBannerPlacementToListenerMap.values.forEach { bannerListener ->
            bannerListener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    error,
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
        }
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        loadBannerInternal(placementId, banner, listener, serverData)
    }

    override fun loadBanner(
        config: JSONObject,
        adData: JSONObject?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        loadBannerInternal(placementId, banner, listener, null)
    }

    private fun loadBannerInternal(
        placementId: String,
        banner: IronSourceBannerLayout?,
        listener: BannerSmashListener,
        serverData: String?
    ) {
        IronLog.ADAPTER_API.verbose("placementId = $placementId")
        if (banner == null) {
            IronLog.INTERNAL.verbose("banner is null")
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }
        val bannerSize = getBannerSize(banner.size)

        // check if banner size is null or not
        if (bannerSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }
        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerSize.width),
            AdapterUtils.dpToPixels(context, bannerSize.height),
            Gravity.CENTER
        )
        val vungleBannerAdListener = VungleBannerAdListener(listener, placementId, layoutParams)

        val vungleBanner = BannerAd(
            ContextProvider.getInstance().applicationContext,
            placementId,
            bannerSize
        ).apply {

            adListener = vungleBannerAdListener
        }
        mPlacementToBannerAd[placementId] = vungleBanner
        IronLog.ADAPTER_API.verbose("bannerSize = $bannerSize")
        vungleBanner.load(serverData)
    }

    override fun destroyBanner(config: JSONObject) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        // get Vungle banner from map
        mPlacementToBannerAd[placementId]?.let { vungleBanner ->
            IronLog.ADAPTER_API.verbose(
                "destroyBanner Vungle ad, with ${VungleAdapter.PLACEMENT_ID} - $placementId"
            )

            // destroy banner
            vungleBanner.finishAd()

            // remove banner obj from the map
            mPlacementToBannerAd.remove(placementId)
        }
    }

    private fun getBannerSize(bannerSize: ISBannerSize): BannerAdSize? {
        return when (bannerSize.description) {
            "BANNER", "LARGE" -> BannerAdSize.BANNER
            "RECTANGLE" -> BannerAdSize.VUNGLE_MREC
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                    BannerAdSize.BANNER_LEADERBOARD
                } else {
                    BannerAdSize.BANNER
                })

            else -> null
        }
    }

    override fun getBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? = adapter.getBiddingData()

    //region memory handling

    override fun releaseMemory(
        adUnit: IronSource.AD_UNIT,
        config: JSONObject?
    ) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

        if (adUnit == IronSource.AD_UNIT.BANNER) {
            mPlacementToBannerAd.values.forEach{bannerAd ->
                bannerAd.finishAd()
            }
            mBannerPlacementToListenerMap.clear()
            mPlacementToBannerAd.clear()

        }
    }

    //endregion
}
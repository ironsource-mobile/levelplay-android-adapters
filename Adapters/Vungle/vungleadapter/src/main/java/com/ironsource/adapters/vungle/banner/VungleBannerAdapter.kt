package com.ironsource.adapters.vungle.banner

import com.ironsource.adapters.vungle.VungleAdapter
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
import com.vungle.ads.VungleAdSize
import com.vungle.ads.VungleBannerView
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class VungleBannerAdapter(adapter: VungleAdapter) :
    AbstractBannerAdapter<VungleAdapter>(adapter) {
    private val mBannerPlacementToListenerMap: ConcurrentHashMap<String, BannerSmashListener> =
        ConcurrentHashMap()
    private val mPlacementToBannerAd: ConcurrentHashMap<String, VungleBannerView> =
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
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        loadBannerInternal(placementId, bannerSize, listener, serverData)
    }

    override fun loadBanner(
        config: JSONObject,
        adData: JSONObject?,
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener
    ) {
        val placementId = config.optString(VungleAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $placementId")
        loadBannerInternal(placementId, bannerSize, listener, null)
    }

    private fun loadBannerInternal(
        placementId: String,
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener,
        serverData: String?
    ) {
        IronLog.ADAPTER_API.verbose("placementId = $placementId")

        if (bannerSize == null) {
            IronLog.INTERNAL.error("banner size is null")
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }
        val vungleBannerSize = getBannerSize(bannerSize)

        // check if banner size is null or not
        if (vungleBannerSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        val vungleBanner = VungleBannerView(
            ContextProvider.getInstance().applicationContext,
            placementId,
            vungleBannerSize
        ).apply {

            adListener = VungleBannerAdListener(listener, placementId, this)
        }

        mPlacementToBannerAd[placementId] = vungleBanner
        IronLog.ADAPTER_API.verbose("size = $vungleBannerSize")
        vungleBanner.load(serverData)
    }

    override fun destroyBanner(config: JSONObject) {
        postOnUIThread {
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
    }

    private fun getBannerSize(bannerSize: ISBannerSize): VungleAdSize? {
        val vungleAdSize = when (bannerSize.description) {
            "BANNER", "LARGE" -> VungleAdSize.BANNER
            "RECTANGLE" -> VungleAdSize.MREC
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                    VungleAdSize.BANNER_LEADERBOARD
                } else {
                    VungleAdSize.BANNER
                })

            "CUSTOM" -> VungleAdSize.getAdSizeWithWidthAndHeight(
                bannerSize.width,
                bannerSize.height
            )

            else -> null
        }

        return vungleAdSize
    }

    override fun collectBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }
}
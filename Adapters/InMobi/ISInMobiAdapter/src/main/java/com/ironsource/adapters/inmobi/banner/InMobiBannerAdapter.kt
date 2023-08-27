package com.ironsource.adapters.inmobi.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.inmobi.ads.InMobiBanner
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.util.concurrent.ConcurrentHashMap

class InMobiBannerAdapter (adapter: InMobiAdapter) :
    AbstractBannerAdapter<InMobiAdapter>(adapter) {
    private val placementToBannerAd: ConcurrentHashMap<String, InMobiBanner> = ConcurrentHashMap()
    private val bannerPlacementToListenerMap: ConcurrentHashMap<String, BannerSmashListener> = ConcurrentHashMap()

    override fun initBanners(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        initBannersInternal(config, listener)
    }

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose("<" + config.optString(InMobiAdapter.PLACEMENT_ID) + ">")

        initBannersInternal(config, listener)
    }

    private fun initBannersInternal(config: JSONObject, listener: BannerSmashListener) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        val accountId = config.optString(InMobiAdapter.ACCOUNT_ID)

        // verified placementId
        if (!isValidPlacementId(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.PLACEMENT_ID))

            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Missing ${InMobiAdapter.PLACEMENT_ID}",
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        // verified accountId
        if (accountId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(InMobiAdapter.ACCOUNT_ID))

            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    "Empty ${InMobiAdapter.ACCOUNT_ID}",
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        // add listener to map
        bannerPlacementToListenerMap[placementId] = listener

        // notify listener about init state
        when (InMobiAdapter.initState) {
            InMobiAdapter.InitState.INIT_STATE_SUCCESS -> {
                IronLog.ADAPTER_API.verbose(
                    "initBanners: succeeded with ${InMobiAdapter.PLACEMENT_ID} - $placementId"
                )
                // call listener init success
                listener.onBannerInitSuccess()
            }
            InMobiAdapter.InitState.INIT_STATE_ERROR -> {
                IronLog.ADAPTER_API.verbose("initBanners: failed with ${InMobiAdapter.PLACEMENT_ID} - $placementId")

                // call listener init failed
                listener.onBannerInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Init Failed",
                        IronSourceConstants.BANNER_AD_UNIT
                    )
                )
            }
            else -> {
                adapter.initSDK(ContextProvider.getInstance().applicationContext, accountId)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        if(adapter.shouldSetAgeRestrictedOnInitSuccess()){
            InMobiAdapter.ageRestrictionCollectingUserData?.let {
                adapter.setAgeRestricted(it)
            }
        }
        bannerPlacementToListenerMap.values.forEach { bannerListener -> bannerListener.onBannerInitSuccess() }
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        val message = "init failed: $error"
        bannerPlacementToListenerMap.values.forEach { bannerListener ->
            bannerListener.onBannerInitFailed(
                IronSourceError(IronSourceError.ERROR_CODE_INIT_FAILED, message)
            )
        }
    }

    override fun loadBanner(
        config: JSONObject,
        adData: JSONObject?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        loadBannerInternal(config, null, banner, listener)
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        loadBannerInternal(config, serverData, banner, listener)
    }

    private fun loadBannerInternal(
        config: JSONObject,
        serverData: String?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener,
    ) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        val dpSize = getDPSize(
            banner.size,
            AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)
        )

        // check if banner size is null or not
        if (dpSize == null) {
            IronLog.INTERNAL.error("dpSize == null")

            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize("InMobi"))
            return
        }

        // build layoutParams
        val widthPixel = AdapterUtils.dpToPixels(
            ContextProvider.getInstance().applicationContext,
            dpSize.width
        )
        val heightPixel = AdapterUtils.dpToPixels(
            ContextProvider.getInstance().applicationContext,
            dpSize.height
        )
        val layoutParams = FrameLayout.LayoutParams(widthPixel, heightPixel)
        layoutParams.gravity = Gravity.CENTER

        parseToLong(placementId)?.let { placement ->
            postOnUIThread {
                val inMobiBanner = InMobiBanner(
                    ContextProvider.getInstance().applicationContext,
                    placement
                )

                val bannerListener = InMobiBannerAdListener(listener, placementId, layoutParams)
                inMobiBanner.setListener(bannerListener)
                inMobiBanner.setBannerSize(dpSize.width, dpSize.height)

                // add InMobi Banner to map
                placementToBannerAd[placementId] = inMobiBanner
                IronLog.ADAPTER_API.verbose("loadBanner InMobi ad")

                try {
                    serverData?.let {
                        try {
                            val bytes = it.toByteArray(Charsets.UTF_8)

                            // load InMobi banner bidding
                            inMobiBanner.load(bytes)
                        } catch (e: UnsupportedEncodingException) {
                            val error = ErrorBuilder.buildLoadFailedError(
                                IronSourceConstants.BANNER_AD_UNIT,
                                "InMobi",
                                "Couldn't parse server data for ${InMobiAdapter.PLACEMENT_ID} = $placementId"
                            )
                            listener.onBannerAdLoadFailed(error)
                        }
                    } ?: run {
                        inMobiBanner.setExtras(adapter.getExtrasMap())
                        inMobiBanner.load()
                    }
                } catch (e: java.lang.Exception) {
                    val error =
                        ErrorBuilder.buildLoadFailedError(
                            "InMobiAdapter loadBanner exception "
                                    + e.message
                        )
                    // banner failed with exception
                    listener.onBannerAdLoadFailed(error)
                }
            }
        }
    }

    override fun destroyBanner(config: JSONObject) {
        val placementId = config.optString(InMobiAdapter.PLACEMENT_ID)
        IronLog.ADAPTER_API.verbose("placementId = <$placementId>")

            // get InMobi banner from map
            placementToBannerAd[placementId]?.let { inMobiBanner ->
                IronLog.ADAPTER_API.verbose(
                    "< destroyBanner InMobi ad, with ${InMobiAdapter.PLACEMENT_ID} - $placementId>"
                )
                // destroy banner
                postOnUIThread {
                    inMobiBanner.destroy()
                }

                // remove banner obj from the map
                placementToBannerAd.remove(placementId)
            }
    }

    override fun getBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? = adapter.getBiddingData()

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        config?.let { destroyBanner(it) }
        placementToBannerAd.clear()
        bannerPlacementToListenerMap.clear()
    }

    //endregion

    private fun getDPSize(banner: ISBannerSize, largeScreen: Boolean): Size? {
        when (banner.description) {
            "BANNER", "LARGE" ->
                return Size(320, 50)
            "RECTANGLE" ->
                return Size(300, 250)
            "SMART" ->
                return if (largeScreen) {
                    Size(728, 90)
                } else {
                    Size(320, 50)
                }
            "CUSTOM" ->
                return Size(banner.width, banner.height)
        }
        return null
    }

    private class Size constructor(val width: Int, val height: Int)

    private fun isValidPlacementId(placementId: String): Boolean {
        parseToLong(placementId)?.let {
            return true
        }
        return false
    }

    private fun parseToLong(placementId: String): Long? {
        var placementIdLong: Long? = null
        try {
            placementIdLong = placementId.toLong()
        } catch (e: Exception) {
            IronLog.INTERNAL.error("parseToLong threw error ${e.message}")
        }
        return placementIdLong
    }

}

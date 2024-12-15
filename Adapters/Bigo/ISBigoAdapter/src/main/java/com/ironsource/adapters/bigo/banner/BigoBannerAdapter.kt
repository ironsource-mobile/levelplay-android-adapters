package com.ironsource.adapters.bigo.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import sg.bigo.ads.api.AdSize
import sg.bigo.ads.api.BannerAd
import sg.bigo.ads.api.BannerAdLoader
import sg.bigo.ads.api.BannerAdRequest
import java.lang.ref.WeakReference

class BigoBannerAdapter(adapter: BigoAdapter) :
    AbstractBannerAdapter<BigoAdapter>(adapter) {

    private var mSmashListener : BannerSmashListener? = null
    private var mAdListener : BigoBannerAdListener? = null
    private var mAdLoader : BannerAdLoader? = null
    private var mBannerViewAd: BannerAd? = null

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val appIdKey = BigoAdapter.getAppIdKey()
        val appId= getConfigStringValueFromKey(config, appIdKey)
        if (appId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appId))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appId),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        //save banner listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            BigoAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            BigoAdapter.Companion.InitState.INIT_STATE_NONE,
            BigoAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appId)
            }
        }

    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {

        IronLog.ADAPTER_API.verbose()

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val bannerSize = getBannerSize(banner.size)
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

        val bannerAdListener = BigoBannerAdListener(
            WeakReference(this),
            listener,
            layoutParams
        )
        mAdListener = bannerAdListener

        val bannerAdLoader =
            BannerAdLoader.Builder().withAdLoadListener(mAdListener)
                .withExt(BigoAdapter.MEDIATION_INFO)
                .build()

        mAdLoader = bannerAdLoader

        val slotIdKey= BigoAdapter.getSlotIdKey()
        val slotId = getConfigStringValueFromKey(config, slotIdKey)

        val bannerAdRequest =
            BannerAdRequest.Builder()
                .withBid(serverData)
                .withSlotId(slotId)
                .withAdSizes(bannerSize).build()

        bannerAdLoader.loadAd(bannerAdRequest)
    }

    override fun destroyBanner(config: JSONObject) {
        IronLog.ADAPTER_API.verbose()
        destroyBannerViewAd()
    }

    override fun getBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? {
        return adapter.getBiddingData()
    }

    //endregion

    //region memory handling

    override fun releaseMemory(
        adUnit: IronSource.AD_UNIT,
        config: JSONObject?
    ) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        destroyBannerViewAd()
        mSmashListener = null
        mAdListener = null
    }

    //endregion

    // region Helpers

    internal fun setBannerView(ad: BannerAd) {
        ad.setAdInteractionListener(mAdListener)
        mBannerViewAd = ad
    }

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mBannerViewAd?.setAdInteractionListener(null)
            mBannerViewAd?.destroy()
            mBannerViewAd = null
            mAdLoader = null
        }
    }

    private fun getBannerSize(bannerSize: ISBannerSize?): AdSize? {
        return when (bannerSize?.description) {
            ISBannerSize.BANNER.description -> AdSize.BANNER
            ISBannerSize.RECTANGLE.description -> AdSize.MEDIUM_RECTANGLE
            ISBannerSize.SMART.description -> if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                AdSize.LARGE_BANNER
            } else {
                AdSize.BANNER
            }
            else -> null
        }
    }

    //endregion
}
package com.ironsource.adapters.ogury.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.ogury.ed.OguryBannerAdSize
import com.ogury.ed.OguryBannerAdView
import org.json.JSONObject
import java.lang.ref.WeakReference

class OguryBannerAdapter(adapter: OguryAdapter) :
    AbstractBannerAdapter<OguryAdapter>(adapter) {

    private var mSmashListener : BannerSmashListener? = null
    private var mAdListener : OguryBannerAdListener? = null
    private var mAdView: OguryBannerAdView? = null

    //region Banner API

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {

        IronLog.ADAPTER_API.verbose()

        //save banner listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            OguryAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            OguryAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onBannerInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.BANNER_AD_UNIT
                    )
                )
            }
            OguryAdapter.Companion.InitState.INIT_STATE_NONE,
            OguryAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(config)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onBannerInitSuccess()
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val bannerSize = getBannerSize(banner.size)
        if (bannerSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerSize.width),
            AdapterUtils.dpToPixels(context, bannerSize.height),
            Gravity.CENTER
        )

        val adUnitIdKey = OguryAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        mAdView = OguryBannerAdView(context)
        mAdView?.setAdSize(bannerSize)
        mAdView?.setAdUnit(adUnitId)

        val bannerAdListener = OguryBannerAdListener(
            listener,
            WeakReference(this),
            mAdView,
            layoutParams
        )

        mAdListener = bannerAdListener
        mAdView?.setListener(mAdListener)
        mAdView?.setAdMarkup(serverData)
        postOnUIThread {
            mAdView?.loadAd() ?: run {
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
            }
        }
    }

    override fun destroyBanner(config: JSONObject) {
        IronLog.ADAPTER_API.verbose()
        destroyBannerViewAd()
    }

    override fun collectBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //endregion

    //region memory handling

    override fun releaseMemory(
        adUnit: IronSource.AD_UNIT,
        config: JSONObject?
    ) {
        IronLog.INTERNAL.verbose()
        destroyBannerViewAd()
        mSmashListener = null
        mAdListener = null
    }

    //endregion

    // region Helpers

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mAdView?.destroy()
            mAdView = null
        }
    }

    internal fun setBannerView(bannerAdView: OguryBannerAdView) {
        mAdView = bannerAdView
    }

    private fun getBannerSize(bannerSize: ISBannerSize?): OguryBannerAdSize? {
        if(bannerSize == null) {
            IronLog.INTERNAL.verbose("Banner size is null")
            return null
        }

        return when (bannerSize.description) {
            ISBannerSize.BANNER.description -> OguryBannerAdSize.SMALL_BANNER_320x50
            ISBannerSize.RECTANGLE.description -> OguryBannerAdSize.MPU_300x250
            ISBannerSize.SMART.description -> if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                null
            } else {
                OguryBannerAdSize.SMALL_BANNER_320x50
            }
            else -> null
        }
    }

    //endregion
}

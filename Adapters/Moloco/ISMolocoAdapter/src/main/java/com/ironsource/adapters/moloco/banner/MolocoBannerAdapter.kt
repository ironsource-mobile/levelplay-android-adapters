package com.ironsource.adapters.moloco.banner

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import org.json.JSONObject

class MolocoBannerAdapter(adapter: MolocoAdapter) :
    AbstractBannerAdapter<MolocoAdapter>(adapter) {

    private var mListener : BannerSmashListener? = null
    private var mAdLoadListener : MolocoBannerAdLoadListener? = null
    private var mAdShowListener : MolocoBannerAdShowListener? = null
    private var mAdView: Banner? = null

    //region Banner API

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val adUnitIdKey = MolocoAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        val appKey = getConfigStringValueFromKey(config, MolocoAdapter.getAppKey())

        if (adUnitId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitId))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitId),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        if (appKey.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appKey))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appKey),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId, appKey = $appKey")

        //save banner listener
        mListener = listener

        when (adapter.getInitState()) {
            MolocoAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            MolocoAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onBannerInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Moloco sdk init failed",
                        IronSourceConstants.BANNER_AD_UNIT
                    )
                )

            }
            MolocoAdapter.Companion.InitState.INIT_STATE_NONE,
            MolocoAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appKey)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mListener?.onBannerInitSuccess()
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mListener?.onBannerInitFailed(
            ErrorBuilder.buildInitFailedError(
                error,
                IronSourceConstants.BANNER_AD_UNIT
            )
        )
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (banner == null) {
            IronLog.INTERNAL.verbose("banner is null")
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
        val layoutParams = createBannerLayoutParams(context,banner.size)

        val adUnitIdKey = MolocoAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        createBannerWithSize(banner.size, adUnitId, listener, layoutParams, serverData)
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
        mAdLoadListener = null
        mAdShowListener = null
        mListener = null
    }

    //endregion

    // region Helpers

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mAdView?.destroy()
            mAdView = null
        }
    }

    private fun createBannerLayoutParams(context : Context, size: ISBannerSize): FrameLayout.LayoutParams {
        var layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(0, 0)
        when (size.description) {
            "BANNER" -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, 320),
                AdapterUtils.dpToPixels(context, 50)
            )
            "LEADERBOARD" -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, 728),
                AdapterUtils.dpToPixels(context, 90)
            )
            "RECTANGLE" -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, 300),
                AdapterUtils.dpToPixels(context, 250)
            )
            "SMART" -> {
                layoutParams = if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, 728),
                        AdapterUtils.dpToPixels(context, 90)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, 320),
                        AdapterUtils.dpToPixels(context, 50)
                    )
                }
            }
        }
        // set gravity
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    private fun handleBannerCreation(
        adView: Banner?,
        listener: BannerSmashListener,
        layoutParams: FrameLayout.LayoutParams,
        serverData: String
    ) {
        adView?.let {ad ->
            mAdView = ad
            mAdLoadListener = MolocoBannerAdLoadListener(listener, layoutParams, ad)
            mAdShowListener = MolocoBannerAdShowListener(listener)
            mAdView?.apply {
                adShowListener = mAdShowListener
                load(serverData, mAdLoadListener)
            }
        } ?: run {
            listener.onBannerAdLoadFailed(
                ErrorBuilder.buildLoadFailedError(MolocoAdapter.INVALID_CONFIGURATION)
            )
        }
    }

    private fun createBannerWithSize(
        size: ISBannerSize,
        adUnitId: String,
        listener: BannerSmashListener,
        layoutParams: FrameLayout.LayoutParams,
        serverData: String,
        createCallback: (Banner?, MolocoAdError.AdCreateError?) -> Unit = { adView, error ->
            if (error != null) {
                val bannerError: IronSourceError = ErrorBuilder.buildShowFailedError(
                    error.errorCode.toString(),
                    error.description
                )
                listener.onBannerAdLoadFailed(bannerError)
            } else {
                handleBannerCreation(adView, listener, layoutParams, serverData)
            }
        }
    ) {
        when (size.description) {
            "BANNER" -> Moloco.createBanner(adUnitId, null, createCallback)
            "LEADERBOARD" -> Moloco.createBannerTablet(adUnitId, null, createCallback)
            "RECTANGLE" -> Moloco.createMREC(adUnitId, null, createCallback)
            "SMART" -> if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                Moloco.createBannerTablet(adUnitId, null, createCallback)
            } else {
                Moloco.createBanner(adUnitId, null, createCallback)
            }
            else -> Moloco.createBanner(adUnitId, null, createCallback)
        }
    }

    //endregion

}
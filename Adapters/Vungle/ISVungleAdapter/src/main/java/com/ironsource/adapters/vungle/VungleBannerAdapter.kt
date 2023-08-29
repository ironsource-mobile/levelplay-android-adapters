package com.ironsource.adapters.vungle

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdListener
import com.vungle.ads.BannerAdSize
import com.vungle.ads.BaseAd
import com.vungle.ads.VungleError

internal class VungleBannerAdapter(
    placementId: String,
    private val mISBannerSize: ISBannerSize,
    loBannerSize: BannerAdSize,
    private val listener: BannerSmashListener?
) : BannerAdListener {
    private var mBannerAd: BannerAd?

    init {
        mBannerAd = BannerAd(
            ContextProvider.getInstance().applicationContext,
            placementId,
            loBannerSize
        )
        mBannerAd?.adListener = this
    }

    fun loadWithBid(serverData: String?) {
        mBannerAd?.load(serverData)
    }

    fun destroy() {
        if (mBannerAd != null) {
            mBannerAd?.finishAd()
            mBannerAd = null
        }
    }

    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.placementId)
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        if (mISBannerSize?.description == null) {
            IronLog.INTERNAL.verbose("banner size is null")
            return
        }
        if (mBannerAd?.canPlayAd() == false) {
            IronLog.ADAPTER_CALLBACK.error("can't play ad")
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("can't play ad"))
            return
        }
        val bannerView = mBannerAd?.getBannerView()
        if (bannerView != null) {
            val params = getBannerLayoutParams(mISBannerSize)
            if (params == null) {
                IronLog.ADAPTER_CALLBACK.error("IS banner size is null")
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Vungle LoadBanner failed - IS banner size is null"))
                return
            }
            listener.onBannerAdLoaded(bannerView, params)
        } else {
            IronLog.ADAPTER_CALLBACK.error("banner view is null")
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Vungle LoadBanner failed - banner view is null"))
        }
    }

    override fun onAdStart(baseAd: BaseAd) {
        // no-op
    }

    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onBannerAdShown()
    }

    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onBannerAdClicked()
    }

    override fun onAdEnd(baseAd: BaseAd) {
        // no-op
    }

    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = ${baseAd.placementId}, error = $adError")
        // no-op
    }

    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToLoad placementId = ${baseAd.placementId}, error = $adError")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        val error = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adError.errorMessage)
        } else {
            ErrorBuilder.buildLoadFailedError(adError.errorMessage)
        }
        listener.onBannerAdLoadFailed(error)
    }

    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onBannerAdLeftApplication()
    }

    private fun getBannerLayoutParams(size: ISBannerSize?): FrameLayout.LayoutParams? {
        if (size == null || size.description == null) {
            return null
        }
        var layoutParams = FrameLayout.LayoutParams(0, 0)
        val activity = ContextProvider.getInstance().currentActiveActivity
        when (size.description) {
            "BANNER", "LARGE" -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(activity, 320),
                AdapterUtils.dpToPixels(activity, 50)
            )

            "RECTANGLE" -> layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(activity, 300),
                AdapterUtils.dpToPixels(activity, 250)
            )

            "SMART" -> layoutParams = if (AdapterUtils.isLargeScreen(activity)) {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(activity, 728),
                    AdapterUtils.dpToPixels(activity, 90)
                )
            } else {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(activity, 320),
                    AdapterUtils.dpToPixels(activity, 50)
                )
            }
        }

        // set gravity
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    companion object {
        @JvmStatic
        fun getBannerSize(size: ISBannerSize?): BannerAdSize? {
            if (size == null || size.description == null) {
                return null
            }
            when (size.description) {
                "BANNER", "LARGE" -> return BannerAdSize.BANNER
                "RECTANGLE" -> return BannerAdSize.VUNGLE_MREC
                "SMART" -> return if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().currentActiveActivity)) BannerAdSize.BANNER_LEADERBOARD else BannerAdSize.BANNER
            }
            return null
        }
    }
}

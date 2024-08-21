package com.ironsource.adapters.vungle.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.vungle.ads.BannerAdListener
import com.vungle.ads.BaseAd
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleError

class VungleBannerAdListener(
    private val mListener: BannerSmashListener,
    private val bannerView: VungleBannerView
) : BannerAdListener {

    /**
     * Called to indicate that an ad was loaded and it can now be shown.
     *
     * @param baseAd Represents the [VungleBannerView] ad which was loaded
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.placementId + " adview size=${bannerView.getAdViewSize()}")

        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerView.getAdViewSize().width),
            AdapterUtils.dpToPixels(context, bannerView.getAdViewSize().height),
            Gravity.CENTER
        )
        mListener.onBannerAdLoaded(bannerView, layoutParams)
    }

    /**
     * Called when Ad failed to load
     *
     * @param baseAd    - BannerView instance
     * @param adError - BMError with additional info about error
     */
    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${adError.code}, errorMessage = ${adError.message}")
        val adapterError = "${adError.errorMessage}( ${adError.code} )"
        val error = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError)
        } else {
            ErrorBuilder.buildLoadFailedError(adapterError)
        }
        mListener.onBannerAdLoadFailed(error)
    }

    /**
     * Called to indicate that an ad started.
     *
     * @param baseAd - Banner instance
     */
    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
    }

    /**
     * Called when Ad Impression has been tracked
     *
     * @param baseAd - BannerView instance
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("${VungleAdapter.PLACEMENT_ID} = ${baseAd.placementId}")
        mListener.onBannerAdShown()
    }

    /**
     * Called when Ad Play failed
     *
     * @param baseAd    - BannerView instance
     * @param adError - BMError with additional info about error
     */
    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${adError.code}, errorMessage = ${adError.errorMessage}")
    }

    /**
     * Called when Ad has been clicked
     *
     * @param baseAd - BannerView instance
     */
    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        mListener.onBannerAdClicked()
    }

    /**
     * Called to indicate that the ad was ended and closed.
     *
     * @param baseAd - Banner instance
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
    }

    /**
     * Called to indicate that the user may leave the application on account of interacting with the ad.
     *
     * @param baseAd Represents the [VungleBannerView] ad
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        mListener.onBannerAdLeftApplication()
    }

}
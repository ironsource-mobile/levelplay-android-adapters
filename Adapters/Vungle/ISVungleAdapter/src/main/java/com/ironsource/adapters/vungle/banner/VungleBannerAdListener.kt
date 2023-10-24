package com.ironsource.adapters.vungle.banner

import android.widget.FrameLayout
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.vungle.ads.BannerAd
import com.vungle.ads.BannerAdListener
import com.vungle.ads.BaseAd
import com.vungle.ads.VungleError

class VungleBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mPlacementId: String,
    private val mLayoutParams: FrameLayout.LayoutParams,
) : BannerAdListener {

    /**
     * Called to indicate that an ad was loaded and it can now be shown.
     *
     * @param baseAd Represents the [VungleBannerAd] ad which was loaded
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + baseAd.placementId)
        (baseAd as? BannerAd)?.getBannerView()?.let { bannerView ->
            mListener.onBannerAdLoaded(bannerView, mLayoutParams)
        } ?: run {
            IronLog.ADAPTER_CALLBACK.error("banner view is null")
            mListener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Vungle LoadBanner failed - banner view is null"))
        }
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
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
    }

    /**
     * Called when Ad Impression has been tracked
     *
     * @param baseAd - BannerView instance
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("${VungleAdapter.PLACEMENT_ID} = $mPlacementId")
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
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onBannerAdClicked()
    }

    /**
     * Called to indicate that the ad was ended and closed.
     *
     * @param baseAd - Banner instance
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
    }

    /**
     * Called to indicate that the user may leave the application on account of interacting with the ad.
     *
     * @param baseAd Represents the [VungleBannerAd] ad
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onBannerAdLeftApplication()
    }

}
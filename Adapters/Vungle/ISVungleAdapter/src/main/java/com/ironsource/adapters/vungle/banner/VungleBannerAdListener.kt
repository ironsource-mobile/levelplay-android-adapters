package com.ironsource.adapters.vungle.banner

import android.view.Gravity
import android.widget.FrameLayout
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
    private val mPlacementId: String,
    private val bannerView: VungleBannerView
) : BannerAdListener {

    /**
     * Callback used to notify that the advertisement assets have been downloaded and are ready to play.
     *
     * @param baseAd - identifier for which the advertisement assets have been downloaded.
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerView.getAdViewSize().width),
            AdapterUtils.dpToPixels(context, bannerView.getAdViewSize().height),
            Gravity.CENTER
        )

        mListener.onBannerAdLoaded(bannerView, layoutParams)
    }

    /**
     * Callback used to notify that an error has occurred while downloading assets. This indicates an unrecoverable error within the SDK, such as lack of network or out of disk space on the device.
     *
     * @param baseAd  - identifier for which the advertisement for which the error occurred.
     * @param adError - Error message and event suggesting the cause of failure
     */
    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId, errorCode = ${adError.code}, errorMessage = ${adError.message}")
        val adapterError = "${adError.errorMessage}( ${adError.code} )"

        val error = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError)
        } else {
            ErrorBuilder.buildLoadFailedError(adapterError)
        }

        mListener.onBannerAdLoadFailed(error)
    }

    /**
     * Called when the Vungle SDK has successfully launched the advertisement and an advertisement will begin playing momentarily.
     *
     * @param baseAd - identifier for which the advertisement being played.
     */
    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
    }

    /**
     * Called when the Vungle SDK has shown an Ad for the play/render request For Vungle adImpression should be same as adStart as we show every Ad only once, hence a unique Ad can never generate multiple impressions
     *
     * @param baseAd - identifier for which the advertisement being played.
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onBannerAdShown()
    }

    /**
     * Callback used to notify that an error has occurred while playing advertisement. This indicates an unrecoverable error within the SDK, such as lack of network or out of disk space on the device.
     *
     * @param baseAd  - identifier for which the advertisement for which the error occurred.
     * @param adError - Error message and event suggesting the cause of failure
     */
    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId, errorCode = ${adError.code}, errorMessage = ${adError.errorMessage}")
    }

    /**
     * Callback for an advertisement tapped. Sent when the user has tapped on an ad.
     *
     * @param baseAd - identifier for which the advertisement that was clicked.
     */
    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onBannerAdClicked()
    }

    /**
     * Callback when the user has left the app.
     *
     * @param baseAd - identifier for which the advertisement that user clicked resulting in leaving app
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onBannerAdLeftApplication()
    }

    /**
     * Callback for an advertisement ending. The Vungle SDK has finished playing the advertisement and the user has closed the advertisement.
     *
     * @param baseAd - identifier for which the advertisement that ended.
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
    }

}
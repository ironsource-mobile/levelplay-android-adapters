package com.ironsource.adapters.vungle.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.adapters.vungle.VungleConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.vungle.ads.BaseAd
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleError

class VungleBannerListener(
    private val listener: BannerAdListener,
    private val bannerView: VungleBannerView
) : com.vungle.ads.BannerAdListener {

    /**
     * Called when the advertisement assets have been downloaded and are ready to play.
     * @param baseAd - identifier for which the advertisement assets have been downloaded.
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        val context = bannerView.context
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerView.getAdViewSize().width),
            AdapterUtils.dpToPixels(context, bannerView.getAdViewSize().height),
            Gravity.CENTER
        )
        val creativeId = baseAd.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(VungleConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess(bannerView, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(VungleConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(bannerView, layoutParams, extraData)
        }
    }

    /**
     * Called when an error has occurred while downloading assets.
     * @param baseAd - identifier for which the advertisement for which the error occurred.
     * @param adError - error details suggesting the cause of failure.
     */
    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.error(VungleConstants.Logs.FAILED_TO_LOAD.format(adError.code, adError.errorMessage))
        listener.onAdLoadFailed(VungleAdapter.getLoadErrorType(adError), adError.code, adError.errorMessage)
    }

    /**
     * Called when the Vungle SDK has launched the advertisement and it will begin playing momentarily.
     * @param baseAd - identifier for which the advertisement being played.
     */
    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the Vungle SDK has shown an ad for the play/render request.
     * @param baseAd - identifier for which the advertisement being played.
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when an error has occurred while playing the advertisement.
     * @param baseAd - identifier for which the advertisement for which the error occurred.
     * @param adError - error details suggesting the cause of failure.
     */
    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.error(VungleConstants.Logs.FAILED_TO_PLAY.format(adError.code, adError.errorMessage))
    }

    /**
     * Called when the user has tapped on an ad.
     * @param baseAd - identifier for which the advertisement that was clicked.
     */
    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user has left the app.
     * @param baseAd - identifier for which the advertisement that resulted in leaving the app.
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Called when the advertisement has finished playing and the user has closed it.
     * @param baseAd - identifier for which the advertisement that ended.
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

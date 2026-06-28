package com.ironsource.adapters.pubmatic.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticConstants
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.banner.POBBannerView

class PubMaticBannerListener(
    private val listener: BannerAdListener,
    private val bannerAdView: POBBannerView
) : POBBannerView.POBBannerViewListener() {

    /**
     * Notifies that a banner ad has been successfully loaded and rendered.
     * @param bannerAd Banner ad instance.
     */
    override fun onAdReceived(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        val size = bannerAd.creativeSize
        if (size == null) {
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                PubMaticConstants.Logs.CREATIVE_SIZE_UNAVAILABLE
            )
            return
        }

        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, size.adWidth),
            AdapterUtils.dpToPixels(context, size.adHeight),
            Gravity.CENTER
        )
        listener.onAdLoadSuccess(bannerAdView, layoutParams)
    }

    /**
     * Notifies an error encountered while loading or rendering an ad.
     * @param bannerAd Banner ad instance.
     * @param error PubMatic ad error.
     */
    override fun onAdFailed(bannerAd: POBBannerView, error: POBError) {
        if (error.errorCode != POBError.RENDER_ERROR) {
            IronLog.ADAPTER_CALLBACK.error(
                PubMaticConstants.Logs.LOAD_FAILED.format(error.errorCode, error.errorMessage)
            )
            listener.onAdLoadFailed(PubMaticAdapter.getLoadError(error), error.errorCode, error.errorMessage)
        } else {
            IronLog.ADAPTER_CALLBACK.verbose(
                PubMaticConstants.Logs.SHOW_FAILED.format(error.errorCode, error.errorMessage)
            )
        }
    }

    /**
     * Notifies that an impression event occurred.
     * @param bannerAd Banner ad instance.
     */
    override fun onAdImpression(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Notifies an ad click.
     * @param bannerAd Banner ad instance.
     */
    override fun onAdClicked(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Notifies that the current app goes to the background due to a user click.
     * @param bannerAd Banner ad instance.
     */
    override fun onAppLeaving(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Notifies that the banner ad will launch a dialog on top of the current view.
     * @param bannerAd Banner ad instance.
     */
    override fun onAdOpened(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    /**
     * Notifies that the banner ad has dismissed the modal on top of the current view.
     * @param bannerAd Banner ad instance.
     */
    override fun onAdClosed(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }
}

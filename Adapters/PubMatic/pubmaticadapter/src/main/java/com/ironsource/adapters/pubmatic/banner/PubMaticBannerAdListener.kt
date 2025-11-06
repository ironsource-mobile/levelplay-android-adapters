package com.ironsource.adapters.pubmatic.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import java.lang.ref.WeakReference

class PubMaticBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mAdapter: WeakReference<PubMaticBannerAdapter>,
    private val mAdUnitId: String,
    private val mAdView: POBBannerView?,
) :   POBBannerView.POBBannerViewListener() {

    /**
     * Callback method Notifies that a banner ad has been successfully loaded and rendered.
     * @param bannerAd - Banner ad instance.
     */
    override fun onAdReceived(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        bannerAd.creativeSize?.let { size ->
            mAdView?.let { mAdapter.get()?.setBannerView(it) }
            val context = ContextProvider.getInstance().applicationContext
            val layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, size.adWidth),
                AdapterUtils.dpToPixels(context, size.adHeight),
                Gravity.CENTER
            )
            mListener.onBannerAdLoaded(mAdView, layoutParams)
        } ?: run {
            mListener.onBannerAdLoadFailed(
                IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Creative size is unavailable")
            )
        }
    }

    /**
     * Callback method Notifies an error encountered while loading or rendering an ad.
     * @param bannerAd - Banner ad instance.
     * @param error - PubMatic Ad Error.
     */
    override fun onAdFailed(bannerAd: POBBannerView, error: POBError) {
        if (error.errorCode != POBError.RENDER_ERROR) {
            IronLog.ADAPTER_CALLBACK.verbose("Load failure - adUnitId = $mAdUnitId, errorCode = ${error.errorCode}, errorMessage = ${error.errorMessage}")
            mListener.onBannerAdLoadFailed(
                PubMaticAdapter.getLoadErrorAndCheckNoFill(
                    error,
                    IronSourceError.ERROR_BN_LOAD_NO_FILL
                )
            )
        } else {
            IronLog.ADAPTER_CALLBACK.verbose("Show failure - adUnitId = $mAdUnitId, errorCode = ${error.errorCode}, errorMessage = ${error.errorMessage}")
        }
    }

    /**
     * Callback method Notifies that an impression event occurred.
     *  @param bannerAd - Banner ad instance.
     */
    override fun onAdImpression(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onBannerAdShown()
    }

    /**
     * Callback method notifies ad click
     *  @param bannerAd - Banner ad instance.
     */
    override fun onAdClicked(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onBannerAdClicked()
    }

    /**
     * Callback method Notifies whenever current app goes in the background due to user click.
     * @param bannerAd - Banner ad instance.
     */
    override fun onAppLeaving(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onBannerAdLeftApplication()
    }

    /**
     * Callback method Notifies that the banner ad will launch a dialog on top of the current view.
     * @param bannerAd - Banner ad instance.
     */
    override fun onAdOpened(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onBannerAdScreenPresented()
    }

    /**
     * Callback method Notifies that the banner ad has dismissed the modal on top of the current view
     *  @param bannerAd - Banner ad instance.
     */
    override fun onAdClosed(bannerAd: POBBannerView) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = $mAdUnitId")
        mListener.onBannerAdScreenDismissed()
    }
}
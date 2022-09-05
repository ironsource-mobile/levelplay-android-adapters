package com.ironsource.adapters.yahoo

import android.widget.FrameLayout
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.yahoo.ads.ErrorInfo
import com.yahoo.ads.inlineplacement.InlineAdView
import java.lang.ref.WeakReference

class YahooBannerAdListener (private val mListener: BannerSmashListener?,
                             private val mAdapter: WeakReference<YahooAdapter>?,
                             private val mPlacementId: String,
                             private val mLayoutParams: FrameLayout.LayoutParams) : InlineAdView.InlineAdListener {

    companion object {
        private const val BANNER_AD_IMPRESSION_EVENT : String = "adImpression"
    }

    // Called when the banner ad request is successfully filled. The banner argument is a View.
    override fun onLoaded(inlineAdView: InlineAdView?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")

        inlineAdView?.let {
            mAdapter?.get()?.setBannerView(mPlacementId, inlineAdView)
            mListener?.onBannerAdLoaded(inlineAdView, mLayoutParams)
        }
    }

    // Called when an error occurs during the InlineAdView lifecycle.
    override fun onLoadFailed(inlineAdView: InlineAdView?, errorInfo: ErrorInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId, error = $errorInfo")
        val bannerError = mAdapter?.get()?.getLoadErrorAndCheckNoFill(errorInfo, IronSourceError.ERROR_BN_LOAD_NO_FILL)
        mListener?.onBannerAdLoadFailed(bannerError)
    }

    // This callback is used to surface additional events to the publisher from the SDK.
    override fun onEvent(inlineAdView: InlineAdView?, eventId: String?, source: String?, arguments: MutableMap<String, Any>?) {
        if (source == BANNER_AD_IMPRESSION_EVENT) {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
            mListener?.onBannerAdShown()
        }
    }

    // Called when an error occurs during the InlineAdView lifecycle.
    override fun onError(inlineAdView: InlineAdView?, errorInfo: ErrorInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId, error = $errorInfo")
    }

    // Called when the InlineAdView has been clicked.
    override fun onClicked(inlineAdView: InlineAdView?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener?.onBannerAdClicked()
    }

    // Called when the InlineAdView causes the user to leave the application.
    override fun onAdLeftApplication(inlineAdView: InlineAdView?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener?.onBannerAdLeftApplication()
    }

    // Called when the InlineAdView has been expanded.
    override fun onExpanded(inlineAdView: InlineAdView?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener?.onBannerAdScreenPresented()
    }

    // Called when the InlineAdView has been collapsed.
    override fun onCollapsed(inlineAdView: InlineAdView?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener?.onBannerAdScreenDismissed()
    }

    // Called when the InlineAdView has been refreshed.
    override fun onAdRefreshed(inlineAdView: InlineAdView?) {
    }

    // Called when the InlineAdView has been resized.
    override fun onResized(inlineAdView: InlineAdView?) {
    }
}
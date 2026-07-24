package com.ironsource.adapters.fyber.banner

import android.content.Context
import android.widget.FrameLayout
import com.fyber.inneractive.sdk.external.ImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerWithImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.ironsource.adapters.fyber.FyberAdapter
import com.ironsource.adapters.fyber.FyberConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog

class FyberBannerListener(
    private val listener: BannerAdListener,
    private val adViewController: InneractiveAdViewUnitController,
    private val layoutParams: FrameLayout.LayoutParams,
    private val context: Context
) : InneractiveAdSpot.RequestListener,
    InneractiveAdViewEventsListenerWithImpressionData {

    /**
     * Called by Inneractive when a banner is ready for display.
     * @param adSpot the spot object for which the ad was loaded.
     */
    override fun onInneractiveSuccessfulAdRequest(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()

        if (!adSpot.isReady) {
            IronLog.INTERNAL.error(FyberConstants.Logs.SPOT_NOT_READY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                FyberConstants.Logs.SPOT_NOT_READY
            )
            return
        }

        val bannerLayout = FrameLayout(context)
        adViewController.bindView(bannerLayout)
        listener.onAdLoadSuccess(bannerLayout, layoutParams)
    }

    /**
     * Called by Inneractive when a banner ad fails loading.
     * @param adSpot the spot object for which the request failed.
     * @param errorCode the failure's error code.
     */
    override fun onInneractiveFailedAdRequest(adSpot: InneractiveAdSpot, errorCode: InneractiveErrorCode?) {
        val errorMessage = errorCode?.toString() ?: FyberConstants.Logs.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(FyberConstants.Logs.FAILED_TO_LOAD.format(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage))
        listener.onAdLoadFailed(FyberAdapter.getLoadErrorType(errorCode), AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
    }

    /**
     * Called by Inneractive when a banner ad activity is shown.
     * @param adSpot the spot object that was shown.
     */
    override fun onAdImpression(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when a banner ad activity is shown, with impression data.
     * @param adSpot the spot object that was shown.
     * @param impressionData the impression data of the shown ad.
     */
    override fun onAdImpression(adSpot: InneractiveAdSpot, impressionData: ImpressionData?) {
        val creativeId = impressionData?.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(FyberConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdOpened()
        } else {
            val extraData: Map<String, Any> = mapOf(FyberConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdOpened(extraData)
        }
    }

    /**
     * Called by Inneractive when a banner ad is clicked.
     * @param adSpot the spot object that was clicked.
     */
    override fun onAdClicked(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called by Inneractive when Inneractive's internal browser, opened by this banner, was closed.
     * @param adSpot the spot object whose internal browser was closed.
     */
    override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }

    /**
     * Called by Inneractive when a banner ad opened an external application.
     * @param adSpot the spot object that opened the external application.
     */
    override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Called by Inneractive when a banner ad encountered an error while trying to show.
     * @param adSpot the spot object that encountered the error.
     * @param adDisplayError the error details suggesting the cause of failure.
     */
    override fun onAdEnteredErrorState(adSpot: InneractiveAdSpot, adDisplayError: InneractiveUnitController.AdDisplayError?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when a banner ad activity is expanded.
     * @param adSpot the spot object that was expanded.
     */
    override fun onAdExpanded(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when a banner ad activity is resized.
     * @param adSpot the spot object that was resized.
     */
    override fun onAdResized(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when a banner ad activity is collapsed.
     * @param adSpot the spot object that was collapsed.
     */
    override fun onAdCollapsed(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

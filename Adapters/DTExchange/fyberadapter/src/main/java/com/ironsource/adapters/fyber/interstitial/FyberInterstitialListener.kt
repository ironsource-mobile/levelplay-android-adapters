package com.ironsource.adapters.fyber.interstitial

import com.fyber.inneractive.sdk.external.ImpressionData
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerWithImpressionData
import com.fyber.inneractive.sdk.external.InneractiveUnitController
import com.ironsource.adapters.fyber.FyberAdapter
import com.ironsource.adapters.fyber.FyberConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog

class FyberInterstitialListener(
    private val listener: InterstitialAdListener
) : InneractiveAdSpot.RequestListener,
    InneractiveFullscreenAdEventsListenerWithImpressionData {

    /**
     * Called by Inneractive when an interstitial ad is ready for display.
     * @param adSpot the spot object for which the ad was loaded.
     */
    override fun onInneractiveSuccessfulAdRequest(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called by Inneractive when an interstitial ad fails loading.
     * @param adSpot the spot object for which the request failed.
     * @param errorCode the failure's error code.
     */
    override fun onInneractiveFailedAdRequest(adSpot: InneractiveAdSpot, errorCode: InneractiveErrorCode?) {
        val errorMessage = errorCode?.toString() ?: FyberConstants.Logs.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(FyberConstants.Logs.FAILED_TO_LOAD.format(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage))
        listener.onAdLoadFailed(FyberAdapter.getLoadErrorType(errorCode), AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
    }

    /**
     * Called by Inneractive when an interstitial ad activity is shown.
     * @param adSpot the spot object that was shown.
     */
    override fun onAdImpression(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when an interstitial ad activity is shown, with impression data.
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
     * Called by Inneractive when an interstitial ad is clicked.
     * @param adSpot the spot object that was clicked.
     */
    override fun onAdClicked(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called by Inneractive when an interstitial ad opened an external application.
     * @param adSpot the spot object that opened the external application.
     */
    override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when an interstitial ad encountered an error while trying to show.
     * @param adSpot the spot object that failed to show.
     * @param adDisplayError the error details suggesting the cause of failure.
     */
    override fun onAdEnteredErrorState(adSpot: InneractiveAdSpot, adDisplayError: InneractiveUnitController.AdDisplayError?) {
        val errorMessage = adDisplayError?.message ?: FyberConstants.Logs.UNKNOWN_ERROR
        IronLog.ADAPTER_CALLBACK.error(FyberConstants.Logs.SHOW_FAILED.format(errorMessage))
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
    }

    /**
     * Called by Inneractive when Inneractive's internal browser was closed.
     * @param adSpot the spot object whose internal browser was closed.
     */
    override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by Inneractive when an interstitial ad activity is closed.
     * @param adSpot the spot object that was closed.
     */
    override fun onAdDismissed(adSpot: InneractiveAdSpot) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}

package com.ironsource.adapters.bidmachine.banner

import android.widget.FrameLayout
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerView
import io.bidmachine.utils.BMError

class BidMachineBannerListener(
    private val listener: BannerAdListener,
    private val layoutParams: FrameLayout.LayoutParams
) : BannerListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     * @param ad - BannerView instance
     */
    override fun onAdLoaded(ad: BannerView) {
        val creativeId = ad.auctionResult?.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(BidMachineConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess(ad, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(BidMachineConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(ad, layoutParams, extraData)
        }
    }

    /**
     * Called when Ad failed to load
     * @param ad - BannerView instance
     * @param error - BMError with additional info about error
     */
    override fun onAdLoadFailed(ad: BannerView, error: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_LOAD.format(error.code, error.message))
        val errorType = BidMachineAdapter.getLoadErrorType(error)
        listener.onAdLoadFailed(errorType, error.code, error.message)
    }

    /**
     * Called when Ad Impression has been tracked
     * @param ad - BannerView instance
     */
    override fun onAdImpression(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when Ad show failed
     * @param ad - BannerView instance
     * @param error - BMError with additional info about error
     */
    override fun onAdShowFailed(ad: BannerView, error: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_SHOW.format(error.code, error.message))
    }

    /**
     * Called when Ad has been clicked
     * @param ad - BannerView instance
     */
    override fun onAdClicked(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when Ad expired
     * @param ad - BannerView instance
     */
    override fun onAdExpired(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

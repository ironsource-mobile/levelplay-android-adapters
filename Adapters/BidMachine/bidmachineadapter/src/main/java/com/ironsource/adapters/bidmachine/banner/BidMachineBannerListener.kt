package com.ironsource.adapters.bidmachine.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.format.bannerad.BannerListener
import io.bidmachine.format.bannerad.BannerView

class BidMachineBannerListener(
    private val listener: BannerAdListener
) : AdLoadCallback<BannerView>, BannerListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     * @param ad - BannerView instance
     */
    override fun onAdLoaded(ad: BannerView) {
        val adSize = ad.getAdSize()
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(ad.context, adSize.width),
            AdapterUtils.dpToPixels(ad.context, adSize.height),
            Gravity.CENTER
        )
        val creativeId = (ad as Ad).getAuctionInfo().creativeId
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
     * @param bmError - BMError with additional info about error
     */
    override fun onAdLoadFailed(bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_LOAD.format(bmError.code, bmError.message))
        val errorType = BidMachineAdapter.getLoadErrorType(bmError)
        listener.onAdLoadFailed(errorType, bmError.code, bmError.message)
    }

    override fun onAdPresented(ad: BannerView) {
        // Keep reporting onAdOpened when the impression is tracked.
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
     * @param bmError - BMError with additional info about error
     */
    override fun onAdShowFailed(ad: BannerView, bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_SHOW.format(bmError.code, bmError.message))
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

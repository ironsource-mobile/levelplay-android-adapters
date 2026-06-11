package com.ironsource.adapters.inmobi.banner

import android.widget.FrameLayout
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.listeners.BannerAdEventListener
import com.ironsource.adapters.inmobi.InMobiConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog

internal class InMobiBannerListener(
    private val listener: BannerAdListener,
    private val layoutParams: FrameLayout.LayoutParams
) : BannerAdEventListener() {

    /**
     * Called to notify that an ad was successfully loaded
     */
    override fun onAdLoadSucceeded(
        inMobiBanner: InMobiBanner,
        adMetaInfo: AdMetaInfo
    ) {
        val creativeId = adMetaInfo.creativeID
        IronLog.ADAPTER_CALLBACK.verbose(InMobiConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess(inMobiBanner, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(InMobiConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(inMobiBanner, layoutParams, extraData)
        }
    }

    /**
     * Called to notify that a request to load an ad failed
     */
    override fun onAdLoadFailed(
        inMobiBanner: InMobiBanner,
        inMobiAdRequestStatus: InMobiAdRequestStatus
    ) {
        val errorMessage = inMobiAdRequestStatus.message ?: inMobiAdRequestStatus.statusCode.toString()

        val adapterErrorType = if (inMobiAdRequestStatus.statusCode == InMobiAdRequestStatus.StatusCode.NO_FILL) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }

        IronLog.ADAPTER_CALLBACK.error(InMobiConstants.Logs.FAILED_TO_LOAD.format(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage))
        listener.onAdLoadFailed(adapterErrorType, AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
    }

    /**
     * Called to notify that an ad was successfully shown (impression)
     */
    override fun onAdImpression(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called to notify that the user interacted with the ad (clicked)
     */
    override fun onAdClicked(
        inMobiBanner: InMobiBanner,
        params: Map<Any, Any>?
    ) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called to notify that the user is about to leave the application
     */
    override fun onUserLeftApplication(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Called to notify that the banner ad was displayed (full-screen overlay presented)
     */
    override fun onAdDisplayed(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    /**
     * Called to notify that the User is about to return to the application after closing the ad (full-screen overlay dismissed)
     */
    override fun onAdDismissed(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }
}

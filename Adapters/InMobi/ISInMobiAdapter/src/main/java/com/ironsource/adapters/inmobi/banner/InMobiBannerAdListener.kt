package com.ironsource.adapters.inmobi.banner

import android.widget.FrameLayout
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.listeners.BannerAdEventListener
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder

internal class InMobiBannerAdListener(
    private val listener: BannerSmashListener,
    private val placementId: String,
    private val layoutParams: FrameLayout.LayoutParams
) : BannerAdEventListener() {

    /**
     * Called to notify that an ad was successfully loaded.
     *
     * @param inMobiBanner Represents the [InMobiBanner] ad which was loaded
     * @param adMetaInfo   Represents the ad meta information
     */
    override fun onAdLoadSucceeded(
        inMobiBanner: InMobiBanner,
        adMetaInfo: AdMetaInfo
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onBannerAdLoaded(inMobiBanner, layoutParams)
    }

    /**
     * Called to notify that a request to load an ad failed.
     *
     * @param inMobiBanner          Represents the [InMobiBanner] ad which failed to load
     * @param inMobiAdRequestStatus Represents the [InMobiAdRequestStatus] status containing error reason
     */
    override fun onAdLoadFailed(
        inMobiBanner: InMobiBanner,
        inMobiAdRequestStatus: InMobiAdRequestStatus
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        val adapterError =
            "${inMobiAdRequestStatus.message}(  ${inMobiAdRequestStatus.statusCode} )"

        val error =
            if (inMobiAdRequestStatus.statusCode == InMobiAdRequestStatus.StatusCode.NO_FILL) {
                IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError)
            } else {
                ErrorBuilder.buildLoadFailedError(adapterError)
            }

        IronLog.ADAPTER_CALLBACK.error("adapterError = $error")

        listener.onBannerAdLoadFailed(error)
    }

    /**
     * Called to notify that an ad was successfully shown.
     *
     * @param inMobiBanner Represents the [InMobiBanner] ad which was shown
     */
    override fun onAdImpression(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onBannerAdShown()
    }

    /**
     * Called to notify that the user interacted with the ad.
     *
     * @param inMobiBanner Represents the [InMobiBanner] ad on which user clicked
     * @param params       Represents the click parameters
     */
    override fun onAdClicked(
        inMobiBanner: InMobiBanner,
        params: Map<Any, Any>?
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onBannerAdClicked()
    }

    /**
     * Called to notify that the user is about to leave the application as a result of interacting with the ad.
     *
     * @param inMobiBanner Represents the [InMobiBanner] ad
     */
    override fun onUserLeftApplication(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onBannerAdLeftApplication()
    }

    /**
     * Called to notify that the banner ad was displayed
     *
     * @param inMobiBanner Represents the [InMobiBanner] ad which was displayed
     */
    override fun onAdDisplayed(inMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onBannerAdScreenPresented()
    }

    /**
     * Called to notify that the User is about to return to the application after closing the ad.
     * @param InMobiBanner Represents the [InMobiBanner] ad which was closed
     */
    override fun onAdDismissed(InMobiBanner: InMobiBanner) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onBannerAdScreenDismissed()
    }

}
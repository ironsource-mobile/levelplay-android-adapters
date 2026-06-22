package com.ironsource.adapters.applovin.banner

import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.ironsource.adapters.applovin.AppLovinAdapter
import com.ironsource.adapters.applovin.AppLovinConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog

class AppLovinBannerListener(
    private val listener: BannerAdListener,
    private val adView: AppLovinAdView,
    private val layoutParams: FrameLayout.LayoutParams
) : AppLovinAdLoadListener,
    AppLovinAdDisplayListener,
    AppLovinAdClickListener,
    AppLovinAdViewEventListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Called by AppLovin when a new banner ad has been received.
     * @param appLovinAd the newly received ad, guaranteed not to be null.
     */
    override fun adReceived(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mainHandler.post {
            adView.renderAd(appLovinAd)
            listener.onAdLoadSuccess(adView, layoutParams)
        }
    }

    /**
     * Called by AppLovin when a banner ad could not be retrieved from the server.
     * @param errorCode the reason the ad failed to load.
     */
    override fun failedToReceiveAd(errorCode: Int) {
        val errorMessage = AppLovinAdapter.getErrorString(errorCode)
        IronLog.ADAPTER_CALLBACK.error(AppLovinConstants.Logs.LOAD_FAILED.format(errorCode, errorMessage))
        listener.onAdLoadFailed(AppLovinAdapter.getLoadErrorType(errorCode), errorCode, errorMessage)
    }

    /**
     * Called by AppLovin when the banner ad is displayed.
     * @param appLovinAd the ad that was just displayed.
     */
    override fun adDisplayed(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called by AppLovin when the banner ad is hidden.
     * @param appLovinAd the ad that was just hidden.
     */
    override fun adHidden(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by AppLovin when the banner ad is clicked.
     * @param appLovinAd the ad that was just clicked.
     */
    override fun adClicked(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called by AppLovin after the ad view presents fullscreen content.
     * @param appLovinAd the ad that the ad view presented fullscreen content for.
     * @param appLovinAdView the ad view that presented fullscreen content.
     */
    override fun adOpenedFullscreen(appLovinAd: AppLovinAd, appLovinAdView: AppLovinAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    /**
     * Called by AppLovin after the fullscreen content is dismissed.
     * @param appLovinAd the ad for which the fullscreen content is dismissed.
     * @param appLovinAdView the ad view whose fullscreen content is dismissed.
     */
    override fun adClosedFullscreen(appLovinAd: AppLovinAd, appLovinAdView: AppLovinAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }

    /**
     * Called by AppLovin before the user is taken out of the application after a click.
     * @param appLovinAd the ad for which the user will be taken out of the application.
     * @param appLovinAdView the ad view containing the ad.
     */
    override fun adLeftApplication(appLovinAd: AppLovinAd, appLovinAdView: AppLovinAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    /**
     * Called by AppLovin if the ad view fails to display an ad.
     * @param appLovinAd the ad for which the ad view failed to display.
     * @param appLovinAdView the ad view which failed to display the ad.
     * @param errorCode the reason the ad view failed to display the ad.
     */
    override fun adFailedToDisplay(
        appLovinAd: AppLovinAd,
        appLovinAdView: AppLovinAdView,
        errorCode: AppLovinAdViewDisplayErrorCode
    ) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

package com.ironsource.adapters.applovin.rewarded

import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdRewardListener
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.ironsource.adapters.applovin.AppLovinAdapter
import com.ironsource.adapters.applovin.AppLovinConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class AppLovinRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<AppLovinRewardedAdapter>
) : AppLovinAdLoadListener,
    AppLovinAdClickListener,
    AppLovinAdDisplayListener,
    AppLovinAdVideoPlaybackListener,
    AppLovinAdRewardListener {

    /**
     * Called by AppLovin when a new rewarded video ad has been received.
     * @param appLovinAd the newly received ad, guaranteed not to be null.
     */
    override fun adReceived(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setLoadedAppLovinAd(appLovinAd)
        adapter.get()?.setRewardedAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called by AppLovin when a rewarded video ad could not be retrieved from the server.
     * @param errorCode the reason the ad failed to load.
     */
    override fun failedToReceiveAd(errorCode: Int) {
        val errorMessage = AppLovinAdapter.getErrorString(errorCode)
        IronLog.ADAPTER_CALLBACK.error(AppLovinConstants.Logs.LOAD_FAILED.format(errorCode, errorMessage))
        adapter.get()?.setRewardedAdAvailability(false)
        listener.onAdLoadFailed(AppLovinAdapter.getLoadErrorType(errorCode), errorCode, errorMessage)
    }

    /**
     * Called by AppLovin when the rewarded video ad is displayed.
     * @param appLovinAd the ad that was just displayed.
     */
    override fun adDisplayed(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called by AppLovin when the rewarded video begins playing.
     * @param appLovinAd the ad in which playback began.
     */
    override fun videoPlaybackBegan(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdStarted()
    }

    /**
     * Called by AppLovin when the rewarded video stops playing.
     * @param appLovinAd the ad in which playback ended.
     * @param percentViewed percent of the video which the user watched.
     * @param isFullyWatched whether the video was watched to, or very near, completion.
     */
    override fun videoPlaybackEnded(appLovinAd: AppLovinAd, percentViewed: Double, isFullyWatched: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdEnded()
        if (isFullyWatched) {
            listener.onAdRewarded()
        }
    }

    /**
     * Called by AppLovin when the rewarded video ad is clicked.
     * @param appLovinAd the ad that was just clicked.
     */
    override fun adClicked(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called by AppLovin when the rewarded video ad is hidden.
     * @param appLovinAd the ad that was just hidden.
     */
    override fun adHidden(appLovinAd: AppLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called by AppLovin when the reward validation request succeeded.
     * @param appLovinAd the ad for which a validation request was submitted.
     * @param response any response extras sent down by AppLovin.
     */
    override fun userRewardVerified(appLovinAd: AppLovinAd, response: Map<String, String>) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by AppLovin when the user has already received the maximum allocated rewards for the day.
     * @param appLovinAd the ad for which a validation request was submitted.
     * @param response any response extras sent down by AppLovin.
     */
    override fun userOverQuota(appLovinAd: AppLovinAd, response: Map<String, String>) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by AppLovin when the user's reward was detected as fraudulent and not awarded.
     * @param appLovinAd the ad for which a validation request was submitted.
     * @param response any response extras sent down by AppLovin.
     */
    override fun userRewardRejected(appLovinAd: AppLovinAd, response: Map<String, String>) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called by AppLovin when the reward validation request failed.
     * @param appLovinAd the ad for which a validation request was submitted.
     * @param errorCode an error code indicating the cause of failure.
     */
    override fun validationRequestFailed(appLovinAd: AppLovinAd, errorCode: Int) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}

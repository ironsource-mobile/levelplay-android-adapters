package com.ironsource.adapters.vungle

import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.RewardedAd
import com.vungle.ads.RewardedAdListener
import com.vungle.ads.VungleError

internal class VungleRewardedAdapter(
    placementId: String,
    adConfig: AdConfig,
    private val listener: RewardedVideoSmashListener?
) : RewardedAdListener {
    private var mRewardedAd: RewardedAd?

    init {
        mRewardedAd =
            RewardedAd(ContextProvider.getInstance().applicationContext, placementId, adConfig)
        mRewardedAd?.adListener = this
    }

    fun loadWithBid(serverData: String?) {
        mRewardedAd?.load(serverData)
    }

    fun canPlayAd(): Boolean {
        return mRewardedAd?.canPlayAd() == true
    }

    fun play() {
        mRewardedAd?.play()
    }

    fun destroy() {
        mRewardedAd = null
    }

    fun setUserId(userID: String) {
        mRewardedAd?.setUserId(userID)
    }

    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAvailabilityChanged(true)
    }

    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAdStarted()
    }

    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAdOpened()
    }

    override fun onAdRewarded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAdRewarded()
    }

    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAdClicked()
    }

    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAdEnded()
        listener.onRewardedVideoAdClosed()
    }

    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = ${baseAd.placementId}, error = $adError")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onRewardedVideoAdShowFailed(
            ErrorBuilder.buildShowFailedError(
                IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
                adError.errorMessage
            )
        )
    }

    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToLoad placementId = ${baseAd.placementId}, error = $adError")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        val error: IronSourceError = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, adError.errorMessage)
        } else {
            ErrorBuilder.buildLoadFailedError(adError.errorMessage)
        }
        listener.onRewardedVideoAvailabilityChanged(false)
        listener.onRewardedVideoLoadFailed(error)
    }

    override fun onAdLeftApplication(baseAd: BaseAd) {
        // no-op
    }
}

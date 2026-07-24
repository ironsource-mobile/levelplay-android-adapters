package com.ironsource.adapters.applovin.rewarded

import android.app.Activity
import android.content.Context
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.sdk.AppLovinAd
import com.ironsource.adapters.applovin.AppLovinAdapter
import com.ironsource.adapters.applovin.AppLovinConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class AppLovinRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<AppLovinAdapter>(networkSettings) {

    companion object {
        private val rewardedZoneIds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    }

    private var rewardedAd: AppLovinIncentivizedInterstitial? = null
    private var rewardedAdListener: AppLovinRewardedListener? = null
    private var loadedAppLovinAd: AppLovinAd? = null
    private var isAdAvailableFlag = false
    private var reservedZoneId: String? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val zoneId = adData.getString(AppLovinConstants.ZONE_ID_KEY)
        IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.ZONE_ID.format(zoneId ?: ""))

        if (zoneId.isNullOrEmpty()) {
            val errorMessage = AppLovinConstants.Logs.MISSING_PARAM.format(AppLovinConstants.ZONE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        if (zoneId in rewardedZoneIds) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.DUPLICATE_REWARDED)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AppLovinConstants.Logs.DUPLICATE_REWARDED
            )
            return
        }

        val appLovinSdk = AppLovinAdapter.appLovinSdk
        if (appLovinSdk == null) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.SDK_NOT_INITIALIZED)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AppLovinConstants.Logs.SDK_NOT_INITIALIZED
            )
            return
        }

        rewardedZoneIds.add(zoneId)
        reservedZoneId = zoneId

        setRewardedAdAvailability(false)

        rewardedAdListener = AppLovinRewardedListener(listener, WeakReference(this))
        rewardedAd = AppLovinIncentivizedInterstitial(zoneId)

        appLovinSdk.adService.loadNextAdForZoneId(zoneId, rewardedAdListener)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        reservedZoneId?.let { rewardedZoneIds.remove(it) }
        reservedZoneId = null

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, AppLovinConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        getDynamicUserId()?.let { userId ->
            if (userId.isNotEmpty()) {
                IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.SET_USER_ID.format(userId))
                AppLovinAdapter.appLovinSdk?.settings?.setUserIdentifier(userId)
            }
        }

        rewardedAd?.show(
            loadedAppLovinAd,
            rewardedAdListener,
            rewardedAdListener,
            rewardedAdListener,
            rewardedAdListener
        )
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        rewardedAd != null && isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        reservedZoneId?.let { rewardedZoneIds.remove(it) }
        reservedZoneId = null
        rewardedAd = null
        rewardedAdListener = null
        loadedAppLovinAd = null
    }

    // endregion

    // region Helper Methods

    internal fun setLoadedAppLovinAd(ad: AppLovinAd) {
        loadedAppLovinAd = ad
    }

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}

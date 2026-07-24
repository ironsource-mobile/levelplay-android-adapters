package com.ironsource.adapters.applovin.interstitial

import android.app.Activity
import android.content.Context
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.sdk.AppLovinAd
import com.ironsource.adapters.applovin.AppLovinAdapter
import com.ironsource.adapters.applovin.AppLovinConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class AppLovinInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<AppLovinAdapter>(networkSettings) {

    companion object {
        private val interstitialZoneIds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    }

    private var interstitialAdListener: AppLovinInterstitialListener? = null
    private var interstitialAd: AppLovinAd? = null
    private var isAdAvailableFlag = false
    private var reservedZoneId: String? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
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

        if (zoneId in interstitialZoneIds) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.DUPLICATE_INTERSTITIAL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AppLovinConstants.Logs.DUPLICATE_INTERSTITIAL
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

        interstitialZoneIds.add(zoneId)
        reservedZoneId = zoneId

        setInterstitialAdAvailability(false)

        interstitialAdListener = AppLovinInterstitialListener(listener, WeakReference(this))

        appLovinSdk.adService.loadNextAdForZoneId(zoneId, interstitialAdListener)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        reservedZoneId?.let { interstitialZoneIds.remove(it) }
        reservedZoneId = null

        val ad = interstitialAd
        if (!isAdAvailable(adData) || ad == null) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, AppLovinConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        val interstitialAdDialog = AppLovinInterstitialAd.create()
        interstitialAdDialog.setAdClickListener(interstitialAdListener)
        interstitialAdDialog.setAdDisplayListener(interstitialAdListener)
        interstitialAdDialog.setAdVideoPlaybackListener(interstitialAdListener)
        interstitialAdDialog.showAndRender(ad)
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        interstitialAd != null && isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        reservedZoneId?.let { interstitialZoneIds.remove(it) }
        reservedZoneId = null
        interstitialAd = null
        interstitialAdListener = null
    }

    // endregion

    // region Helper Methods

    internal fun setInterstitialAd(ad: AppLovinAd) {
        interstitialAd = ad
    }

    internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}
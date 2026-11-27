package com.ironsource.adapters.bidmachine.interstitial

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.format.interstitialad.InterstitialAd

class BidMachineInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<BidMachineAdapter>(networkSettings) {

    private var interstitialAd: InterstitialAd? = null
    private var loadId = 0

    // region LevelPlay Interstitial API

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: InterstitialAdListener
    ) {
        val currentLoadId = resetAd()

        val instance = BidMachineAdapter.bidMachineInstance
        if (instance == null) {
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                BMError.NOT_INITIALIZED,
                BidMachineConstants.SDK_NOT_INITIALIZED
            )
            return
        }

        val placementId = adData.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val bidMachineListener = BidMachineInterstitialListener(listener)
        val loadCallback = object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(ad: InterstitialAd) {
                ad.setListener(bidMachineListener)
                val isCurrentLoad = synchronized(this@BidMachineInterstitialAdapter) {
                    if (currentLoadId == loadId) {
                        interstitialAd = ad
                        true
                    } else {
                        false
                    }
                }
                // Discard results from a destroyed or replaced load.
                if (!isCurrentLoad) {
                    ad.setListener(null)
                    (ad as Ad).destroy()
                    return
                }
                bidMachineListener.onAdLoaded(ad)
            }

            override fun onAdLoadFailed(bmError: BMError) {
                synchronized(this@BidMachineInterstitialAdapter) {
                    if (currentLoadId != loadId) {
                        return
                    }
                }
                bidMachineListener.onAdLoadFailed(bmError)
            }
        }

        synchronized(this) {
            if (currentLoadId != loadId) {
                return
            }
        }
        val bidResponse = adData.serverData
        if (!bidResponse.isNullOrEmpty()) {
            instance.loadInterstitial(bidResponse, loadCallback)
        } else {
            instance.loadInterstitial(BidMachineAdapter.createPlacement(placementId), loadCallback)
        }
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: InterstitialAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val ad = synchronized(this) { interstitialAd }
        if (ad == null || !(ad as Ad).canShow()) {
            IronLog.ADAPTER_API.error(BidMachineConstants.AD_NOT_READY)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                BidMachineConstants.AD_NOT_READY
            )
            return
        }

        ad.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        val ad = synchronized(this) { interstitialAd }
        return (ad as Ad?)?.canShow() ?: false
    }

    override fun destroyAd(adData: AdData) {
        resetAd()
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val placementId = adData?.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback, placementId)
    }

    // endregion

    // region Helper Methods

    private fun resetAd(): Int {
        IronLog.ADAPTER_API.verbose()
        val ad: InterstitialAd?
        val currentLoadId: Int
        synchronized(this) {
            loadId++
            currentLoadId = loadId
            ad = interstitialAd
            interstitialAd = null
        }
        ad?.setListener(null)
        (ad as Ad?)?.destroy()
        return currentLoadId
    }

    // endregion
}

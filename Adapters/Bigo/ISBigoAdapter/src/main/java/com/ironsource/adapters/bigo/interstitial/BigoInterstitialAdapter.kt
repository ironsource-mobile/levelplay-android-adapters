package com.ironsource.adapters.bigo.interstitial

import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.json.JSONObject
import sg.bigo.ads.api.InterstitialAd
import sg.bigo.ads.api.InterstitialAdLoader
import sg.bigo.ads.api.InterstitialAdRequest
import java.lang.ref.WeakReference


class BigoInterstitialAdapter(adapter: BigoAdapter) :
    AbstractInterstitialAdapter<BigoAdapter>(adapter) {

    private var mSmashListener : InterstitialSmashListener? = null
    private var mAdListener : BigoInterstitialAdListener? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var mAdLoader: InterstitialAdLoader? = null
    private var isInterstitialAdAvailable = false

    override fun initInterstitialForBidding(
        appId: String?,
        userId: String?,
        config: JSONObject,
        listener: InterstitialSmashListener
    ) {
        val appId = config.optString(BigoAdapter.getAppIdKey())
        if (appId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appId))
            listener.onInterstitialInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appId),
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
            return
        }

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            BigoAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onInterstitialInitSuccess()
            }
            BigoAdapter.Companion.InitState.INIT_STATE_NONE,
            BigoAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appId)
            }
        }

    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onInterstitialInitSuccess()
    }

    override fun loadInterstitialForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: InterstitialSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        setInterstitialAdAvailability(false)

        val slotIdKey= BigoAdapter.getSlotIdKey()
        val slotId = getConfigStringValueFromKey(config, slotIdKey)

        val interstitialAdListener = BigoInterstitialAdListener(WeakReference(this), listener)
        mAdListener = interstitialAdListener

        val interstitialAdLoader = InterstitialAdLoader.Builder()
            .withAdLoadListener(mAdListener)
            .withExt(BigoAdapter.MEDIATION_INFO)
            .build()

        mAdLoader = interstitialAdLoader

        val interstitialAdRequest =
            InterstitialAdRequest.Builder()
                .withBid(serverData)
                .withSlotId(slotId)
                .build()
        interstitialAdLoader.loadAd(interstitialAdRequest)

    }

    override fun showInterstitial(config: JSONObject, listener: InterstitialSmashListener) {
        IronLog.ADAPTER_API.verbose()
        if (!isInterstitialReady(config)) {
            listener.onInterstitialAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.INTERSTITIAL_AD_UNIT
                )
            )
        } else {
            mInterstitialAd?.setAdInteractionListener(mAdListener)
            mInterstitialAd?.show()
            setInterstitialAdAvailability(false)
        }
    }

    override fun isInterstitialReady(config: JSONObject): Boolean {
        return mInterstitialAd !=null && mInterstitialAd?.isExpired == false
    }

    override fun getInterstitialBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? {
        return adapter.getBiddingData()
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")
        destroyInterstitialAd()
        mAdListener = null
        mSmashListener = null
    }

    //end region

    //region Helpers

    internal fun setInterstitialAd(ad: InterstitialAd) {
        mInterstitialAd = ad
    }
    internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
        isInterstitialAdAvailable = isAvailable
    }

    internal fun destroyInterstitialAd() {
        mInterstitialAd?.setAdInteractionListener(null)
        mInterstitialAd?.destroy()
        mAdLoader = null
        mInterstitialAd = null
    }

    //end region
}
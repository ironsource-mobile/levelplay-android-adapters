package com.ironsource.adapters.aps

import android.content.Context
import android.content.res.Configuration
import com.amazon.aps.ads.Aps
import com.amazon.aps.ads.model.ApsAdNetwork
import com.amazon.device.ads.AdError
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdNetworkInfo
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.amazon.device.ads.SDKUtilities
import com.ironsource.mediationsdk.SetAPSInterface
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import org.json.JSONObject

class APSAdapter : LevelPlayBaseAdapter(), SetAPSInterface {

    companion object {

        private const val GitHash: String = BuildConfig.GitHash

        private var usPrivacyValue: String = APSConstants.US_PRIVACY_NOT_APPLICABLE

        @JvmStatic
        fun networkAdapterVersion(): String = APSConstants.ADAPTER_VERSION
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = APSConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = Aps.getSdkVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        IronLog.ADAPTER_API.verbose()

        // The APS SDK is initialized by the publisher directly, so once init is called
        // we can assume the SDK is ready and report success immediately.
        networkInitializationListener?.onInitSuccess()
    }

    override fun setAPSData(adFormat: LevelPlay.AdFormat, apsData: JSONObject) {
        IronLog.ADAPTER_API.error(APSConstants.Logs.APS_MANUAL_LOADING_NOT_REQUIRED)
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        // This is a list of 1 value
        val value = values[0]
        IronLog.ADAPTER_API.verbose(APSConstants.Logs.META_DATA_SET.format(key ?: "", value ?: ""))

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
        }
    }

    private fun setCCPAValue(doNotSell: Boolean) {
        IronLog.ADAPTER_API.verbose(APSConstants.Logs.CCPA_OPT_OUT.format(doNotSell))
        usPrivacyValue = if (doNotSell) APSConstants.US_PRIVACY_OPT_OUT else APSConstants.US_PRIVACY_OPT_IN
    }

    private fun getUSPrivacyStrings(): Map<String, String> {
        IronLog.ADAPTER_API.verbose(APSConstants.Logs.US_PRIVACY.format(usPrivacyValue))
        return mapOf(APSConstants.US_PRIVACY_KEY to usPrivacyValue)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(
        adSize: DTBAdSize,
        biddingDataCallback: BiddingDataCallback,
        onSuccess: (adResponse: DTBAdResponse) -> Unit
    ) {
        val adNetworkInfo = DTBAdNetworkInfo(ApsAdNetwork.UNITY_LEVELPLAY)
        val request = DTBAdRequest(adNetworkInfo).apply {
            setSizes(adSize)

            // Add U.S. Privacy Strings as custom targets if available
            getUSPrivacyStrings().forEach { (key, value) ->
                putCustomTarget(key, value)
            }
        }

        request.loadAd(object : DTBAdCallback {
            override fun onFailure(error: AdError) {
                val errorMessage = APSConstants.Logs.TOKEN_FAILURE.format(error.message)
                IronLog.ADAPTER_CALLBACK.error(errorMessage)
                biddingDataCallback.onFailure(errorMessage)
            }

            override fun onSuccess(adResponse: DTBAdResponse) {
                IronLog.ADAPTER_CALLBACK.verbose()
                onSuccess.invoke(adResponse)

                val biddingData = mapOf(
                    APSConstants.PRICE_POINT_ENCODED to SDKUtilities.getPricePoint(adResponse),
                    APSConstants.UUID to adSize.slotUUID,
                    APSConstants.WIDTH to adSize.width,
                    APSConstants.HEIGHT to adSize.height
                )
                biddingDataCallback.onSuccess(biddingData)
            }
        })
    }

    internal fun getVideoSize(context: Context, slotId: String): DTBAdSize {
        val orientation = context.resources?.configuration?.orientation
        return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            DTBAdSize.DTBVideo(APSConstants.VIDEO_PORTRAIT_WIDTH, APSConstants.VIDEO_PORTRAIT_HEIGHT, slotId)
        } else {
            DTBAdSize.DTBVideo(APSConstants.VIDEO_LANDSCAPE_WIDTH, APSConstants.VIDEO_LANDSCAPE_HEIGHT, slotId)
        }
    }

    // endregion
}

package com.ironsource.adapters.aps

import android.content.Context
import android.content.res.Configuration
import com.amazon.aps.ads.Aps
import com.amazon.aps.ads.model.ApsAdNetwork
import com.amazon.aps.shared.util.ApsResult
import com.amazon.device.ads.AdError
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdNetworkInfo
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.amazon.device.ads.SDKUtilities
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class APSAdapter : LevelPlayBaseAdapter() {

    companion object {

        private const val GitHash: String = BuildConfig.GitHash

        private var usPrivacyValue: String = APSConstants.US_PRIVACY_NOT_APPLICABLE

        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        private var isTestMode: Boolean = false

        @Suppress("ACCIDENTAL_OVERRIDE")
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
        val appId = adData.getString(APSConstants.APP_ID)
        if (appId.isNullOrEmpty()) {
            val errorMessage = APSConstants.Logs.MISSING_APS_CONFIGURATION.format(APSConstants.APP_ID)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(APSConstants.Logs.INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                APSConstants.Logs.INIT_FAILED
            )
            return
        }

        networkInitializationListener?.let { initListeners.add(it) }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(APSConstants.Logs.APP_ID_LOG.format(appId))

            Aps.enableLogging(isAdaptersDebugEnabled())
            Aps.initialize(context.applicationContext, appId) { status ->
                if (status?.result == ApsResult.SUCCESS) {
                    onInitializationSuccess()
                } else {
                    onInitializationFailure()
                }
            }
        }
    }

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(APSConstants.Logs.INIT_SUCCESS)

        if (isTestMode) {
            Aps.setTestingMode(true)
        }

        initState = InitState.INIT_STATE_SUCCESS

        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun onInitializationFailure() {
        IronLog.ADAPTER_CALLBACK.error(APSConstants.Logs.INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, APSConstants.Logs.INIT_FAILED)
        }

        initListeners.clear()
    }

    override fun setTestMode(enabled: Boolean) {
        IronLog.ADAPTER_API.verbose(APSConstants.Logs.TEST_MODE.format(enabled))
        isTestMode = enabled

        if (initState == InitState.INIT_STATE_SUCCESS) {
            Aps.setTestingMode(enabled)
        }
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

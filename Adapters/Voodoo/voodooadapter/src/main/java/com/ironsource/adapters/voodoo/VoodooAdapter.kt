package com.ironsource.adapters.voodoo

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import io.adn.sdk.publisher.AdnAdError
import io.adn.sdk.publisher.AdnAdPlacement
import io.adn.sdk.publisher.AdnBidTokenCallback
import io.adn.sdk.publisher.AdnInitializationCallback
import io.adn.sdk.publisher.AdnInitializationStatus
import io.adn.sdk.publisher.AdnMediationType
import io.adn.sdk.publisher.AdnSdk
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class VoodooAdapter : LevelPlayBaseAdapter() {

    companion object {

        private const val GitHash: String = BuildConfig.GitHash

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        @JvmStatic
        fun networkAdapterVersion(): String = VoodooConstants.ADAPTER_VERSION

        @JvmStatic
        fun getLoadError(error: AdnAdError): AdapterErrorType {
            return if (error == AdnAdError.NoFill) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = VoodooConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = AdnSdk.getVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val placementId = adData.getString(VoodooConstants.PLACEMENT_ID_KEY)
        if (placementId.isNullOrEmpty()) {
            val errorMessage = VoodooConstants.Logs.MISSING_PARAM.format(VoodooConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Init previously failed - report failure immediately
        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(VoodooConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                VoodooConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(VoodooConstants.Logs.PLACEMENT_ID.format(placementId))

            AdnSdk.setVerbose(isAdaptersDebugEnabled())
            AdnSdk.setMediationType(AdnMediationType.IRONSOURCE)
            AdnSdk.initialize(context.applicationContext, object : AdnInitializationCallback {
                override fun onCompletion(status: AdnInitializationStatus) {
                    when (status) {
                        AdnInitializationStatus.Success -> initializationSuccess()
                        AdnInitializationStatus.Failure -> initializationFailure()
                        else -> {}
                    }
                }
            })
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose(VoodooConstants.Logs.INIT_SUCCESS)

        initState = InitState.INIT_STATE_SUCCESS

        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.error(VoodooConstants.Logs.SDK_INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, VoodooConstants.Logs.SDK_INIT_FAILED)
        }

        initListeners.clear()
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback, adnAdPlacement: AdnAdPlacement) {
        if (initState == InitState.INIT_STATE_NONE) {
            IronLog.INTERNAL.verbose(VoodooConstants.Logs.TOKEN_INIT_NOT_STARTED)
            biddingDataCallback.onFailure(VoodooConstants.Logs.TOKEN_INIT_NOT_STARTED)
            return
        }

        AdnSdk.getBidToken(adnAdPlacement, object : AdnBidTokenCallback {
            override fun onComplete(response: String) {
                val sdkVersion = getNetworkSDKVersion()
                IronLog.ADAPTER_API.verbose(VoodooConstants.Logs.TOKEN.format(response, sdkVersion))
                val biddingData: MutableMap<String, Any> = HashMap()
                biddingData[VoodooConstants.TOKEN_KEY] = response
                biddingData[VoodooConstants.SDK_VERSION_KEY] = sdkVersion
                biddingDataCallback.onSuccess(biddingData)
            }
        })
    }

    // endregion
}

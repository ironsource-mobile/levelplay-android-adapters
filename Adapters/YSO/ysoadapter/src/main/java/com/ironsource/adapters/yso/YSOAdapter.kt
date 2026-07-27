package com.ironsource.adapters.yso

import android.app.Application
import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import com.ysocorp.ysonetwork.YsoNetwork
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class YSOAdapter : LevelPlayBaseAdapter() {

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
        fun networkAdapterVersion(): String = YSOConstants.ADAPTER_VERSION
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = YSOConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = YsoNetwork.getSdkVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val placementKey = adData.getString(YSOConstants.PLACEMENT_KEY)
        if (placementKey.isNullOrEmpty()) {
            val errorMessage = YSOConstants.Logs.MISSING_PARAM.format(YSOConstants.PLACEMENT_KEY)
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
            IronLog.INTERNAL.error(YSOConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                YSOConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(YSOConstants.Logs.PLACEMENT_KEY_LOG.format(placementKey))

            try {
                YsoNetwork.initialize(context.applicationContext as Application)
                if (YsoNetwork.isInitialize()) {
                    initializationSuccess()
                } else {
                    initializationFailure(YSOConstants.Logs.SDK_INIT_FAILED)
                }
            } catch (e: Exception) {
                initializationFailure(YSOConstants.Logs.INIT_EXCEPTION.format(e.message ?: ""))
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun initializationFailure(errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(errorMessage)

        initState = InitState.INIT_STATE_FAILED

        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
        }

        initListeners.clear()
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(YSOConstants.Logs.TOKEN_INIT_NOT_COMPLETED)
            biddingDataCallback.onFailure(YSOConstants.Logs.TOKEN_INIT_NOT_COMPLETED)
            return
        }

        val token = YsoNetwork.getSignal()
        if (token.isNullOrEmpty()) {
            IronLog.INTERNAL.verbose(YSOConstants.Logs.TOKEN_EMPTY)
            biddingDataCallback.onFailure(YSOConstants.Logs.TOKEN_EMPTY)
            return
        }

        val sdkVersion = getNetworkSDKVersion()
        IronLog.ADAPTER_API.verbose(YSOConstants.Logs.TOKEN.format(token, sdkVersion))
        val biddingData: MutableMap<String?, Any?> = HashMap()
        biddingData[YSOConstants.SDK_VERSION_KEY] = sdkVersion
        biddingData[YSOConstants.TOKEN_KEY] = token
        biddingDataCallback.onSuccess(biddingData)
    }

    // endregion
}

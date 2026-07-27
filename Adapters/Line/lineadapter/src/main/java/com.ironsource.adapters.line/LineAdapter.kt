package com.ironsource.adapters.line

import android.content.Context
import com.five_corp.ad.AdLoader
import com.five_corp.ad.FiveAdConfig
import com.five_corp.ad.FiveAdErrorCode
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class LineAdapter : LevelPlayBaseAdapter() {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private const val GitHash: String = BuildConfig.GitHash

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()
        private var fiveAdConfig: FiveAdConfig? = null

        @JvmStatic
        fun getLoadErrorType(errorCode: FiveAdErrorCode): AdapterErrorType {
            return when (errorCode) {
                FiveAdErrorCode.NO_AD -> AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
                else -> AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }

        @JvmStatic
        fun networkAdapterVersion(): String = LineConstants.ADAPTER_VERSION

        internal fun getFiveAdConfig(appId: String): FiveAdConfig {
            return fiveAdConfig ?: FiveAdConfig(appId).also { fiveAdConfig = it }
        }

        internal fun getAdLoader(context: Context): AdLoader? {
            return fiveAdConfig?.let { AdLoader.forConfig(context.applicationContext, it) }
        }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = LineConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = AdLoader.getSemanticVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate configuration params first before any other checks
        val appId = adData.getString(LineConstants.APP_ID_KEY)
        if (appId.isNullOrEmpty()) {
            val errorMessage = LineConstants.Logs.MISSING_PARAM.format(LineConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        val slotId = adData.getString(LineConstants.SLOT_ID_KEY)
        if (slotId.isNullOrEmpty()) {
            val errorMessage = LineConstants.Logs.MISSING_PARAM.format(LineConstants.SLOT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                LineConstants.Logs.INIT_FAILED
            )
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(LineConstants.Logs.APP_ID_AND_SLOT_ID.format(appId, slotId))
            try {
                val loader = AdLoader.forConfig(context.applicationContext, getFiveAdConfig(appId))
                if (loader == null) {
                    IronLog.INTERNAL.error(LineConstants.Logs.AD_LOADER_NULL)
                    initializationFailure()
                } else {
                    initializationSuccess()
                }
            } catch (e: IllegalArgumentException) {
                IronLog.INTERNAL.error(LineConstants.Logs.FAILED_TO_LOAD.format(LineConstants.Logs.INIT_FAILED, e.message ?: ""))
                initializationFailure()
            }
        }
    }

    private fun initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        // Iterate over all the adapter instances and report init success
        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.error(LineConstants.Logs.INIT_FAILED)

        initState = InitState.INIT_STATE_FAILED

        // Iterate over all the adapter instances and report init failed
        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, LineConstants.Logs.INIT_FAILED)
        }

        initListeners.clear()
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(
        context: Context,
        appId: String?,
        slotId: String,
        biddingDataCallback: BiddingDataCallback
    ) {
        if (appId.isNullOrEmpty()) {
            val errorMessage = LineConstants.Logs.MISSING_PARAM.format(LineConstants.APP_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        val loader = AdLoader.forConfig(context.applicationContext, getFiveAdConfig(appId))
        if (loader == null) {
            IronLog.INTERNAL.error(LineConstants.Logs.TOKEN_AD_LOADER_NULL)
            biddingDataCallback.onFailure(LineConstants.Logs.TOKEN_AD_LOADER_NULL)
            return
        }

        loader.collectSignal(slotId, object : AdLoader.CollectSignalCallback {
            override fun onCollect(token: String) {
                IronLog.ADAPTER_API.verbose(LineConstants.Logs.TOKEN.format(token))
                val ret: MutableMap<String?, Any?> = HashMap()
                ret[LineConstants.TOKEN_KEY] = token
                biddingDataCallback.onSuccess(ret)
            }

            override fun onError(fiveAdErrorCode: FiveAdErrorCode) {
                val errorMessage = LineConstants.Logs.TOKEN_FAILURE.format(fiveAdErrorCode.name)
                IronLog.INTERNAL.error(errorMessage)
                biddingDataCallback.onFailure(errorMessage)
            }
        })
    }

    // endregion
}

package com.ironsource.adapters.smaato

import android.app.Application
import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.smaato.sdk.core.Config
import com.smaato.sdk.core.SmaatoSdk
import com.smaato.sdk.core.ad.AdRequestParams
import com.smaato.sdk.core.log.LogLevel
import com.smaato.sdk.iahb.InAppBid
import com.smaato.sdk.iahb.InAppBiddingException
import com.smaato.sdk.iahb.SmaatoSdkInAppBidding
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class SmaatoAdapter : LevelPlayBaseAdapter() {

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

        internal fun getAdRequest(serverData: String): AdRequestParams? {
            return try {
                val inAppBid = InAppBid.create(serverData)
                val uniqueId = SmaatoSdkInAppBidding.saveBid(inAppBid)
                AdRequestParams.builder().setUBUniqueId(uniqueId).build()
            } catch (ex: InAppBiddingException) {
                IronLog.ADAPTER_API.error(SmaatoConstants.Logs.INVALID_AD_REQUEST_ERROR.format(ex.message ?: ""))
                null
            }
        }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = SmaatoConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = SmaatoSdk.getVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val publisherId = adData.getString(SmaatoConstants.PUBLISHER_ID_KEY)
        val adSpaceId = adData.getString(SmaatoConstants.AD_SPACE_ID_KEY)

        if (publisherId.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.PUBLISHER_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (adSpaceId.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.AD_SPACE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(SmaatoConstants.Logs.SDK_INIT_FAILED)
            networkInitializationListener?.onInitFailed(
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                SmaatoConstants.Logs.SDK_INIT_FAILED
            )
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(SmaatoConstants.Logs.PUBLISHER_ID.format(publisherId))

            val config = Config.builder().apply {
                if (isAdaptersDebugEnabled()) {
                    setLogLevel(LogLevel.DEBUG)
                }
            }.build()

            SmaatoSdk.init(
                context.applicationContext as Application,
                config,
                publisherId,
                object : SmaatoSdk.SmaatoSdkInitialisationListener {
                    override fun onInitialisationSuccess() {
                        initializationSuccess()
                    }

                    override fun onInitialisationFailure(errorMessage: String) {
                        initializationFailure(errorMessage)
                    }
                }
            )
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
        IronLog.ADAPTER_CALLBACK.error(SmaatoConstants.Logs.INIT_FAILED.format(errorMessage))

        initState = InitState.INIT_STATE_FAILED

        for (listener: NetworkInitializationListener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(SmaatoConstants.Logs.CONSENT.format(consent))
        SmaatoSdk.setLgpdConsentEnabled(consent)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback) {
        if (initState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose(SmaatoConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(SmaatoConstants.Logs.TOKEN_ERROR)
            return
        }

        val returnedToken = SmaatoSdk.collectSignals(context.applicationContext).orEmpty()
        IronLog.ADAPTER_API.verbose(SmaatoConstants.Logs.TOKEN.format(returnedToken))

        val ret: MutableMap<String?, Any?> = HashMap()
        ret[SmaatoConstants.TOKEN_KEY] = returnedToken
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}

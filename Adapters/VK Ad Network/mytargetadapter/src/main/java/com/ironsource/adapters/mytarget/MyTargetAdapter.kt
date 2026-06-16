package com.ironsource.adapters.mytarget

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import com.my.target.common.MyTargetManager
import com.my.target.common.MyTargetPrivacy
import com.my.target.common.MyTargetVersion
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MyTargetAdapter : LevelPlayBaseAdapter() {

    companion object {

        // Init state possible values
        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS
        }

        private const val GitHash: String = BuildConfig.GitHash

        // Handle init callback for all adapter instances
        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = MyTargetConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = MyTargetVersion.VERSION

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        // Validate slotId first before any other checks
        val slotId = adData.getString(MyTargetConstants.SLOT_ID_KEY)
        if (slotId.isNullOrEmpty()) {
            val errorMessage = MyTargetConstants.Logs.MISSING_PARAM.format(MyTargetConstants.SLOT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        // Check if already initialized
        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        // Add to the init listeners only if init is not finished yet
        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS
            IronLog.ADAPTER_API.verbose(MyTargetConstants.Logs.SLOT_ID.format(slotId))

            MyTargetManager.setDebugMode(isAdaptersDebugEnabled())

            // MyTarget initialization is synchronous and only reports success
            MyTargetManager.initSdk(context.applicationContext)
            initializationSuccess()
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

    // endregion

    // region Legal Methods

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(MyTargetConstants.Logs.CONSENT.format(consent))
        MyTargetPrivacy.setUserConsent(consent)
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback) {
        val bidderToken = MyTargetManager.getBidderToken(context.applicationContext)

        if (bidderToken.isNullOrEmpty()) {
            IronLog.INTERNAL.verbose(MyTargetConstants.Logs.TOKEN_NOT_RECEIVED)
            biddingDataCallback.onFailure(MyTargetConstants.Logs.TOKEN_NOT_RECEIVED)
            return
        }

        IronLog.ADAPTER_API.verbose(MyTargetConstants.Logs.TOKEN.format(bidderToken))
        val ret: MutableMap<String?, Any?> = HashMap()
        ret[MyTargetConstants.TOKEN_KEY] = bidderToken
        biddingDataCallback.onSuccess(ret)
    }

    // endregion
}

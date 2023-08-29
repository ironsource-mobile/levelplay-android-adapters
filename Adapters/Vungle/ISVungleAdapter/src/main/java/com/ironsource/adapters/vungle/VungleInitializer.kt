package com.ironsource.adapters.vungle

import android.content.Context
import com.ironsource.mediationsdk.logger.IronLog
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleAds.WrapperFramework
import com.vungle.ads.VungleError
import java.util.concurrent.atomic.AtomicBoolean

class VungleInitializer private constructor() : InitializationListener {
    private val isInitializing = AtomicBoolean(false)
    private val initListeners = mutableListOf<VungleInitializationListener>()

    init {
        VungleAds.setIntegrationName(
            WrapperFramework.ironsource,
            BuildConfig.VERSION_NAME.replace('.', '_')
        )
    }

    fun initialize(
        appId: String,
        context: Context,
        listener: VungleInitializationListener
    ) {
        if (VungleAds.isInitialized()) {
            listener.onInitializeSuccess()
            return
        }
        if (isInitializing.getAndSet(true)) {
            initListeners.add(listener)
            return
        }
        IronLog.ADAPTER_API.verbose("appId = $appId")
        VungleAds.init(context, appId, this)
        initListeners.add(listener)
    }

    override fun onSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("Succeeded to initialize SDK")
        for (listener in initListeners) {
            listener.onInitializeSuccess()
        }
        initListeners.clear()
        isInitializing.set(false)
    }

    override fun onError(vungleError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose(
            "Failed to initialize SDK - " + vungleError.errorMessage
        )
        for (listener in initListeners) {
            listener.onInitializeError(vungleError.errorMessage)
        }
        initListeners.clear()
        isInitializing.set(false)
    }

    interface VungleInitializationListener {
        fun onInitializeSuccess()
        fun onInitializeError(error: String?)
    }

    companion object {
        @JvmField
        val instance = VungleInitializer()
    }
}

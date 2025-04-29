package com.ironsource.adapters.vungle

import com.ironsource.mediationsdk.logger.IronLog
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleError


class VungleInitListener : InitializationListener {

    override fun onSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("Succeeded to initialize SDK")
        // set init success
        VungleAdapter.mInitState = VungleAdapter.Companion.InitState.INIT_STATE_SUCCESS
        // iterate over all the adapter instances and report init success
        VungleAdapter.initCallbackListeners.forEach { adapter ->
            adapter.onNetworkInitCallbackSuccess()
        }
        VungleAdapter.initCallbackListeners.clear()
    }

    override fun onError(vungleError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to initialize SDK")
        // set init failed
        VungleAdapter.mInitState = VungleAdapter.Companion.InitState.INIT_STATE_FAILED
        // iterate over all the adapter instances and report init failed
        VungleAdapter.initCallbackListeners.forEach { adapter ->
            adapter.onNetworkInitCallbackFailed(vungleError.message)
        }
        VungleAdapter.initCallbackListeners.clear()
    }
}
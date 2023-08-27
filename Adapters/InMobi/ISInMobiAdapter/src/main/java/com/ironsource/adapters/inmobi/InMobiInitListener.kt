package com.ironsource.adapters.inmobi

import com.inmobi.sdk.SdkInitializationListener

class InMobiInitListener : SdkInitializationListener {

    /**
     * Called to notify that an init was completed .
     *
     * @param error Represents the [Error] error which was occurred during initialization
     */
    override fun onInitializationComplete(error: Error?) {
        error?.let {
            // set init failed
            InMobiAdapter.initState = InMobiAdapter.InitState.INIT_STATE_ERROR

            InMobiAdapter.initCallbackListeners.forEach { adapter ->
                adapter.onNetworkInitCallbackFailed(it.message)
            }
        }?: run{
            // set init success
            InMobiAdapter.initState = InMobiAdapter.InitState.INIT_STATE_SUCCESS

            InMobiAdapter.initCallbackListeners.forEach { adapter ->
                adapter.onNetworkInitCallbackSuccess()
            }

        }

        InMobiAdapter.initCallbackListeners.clear()
    }

}
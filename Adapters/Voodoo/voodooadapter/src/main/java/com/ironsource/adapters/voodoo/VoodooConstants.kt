package com.ironsource.adapters.voodoo

object VoodooConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Voodoo configuration keys
    const val PLACEMENT_ID_KEY = "placementId"

    // Bidding data keys
    const val TOKEN_KEY = "token"
    const val SDK_VERSION_KEY = "sdkVersion"

    // Log messages
    object Logs {
        const val PLACEMENT_ID = "placementId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val SDK_INIT_FAILED = "SDK initialization failed"
        const val INIT_SUCCESS = "Initialization success"
        const val SERVER_DATA_EMPTY = "serverData is empty"
        const val ADAPTER_UNAVAILABLE = "Network adapter is null"
        const val AD_NOT_AVAILABLE = "Ad not available"
        const val AD_NULL = "Ad is null"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW = "Failed to show, errorCode = %s, errorMessage = %s"
        const val TOKEN = "token = %s, sdkVersion = %s"
        const val TOKEN_INIT_NOT_STARTED = "returning null as token since init hasn't started - Voodoo"
    }
}

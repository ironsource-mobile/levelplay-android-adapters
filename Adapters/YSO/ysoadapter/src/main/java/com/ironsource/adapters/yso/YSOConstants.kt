package com.ironsource.adapters.yso

object YSOConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // YSO configuration keys
    const val PLACEMENT_KEY = "placementKey"

    // Bidding data keys
    const val TOKEN_KEY = "token"
    const val SDK_VERSION_KEY = "sdkVersion"

    // Log messages
    object Logs {
        const val PLACEMENT_KEY_LOG = "placementKey = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val SDK_INIT_FAILED = "SDK initialization failed"
        const val INIT_EXCEPTION = "YSO Network initialization failed with exception - %s"
        const val SERVER_DATA_EMPTY = "serverData is empty"
        const val ADAPTER_UNAVAILABLE = "Network adapter is null"
        const val AD_NOT_AVAILABLE = "Ad not available"
        const val AD_DISPLAY_FAILED = "Ad failed to display"
        const val FAILED_TO_LOAD = "Failed to load, error code = %s, error = %s"
        const val TOKEN = "token = %s, sdkVersion = %s"
        const val TOKEN_INIT_NOT_COMPLETED = "returning null as token since init isn't completed - YSO"
        const val TOKEN_EMPTY = "failed to receive token - returned null/empty token - YSO"
    }
}

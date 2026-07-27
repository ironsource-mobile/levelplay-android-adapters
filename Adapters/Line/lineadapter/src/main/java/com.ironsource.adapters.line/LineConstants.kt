package com.ironsource.adapters.line

object LineConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // Network configuration keys
    const val APP_ID_KEY: String = "appId"
    const val SLOT_ID_KEY: String = "slotId"
    const val SERVER_DATA: String = "serverData"

    // Bidding keys
    const val TOKEN_KEY: String = "token"

    // Logging Messages
    object Logs {
        // Init/adapter logs
        const val MISSING_PARAM: String = "Missing params - %s"
        const val APP_ID_AND_SLOT_ID: String = "appId = %s, slotId = %s"
        const val INIT_FAILED: String = "Line SDK init failed"
        const val AD_LOADER_NULL: String = "AdLoader is null"
        const val ADAPTER_UNAVAILABLE: String = "adapter is not available"

        // Ad loading/showing logs
        const val AD_NOT_AVAILABLE: String = "Ad is not available"
        const val FAILED_TO_LOAD: String = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW: String = "Failed to show, errorCode = %s, errorMessage = %s"

        // Bidding/token logs
        const val TOKEN: String = "token = %s"
        const val TOKEN_AD_LOADER_NULL: String = "failed to receive token - AdLoader is null - Line"
        const val TOKEN_FAILURE: String = "failed to receive token - Line, error = %s"
    }
}

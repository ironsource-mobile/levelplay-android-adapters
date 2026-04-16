package com.ironsource.adapters.verve

object VerveConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // Network configuration keys
    const val ZONE_ID_KEY = "zoneId"
    const val APP_TOKEN_KEY = "appToken"
    const val SERVER_DATA = "serverData"

    // Mediation name
    const val MEDIATION_NAME = "lp"

    // Metadata keys
    const val META_DATA_COPPA_KEY = "LevelPlay_ChildDirected"
    const val META_DATA_CCPA_YES_VALUE = "1YY-"
    const val META_DATA_CCPA_NO_VALUE = "1YN-"

    // Bidding key
    const val TOKEN_KEY = "token"

    // Error messages
    const val UNKNOWN_ERROR = "Unknown error"

    // Log messages
    object Logs {
        // Init/adapter logs
        const val INIT_FAILED = "Verve initialization failed"
        const val SDK_INIT_FAILED = "Verve SDK init failed"
        const val APP_TOKEN = "appToken = %s"
        const val ZONE_ID = "zoneId = %s"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val MISSING_PARAM = "Missing params - %s"

        // Legal/consent logs
        const val META_DATA_SET = "key = %s, value = %s"
        const val CCPA_VALUE = "ccpa value = %s"
        const val COPPA_VALUE = "isCoppa = %s"

        // Ad loading logs
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"

        // Banner-specific logs
        const val BANNER_SIZE_NULL = "banner size is null"

        // Bidding/token logs
        const val TOKEN = "token = %s"
        const val TOKEN_ERROR: String = "returning null as token since init isn't completed"
    }
}

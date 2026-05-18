package com.ironsource.adapters.bigo

object BigoConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // Network name
    const val MEDIATION_NAME: String = "LevelPlay"

    // Network configuration keys
    const val APP_ID_KEY: String = "appId"
    const val SLOT_ID_KEY: String = "slotId"

    // Metadata keys
    const val META_DATA_BIGO_COPPA_KEY: String = "LevelPlay_ChildDirected"

    // Bidding and config keys
    const val TOKEN_KEY: String = "token"

    // Mediation info JSON keys
    const val MEDIATION_INFO_MEDIATION_NAME: String = "mediationName"
    const val MEDIATION_INFO_MEDIATION_VERSION: String = "mediationVersion"
    const val MEDIATION_INFO_ADAPTER_VERSION: String = "adapterVersion"

    // Logging messages
    object Logs {
        // Init/adapter logs
        const val ADAPTER_UNAVAILABLE: String = "Network adapter is null"
        const val MISSING_PARAM: String = "Missing params - %s"
        const val APP_ID: String = "appId = %s"
        const val SLOT_ID: String = "slotId = %s"
        const val SDK_INITIALIZED: String = "BIGO SDK Initialized"

        // Legal/consent logs
        const val CONSENT: String = "consent = %s"
        const val COPPA: String = "isCoppa = %s"
        const val CCPA: String = "ccpa = %s"
        const val META_DATA_SET: String = "key = %s, value = %s"

        // Ad loading logs
        const val FAILED_TO_LOAD: String = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW: String = "Failed to show, errorCode = %s, errorMessage = %s"
        const val AD_NOT_AVAILABLE: String = "Ad is not available"
        const val SERVER_DATA_EMPTY: String = "serverData is empty"

        // Banner-specific logs
        const val UNSUPPORTED_BANNER_SIZE: String = "Unsupported banner size"

        // Bidding/token logs
        const val TOKEN: String = "token = %s"
        const val TOKEN_ERROR: String = "returning null as token since init isn't completed"
    }
}

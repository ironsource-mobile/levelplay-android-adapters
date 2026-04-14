package com.ironsource.adapters.mobilefuse

object MobileFuseConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Configuration keys
    const val PLACEMENT_ID_KEY = "placementId"

    // Mediation name
    const val MEDIATION_NAME = "unity_bidding"

    // Meta data keys
    const val META_DATA_COPPA_KEY = "LevelPlay_ChildDirected"

    // Privacy values
    const val DO_NOT_SELL_YES_VALUE = "1YY-"
    const val DO_NOT_SELL_NO_VALUE = "1YN-"
    const val DEFAULT_DO_NOT_SELL_VALUE = "1-"

    // Test mode
    const val TEST_MODE = false

    // Map keys
    const val TOKEN_KEY = "token"

    // Error messages
    const val UNKNOWN_ERROR = "Unknown error"
    const val SERVER_DATA_EMPTY = "serverData is empty"
    const val AD_NOT_AVAILABLE = "Ad not available"
    const val UNSUPPORTED_BANNER_SIZE = "Unsupported banner size"
    const val AD_EXPIRED = "Ad expired"
    const val BANNER_AD_NOT_FILLED = "Banner ad not filled"
    const val INTERSTITIAL_AD_NOT_FILLED = "Interstitial ad not filled"
    const val REWARDED_AD_NOT_FILLED = "Rewarded video ad not filled"

    // Logging messages
    object Logs {
        const val PLACEMENT_ID = "placementId = %s"
        const val INIT_FAILED = "MobileFuse sdk init failed"
        const val INIT_SUCCESS = "SDK initialized successfully"
        const val PLACEMENT_ID_EMPTY = "Missing params - placementId"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val TOKEN_ERROR = "returning null as token since init isn't completed"
        const val TOKEN = "token = %s"
        const val TOKEN_GENERATION_FAILED = "Token generation failed with error: %s"
        const val CONSENT = "consent = %s"
        const val CCPA = "ccpa = %s"
        const val COPPA = "isCoppa = %s"
        const val META_DATA = "key = %s, value = %s"
        const val AD_LOAD_ERROR = "Failed to load/show, errorCode = %s, errorMessage = %s"
    }
}

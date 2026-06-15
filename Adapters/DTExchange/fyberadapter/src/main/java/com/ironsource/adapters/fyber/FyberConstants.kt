package com.ironsource.adapters.fyber

object FyberConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME
    const val MEDIATION_NAME = "IronSource"

    // Network configuration keys
    const val APP_ID_KEY = "appId"
    const val SPOT_ID_KEY = "adSpotId"
    const val CREATIVE_ID_KEY = "creativeId"

    // Meta data keys
    const val META_DATA_DT_IS_CHILD_KEY = "DT_IsChild"
    const val META_DATA_DT_COPPA_KEY = "DT_COPPA"

    // Network data keys
    const val NETWORK_DATA_IS_CHILD_KEY = "AudienceIsChild"

    // Bidding keys
    const val TOKEN_KEY = "token"

    // CCPA US privacy strings
    const val CCPA_OPT_IN_STRING = "1YY-"
    const val CCPA_OPT_OUT_STRING = "1YN-"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"

    // Banner size dimensions (dp)
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LARGE_WIDTH = 728
    const val LARGE_HEIGHT = 90

    // Log messages
    object Logs {
        const val APP_ID = "appId = %s"
        const val SPOT_ID = "spotId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val INIT_SUCCESS = "Succeeded to initialize SDK"
        const val INIT_FAILED = "Failed to initialize SDK"
        const val SDK_INIT_FAILED = "Fyber SDK init failed"
        const val KEY_VALUE = "key = %s, value = %s"
        const val SET_USER_ID = "setUserId to %s"
        const val CONSENT = "consent = %s"
        const val CCPA = "ccpa = %s"
        const val COPPA = "calling currentAudienceAppliesToCoppa"
        const val IS_CHILD = "calling currentAudienceIsAChild"
        const val CREATIVE_ID = "creativeId = %s"
        const val TOKEN = "token = %s"
        const val TOKEN_NOT_READY = "returning null as token since init did not finish"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val UNKNOWN_ERROR = "Unknown error"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val UNSUPPORTED_BANNER_SIZE = "Unsupported banner size"
        const val SPOT_NOT_READY = "Spot is not ready"
        const val SHOW_FAILED = "Failed to show, errorMessage = %s"
    }
}

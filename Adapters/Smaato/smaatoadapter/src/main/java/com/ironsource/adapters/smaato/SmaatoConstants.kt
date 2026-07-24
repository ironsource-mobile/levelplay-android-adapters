package com.ironsource.adapters.smaato

object SmaatoConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // Network configuration keys
    const val PUBLISHER_ID_KEY: String = "publisherId"
    const val AD_SPACE_ID_KEY: String = "adspaceID"
    const val CREATIVE_ID_KEY: String = "creativeId"
    const val SERVER_DATA: String = "serverData"

    // Bidding
    const val TOKEN_KEY: String = "token"

    // Banner size keys
    const val BANNER_SIZE_BANNER: String = "BANNER"
    const val BANNER_SIZE_RECTANGLE: String = "RECTANGLE"
    const val BANNER_SIZE_SMART: String = "SMART"

    // Banner dimensions
    const val BANNER_WIDTH: Int = 320
    const val BANNER_HEIGHT: Int = 50
    const val RECTANGLE_WIDTH: Int = 300
    const val RECTANGLE_HEIGHT: Int = 250
    const val LEADERBOARD_WIDTH: Int = 728
    const val LEADERBOARD_HEIGHT: Int = 90

    // Logging Messages
    object Logs {
        const val MISSING_PARAM: String = "Missing params - %s"
        const val PUBLISHER_ID: String = "publisherId = %s"
        const val AD_SPACE_ID: String = "adspaceID = %s"
        const val SDK_INIT_FAILED: String = "Smaato sdk init failed"
        const val INIT_FAILED: String = "init failed - %s"
        const val CONSENT: String = "consent = %s"
        const val CREATIVE_ID: String = "creativeId = %s"
        const val AD_NOT_AVAILABLE: String = "Ad is not available"
        const val ADAPTER_UNAVAILABLE: String = "adapter is not available"
        const val UNSUPPORTED_BANNER_SIZE: String = "Unsupported banner size"
        const val INVALID_AD_REQUEST: String = "Error while creating Smaato AdRequestParams"
        const val INVALID_AD_REQUEST_ERROR: String = "Cannot create AdRequest, error: %s"
        const val TOKEN: String = "token = %s"
        const val TOKEN_ERROR: String = "returning null as token since init isn't completed"
        const val FAILED_TO_LOAD: String = "Failed to load, error = %s"
        const val FAILED_TO_SHOW: String = "Failed to show, error = %s"
    }
}

package com.ironsource.adapters.moloco

object MolocoConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Moloco configuration keys
    const val APP_KEY = "appKey"
    const val AD_UNIT_ID_KEY = "adUnitId"

    // Meta data keys
    const val META_DATA_MOLOCO_COPPA_KEY = "Moloco_COPPA"

    // Mediation info
    const val MEDIATION_NAME = "LevelPlay"

    // Bidding token key
    const val TOKEN_KEY = "token"

    // Error messages
    const val INVALID_CONFIGURATION = "invalid configuration"
    const val AD_NOT_AVAILABLE = "Ad not available"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LEADERBOARD"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"

    // Banner dimensions
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val LEADERBOARD_WIDTH = 728
    const val LEADERBOARD_HEIGHT = 90
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250

    // Log messages
    object Logs {
        const val APP_KEY_AND_AD_UNIT_ID = "appKey = %s, adUnitId = %s"
        const val AD_UNIT_ID_LOG = "adUnitId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val ADAPTER_UNAVAILABLE = "Adapter is not available"
        const val INIT_SUCCESS = "Initialization success %s"
        const val SDK_INIT_FAILED = "SDK initialization failed"
        const val SERVER_DATA_EMPTY = "serverData is empty"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW = "Failed to show, errorCode = %s, errorMessage = %s"
        const val INIT_NOT_COMPLETED = "returning null as token since init isn't completed"
        const val INIT_NOT_COMPLETED_TOKEN = "returning null as token since init isn't completed - Moloco"
        const val FAILED_TO_RECEIVE_TOKEN = "failed to receive token - Moloco, errorCode = %s, error = %s"
        const val CONSENT = "consent = %s"
        const val KEY_VALUE = "key = %s, value = %s"
        const val INIT_ERROR = "error code = %s, message = %s"
        const val VALUE = "value = %s"
        const val IS_COPPA = "isCoppa = %s"
        const val TOKEN = "token = %s"
        const val CREATE_AD_ERROR = "errorCode = %s, errorMessage = %s"
    }
}

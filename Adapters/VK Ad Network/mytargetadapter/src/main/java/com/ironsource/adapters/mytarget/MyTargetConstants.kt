package com.ironsource.adapters.mytarget

object MyTargetConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Network configuration keys
    const val SLOT_ID_KEY = "slotId"

    // Bidding keys
    const val TOKEN_KEY = "token"

    // Custom params
    const val CUSTOM_PARAM_MEDIATION_KEY = "mediation"
    const val IRONSOURCE_MEDIATION = "8"

    // Banner size descriptions
    const val BANNER_SIZE_DESCRIPTION = "BANNER"
    const val RECTANGLE_SIZE_DESCRIPTION = "RECTANGLE"
    const val SMART_SIZE_DESCRIPTION = "SMART"

    // Banner dimensions
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LARGE_WIDTH = 728
    const val LARGE_HEIGHT = 90

    // Log messages
    object Logs {
        const val SLOT_ID = "slotId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val CONSENT = "consent = %s"
        const val TOKEN = "token = %s"
        const val TOKEN_NOT_RECEIVED = "returning null as token since init hasn't started - MyTarget"
        const val ERROR_PARSING_PLACEMENT = "error parsing placement"
        const val SERVER_DATA_IS_EMPTY = "serverData is empty"
        const val UNSUPPORTED_BANNER_SIZE = "Unsupported banner size"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val AD_SHOW_FAILED = "Failed to show ad"
        const val FAILED_TO_LOAD = "Failed to load ad, errorCode = %s, errorMessage = %s"
    }
}

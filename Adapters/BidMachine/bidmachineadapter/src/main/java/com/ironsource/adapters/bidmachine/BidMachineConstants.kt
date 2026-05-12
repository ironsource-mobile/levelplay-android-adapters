package com.ironsource.adapters.bidmachine

object BidMachineConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Configuration keys
    const val SOURCE_ID_KEY = "sourceId"
    const val PLACEMENT_ID_KEY = "placementId"

    // Map keys
    const val BANNER_SIZE_KEY = "bannerSize"
    const val CREATIVE_ID_KEY = "creativeId"
    const val TOKEN_KEY = "token"

    // Banner sizes
    const val BANNER = "BANNER"
    const val LEADERBOARD = "LEADERBOARD"
    const val RECTANGLE = "RECTANGLE"
    const val SMART = "SMART"

    // Meta data keys
    const val META_DATA_COPPA_KEY = "BidMachine_COPPA"

    // CCPA values
    const val CCPA_NO_CONSENT_VALUE = "1YY-"
    const val CCPA_CONSENT_VALUE = "1YN-"

    // Error messages
    const val BANNER_SIZE_INVALID = "Banner size is invalid or not of type ISBannerSize"
    const val UNSUPPORTED_BANNER_SIZE = "Unsupported or null banner size"
    const val TOKEN_EMPTY = "failed to receive token - returned null/empty token"
    const val AD_EXPIRED = "Ad expired"
    const val AD_NOT_READY = "Ad is not ready"

    // Logging messages
    object Logs {
        const val SOURCE_ID = "sourceId = %s"
        const val PLACEMENT_ID = "placementId = %s"
        const val CREATIVE_ID = "creativeId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val TOKEN_ERROR = "returning null as token since init isn't completed"
        const val TOKEN = "token = %s"
        const val TOKEN_FAILED = "%s - BidMachine"
        const val CONSENT = "consent = %s"
        const val CCPA = "value = %s"
        const val COPPA = "isCoppa = %s"
        const val META_DATA = "key = %s, value = %s"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW = "Failed to show, errorCode = %s, errorMessage = %s"
    }
}

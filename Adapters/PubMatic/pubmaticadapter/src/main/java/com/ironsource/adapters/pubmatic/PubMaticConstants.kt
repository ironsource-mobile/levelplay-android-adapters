package com.ironsource.adapters.pubmatic

object PubMaticConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Network name
    const val NETWORK_NAME = "PubMatic"

    // Network configuration keys
    const val PUBLISHER_ID_KEY = "publisherId"
    const val PROFILE_ID_KEY = "profileId"
    const val AD_UNIT_ID_KEY = "adUnitId"
    const val BANNER_SIZE_KEY = "bannerSize"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"

    // Meta data keys
    const val META_DATA_PUBMATIC_COPPA_KEY = "LevelPlay_ChildDirected"

    // Bidding keys
    const val TOKEN_KEY = "token"

    // Logging Messages
    object Logs {
        // Init/adapter logs
        const val MISSING_PARAM = "Missing params - %s"
        const val PUBLISHER_ID_AND_PROFILE_ID = "publisherId = %s, profileId = %s"
        const val AD_UNIT_ID = "adUnitId = %s"
        const val INIT_SUCCESS = "Initialization success"
        const val INIT_FAILED = "PubMatic sdk init failed - errorMessage = %s, errorCode = %s"
        const val SDK_INIT_FAILED = "PubMatic sdk init failed"
        const val ADAPTER_UNAVAILABLE = "adapter is not available"

        // Legal/consent logs
        const val META_DATA_SET = "key = %s, value = %s"
        const val COPPA = "isCoppa = %s"

        // Ad loading logs
        const val SERVER_DATA_IS_NULL = "serverData is empty"
        const val AD_IS_NULL = "Ad is null"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val LOAD_FAILED = "Load failure - errorCode = %s, errorMessage = %s"
        const val SHOW_FAILED = "Show failure - errorCode = %s, errorMessage = %s"

        // Banner-specific logs
        const val UNSUPPORTED_BANNER_SIZE = "Unsupported or null banner size"
        const val BANNER_SIZE_NULL = "Banner size is null"
        const val CREATIVE_SIZE_UNAVAILABLE = "Creative size is unavailable"

        // Bidding/token logs
        const val TOKEN = "token = %s"
        const val TOKEN_ERROR = "returning null as token since init isn't completed"
    }
}

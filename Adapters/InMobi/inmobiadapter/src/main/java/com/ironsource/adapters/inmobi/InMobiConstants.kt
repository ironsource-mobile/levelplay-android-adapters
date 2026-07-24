package com.ironsource.adapters.inmobi

object InMobiConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // Empty string constant
    const val EMPTY_STRING = ""

    // Configuration keys
    const val ACCOUNT_ID_KEY = "accountId"
    const val PLACEMENT_ID_KEY = "placementId"

    // MetaData keys
    const val META_DATA_AGE_RESTRICTED_KEY = "inMobi_AgeRestricted"
    const val META_DATA_CHILD_DIRECTED_KEY = "LevelPlay_Child_Directed"

    // Token extras keys
    const val EXTRAS_TP_KEY = "tp"
    const val EXTRAS_TP_VALUE = "c_supersonic"
    const val EXTRAS_TP_VER_KEY = "tp-ver"
    const val INMOBI_DO_NOT_SELL_KEY = "do_not_sell"
    const val INMOBI_DO_NOT_SELL_VALUE_TRUE = "1"
    const val INMOBI_DO_NOT_SELL_VALUE_FALSE = "0"

    // Map keys
    const val CREATIVE_ID_KEY = "creativeId"
    const val TOKEN_KEY = "token"

    // Server data key
    const val SERVER_DATA_KEY = "serverData"

    // Banner size descriptions
    const val BANNER_SIZE_DESCRIPTION = "BANNER"
    const val LARGE_SIZE_DESCRIPTION = "LARGE"
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
        const val PLACEMENT_ID = "placementId = %s"
        const val ACCOUNT_ID_PLACEMENT_ID = "accountId = %s, placementId = %s"
        const val INIT_FAILED = "InMobi sdk init failed - %s"
        const val INIT_SUCCESS = "InMobi sdk init success"
        const val SDK_INIT_FAILED = "InMobi sdk init failed"
        const val MISSING_PARAM = "Missing params - %s"
        const val FAILED_TO_LOAD = "Failed to load ad with error code: %d and message: %s"
        const val AD_DISPLAY_FAILED = "Ad display failed"
        const val AD_NOT_READY_INTERSTITIAL = "InMobi interstitial ad is not ready"
        const val AD_NOT_READY_REWARDED_VIDEO = "InMobi rewarded video ad is not ready"
        const val TOKEN_NULL = "Returning null as token since init did not finish"
        const val TOKEN = "token = %s"
        const val CREATIVE_ID = "creativeId = %s"
        const val CONSENT = "consent = %s"
        const val AGE_RESTRICTED = "isAgeRestricted = %s"
        const val META_DATA_KEY_VALUE = "key = %s, value = %s"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val UNSUPPORTED_BANNER_SIZE = "Unsupported banner size"
    }
}

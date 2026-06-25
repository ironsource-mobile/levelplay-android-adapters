package com.ironsource.adapters.chartboost

object ChartboostConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Chartboost configuration keys
    const val APP_ID_KEY = "appID"
    const val APP_SIGNATURE_KEY = "appSignature"
    const val AD_LOCATION_KEY = "adLocation"

    // Extra data keys
    const val CREATIVE_ID_KEY = "creativeId"

    // Token key
    const val TOKEN_KEY = "token"

    // Mediation provider
    const val MEDIATION_NAME = "ironSource"

    // Meta data keys
    const val META_DATA_COPPA_KEY = "chartboost_coppa"

    // Consent values
    const val CONSENT_BEHAVIORAL = "BEHAVIORAL"
    const val CONSENT_NON_BEHAVIORAL = "NON_BEHAVIORAL"
    const val CCPA_OPT_OUT_SALE = "OPT_OUT_SALE"
    const val CCPA_OPT_IN_SALE = "OPT_IN_SALE"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"
    const val BANNER_SIZE_CUSTOM = "CUSTOM"

    // Banner dimensions
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LEADERBOARD_WIDTH = 728
    const val LEADERBOARD_HEIGHT = 90

    // Custom banner height bounds
    const val CUSTOM_BANNER_MIN_HEIGHT = 40
    const val CUSTOM_BANNER_MAX_HEIGHT = 60

    // Log messages
    object Logs {
        const val APP_ID_AND_SIGNATURE = "appId = %s, appSignature = %s"
        const val AD_LOCATION = "adLocation = %s"
        const val MISSING_PARAM = "Missing param - %s"
        const val INIT_FAILED = "Chartboost sdk init failed"
        const val INIT_NOT_COMPLETED = "returning null as token since init isn't completed"
        const val TOKEN = "token = %s"
        const val CONSENT = "consent = %s"
        const val CCPA = "value = %s"
        const val COPPA = "value = %s"
        const val KEY_VALUE = "key = %s, value = %s"
        const val CREATIVE_ID = "creativeId = %s"
        const val CACHE_ERROR = "error = %s"
        const val SHOW_ERROR = "error = %s"
        const val CLICK_ERROR = "clickError = %s"
        const val BANNER_VIEW_NULL = "bannerView is null"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val ADAPTER_UNAVAILABLE = "Network adapter is null"
        const val UNSUPPORTED_BANNER_SIZE = "Banner size is not supported - %s"
    }
}

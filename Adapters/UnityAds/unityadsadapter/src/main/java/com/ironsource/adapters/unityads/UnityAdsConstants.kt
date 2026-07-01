package com.ironsource.adapters.unityads

object UnityAdsConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME
    const val MEDIATION_NAME = "ironSource"

    // Network configuration keys
    const val SOURCE_ID_KEY = "sourceId"
    const val ZONE_ID_KEY = "zoneId"
    const val AD_UNIT_ID_KEY = "adUnitId"
    const val BANNER_SIZE_KEY = "bannerSize"

    // Init extras keys
    const val UADS_INIT_BLOB = "uads_init_blob"
    const val UADS_TRAITS = "traits"

    // Bidding keys
    const val TOKEN_KEY = "token"

    // Meta data flags
    const val CONSENT_GDPR = "gdpr.consent"
    const val CONSENT_CCPA = "privacy.consent"
    const val UNITYADS_COPPA = "user.nonBehavioral"
    const val UNITYADS_METADATA_COPPA_KEY = "unityads_coppa"

    // Network no-fill error code
    const val UNITYADS_NO_FILL_ERROR_CODE = 52100

    // Banner size keys
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"

    // Banner dimensions
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LEADERBOARD_WIDTH = 728
    const val LEADERBOARD_HEIGHT = 90

    // Log messages
    object Logs {
        const val SOURCE_ID = "sourceId = %s"
        const val ZONE_ID = "zoneId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val INIT_FAILED = "init failed, errorCode = %s, errorMessage = %s"
        const val SDK_INIT_FAILED = "UnityAds SDK init failed"
        const val META_DATA_SET = "key = %s, value = %s"
        const val CCPA = "ccpa = %s"
        const val COPPA = "coppa = %s"
        const val CONSENT = "consent = %s"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW = "Failed to show, errorCode = %s, errorMessage = %s"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val UNSUPPORTED_BANNER_SIZE = "Banner size is not supported"
        const val TOKEN = "token = %s"
        const val TOKEN_FAILURE = "failed to receive token - UnityAds"
    }
}

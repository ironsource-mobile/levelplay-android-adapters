package com.ironsource.adapters.mintegral

object MintegralConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Mintegral configuration keys
    const val APP_ID_KEY = "appId"
    const val APP_KEY = "appKey"
    const val PLACEMENT_ID_KEY = "placementId"
    const val UNIT_ID_KEY = "unitId"

    // Meta data keys
    const val META_DATA_MINTEGRAL_COPPA_KEY = "Mintegral_COPPA"

    // Mintegral error codes
    const val MINTEGRAL_NO_FILL_ERROR_CODE = 708

    // Creative ID key
    const val CREATIVE_ID_KEY = "creativeId"

    // Token key
    const val TOKEN_KEY = "token"

    // Channel code
    const val CHANNEL_CODE_METHOD = "b"
    const val CHANNEL_CODE_VALUE = "Y+H6DFttYrPQYcIb+F2F+F5/Hv=="

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"
    const val BANNER_SIZE_CUSTOM = "CUSTOM"

    // Banner dimensions
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val LARGE_WIDTH = 320
    const val LARGE_HEIGHT = 90
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LEADERBOARD_WIDTH = 728
    const val LEADERBOARD_HEIGHT = 90

    // Log messages
    object Logs {
        const val APP_ID_AND_APP_KEY = "appId = %s, appKey = %s"
        const val PLACEMENT_ID_AND_UNIT_ID = "placementId = %s, unitId = %s"
        const val PLACEMENT_ID = "placementId = %s"
        const val MISSING_PARAM = "Missing param - %s"
        const val ADAPTER_UNAVAILABLE = "Adapter is not available"
        const val SDK_INIT_FAILED = "SDK initialization failed"
        const val INIT_NOT_COMPLETED = "returning null as token since init isn't completed"
        const val INIT_NOT_COMPLETED_TOKEN = "returning null as token since init isn't completed - Mintegral"
        const val INIT_FAILED = "Mintegral SDK initialization failed - %s"
        const val CONSENT = "consent = %s"
        const val CONSENT_STATUS = "setConsentStatus consentStatus = %s"
        const val DO_NOT_TRACK_STATUS = "setDoNotTrackStatus with ccpa = %s"
        const val COPPA_VALUE = "set coppa value = %s"
        const val CHANNEL_CODE_ERROR = "Error setting channel code %s"
        const val LOAD_INTERSTITIAL = "load interstitial with placementId=%s unitId=%s serverData=%s"
        const val LOAD_REWARDED = "load rewarded video with placementId=%s unitId=%s serverData=%s"
        const val LOAD_BANNER = "load banner with size %sX%s placementId=%s unitId=%s serverData=%s"
        const val CREATIVE_ID = "creativeId = %s"
        const val ERROR_CODE_MSG = "errorCode = %s, errorMsg = %s"
        const val REWARDED_INFO = "rewarded: %s"
        const val KEY_VALUE = "key = %s, value = %s"
        const val TOKEN = "token = %s"
        const val AD_NOT_AVAILABLE = "Ad not available"
        const val DUPLICATE_PLACEMENT_RV = "Rewarded video load request skipped. A rewarded video ad with the same configuration is currently in use"
        const val DUPLICATE_PLACEMENT_IS = "Interstitial load request skipped. An interstitial ad with the same configuration is currently in use"
    }
}

package com.ironsource.adapters.aps

object APSConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // Network configuration keys
    const val UUID: String = "uuid"
    const val APS_FORMAT: String = "apsFormat"
    const val APS_FORMAT_VIDEO: String = "video"
    const val DIMENSIONS_KEY: String = "dimensions"
    const val DIMENSION_WIDTH_KEY: String = "w"
    const val DIMENSION_HEIGHT_KEY: String = "h"

    // Bidding data keys
    const val PRICE_POINT_ENCODED: String = "pricePointEncoded"
    const val WIDTH: String = "width"
    const val HEIGHT: String = "height"

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

    // Video dimensions
    const val VIDEO_PORTRAIT_WIDTH: Int = 320
    const val VIDEO_PORTRAIT_HEIGHT: Int = 480
    const val VIDEO_LANDSCAPE_WIDTH: Int = 480
    const val VIDEO_LANDSCAPE_HEIGHT: Int = 320

    // Custom target key for IAB U.S. Privacy String (CCPA)
    const val US_PRIVACY_KEY: String = "us_privacy"

    // IAB U.S. Privacy String values
    const val US_PRIVACY_NOT_APPLICABLE: String = "1---"
    const val US_PRIVACY_OPT_IN: String = "1YN-"
    const val US_PRIVACY_OPT_OUT: String = "1YY-"

    // Logging Messages
    object Logs {
        const val MISSING_APS_CONFIGURATION: String = "Missing APS LevelPlay Platform configuration: %s"
        const val INVALID_AD_SIZE: String = "Missing APS LevelPlay Platform configuration: ad size (%s, %s)"
        const val APS_NOT_INITIALIZED: String = "APS is not initialized"
        const val MISSING_AD_DATA: String = "Missing ad data"
        const val AD_RESPONSE_MISSING: String = "APS adResponse is missing"
        const val AD_NOT_AVAILABLE: String = "Ad is not available"
        const val ADAPTER_UNAVAILABLE: String = "adapter is not available"
        const val BANNER_LOAD_FAILED: String = "APS banner load failed"
        const val BANNER_VIEW_MISSING: String = "APS banner view is missing"
        const val UNSUPPORTED_BANNER_SIZE: String = "Unsupported banner size"
        const val INTERSTITIAL_LOAD_FAILED: String = "APS interstitial load failed"
        const val REWARDED_LOAD_FAILED: String = "APS rewarded video load failed"
        const val LOAD_EXCEPTION: String = "APSAdapter loadBanner exception %s"
        const val TOKEN_FAILURE: String = "APS failed to receive token - %s"
        const val US_PRIVACY: String = "us_privacy = %s"
        const val CCPA_OPT_OUT: String = "CCPA opt-out = %s"
        const val META_DATA_SET: String = "key = %s, value = %s"
        const val UUID_LOG: String = "uuid = %s"
        const val APS_MANUAL_LOADING_NOT_REQUIRED: String =
            "APS loading is handled by Mediation and does not require any additional implementation in your code."
    }
}

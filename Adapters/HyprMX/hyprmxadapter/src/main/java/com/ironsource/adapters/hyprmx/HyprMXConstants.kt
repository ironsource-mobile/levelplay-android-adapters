package com.ironsource.adapters.hyprmx

object HyprMXConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // HyprMX configuration keys
    const val PROPERTY_ID_KEY = "propertyId"
    const val DISTRIBUTOR_ID_KEY = "distributorId"

    // Meta data keys
    const val META_DATA_AGE_RESTRICTION_KEY = "HyprMX_ageRestricted"

    // Mediation provider
    const val MEDIATION_NAME = "ironsource"

    // Token key
    const val TOKEN_KEY = "token"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
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
        const val PROPERTY_ID = "propertyId = %s"
        const val DISTRIBUTOR_ID = "distributorId = %s"
        const val MISSING_PARAM = "Missing param - %s"
        const val ADAPTER_UNAVAILABLE = "Adapter is not available"
        const val INIT_FAILED = "HyprMX SDK failed to initiate"
        const val INIT_NOT_COMPLETED = "returning null as token since init isn't completed"
        const val INIT_NOT_COMPLETED_TOKEN = "returning null as token since init isn't completed - HyprMX"
        const val TOKEN = "token = %s"
        const val TOKEN_FAILED = "failed to receive token - returned null/empty token"
        const val TOKEN_FAILED_TOKEN = "failed to receive token - returned null/empty token - HyprMX"
        const val CONSENT = "consent = %s"
        const val AGE_RESTRICTED = "ageRestricted = %s"
        const val KEY_VALUE = "key = %s, value = %s"
        const val AD_NOT_AVAILABLE = "Ad not available"
        const val DISPLAY_ERROR = "onAdDisplayError %s"
        const val DUPLICATE_PLACEMENT_RV = "Rewarded video load request skipped. A rewarded video ad with the same configuration is currently in use"
        const val DUPLICATE_PLACEMENT_IS = "Interstitial load request skipped. An interstitial ad with the same configuration is currently in use"
        const val UNSUPPORTED_BANNER_SIZE = "Banner size is not supported - %s"
    }
}

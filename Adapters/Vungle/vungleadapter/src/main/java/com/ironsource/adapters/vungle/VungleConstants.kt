package com.ironsource.adapters.vungle

object VungleConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME
    const val MEDIATION_NAME = "ironsource"

    // Network configuration keys
    const val APP_ID_KEY = "AppID"
    const val PLACEMENT_ID_KEY = "PlacementId"
    const val CREATIVE_ID_KEY = "creativeId"

    // Meta data keys
    const val META_DATA_VUNGLE_COPPA_KEY = "Vungle_COPPA"

    // Publisher-controlled consent policy version
    const val META_DATA_VUNGLE_CONSENT_MESSAGE_VERSION = "1.0.0"

    // Bidding keys
    const val TOKEN_KEY = "token"
    const val SDK_VERSION_KEY = "sdkVersion"

    // Adapter ad format identifiers
    const val ADAPTER_FORMAT_REWARDED = "ISVungleRewardedVideo"
    const val ADAPTER_FORMAT_INTERSTITIAL = "ISVungleInterstitial"
    const val ADAPTER_FORMAT_BANNER = "ISVungleBanner"

    // Banner size keys
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"
    const val BANNER_SIZE_CUSTOM = "CUSTOM"

    // Log messages
    object Logs {
        const val APP_ID = "appId = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val INIT_SUCCESS = "Succeeded to initialize SDK"
        const val INIT_FAILED = "Failed to initialize SDK, errorCode = %s, errorMessage = %s"
        const val SDK_INIT_FAILED = "SDK initialization failed"
        const val META_DATA_SET = "key = %s, value = %s"
        const val CCPA = "ccpa = %s"
        const val COPPA = "coppa = %s"
        const val CONSENT = "gdpr = %s"
        const val PLACEMENT_ID = "placementId = %s"
        const val CREATIVE_ID = "creativeId = %s"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_PLAY = "Failed to play, errorCode = %s, errorMessage = %s"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val UNSUPPORTED_BANNER_SIZE = "Unsupported banner size"
        const val CUSTOM_SIZE_MISMATCH = "CustomBannerSizeMismatch:w-%s|h-%s"
        const val TOKEN = "sdkVersion = %s, token = %s"
        const val TOKEN_FAILURE = "failed to receive token - Vungle , error = %s"
    }
}

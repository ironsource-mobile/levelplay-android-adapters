package com.ironsource.adapters.applovin

object AppLovinConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Network configuration keys
    const val ZONE_ID_KEY = "zoneId"
    const val SDK_KEY = "sdkKey"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"
    const val BANNER_SIZE_CUSTOM = "CUSTOM"

    // Banner size dimensions (dp)
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LARGE_WIDTH = 728
    const val LARGE_HEIGHT = 90

    // Custom banner height bounds (dp)
    const val CUSTOM_BANNER_MIN_HEIGHT = 40
    const val CUSTOM_BANNER_MAX_HEIGHT = 60

    // Log messages
    object Logs {
        const val ZONE_ID = "zoneId = %s"
        const val SDK_KEY = "sdkKey = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val SDK_NOT_INITIALIZED = "AppLovin SDK instance is null"
        const val SET_USER_ID = "setUserIdentifier to %s"
        const val CONSENT = "consent = %s"
        const val CCPA = "value = %s"
        const val KEY_VALUE = "key = %s, value = %s"
        const val INIT_FAILED = "Failed to initialize SDK - %s"
        const val SDK_INIT_FAILED = "AppLovin sdk init failed"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val DUPLICATE_INTERSTITIAL = "Interstitial load request skipped. An interstitial ad with the same configuration is currently in use"
        const val DUPLICATE_REWARDED = "Rewarded video load request skipped. A rewarded video ad with the same configuration is currently in use"
        const val UNSUPPORTED_BANNER_SIZE = "Unsupported banner size"
        const val LOAD_FAILED = "Failed to load, errorCode = %s, errorMessage = %s"

        // AppLovin SDK error descriptions
        const val ERROR_SDK_DISABLED = "The SDK is currently disabled."
        const val ERROR_FETCH_AD_TIMEOUT = "The network conditions prevented the SDK from receiving an ad."
        const val ERROR_NO_NETWORK = "The device had no network connectivity at the time of an ad request, either due to airplane mode or no service."
        const val ERROR_NO_FILL = "No ads are currently eligible for your device."
        const val ERROR_UNABLE_TO_RENDER_AD = "There has been a failure to render an ad on screen."
        const val ERROR_INVALID_ZONE = "The zone provided is invalid; the zone needs to be added to your AppLovin account or may still be propagating to our servers."
        const val ERROR_INVALID_AD_TOKEN = "The provided ad token is invalid; ad token must be returned from AppLovin S2S integration."
        const val ERROR_UNSPECIFIED = "The system is in unexpected state."
        const val ERROR_INCENTIVIZED_NO_AD_PRELOADED = "The developer called for a rewarded video before one was available."
        const val ERROR_INCENTIVIZED_UNKNOWN_SERVER_ERROR = "An unknown server-side error occurred."
        const val ERROR_INCENTIVIZED_SERVER_TIMEOUT = "A reward validation requested timed out (usually due to poor connectivity)."
        const val ERROR_INCENTIVIZED_USER_CLOSED_VIDEO = "The user exited out of the ad early. You may or may not wish to grant a reward depending on your preference."
        const val ERROR_INVALID_RESPONSE = "The AppLovin servers have returned an invalid response"
        const val ERROR_INVALID_URL = "A postback URL you attempted to dispatch was empty or nil."
        const val ERROR_UNABLE_TO_PRECACHE_RESOURCES = "An attempt to cache a resource to the filesystem failed; the device may be out of space."
        const val ERROR_UNABLE_TO_PRECACHE_IMAGE_RESOURCES = "An attempt to cache an image resource to the filesystem failed; the device may be out of space."
        const val ERROR_UNABLE_TO_PRECACHE_VIDEO_RESOURCES = "An attempt to cache a video resource to the filesystem failed; the device may be out of space."
        const val ERROR_INVALID_BODY = "The request body sent to the server was malformed or invalid."
        const val ERROR_UNABLE_TO_PRECACHE_HTML_RESOURCES = "An attempt to cache HTML content failed; the device may be out of space or the resource was malformed."
        const val ERROR_UNKNOWN = "Unknown error"
    }
}

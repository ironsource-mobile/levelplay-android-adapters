package com.ironsource.adapters.facebook

object FacebookConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME
    const val MEDIATION_NAME = "ironSource"

    // Network configuration keys
    const val PLACEMENT_ID_KEY = "placementId"
    const val ALL_PLACEMENT_IDS_KEY = "placementIds"

    // Meta data flags
    const val FACEBOOK_INTERSTITIAL_CACHE_FLAG = "facebook_is_cacheflag"
    const val META_INTERSTITIAL_CACHE_FLAG = "meta_is_cacheflag"
    const val META_MIXED_AUDIENCE = "meta_mixed_audience"

    // Network data flags
    const val NETWORK_DATA_CACHE_FLAG = "CacheFlag"

    // Bidding keys
    const val TOKEN_KEY = "token"

    // Placement ids separator
    const val PLACEMENT_IDS_SEPARATOR = ","

    // Banner size keys
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"
    const val BANNER_SIZE_CUSTOM = "CUSTOM"

    // Banner sizes
    const val BANNER_WIDTH = 320
    const val RECTANGLE_WIDTH = 300
    const val LARGE_WIDTH = 728
    const val BANNER_HEIGHT = 50
    const val LARGE_HEIGHT = 90
    const val RECTANGLE_HEIGHT = 250

    // Log messages
    object Logs {
        const val INIT = "Initialize Meta with placement ids = %s"
        const val MISSING_PARAM = "Missing params - %s"
        const val PLACEMENT_ID = "placementId = %s"
        const val INIT_SUCCESS = "Succeeded to initialize SDK"
        const val INIT_FAILED = "Failed to initialize SDK - %s"
        const val SDK_INIT_FAILED = "Meta SDK init failed"
        const val MEDIATION_SERVICE_INFO = "mediationServiceInfo = %s"
        const val TEST_MODE = "setTestMode = %s"
        const val META_DATA = "key = %s, value = %s"
        const val CACHE_FLAGS = "key = %s, values = %s"
        const val CACHE_FLAG_VALUE = "flag for value %s is %s"
        const val CACHE_FLAGS_ALL = "all flags = %s"
        const val CACHE_FLAG_UNKNOWN = "flag is unknown or all, set all as default"
        const val MIXED_AUDIENCE = "isMixedAudience = %s"
        const val TOKEN = "token = %s"
        const val TOKEN_INIT_FAILED = "returning failure as token since init failed"
        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val AD_NOT_AVAILABLE = "ad is not available"
        const val LOADING_INTERSTITIAL = "loading placementId = %s with facebook cache flags = %s"
        const val UNSUPPORTED_BANNER_SIZE = "size not supported, size = %s"
        const val LOAD_EXCEPTION = "load failed with an exception = %s"
        const val SHOW_EXCEPTION = "show failed with an exception = %s"
        const val FAILED_TO_LOAD = "Failed to load, errorCode = %s, errorMessage = %s"
        const val NATIVE_AD_TYPE_MISMATCH = "Expected an instance of NativeAd, received %s"
        const val NATIVE_AD_VIEW_IS_NULL = "nativeAdView is null"
        const val NATIVE_ICON_DOWNLOAD_FAILED = "error while trying to download the native ad icon resource - %s"
        const val NATIVE_TITLE = "headline = %s"
        const val NATIVE_ADVERTISER = "advertiser = %s"
        const val NATIVE_BODY = "body = %s"
        const val NATIVE_CTA = "cta = %s"
        const val NATIVE_ICON_URI = "icon uri = %s"
    }
}

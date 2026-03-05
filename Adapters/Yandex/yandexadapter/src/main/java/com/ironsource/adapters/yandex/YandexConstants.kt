package com.ironsource.adapters.yandex

object YandexConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME
    const val MEDIATION_NAME: String = "ironsource"

    // Network configuration keys
    const val APP_ID_KEY: String = "appId"
    const val AD_UNIT_ID_KEY: String = "adUnitId"
    const val CREATIVE_ID_KEY: String = "creativeId"

    // Meta data keys
    const val META_DATA_YANDEX_COPPA_KEY: String = "Yandex_COPPA"

    // Bidding and config keys
    const val TOKEN_KEY: String = "token"
    const val ADAPTER_VERSION_KEY: String = "adapter_version"
    const val ADAPTER_NETWORK_NAME_KEY: String = "adapter_network_name"
    const val ADAPTER_NETWORK_SDK_VERSION_KEY: String = "adapter_network_sdk_version"

    // Banner size keys
    const val BANNER_SIZE_KEY: String = "bannerSize"
    const val BANNER_SIZE_BANNER: String = "BANNER"
    const val BANNER_SIZE_LARGE: String = "LARGE"
    const val BANNER_SIZE_RECTANGLE: String = "RECTANGLE"
    const val BANNER_SIZE_SMART: String = "SMART"
    const val BANNER_SIZE_CUSTOM: String = "CUSTOM"

    // Logging Messages
    object Logs {
        // Init/adapter logs
        const val AD_NOT_AVAILABLE: String = "Ad is not available"
        const val ADAPTER_UNAVAILABLE: String = "adapter is not available"
        const val APP_ID: String = "appId = %s"
        const val AD_UNIT_ID: String = "adUnitId = %s"

        // Legal/consent logs
        const val CONSENT: String = "consent = %s"
        const val COPPA: String = "isCoppa = %s"
        const val META_DATA_SET: String = "key = %s, value = %s"

        // Ad loading logs
        const val FAILED_TO_LOAD: String = "Failed to load, errorCode = %s, errorMessage = %s"
        const val FAILED_TO_SHOW: String = "Failed to show, errorMessage = %s"
        const val SERVER_DATA_EMPTY: String = "serverData is empty"
        const val AD_UNIT_ID_EMPTY: String = "Missing params - adUnitId"
        const val CREATIVE_ID: String = "creativeId = %s"

        // Banner-specific logs
        const val BANNER_SIZE_IS_NULL: String = "banner size is null, banner has been destroyed"
        const val BANNER_SIZE_NULL_LOG: String = "Banner size is null"
        const val UNSUPPORTED_BANNER_SIZE: String = "Unsupported banner size"

        // Bidding/token logs
        const val TOKEN: String = "token = %s"
        const val TOKEN_ERROR: String = "returning null as token since init isn't completed"
        const val TOKEN_FAILURE: String = "failed to receive token - Yandex %s"
    }
}

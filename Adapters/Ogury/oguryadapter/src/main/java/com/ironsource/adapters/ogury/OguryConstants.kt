package com.ironsource.adapters.ogury

object OguryConstants {

    // Adapter version and mediation
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME
    const val MEDIATION_NAME = "Unity LevelPlay"

    // Ogury configuration keys
    const val ASSET_KEY = "assetKey"
    const val AD_UNIT_ID_KEY = "adUnitId"

    // Meta data keys
    const val META_DATA_OGURY_COPPA_KEY = "LevelPlay_ChildDirected"

    // Token key
    const val TOKEN_KEY = "token"

    // Log messages
    object Logs {
        const val ASSET_KEY = "assetKey = %s"
        const val AD_UNIT_ID = "adUnitId = %s"
        const val MISSING_PARAM = "Missing param - %s"
        const val INIT_FAILED = "Ogury sdk init failed"
        const val INIT_FAILED_WITH_ERROR = "Ogury sdk init failed, errorCode = %s, errorMessage = %s"
        const val ADAPTER_UNAVAILABLE = "Adapter is not available"
        const val AD_NOT_AVAILABLE = "Ad is not available"
        const val SERVER_DATA_EMPTY = "serverData is empty"
        const val LOAD_FAILED = "Failed to load, errorCode = %s, errorMessage = %s"
        const val SHOW_FAILED = "Failed to show, errorCode = %s, errorMessage = %s"
        const val KEY_VALUE = "key = %s, value = %s"
        const val COPPA = "isCoppa = %s"
        const val TOKEN = "token = %s"
        const val TOKEN_FAILURE = "failed to receive token - Ogury, errorCode = %s, error = %s"
        const val UNSUPPORTED_BANNER_SIZE = "Banner size is not supported"
    }
}

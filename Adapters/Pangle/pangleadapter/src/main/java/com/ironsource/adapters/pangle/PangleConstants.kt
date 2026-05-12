package com.ironsource.adapters.pangle

object PangleConstants {

    // Adapter version
    const val ADAPTER_VERSION = BuildConfig.VERSION_NAME

    // Configuration keys
    const val SLOT_ID_KEY = "slotID"
    const val APP_ID_KEY = "appID"

    // Mediation info keys
    const val NAME_KEY = "name"
    const val VALUE_KEY = "value"
    const val MEDIATION_NAME_KEY = "mediation"
    const val MEDIATION_NAME = "Ironsource"
    const val ADAPTER_VERSION_KEY = "adapter_version"
    const val LEVELPLAY_ADXID = "33"

    // Bidding keys
    const val TOKEN_KEY = "token"

    // Pangle error codes
    const val PANGLE_NO_FILL_ERROR_CODE = 20001
    const val PANGLE_NOT_ALLOW_CHILD_ERROR_CODE = 20002

    // Pangle COPPA/Child-related constants
    const val PANGLE_CHILD_DIRECTED_TYPE_CHILD = 1
    const val PANGLE_CHILD_DIRECTED_TYPE_NON_CHILD = 0
    const val PANGLE_CHILD_DIRECTED_TYPE_DEFAULT = -1
    const val META_DATA_PANGLE_COPPA_KEY = "Pangle_COPPA"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"

    // Banner dimensions in dp
    const val BANNER_WIDTH = 320
    const val BANNER_HEIGHT = 50
    const val RECTANGLE_WIDTH = 300
    const val RECTANGLE_HEIGHT = 250
    const val LARGE_WIDTH = 728
    const val LARGE_HEIGHT = 90

    // GDPR/CCPA consent values
    const val CONSENT_TYPE_CONSENT_STRING = "PAG_PA_CONSENT_TYPE_CONSENT"
    const val CONSENT_TYPE_NO_CONSENT_STRING = "PAG_PA_CONSENT_TYPE_NO_CONSENT"
    const val CHILD_DIRECTED_TYPE_CHILD_STRING = "PANGLE_CHILD_DIRECTED_TYPE_CHILD"
    const val CHILD_DIRECTED_TYPE_NON_CHILD_STRING = "PANGLE_CHILD_DIRECTED_TYPE_NON_CHILD"
    const val CHILD_DIRECTED_TYPE_DEFAULT_STRING = "PANGLE_CHILD_DIRECTED_TYPE_DEFAULT"

    // Error messages
    const val PANGLE_NOT_ALLOW_CHILD_ERROR_MSG = "Pangle_COPPA indicates the user is a child. Pangle SDK V71 or higher does not support child users."
    const val PANGLE_GDPR_CONSENT_MSG = "Manual configuration of GDPR information is no longer supported. Pangle will automatically read the settings from the CMP in the consent function."
    const val NO_AD_TO_SHOW = "No ad to show"

    object Logs {
        const val APP_ID = "appId = %s"
        const val SLOT_ID = "slotId = %s"
        const val APP_ID_AND_SLOT_ID = "appId = %s, slotId = %s"
        const val NETWORK_ADAPTER_IS_NULL = "network adapter is null"
        const val INIT_FAILED = "Pangle SDK initialization failed - error code = %s, message = %s"
        const val SDK_INIT_FAILED = "Pangle SDK init failed"
        const val MISSING_PARAM = "Missing param - %s"
        const val CHILD_USER_ERROR = "Child user - %s"
        const val TOKEN_COLLECTED = "token = %s"
        const val TOKEN_FAILED = "Failed to receive token - Pangle %s"
        const val TOKEN_ERROR: String = "returning null as token since init isn't completed"
        const val META_DATA_VALUE = "key = %s, value = %s"
        const val CCPA_VALUE = "ccpaValue = %s"
        const val COPPA_VALUE = "coppaValue = %s"
        const val MEDIATION_INFO = "mediationInfo = %s"
        const val MEDIATION_INFO_ERROR = "Error while creating mediation info object - %s"
        const val FAILED_TO_LOAD = "Failed to load - error code = %s, message = %s"
        const val FAILED_TO_REWARD = "Failed to reward - error code = %s, message = %s"
    }
}

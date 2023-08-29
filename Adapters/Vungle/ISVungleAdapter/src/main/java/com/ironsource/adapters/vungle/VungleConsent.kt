package com.ironsource.adapters.vungle

import com.ironsource.mediationsdk.logger.IronLog
import com.vungle.ads.VunglePrivacySettings.setCCPAStatus
import com.vungle.ads.VunglePrivacySettings.setCOPPAStatus
import com.vungle.ads.VunglePrivacySettings.setPublishAndroidId

object VungleConsent {
    @JvmStatic
    fun setCCPAValue(ccpa: Boolean) {
        // The Vungle CCPA API expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        val optIn = !ccpa
        IronLog.ADAPTER_API.verbose("ccpa = $optIn")
        setCCPAStatus(optIn)
    }

    @JvmStatic
    fun setCOPPAValue(isUserCoppa: Boolean) {
        IronLog.ADAPTER_API.verbose("coppa = $isUserCoppa")
        setCOPPAStatus(isUserCoppa)
    }

    @JvmStatic
    fun setGDPRStatus(consent: Boolean, consentMessageVersion: String?) {
        IronLog.ADAPTER_API.verbose("gdpr = $consent")
        setGDPRStatus(consent, consentMessageVersion)
    }

    @JvmStatic
    fun publishAndroidId(publish: Boolean) {
        IronLog.ADAPTER_API.verbose("publish androidId = $publish")
        setPublishAndroidId(publish)
    }
}

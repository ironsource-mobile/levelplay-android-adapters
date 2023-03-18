package com.ironsource.adapters.vungle;

import com.ironsource.mediationsdk.logger.IronLog;
import com.vungle.ads.VunglePrivacySettings;

public class VungleConsent {

    public static void setCCPAValue(final boolean ccpa) {
        // The Vungle CCPA API expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        boolean optIn = !ccpa;
        IronLog.ADAPTER_API.verbose("ccpa = " + optIn);
        VunglePrivacySettings.setCCPAStatus(optIn);
    }

    public static void setCOPPAValue(final boolean isUserCoppa) {
        IronLog.ADAPTER_API.verbose("coppa = " + isUserCoppa);
        VunglePrivacySettings.setCOPPAStatus(isUserCoppa);
    }

    public static void setGDPRStatus(boolean consent, String consentMessageVersion) {
        IronLog.ADAPTER_API.verbose("gdpr = " + consent);
        VunglePrivacySettings.setGDPRStatus(consent, consentMessageVersion);
    }

    public static void publishAndroidId(boolean publish) {
        IronLog.ADAPTER_API.verbose("publish androidId = " + publish);
        VunglePrivacySettings.setPublishAndroidId(publish);
    }

}

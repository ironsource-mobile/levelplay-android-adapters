package com.ironsource.adapters.vungle;

import com.ironsource.mediationsdk.logger.IronLog;
import com.vungle.ads.VungleAds;
import com.vungle.ads.internal.privacy.PrivacyConsent;

public class VungleConsent {

    public static void setCCPAValue(final boolean ccpa) {
        // The Vungle CCPA API expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        boolean optIn = !ccpa;
        PrivacyConsent status = optIn ? PrivacyConsent.OPT_IN : PrivacyConsent.OPT_OUT;
        IronLog.ADAPTER_API.verbose("key = Vungle.Consent" + ", value = " + status.name());
        VungleAds.updateCCPAStatus(status);
    }

    public static void setCOPPAValue(final boolean isUserCoppa) {
        IronLog.ADAPTER_API.verbose("coppa = " + isUserCoppa);
        VungleAds.updateUserCoppaStatus(isUserCoppa);
    }

    public static void updateConsentStatus(boolean consent, String consentMessageVersion) {
        IronLog.ADAPTER_API.verbose("consent = " + consent);
        PrivacyConsent status = consent ? PrivacyConsent.OPT_IN : PrivacyConsent.OPT_OUT;
        VungleAds.updateGDPRConsent(status, consentMessageVersion);
    }

}

# Changelog

**Important!** As part of supporting Applovin SDK 12.4.0 and above, we have updated the Applovin initialization API to AppLovinSdkInitializationConfiguration. This update is effective for all adapters version 4.3.43 and later. If your AndroidManifest.xml contains an entry for applovin.sdk.key, it must be removed.

## Version 5.3.0
* Supporting SDK version 13.6.0
* **Requirements:** Minimum SDK Version 23

## Version 5.2.0
* Supporting SDK version 13.5.1

## Version 5.1.0
* Supporting SDK version 13.5.0

## Version 5.0.0
* The adapter is compatible with LevelPlay 9.0.0 and above

---

### ⚠️ Breaking Change – Minimum SDK Requirement

**All adapter releases listed **above** this section support LevelPlay SDK 9.0.0 and above only.**  
Adapter releases listed **below this section** continue to support earlier LevelPlay SDK versions.

---

## Version 4.3.55
* Supporting SDK version 13.4.0
* Improved performance when loading Rewarded Videos

## Version 4.3.54
* Supporting SDK version 13.3.1

## Version 4.3.53
* Supporting SDK version 13.2.0

## Version 4.3.51
* Improvement for Rewarded Videos loading time

## Version 4.3.50
* Supporting SDK version 13.1.0

## Version 4.3.49
* Improvement for release memory logic
* Required for Ad Units APIs
* **Important!** This adapter is compatible with ironSource SDK 8.6.1 and above

## Version 4.3.48
* Supporting SDK version 13.0.1
* Add protection for null banner

## Version 4.3.47
* Supporting SDK version 13.0.0
* COPPA support was removed from AppLovin SDK
* **Important!** This adapter is compatible with ironSource SDK 8.4.0 and above

## Version 4.3.46
* Supporting SDK version 12.6.1

## Version 4.3.45
* Supporting SDK version 12.6.0

## Version 4.3.44
* Supporting SDK version 12.5.0

## Version 4.3.43
* Supporting SDK version 12.4.3
* Support AppLovin AppLovinSdkInitializationConfiguration API
* **Important!** If your AndroidManifest.xml contains an entry for "applovin.sdk.key", you must remove it. [Learn more here](https://developers.applovin.com/en/android/overview/new-sdk-initialization-api/#create-the-sdk-initialization-configuration)

## Version 4.3.42
* Supporting SDK version 12.3.1

## Version 4.3.41
* Supporting SDK version 12.1.0

## Version 4.3.40
* Supporting SDK version 11.11.3
* Add banner clicks reporting support

## Version 4.3.39
* Supporting SDK version 11.10.1
* Support sharing app's activity on show API

## Version 4.3.38
* Supporting SDK version 11.10.0

## Version 4.3.37
* Supporting SDK version 11.7.1
* **Important!** This adapter is compatible with ironSource SDK 7.3.0 and above

## Version 4.3.36
* Supporting SDK version 11.6.1
* Adding indication for LevelPlay Mediation

## Version 4.3.35
* Supporting SDK version 11.5.5

## Version 4.3.34
* Supporting SDK version 11.5.0

## Version 4.3.33
* Supporting SDK version 11.4.4

## Version 4.3.32
* Supporting SDK version 11.4.2
* Improve banner memory release (Android)

## Version 4.3.31
* Supporting SDK version 11.3.3

## Version 4.3.30
* Supporting SDK version 11.2.2
* Resolve potential memory leaks

## Version 4.3.29
* Supporting SDK version 10.3.5
* Adding MREC (banner) support

## Version 4.3.28
* **Important!** This adapter is compatible with ironSource SDK 7.1.10 and above

## Version 4.3.27
* Supporting SDK version 10.3.2

## Version 4.3.26
* Supporting SDK version 10.3.1

## Version 4.3.25
* Supporting SDK version 10.3.0

## Version 4.3.24
* Supporting SDK version 10.1.2

## Version 4.3.23
* Supporting SDK version 10.0.1
* Migrate over to Maven Central

## Version 4.3.22
* Supporting SDK version 9.15.3

## Version 4.3.21
* Supporting SDK version 9.14.12

## Version 4.3.20
* Supporting SDK version 9.14.6

## Version 4.3.19
* Supporting SDK version 9.14.4

## Version 4.3.18
* Supporting SDK version 9.14.3
* Removing banner MREC support

## Version 4.3.17
* Supporting SDK version 9.14.1

## Version 4.3.16
* Supporting SDK version 9.13.4

## Version 4.3.15
* Supporting SDK version 9.13.1
* Allow to report age-restriction using SetMetaData API. This will replace the setAge reporting, in future SDK Versions.

## Version 4.3.14
* Supporting SDK version 9.13.0

## Version 4.3.13
* Adapter released as AAR
* **Important!** This adapter is compatible with ironSource SDK 6.17.0 and above

## Version 4.3.12
* Fix bugs related to Interstitial callback response, after ad show

## Version 4.3.11
* Supporting SDK version 9.12.7
* Crash fix for calculateBannerSize

## Version 4.3.10
* Supporting SDK version 9.12.5

## Version 4.3.9
* Supporting SDK version 9.11.6

## Version 4.3.8
* Supporting SDK version 9.11.2
* Add CCPA support
* **Important!** This adapter is compatible with ironSource SDK 6.14.0.1 and above

## Version 4.3.7
* Supporting SDK version 9.11.1
* Add SetAge support

## Version 4.3.6
* Supporting SDK version 9.9.2
* Bug fixes

## Version 4.3.5
* Supporting SDK version 9.9.1

## Version 4.3.4
* Supporting SDK version 9.2.1

## Version 4.3.3
* Interstitial availability reporting minor bug fix

## Version 4.3.0
* Adjustments to support latest banner enhancements

## Version 4.2.0
* Adjustments to support latest banner enhancements
* Bug fix for GDPR consent setup

## Version 4.1.5
* Supporting SDK version 8.0.1
* Consent API integration
* isInterstitialAvailable() may result false in case ad available

## Version 4.0.5
* Supporting SDK version 7.8.2
* Supporting Multiple Instances

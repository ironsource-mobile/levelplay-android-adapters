# Changelog

## Version 5.0.0
* Supporting SDK version 11.1.0+
* The adapter is compatible with LevelPlay 9.0.0 and above
* Simplified Ad Loading: LevelPlay now automatically handles APS ad loading; manual load calls are no longer required. (APS SDK still needs independent initialization).
* MADU Support: This version introduces support for Multiple Ad Units.
* Migration: Refer to the [APS migration guide](https://docs.unity.com/en-us/grow/levelplay/sdk/android/networks/guides/aps-migration) for details.
* **Requirements:** Minimum Kotlin version 2.1.0

---

### ⚠️ Breaking Change – Minimum SDK Requirement

**All adapter releases listed **above** this section support LevelPlay SDK 9.0.0 and above only.**  
Adapter releases listed **below this section** continue to support earlier LevelPlay SDK versions.

---

## Version 4.3.17
* Supporting SDK version 11.0.0+

## Version 4.3.16
* Supporting SDK version 10.0.0+

## Version 4.3.15
* Add missing onVideoCompleted callback

## Version 4.3.14
* Bug fix related to Ad Open callback for banners
* Add crash protections
* **Important!** This adapter is compatible with ironSource SDK 8.4.0 and above

## Version 4.3.13
* Supporting SDK version 9.10.0+

## Version 4.3.12
* Supporting SDK version 9.9.3+
* Crash fix related to onAdError callback

## Version 4.3.11
* Add crash protections when banner size is null

## Version 4.3.10
* Add crash protections

## Version 4.3.9
* Supporting SDK version 9.8.0 – 9.8.8
* Supporting rewarded video ad unit
* **Important!** As of adapter 4.3.9 the APS Android SDK will be supported by ranges. [Learn more here](https://developers.is.com/ironsource-mobile/android/aps-integration-guide/#step-1)

## Version 4.3.8
* Bug fix for APS adapter 4.3.7
* Supporting SDK version 9.7.1

## Version 4.3.7
* Supporting SDK version 9.7.0
* **Important!** This adapter is compatible with ironSource SDK 7.3.0 and above

## Version 4.3.6
* Supporting SDK version 9.6.2

## Version 4.3.5
* Supporting SDK version 9.6.0

## Version 4.3.4
* Supporting SDK version 9.5.5
* Supporting Interstitial Videos

## Version 4.3.3
* Supporting SDK version 9.4.3

## Version 4.3.2
* Crash fix – when loading ads on background threads

## Version 4.3.1
* Supporting SDK version 9.3.0

## Version 4.3.0
* Supporting SDK version 9.2.2
* Required ironSource SDK version 7.1.13.1
* Supporting ad units Banner and Interstitial
* Min API 20

# Changelog

## Version 5.1.0
* Supporting Meta Audience Network SDK version 6.21.0

## Version 5.0.0
* The adapter is compatible with LevelPlay 9.0.0 and above

---

### ⚠️ Breaking Change – Minimum SDK Requirement

**All adapter releases listed **above** this section support LevelPlay SDK 9.0.0 and above only.**  
Adapter releases listed **below this section** continue to support earlier LevelPlay SDK versions.

---

## Version 4.3.51
* Added protection to prevent potential crashes while showing Rewarded Video ads

## Version 4.3.50
* Supporting Meta Audience Network SDK version 6.20.0

## Version 4.3.49
* Supporting Meta Audience Network SDK version 6.19.0
* Add nullability protection when loading banners from UI thread

## Version 4.3.48
* Supporting Meta Audience Network SDK version 6.18.0
* **Important!** This adapter is compatible with ironSource SDK 8.4.0 and above

## Version 4.3.47
* Fix a potential bug related reloading banners that results in an error log and invisible banners
* **Important!** This adapter is compatible with ironSource SDK 8.2.0 and above

## Version 4.3.46
* Supporting Meta Audience Network SDK version 6.17.0
* Bug fix for native ads

## Version 4.3.45
* Supporting Meta Audience Network SDK version 6.16.0

## Version 4.3.44
* Supporting Meta Audience Network SDK version 6.15.0

## Version 4.3.43
* Support Native ads
* Compatible with 7.3.1

## Version 4.3.42
* Support sharing app's activity on show API

## Version 4.3.41
* Supporting Meta Audience Network SDK version 6.14.0

## Version 4.3.40
* Supporting Meta Audience Network SDK version 6.13.7

## Version 4.3.39
* **Important!** This adapter is compatible with ironSource SDK 7.3.0 and above

## Version 4.3.38
* Supporting Meta Audience Network SDK version 6.12.0

## Version 4.3.37
* Supporting adapter as open-source

## Version 4.3.36
* Supporting Meta Audience Network SDK version 6.11.0

## Version 4.3.35
* Supporting Meta Audience Network SDK version 6.10.0
* New SetMetaData flag was added **Meta_Mixed_Audience,** to support Meta Mixed Audience reporting
* Adding support for CacheFlag functionality using Meta_Is_CacheFlag

## Version 4.3.34
* Fix potential crash

## Version 4.3.33
* Performance enhancements

## Version 4.3.32
* Performance enhancements

## Version 4.3.31
* Supporting FAN SDK version 6.8.0

## Version 4.3.30
* **Important!** This adapter is compatible with ironSource SDK 7.1.10 and above

## Version 4.3.29
* Supporting FAN SDK version 6.6.0

## Version 4.3.28
* Supporting FAN SDK version 6.5.1

## Version 4.3.27
* Supporting FAN SDK version 6.5.0

## Version 4.3.26
* Supporting FAN SDK version 6.4.0

## Version 4.3.25
* Supporting FAN SDK version 6.3.0

## Version 4.3.24
* Supporting FAN SDK version 6.2.1

## Version 4.3.23
* Supporting FAN SDK version 6.2.0

## Version 4.3.22
* Supporting FAN SDK version 6.1.0

## Version 4.3.21
* Support FAN In App Bidding for banners
* Support multiple value assignment for CacheFlag
* **Important!** This adapter is compatible with ironSource SDK 7.0.2 and above

## Version 4.3.20
* Supporting FAN SDK version 6.0.0

## Version 4.3.19
* Supporting FAN SDK version 5.11.0

## Version 4.3.18
* Supporting FAN SDK version 5.10.1
* CCPA support, by setting FAN **Limited Data Use** flag before initializing ironSource Mediation. [Read more about FAN implementation here](https://developers.facebook.com/docs/marketing-apis/data-processing-options#audience-network-sdk)

## Version 4.3.17
* Supporting FAN SDK version 5.9.1

## Version 4.3.16
* Adapter released as AAR
* **Important!** This adapter is compatible with ironSource SDK 6.17.0 and above

## Version 4.3.15
* Add CacheFlag support for Interstitial ads

## Version 4.3.14
* Supporting FAN SDK version 5.9.0

## Version 4.3.13
* Init process improvements

## Version 4.3.12
* Supporting FAN SDK version 5.8.0

## Version 4.3.11
* Supporting SDK version 5.7.1
* Meta activities should be removed from integration: RemoteANActivity, AdsProcessPriorityService, AdsMessengerService

## Version 4.3.10
* Supporting SDK version 5.7.0

## Version 4.3.9
* Supporting SDK version 5.6.1

## Version 4.3.8
* Support Meta In App Bidding, Rewarded Video and Interstitial

## Version 4.3.7
* Bug fix – prevent unnecessary ad requests

## Version 4.3.6
* Supporting SDK version 5.6.0

## Version 4.3.5
* Supporting SDK version 5.5.0

## Version 4.3.4
* Synchronization ANR bug fix

## Version 4.3.3
* Supporting SDK version 5.3.1
* **Please note –** Due to the use of .aar files. Starting this adapter (4.3.3), Meta adapter will not hold the network's SDK within the adapter and needs to be added separately as an .aar file. If you are updating Meta adapter to this or higher version, please make sure you also include Meta Audience Network SDK.

## Version 4.3.2
* Supporting SDK version 5.1.1
* Synchronization minor bug fix
* Supporting FAN's isInAdsProcess

## Version 4.3.1
* Supporting SDK version 5.1.0
* Network Security Settings (please watch Step 10. in Meta integration guide)

## Version 4.3.0
* Adjustments to support latest banner enhancements

## Version 4.2.0
* Adjustments to support latest banner enhancements

## Version 4.1.5
* Supporting SDK version 4.28.1

## Version 4.1.4
* Supporting SDK version 4.28.0

## Version 4.1.3
* Adjust Banner mediation core logic – banner refresh rate is now enforced by the mediation layer (Developers should turn off refresh rate at the networks dashboard).

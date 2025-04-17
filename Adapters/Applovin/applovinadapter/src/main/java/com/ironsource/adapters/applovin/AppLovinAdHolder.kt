package com.ironsource.adapters.applovin
import java.util.*

internal class AppLovinAdHolder<V> {
  private val appLovinAds: MutableMap<AppLovinAdapter, V> = Collections.synchronizedMap(WeakHashMap())

  fun storeAd(key: AppLovinAdapter, value: V) {
    appLovinAds[key] = value
  }

  fun retrieveAd(key: AppLovinAdapter): V? {
    return appLovinAds[key]
  }

  fun removeAd(key: AppLovinAdapter) {
    appLovinAds.remove(key)
  }

  fun getAdapters(): Set<AppLovinAdapter> {
    return appLovinAds.keys
  }

}
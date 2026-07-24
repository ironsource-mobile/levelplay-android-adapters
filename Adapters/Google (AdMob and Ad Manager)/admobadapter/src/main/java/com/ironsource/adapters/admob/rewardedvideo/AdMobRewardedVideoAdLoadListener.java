package com.ironsource.adapters.admob.rewardedvideo;

import android.text.TextUtils;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

// AdMob rewarded video load listener
public class AdMobRewardedVideoAdLoadListener implements AdLoadCallback<RewardedAd> {

    // data
    private WeakReference<AdMobRewardedVideoAdapter> mRewardedVideoAdapter;
    private RewardedVideoSmashListener mListener;
    private String mAdUnitId;

    AdMobRewardedVideoAdLoadListener(AdMobRewardedVideoAdapter adapter, String adUnitId, RewardedVideoSmashListener listener) {
        mRewardedVideoAdapter = new WeakReference<>(adapter);
        mAdUnitId = adUnitId;
        mListener = listener;
    }

    //rewarded video ad was loaded
    @Override
    public void onAdLoaded(@NotNull RewardedAd rewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mRewardedVideoAdapter == null || mRewardedVideoAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mRewardedVideoAdapter.get().onRewardedVideoAdLoaded(mAdUnitId, rewardedAd);

        ResponseInfo responseInfo = rewardedAd.getResponseInfo();
        String creativeId = (responseInfo != null) ? responseInfo.getResponseId() : null;

        if (TextUtils.isEmpty(creativeId)) {
          mListener.onRewardedVideoAvailabilityChanged(true);
        } else {
          Map<String, Object> extraData = new HashMap<>();
          extraData.put(AdMobAdapter.CREATIVE_ID_KEY, creativeId);
          IronLog.ADAPTER_CALLBACK.verbose(AdMobAdapter.CREATIVE_ID_KEY + " = " + creativeId);
          mListener.onRewardedVideoAvailabilityChanged(true, extraData);
        }
    }

    //rewarded video ad failed to load
    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        int errorCode;
        String adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " )";

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        //check if error is no fill error
        if (AdMobAdapter.isNoFillError(loadAdError.getCode())) {
            errorCode = IronSourceError.ERROR_RV_LOAD_NO_FILL;
            adapterError = "No Fill";
        } else {
            errorCode = IronSourceError.ERROR_CODE_GENERIC;
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        mListener.onRewardedVideoAvailabilityChanged(false);
        mListener.onRewardedVideoLoadFailed(new IronSourceError(errorCode, adapterError));
    }
}

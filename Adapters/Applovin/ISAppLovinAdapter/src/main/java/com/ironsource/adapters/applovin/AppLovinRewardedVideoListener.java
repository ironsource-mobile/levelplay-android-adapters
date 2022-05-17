package com.ironsource.adapters.applovin;

import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import java.lang.ref.WeakReference;
import java.util.Map;

public class AppLovinRewardedVideoListener implements AppLovinAdLoadListener, AppLovinAdClickListener, AppLovinAdDisplayListener, AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener {

    private String mZoneId;
    private RewardedVideoSmashListener mListener;
    private WeakReference<AppLovinAdapter> mAdapter;

    public AppLovinRewardedVideoListener(AppLovinAdapter adapter, RewardedVideoSmashListener listener, String zoneId) {
        mAdapter = new WeakReference<>(adapter);
        mZoneId = zoneId;
        mListener = listener;
    }

    /**
     This method is called when a new ad has been received.
     This method is invoked on the UI thread.
     Params:
     ad – Newly received ad. Guaranteed not to be null.
     **/
    @Override
    public void adReceived(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAvailabilityChanged(true);
    }

    /**
     This method is called when an ad could not be retrieved from the server.
     This method is invoked on the UI thread
     Common error codes are: 204 -- no ad is available 5xx -- internal server error negative number -- internal errors
     Params:
     errorCode – An error code representing the reason the ad failed to load. Common error codes are defined in AppLovinErrorCodes.
     **/
    @Override
    public void failedToReceiveAd(int errorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId + ", errorCode = " + errorCode);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        // create error
        int adapterErrorCode = errorCode == AppLovinErrorCodes.NO_FILL ? IronSourceError.ERROR_RV_LOAD_NO_FILL : errorCode;
        IronSourceError error = new IronSourceError(adapterErrorCode, mAdapter.get().getErrorString(errorCode));

        // return fail
        mListener.onRewardedVideoAvailabilityChanged(false);
        mListener.onRewardedVideoLoadFailed(error);
    }

    /**
     This method is invoked when an ad is displayed inside of the com.applovin.adview.AppLovinAdView or com.applovin.adview.AppLovinInterstitialAdDialog.
     This method is invoked on the main UI thread.
     Params:
     ad – Ad that was just displayed. Guaranteed not to be null.
     **/
    @Override
    public void adDisplayed(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdOpened();
    }

    /**
     Triggered when a video begins playing in a video advertisement.
     If your app plays other videos or music, please pause them upon receiving this callback.
     Params:
     ad – Ad in which playback began.
     **/
    @Override
    public void videoPlaybackBegan(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdStarted();
    }

    /**
     This method is invoked when the ad is clicked.
     This method is invoked on the main UI thread.
     Params:
     ad – Ad that was just clicked. Guaranteed not to be null
     **/
    @Override
    public void adClicked(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClicked();
    }

    /**
     Triggered when a video stops playing in a video advertisement.
     If your app was playing music when the video began, this is a good opportunity to resume it. If your app was playing video or otherwise requires user interaction, you probably want to use adHidden() in AppLovinAdDisplayListener instead.
     Params:
     ad – Ad in which playback ended.
     percentViewed – Percent of the video which the user watched.
     fullyWatched – Whether or not the video was watched to, or very near, completion. This parameter is a simply convenience and is computed as (percentViewed >= 95)
     **/
    @Override
    public void videoPlaybackEnded(AppLovinAd appLovinAd, double percentViewed, boolean isFullyWatched) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId + ", isFullyWatched = " + isFullyWatched);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdEnded();
        if (isFullyWatched) {
            mListener.onRewardedVideoAdRewarded();
        }
    }

    /**
     This method is invoked when an ad is displayed inside of the com.applovin.adview.AppLovinAdView or com.applovin.adview.AppLovinInterstitialAdDialog. This occurs when it is explicitly closed (in the case of INTERSTITIALs).
     This method is invoked on the main UI thread.
     Params:
     ad – Ad that was just hidden. Guaranteed not to be null
     **/
    @Override
    public void adHidden(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAdClosed();
    }

    /**
     If you are using reward validation for incentivized videos, this method will be invoked if we contacted AppLovin successfully. This means that we will be pinging your currency endpoint shortly, so you may wish to refresh the user's coins from your server.
     Params:
     ad – An ad for which a validation request was submitted.
     response – Any response extras sent down by AppLovin. Typically, this includes the keys "currency" and "amount", which point to Strings containing the name and amount of the virtual currency to be awarded.
     **/
    @Override
    public void userRewardVerified(AppLovinAd appLovinAd, Map<String, String> map) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);
    }

    /**
     This method will be invoked if the user has already received the maximum allocated rewards for the day.
     Params:
     ad – An ad for which a validation request was submitted.
     response – Any response extras sent down by AppLovin.
     **/
    @Override
    public void userOverQuota(AppLovinAd appLovinAd, Map<String, String> map) {
        // User reached maximum reward as provided in AppLovin's dashboard
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);
    }

    /**
     This method will be invoked if the user's reward was detected as fraudulent and not awarded.
     Params:
     ad – An ad for which a validation request was submitted.
     response – Any response extras sent down by AppLovin.
     **/
    @Override
    public void userRewardRejected(AppLovinAd appLovinAd, Map<String, String> map) {
        // Reward was marked as a fraud
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);
    }

    /**
     This method will be invoked if we were unable to contact AppLovin, therefore no ping will be heading to your server.
     Params:
     ad – An ad for which a validation request was submitted.
     errorCode – An error code indicating the cause of failure. Common error codes are defined in AppLovinErrorCodes.
     **/
    @Override
    public void validationRequestFailed(AppLovinAd appLovinAd, int errorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId + ", errorCode = " + errorCode);
    }
}

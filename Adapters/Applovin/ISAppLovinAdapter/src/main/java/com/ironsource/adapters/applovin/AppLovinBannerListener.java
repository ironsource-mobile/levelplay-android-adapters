package com.ironsource.adapters.applovin;

import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;

import java.lang.ref.WeakReference;

public class AppLovinBannerListener implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdViewEventListener {

    private String mZoneId;
    private BannerSmashListener mListener;
    private WeakReference<AppLovinAdapter> mAdapter;
    private FrameLayout.LayoutParams mBannerLayout;

    public AppLovinBannerListener(AppLovinAdapter adapter, BannerSmashListener listener, String zoneId, FrameLayout.LayoutParams bannerLayout) {
        mAdapter = new WeakReference<>(adapter);
        mZoneId = zoneId;
        mListener = listener;
        mBannerLayout = bannerLayout;
    }

    /**
     This method is called when a new ad has been received.
     This method is invoked on the UI thread.
     Params:
     ad – Newly received ad. Guaranteed not to be null.
     **/
    @Override
    public void adReceived(final AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        if (mBannerLayout == null) {
            IronLog.INTERNAL.verbose("banner layout is null");
            return;
        }

        final AppLovinAdView adView = mAdapter.get().mZoneIdToBannerAd.get(mZoneId);
        if (adView == null) {
            IronLog.INTERNAL.verbose("adView is null");
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                adView.renderAd(appLovinAd);
                mListener.onBannerAdLoaded(adView, mBannerLayout);
                mListener.onBannerAdShown();
            }
        });
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
        int adapterErrorCode = errorCode == AppLovinErrorCodes.NO_FILL ? IronSourceError.ERROR_BN_LOAD_NO_FILL : errorCode;
        IronSourceError error = new IronSourceError(adapterErrorCode, mAdapter.get().getErrorString(errorCode));

        // return fail
        mListener.onBannerAdLoadFailed(error);
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
    }

    /**
     This method is invoked if the ad view fails to display an ad.
     This method is invoked on the main UI thread.
     Params:
     ad – Ad for which the ad view failed to display for.
     adView – Ad view which failed to display the ad.
     code – Error code specifying the reason why the ad view failed to display ad.
     **/
    @Override
    public void adFailedToDisplay(AppLovinAd appLovinAd, AppLovinAdView appLovinAdView, AppLovinAdViewDisplayErrorCode appLovinAdViewDisplayErrorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);
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

        mListener.onBannerAdClicked();
    }

    /**
     This method is invoked after the ad view presents fullscreen content.
     This method is invoked on the main UI thread.
     Params:
     ad – Ad that the ad view presented fullscreen content for.
     adView – Ad view that presented fullscreen content.
     **/
    @Override
    public void adOpenedFullscreen(AppLovinAd appLovinAd, AppLovinAdView appLovinAdView) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenPresented();
    }

    
    /**
     This method is invoked after the fullscreen content is dismissed.
     This method is invoked on the main UI thread.
     Params:
     ad – Ad for which the fullscreen content is dismissed for.
     adView – Ad view for which the fullscreen content it presented is dismissed for
     **/
    @Override
    public void adClosedFullscreen(AppLovinAd appLovinAd, AppLovinAdView appLovinAdView) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenDismissed();
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
    }

    /**
     This method is invoked before the user is taken out of the application after a click.
     This method is invoked on the main UI thread.
     Params:
     ad – Ad for which the user will be taken out of the application for.
     adView – Ad view containing the ad for which the user will be taken out of the application for.
     **/
    @Override
    public void adLeftApplication(AppLovinAd appLovinAd, AppLovinAdView appLovinAdView) {
        IronLog.ADAPTER_CALLBACK.verbose("mZoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdLeftApplication();
    }
}

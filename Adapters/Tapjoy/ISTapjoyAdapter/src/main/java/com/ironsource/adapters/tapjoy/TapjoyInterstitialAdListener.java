package com.ironsource.adapters.tapjoy;

import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;

import java.lang.ref.WeakReference;

public class TapjoyInterstitialAdListener implements TJPlacementListener, TJPlacementVideoListener {

    // data
    private String mPlacementName;
    private InterstitialSmashListener mListener;
    private WeakReference<TapjoyAdapter> mAdapter;
    private final int LOAD_ERROR_NOT_AVAILABLE = 5001;

    TapjoyInterstitialAdListener(TapjoyAdapter adapter, InterstitialSmashListener listener, String placementName) {
        mAdapter = new WeakReference<>(adapter);
        mPlacementName = placementName;
        mListener = listener;
    }

    @Override
    public void onContentReady(TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mListener.onInterstitialAdReady();
        mAdapter.get().mInterstitialPlacementToIsReady.put(mPlacementName, true);
    }

    @Override
    public void onRequestSuccess(final TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        // This just means the SDK has made contact with Tapjoy's servers. It does not necessarily mean that any content is available.
        TapjoyAdapter.postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!tjPlacement.isContentAvailable()) {
                    mListener.onInterstitialAdLoadFailed(new IronSourceError(LOAD_ERROR_NOT_AVAILABLE, "No content available"));
                }
            }
        });
    }

    @Override
    public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName + ", errorMessage = " + tjError.toString());

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdLoadFailed(new IronSourceError(tjError.code, tjError.message));
    }

    @Override
    public void onVideoStart(TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
        mListener.onInterstitialAdShowSucceeded();
    }

    @Override
    public void onClick(TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    @Override
    public void onContentDismiss(TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClosed();
    }

    @Override
    public void onContentShow(TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);
    }

    @Override
    public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String productId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);
    }

    @Override
    public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String itemId, int quantity) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);
    }

    @Override
    public void onVideoError(TJPlacement tjPlacement, String errorMessage) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);
    }

    @Override
    public void onVideoComplete(TJPlacement tjPlacement) {
        IronLog.ADAPTER_CALLBACK.verbose("placementName = " + mPlacementName);
    }
}

package com.ironsource.adapters.unityads;

import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;

import java.lang.ref.WeakReference;

final class UnityAdsInterstitialListener implements IUnityAdsLoadListener, IUnityAdsShowListener {

    // data
    private String mPlacementId;
    private InterstitialSmashListener mListener;
    private WeakReference<UnityAdsAdapter> mAdapter;

    UnityAdsInterstitialListener(UnityAdsAdapter adapter, InterstitialSmashListener listener, String placementId) {
        mAdapter = new WeakReference<>(adapter);
        mPlacementId = placementId;
        mListener = listener;
    }

    //region IUnityAdsLoadListener Callbacks
    /**
     * Callback triggered when a load request has successfully filled the specified placementId with an ad that is ready to show.
     *
     * @param placementId placementId, as defined in Unity Ads admin tools
     */
    @Override
    public void onUnityAdsAdLoaded(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mInterstitialAdsAvailability.put(mPlacementId, true);
        mListener.onInterstitialAdReady();
    }

    /**
     * Callback triggered when load request has failed to load an ad for a requested placementId.
     *
     * @param placementId placementId, as defined in Unity Ads admin tools
     * @param error Error code related to the error  See: {@link com.unity3d.ads.UnityAds.UnityAdsLoadError}
     * @param message Human-readable error message
     */
    @Override
    public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        IronSourceError ironSourceError;

        if (error != null) {
            int errorCode = (error == UnityAds.UnityAdsLoadError.NO_FILL) ? IronSourceError.ERROR_IS_LOAD_NO_FILL : mAdapter.get().getUnityAdsLoadErrorCode(error);
            ironSourceError = new IronSourceError(errorCode, message);
        } else {
            ironSourceError = ErrorBuilder.buildLoadFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT,mAdapter.get().getProviderName(), message);
        }

        IronLog.ADAPTER_CALLBACK.error("placementId = " + mPlacementId + " ironSourceError = " + ironSourceError);

        mListener.onInterstitialAdLoadFailed(ironSourceError);
        mAdapter.get().mInterstitialAdsAvailability.put(mPlacementId, false);
    }
    //endregion

    //region IUnityAdsShowListener Callbacks
    /**
     * Callback which notifies that UnityAds has started to show ad with a specific placementId.
     *
     * @param placementId placementId, as defined in Unity Ads admin tools
     */
    @Override
    public void onUnityAdsShowStart(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdOpened();
        mListener.onInterstitialAdShowSucceeded();
    }

    /**
     * Callback which notifies that UnityAds has failed to show a specific placementId with an error message and error category.
     *
     * @param placementId placementId, as defined in Unity Ads admin tools
     * @param error If UnityAdsShowError.NOT_INITIALIZED, the show operation failed due to SDK is not initialized
     *              If UnityAdsShowError.NOT_READY, the show operation failed due to placement not ready to show
     *              If UnityAdsShowError.VIDEO_PLAYER_ERROR, the show operation failed due to an error in playing the video
     *              If UnityAdsShowError.INVALID_ARGUMENT, the show operation failed due to invalid placement ID
     *              If UnityAdsShowError.NO_CONNECTION, the show operation failed due to no internet connection
     *              If UnityAdsShowError.ALREADY_SHOWING, the show operation failed due to ad is already being shown
     *              If UnityAdsShowError.INTERNAL_ERROR, the show operation failed due to environment or internal services
     *              If UnityAdsShowError.TIMEOUT, the show operation failed due to timeout
     * @param message Human-readable error message
     */
    @Override
    public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        IronSourceError ironSourceError;

        if (error != null) {
            ironSourceError = new IronSourceError(mAdapter.get().getUnityAdsShowErrorCode(error), message);
        } else {
            ironSourceError = ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, message);
        }

        IronLog.ADAPTER_CALLBACK.error("placementId = " + mPlacementId + "ironSourceError = " + ironSourceError);
        mListener.onInterstitialAdShowFailed(ironSourceError);
    }

    /**
     * Callback which notifies that UnityAds has received a click while showing ad for a specific placementId.
     *
     * @param placementId placementId, as defined in Unity Ads admin tools
     */
    @Override
    public void onUnityAdsShowClick(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    /**
     * Callback triggered when the show operation completes successfully for a placementId.
     *
     * @param placementId placementId, as defined in Unity Ads admin tools
     * @param completionState If UnityAdsShowCompletionState.SKIPPED, the show operation completed after the user skipped the video playback
     *              If UnityAdsShowCompletionState.COMPLETED, the show operation completed after the user allowed the video to play to completion before dismissing the ad
     */
    @Override
    public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState completionState) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + mPlacementId + " completionState: " + completionState);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        switch (completionState) {
            case SKIPPED:
            case COMPLETED:
                mListener.onInterstitialAdClosed();
                break;
            default:
                break;
        }
    }
}

package com.rnopentok;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

public class RNOpenTokSubscriberView
    extends RNOpenTokView
    implements SubscriberKit.SubscriberListener,
               SubscriberKit.VideoListener {

    private Subscriber mSubscriber;
    private Boolean mAudioEnabled;
    private Boolean mVideoEnabled;

    public RNOpenTokSubscriberView(ThemedReactContext context) {
        super(context);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        RNOpenTokSessionManager.getSessionManager().setSubscriberListener(mSessionId, this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RNOpenTokSessionManager.getSessionManager().removeSubscriberListener(mSessionId);
        cleanUpSubscriber();
    }

    public void setAudio(Boolean enabled) {
        if (mSubscriber != null) {
            mSubscriber.setSubscribeToAudio(enabled);
        }

        mAudioEnabled = enabled;
    }

    public void setVideo(Boolean enabled) {
        if (mSubscriber != null) {
            mSubscriber.setSubscribeToVideo(enabled);
        }

        mVideoEnabled = enabled;
    }

    public void startStreaming(Session session, Stream stream) {
        mSubscriber = new Subscriber(getContext(), stream);
        mSubscriber.setSubscriberListener(this);
        mSubscriber.setVideoListener(this);
        mSubscriber.setSubscribeToAudio(mAudioEnabled);
        mSubscriber.setSubscribeToVideo(mVideoEnabled);

        mSubscriber
            .getRenderer()
            .setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

        session.subscribe(mSubscriber);

        sendEvent(Events.EVENT_SUBSCRIBE_START, Arguments.createMap());
        attachSubscriberView();
    }

    public void stopStreaming() {
        sendEvent(Events.EVENT_SUBSCRIBE_STOP, Arguments.createMap());
        cleanUpSubscriber();
    }

    private void attachSubscriberView() {
        addView(mSubscriber.getView(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        requestLayout();
    }

    private void cleanUpSubscriber() {
        if (mSubscriber != null) {
            removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }
    }

    private void onVideoChanged(Boolean video) {
        WritableMap payload = Arguments.createMap();
        payload.putBoolean("video", video);
        sendEvent(Events.EVENT_SUBSCRIBE_VIDEO_CHANGED, payload);
    }

    /** SubscriberListener **/

    @Override
    public void onConnected(SubscriberKit subscriberKit) {}

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {}

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        WritableMap payload = Arguments.createMap();
        payload.putString("connectionId", opentokError.toString());

        sendEvent(Events.EVENT_SUBSCRIBE_ERROR, payload);
    }

    /** VideoListener **/

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String reason) {
      if (reason.equals(SubscriberKit.VIDEO_REASON_PUBLISH_VIDEO)) {
        onVideoChanged(false);
      }
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String reason) {
      if (reason.equals(SubscriberKit.VIDEO_REASON_PUBLISH_VIDEO)) {
        onVideoChanged(true);
      }
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {}

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriber) {}

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriber) {}
}

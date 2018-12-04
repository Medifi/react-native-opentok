package com.rnopentok;

import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;

public class RNOpenTokSessionWrapper {

    private Session mSession;
    private RNOpenTokPublisherView mPublisher;
    private RNOpenTokSubscriberView mSubscriber;
    private Stream mCurrentStream;
    private Boolean isConnected = false;

    public RNOpenTokSessionWrapper (Session session) {
        this.mSession = session;
    }

    public Session getSession () {
        return this.mSession;
    }

    public void setPublisherView (RNOpenTokPublisherView publisher) {
        this.mPublisher = publisher;
        tryPublishing();
    }

    public void removePublisherView () {
        this.mPublisher = null;
    }

    public void setSubscriberView (RNOpenTokSubscriberView subscriber) {
        this.mSubscriber = subscriber;
        trySubscribing();
    }

    public void removeSubscriberView () {
        this.mSubscriber = null;
    }

    public void connect (String token) {
        this.mSession.connect(token);
    }

    public void disconnect () {
        this.mSession.disconnect();
    }

    public void sendSignal (String type, String data) {
        this.mSession.sendSignal(type, data);
    }

    private void tryPublishing () {
        if (this.isConnected && this.mPublisher != null) {
            this.mPublisher.startPublishing(this.mSession);
        }
    }

    private void trySubscribing() {
        if (this.mCurrentStream != null && this.mSubscriber != null) {
            this.mSubscriber.startStreaming(this.mSession, this.mCurrentStream);
        }
    }

    private void tryUnsubscribing() {
        if (this.mSubscriber != null) {
            this.mSubscriber.stopStreaming();
        }
    }

    public void onConnected(Session session) {
        this.isConnected = true;
        tryPublishing();
    }

    public void onDisconnected(Session session) {
        this.isConnected = false;
    }

    public void onStreamReceived(Session session, Stream stream) {
        this.mCurrentStream = stream;
        trySubscribing();
    }

    public void onStreamDropped(Session session, Stream stream) {
        this.mCurrentStream = null;
        tryUnsubscribing();
    }

    public void onError(Session session, OpentokError opentokError) {}

    public void onReconnected(Session session) {
        onConnected(session);
    }

    public void onReconnecting(Session session) {}
}

package com.rnopentok;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;

import com.facebook.react.bridge.ReactApplicationContext;
import com.opentok.android.Stream;

import java.util.HashMap;

public class RNOpenTokSessionManager
    implements Session.ConnectionListener,
               Session.SessionListener,
               Session.SignalListener,
               Session.ReconnectionListener,
               Session.ArchiveListener {

    private static RNOpenTokSessionManager instance;
    private ReactApplicationContext mContext;
    private String mApiKey;
    private HashMap<String, RNOpenTokSessionWrapper> mSessions;

    private RNOpenTokSessionManager(ReactApplicationContext context, String apiKey) {
        this.mSessions = new HashMap<String, RNOpenTokSessionWrapper>();
        this.mApiKey = apiKey;
        this.mContext = context;
    }

    public static RNOpenTokSessionManager initSessionManager (ReactApplicationContext context) {
        if (instance == null) {
            synchronized (RNOpenTokSessionManager.class) {
                if (instance == null) {
                    String apiKey = "";
                    ApplicationInfo ai = null;
                    try {
                        ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                        apiKey = ai.metaData.get("OPENTOK_API_KEY").toString();
                    } catch (PackageManager.NameNotFoundException | NullPointerException e) {
                        e.printStackTrace();
                    }
                    instance = new RNOpenTokSessionManager (context, apiKey);
                }
            }
        } else if (context != null && instance.getContext() != context) {
            instance.setContext(context);
        }
        return instance;
    }

    public static RNOpenTokSessionManager getSessionManager() {
        return RNOpenTokSessionManager.initSessionManager(null);
    }

    public ReactApplicationContext getContext() {
        return this.mContext;
    }

    public void setContext(ReactApplicationContext context) {
        this.mContext = context;
    }

    public void connectToSession (String sessionId, String token) {
        getSession(sessionId).connect(token);
    }

    public void disconnectSession(String sessionId) {
        getSession(sessionId).disconnect();
        this.mSessions.remove(sessionId);
    }

    public void disconnectAllSessions () {
        for(RNOpenTokSessionWrapper session : this.mSessions.values()) {
            session.disconnect();
        }

        this.mSessions.clear();
    }

    public void sendSignal (String sessionId, String type, String data) {
        getSession(sessionId).sendSignal(type, data);
    }

    public void setPublisherListener (String sessionId, RNOpenTokPublisherView view) {
        getSession(sessionId).setPublisherView(view);
    }

    public void removePublisherListener(String sessionId) {
        getSession(sessionId).removePublisherView();
    }

    public void setSubscriberListener(String sessionId, RNOpenTokSubscriberView view) {
        getSession(sessionId).setSubscriberView(view);
    }

    public void removeSubscriberListener(String sessionId) {
        getSession(sessionId).removeSubscriberView();
    }

    private RNOpenTokSessionWrapper getSession (String sessionId) {
        RNOpenTokSessionWrapper sessionWrapper = this.mSessions.get(sessionId);

        if (sessionWrapper == null) {
            Session session = new Session
                .Builder(this.mContext, this.mApiKey, sessionId)
                .sessionOptions(sessionOptions).build();

            setListeners(session);
            sessionWrapper = new RNOpenTokSessionWrapper(session);
            this.mSessions.put(sessionId, sessionWrapper);
        }

        return sessionWrapper;
    }

    private void setListeners (Session session) {
        session.setSessionListener(this);
        session.setSignalListener(this);
        session.setReconnectionListener(this);
        session.setArchiveListener(this);
        session.setConnectionListener(this);
    }

    private void emitEvent(Events event, WritableMap payload) {
        this.mContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(event.toString(), payload);
    }

    @Override
    public void onConnected (Session session) {
        String sessionId = session.getSessionId();
        getSession(sessionId).onConnected(session);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        emitEvent(Events.ON_SESSION_DID_CONNECT, payload);
    }

    @Override
    public void onDisconnected(Session session) {
        String sessionId = session.getSessionId();
        getSession(sessionId).onDisconnected(session);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        emitEvent(Events.ON_SESSION_DID_CONNECT, payload);
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        String sessionId = session.getSessionId();
        String streamId = stream.getStreamId();
        getSession(sessionId).onStreamReceived(session, stream);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        payload.putString("streamId", streamId);
        emitEvent(Events.ON_SESSION_STREAM_CREATED, payload);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        String sessionId = session.getSessionId();
        String streamId = stream.getStreamId();
        getSession(sessionId).onStreamDropped(session, stream);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        payload.putString("streamId", streamId);
        emitEvent(Events.ON_SESSION_STREAM_DESTROYED, payload);
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        String sessionId = session.getSessionId();
        String message = opentokError.getMessage();
        getSession(sessionId).onError(session, opentokError);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        payload.putString("error", message);
        emitEvent(Events.ON_SESSION_DID_FAIL_WITH_ERROR, payload);
    }

    @Override
    public void onReconnected(Session session) {
        String sessionId = session.getSessionId();
        getSession(sessionId).onReconnected(session);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        emitEvent(Events.ON_SESSION_DID_RECONNECT, payload);
    }

    @Override
    public void onReconnecting(Session session) {
        String sessionId = session.getSessionId();
        getSession(sessionId).onReconnecting(session);

        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", sessionId);
        emitEvent(Events.ON_SESSION_DID_BEGIN_RECONNECTING, payload);
    }

    @Override
    public void onArchiveStarted(Session session, String id, String name) {
        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", session.getSessionId());
        payload.putString("id", id);
        payload.putString("name", name);
        emitEvent(Events.ON_ARCHIVE_STARTED_WITH_ID, payload);
    }

    @Override
    public void onArchiveStopped(Session session, String id) {
        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", session.getSessionId());
        payload.putString("id", id);
        emitEvent(Events.ON_ARCHIVE_STOPPED_WITH_ID, payload);
    }

    @Override
    public void onConnectionCreated(Session session, Connection connection) {
        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", session.getSessionId());
        payload.putString("connectionId", connection.getConnectionId());
        emitEvent(Events.ON_SESSION_CONNECTION_CREATED, payload);
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection) {
        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", session.getSessionId());
        payload.putString("connectionId", connection.getConnectionId());
        emitEvent(Events.ON_SESSION_CONNECTION_DESTROYED, payload);
    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {
        WritableMap payload = Arguments.createMap();
        payload.putString("sessionId", session.getSessionId());
        payload.putString("type", type);
        payload.putString("data", data);
        emitEvent(Events.EVENT_ON_SIGNAL_RECEIVED, payload);
    }

    private final Session.SessionOptions sessionOptions = new Session.SessionOptions () {
        @Override
        public boolean useTextureViews () {
            return true;
        }
    };
}

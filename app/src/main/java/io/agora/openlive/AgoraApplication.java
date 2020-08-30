package io.agora.openlive;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.agora.openlive.rtc.EngineConfig;
import io.agora.openlive.rtc.AgoraEventHandler;
import io.agora.openlive.rtc.EventHandler;
import io.agora.openlive.rtm.ChatManager;
import io.agora.openlive.stats.StatsManager;
import io.agora.openlive.utils.FileUtil;
import io.agora.openlive.utils.PrefManager;
import io.agora.rtc.RtcEngine;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmClient;

public class AgoraApplication extends Application {

    private RtcEngine mRtcEngine;
    private RtmClient mRtmClient;
    private EngineConfig mGlobalConfig = new EngineConfig();
    private AgoraEventHandler mHandler = new AgoraEventHandler();
    private StatsManager mStatsManager = new StatsManager();
    private ChatManager mChatManager;
    private static AgoraApplication sInstance;
    final int min = 1000;
    final int max = 9999;

    public static AgoraApplication the() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        mChatManager = new ChatManager(this);
        mChatManager.init();

        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.private_app_id), mHandler);
            // Sets the channel profile of the Agora RtcEngine.
            // The Agora RtcEngine differentiates channel profiles and applies different optimization algorithms accordingly. For example, it prioritizes smoothness and low latency for a video call, and prioritizes video quality for a video broadcast.
            mRtcEngine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableVideo();
            mRtcEngine.setLogFile(FileUtil.initializeLogFile(this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mRtmClient = mChatManager.getRtmClient();
            mRtmClient.login(null, getString(R.string.session) + getRandom(), new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void responseInfo) {
                    Log.i("AgoraApplication", "login success");
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    Log.i("AgoraApplication", "login failed: " + errorInfo.getErrorDescription());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        initConfig();
    }

    private int getRandom() {
        return new Random().nextInt((max - min) + 1) + min;
    }

    private void initConfig() {
        SharedPreferences pref = PrefManager.getPreferences(getApplicationContext());
        mGlobalConfig.setVideoDimenIndex(pref.getInt(
                Constants.PREF_RESOLUTION_IDX, Constants.DEFAULT_PROFILE_IDX));

        boolean showStats = pref.getBoolean(Constants.PREF_ENABLE_STATS, false);
        mGlobalConfig.setIfShowVideoStats(showStats);
        mStatsManager.enableStats(showStats);

        mGlobalConfig.setMirrorLocalIndex(pref.getInt(Constants.PREF_MIRROR_LOCAL, 0));
        mGlobalConfig.setMirrorRemoteIndex(pref.getInt(Constants.PREF_MIRROR_REMOTE, 0));
        mGlobalConfig.setMirrorEncodeIndex(pref.getInt(Constants.PREF_MIRROR_ENCODE, 0));
    }

    public EngineConfig engineConfig() {
        return mGlobalConfig;
    }

    public RtcEngine rtcEngine() {
        return mRtcEngine;
    }

    public RtmClient rtmClient() {
        return mRtmClient;
    }

    public StatsManager statsManager() {
        return mStatsManager;
    }

    public void registerEventHandler(EventHandler handler) {
        mHandler.addHandler(handler);
    }

    public void removeEventHandler(EventHandler handler) {
        mHandler.removeHandler(handler);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        RtcEngine.destroy();
        mRtmClient.logout(null);
    }

    public ChatManager getChatManager() {
        return mChatManager;
    }
}

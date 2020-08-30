package io.agora.openlive.rtc;

import io.agora.openlive.Constants;

public class EngineConfig {
    // private static final int DEFAULT_UID = 0;
    // private int mUid = DEFAULT_UID;

    private String mChannelName;
    private String mBroadcasterName;
    private String mSessionName;
    private boolean mShowVideoStats;
    private int mDimenIndex = Constants.DEFAULT_PROFILE_IDX;
    private int mMirrorLocalIndex;
    private int mMirrorRemoteIndex;
    private int mMirrorEncodeIndex;


    public int getVideoDimenIndex() {
        return mDimenIndex;
    }

    public void setVideoDimenIndex(int index) {
        mDimenIndex = index;
    }

    public String getBroadcasterName() {
        return mBroadcasterName;
    }

    public void setBroadcasterName(String mBroadcasterName) {
        this.mBroadcasterName = mBroadcasterName;
    }

    public String getChannelName() {
        return mChannelName;
    }

    public void setChannelName(String mChannel) {
        this.mChannelName = mChannel;
    }

    public String getSessionName() {
        return mSessionName;
    }

    public void setSessionName(String mSessionName) {
        this.mSessionName = mSessionName;
    }

    public boolean ifShowVideoStats() {
        return mShowVideoStats;
    }

    public void setIfShowVideoStats(boolean show) {
        mShowVideoStats = show;
    }

    public int getMirrorLocalIndex() {
        return mMirrorLocalIndex;
    }

    public void setMirrorLocalIndex(int index) {
        mMirrorLocalIndex = index;
    }

    public int getMirrorRemoteIndex() {
        return mMirrorRemoteIndex;
    }

    public void setMirrorRemoteIndex(int index) {
        mMirrorRemoteIndex = index;
    }

    public int getMirrorEncodeIndex() {
        return mMirrorEncodeIndex;
    }

    public void setMirrorEncodeIndex(int index) {
        mMirrorEncodeIndex = index;
    }
}

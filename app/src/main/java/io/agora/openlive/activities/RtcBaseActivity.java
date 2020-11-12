package io.agora.openlive.activities;

import android.content.SyncRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.text.TextUtils;

import io.agora.openlive.Constants;
import io.agora.openlive.rtc.EventHandler;
import io.agora.rtc.IMetadataObserver;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.live.LiveInjectStreamConfig;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.openlive.R;

public abstract class RtcBaseActivity extends BaseActivity implements EventHandler, IMetadataObserver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerRtcEventHandler(this);
        Log.i("onCreate", "-- RtcBaseActivity");
        configVideo();
        joinChannel();
    }

    private void configVideo() {
        VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(
                Constants.VIDEO_DIMENSIONS[config().getVideoDimenIndex()],
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        );
        //configuration.mirrorMode = Constants.VIDEO_MIRROR_MODES[config().getMirrorEncodeIndex()];
        rtcEngine().setVideoEncoderConfiguration(configuration);
    }

    private void joinChannel() {
        // Initialize token, extra info here before joining channel
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name and uid that
        // you use to generate this token.
        String token = getString(R.string.agora_access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null; // default, no token
        }

        rtcEngine().registerMediaMetadataObserver(this, VIDEO_METADATA);
        rtcEngine().joinChannel(token, config().getChannelName(), "", 123);
    }

    protected SurfaceView prepareRtcVideo(int uid, boolean local) {

        // Render local/remote video on a SurfaceView

        SurfaceView surface = RtcEngine.CreateRendererView(getApplicationContext());
        if (local) {
            rtcEngine().setupLocalVideo(
                    new VideoCanvas(
                            surface,
                            VideoCanvas.RENDER_MODE_HIDDEN,
                            0,
                            Constants.VIDEO_MIRROR_MODES[config().getMirrorLocalIndex()]
                    )
            );
        } else {
            rtcEngine().setupRemoteVideo(
                    new VideoCanvas(
                            surface,
                            VideoCanvas.RENDER_MODE_HIDDEN,
                            uid,
                            Constants.VIDEO_MIRROR_MODES[config().getMirrorRemoteIndex()]
                    )
            );
        }
        return surface;
    }

    protected SurfaceView prepareRecordedRtcVideo(int uid, boolean local) {

        // Render remote video on a SurfaceView

        SurfaceView surface = RtcEngine.CreateRendererView(getApplicationContext());
        rtcEngine().setupRemoteVideo(
                new VideoCanvas(
                        surface,
                        VideoCanvas.RENDER_MODE_FIT,
                        uid,
                        Constants.VIDEO_MIRROR_MODES[config().getMirrorRemoteIndex()]
                )
        );
        return surface;
    }

    protected void removeRtcVideo(int uid, boolean local) {
        if (local) {
            rtcEngine().setupLocalVideo(null);
        } else {
            rtcEngine().setupRemoteVideo(new VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeRtcEventHandler(this);
        rtcEngine().leaveChannel();
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
    }

}

package io.agora.openlive.activities;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.agora.openlive.R;
import io.agora.openlive.pojo.Question;
import io.agora.openlive.stats.LocalStatsData;
import io.agora.openlive.stats.RemoteStatsData;
import io.agora.openlive.stats.StatsData;
import io.agora.openlive.ui.VideoGridContainer;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.live.LiveInjectStreamConfig;
import io.agora.rtc.live.LiveTranscoding;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.rtm.ChannelAttributeOptions;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmMessage;

import static io.agora.rtc.live.LiveTranscoding.AudioSampleRateType.TYPE_44100;
import static io.agora.rtc.live.LiveTranscoding.VideoCodecProfileType.HIGH;

public class LiveActivity extends RtcBaseActivity {
    private static final String TAG = LiveActivity.class.getSimpleName();

    private VideoGridContainer mVideoGridContainer;
    private ImageView mMuteAudioBtn;
    private ImageView mMuteVideoBtn;
    private RelativeLayout mStartQuizRl;

    private VideoEncoderConfiguration.VideoDimensions mVideoDimension;
    private RtmChannel mRtmChannel;
    private RtmChannel mRtmAudienceChannel;
    boolean isBroadcaster = false;
    private String mChannelAttributeKey;
    List<Question> questionArrayList = new ArrayList<>();
    private boolean isStartQuiz = false;
    private AlertDialog dialog;
    private AlertDialog dialog_broadcaster;
    private int isCount = -1;
    private int TOTAL_TIME = 90000;
    private int INTERVAL = 30000;
    private boolean isRecorded = false;
    CountDownTimer timer;

    Question question = new Question();
    final Handler handler = new Handler();
    private boolean isAnswered = false;
    private boolean isWaiting = false;
    private boolean isTimeSet = true;
    private LinearLayout mBeginTimeRl;
    private TextView mBeginTimeTv;
    private TextView mBeginTimeContentTv;

    int itemSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room);
        Log.i("onCrete", " == LiveActivity");
        question.setCount(0);
        initUI();
        initData();

    }

    private void initAgoraEngine() {
        if (isBroadcaster) {
            config().setSessionName(getString(R.string.session));
            joinChannel();
        }
        joinAudienceChannel();
    }

    private void initUI() {

        TextView roomName = findViewById(R.id.live_room_name);
        roomName.setText(config().getChannelName());
        roomName.setSelected(true);
        TextView userName = findViewById(R.id.live_room_broadcaster_uid);
        userName.setText(config().getBroadcasterName());
        userName.setSelected(true);

        initUserIcon();

        int role = getIntent().getIntExtra(
                io.agora.openlive.Constants.KEY_CLIENT_ROLE,
                Constants.CLIENT_ROLE_AUDIENCE);
        isBroadcaster = (role == Constants.CLIENT_ROLE_BROADCASTER);
        isRecorded = getIntent().getBooleanExtra("isRecorded", false);

        mStartQuizRl = findViewById(R.id.start_quiz_rl);
        mBeginTimeRl = findViewById(R.id.begin_time_rl);
        mBeginTimeTv = findViewById(R.id.beginTimeTv);
        mBeginTimeContentTv = findViewById(R.id.begin_Time_ContentTv);
        mMuteVideoBtn = findViewById(R.id.live_btn_mute_video);
        mMuteVideoBtn.setActivated(isBroadcaster);
        mMuteAudioBtn = findViewById(R.id.live_btn_mute_audio);
        mMuteAudioBtn.setActivated(isBroadcaster);


        ImageView beautyBtn = findViewById(R.id.live_btn_beautification);
        beautyBtn.setActivated(true);
        rtcEngine().setBeautyEffectOptions(beautyBtn.isActivated(),
                io.agora.openlive.Constants.DEFAULT_BEAUTY_OPTIONS);

        mVideoGridContainer = findViewById(R.id.live_video_grid_layout);
        mVideoGridContainer.setStatsManager(statsManager());

        initAgoraEngine();
        rtcEngine().setClientRole(role);

        if (isBroadcaster && !isRecorded) {
            startBroadcast();
        } else {
            RelativeLayout bottom_container = findViewById(R.id.bottom_container);
            bottom_container.setVisibility(View.GONE);
        }

        //if (isBroadcaster) startBroadcast();
    }

    private void initUserIcon() {
        Bitmap origin = BitmapFactory.decodeResource(getResources(), R.drawable.fake_user_icon);
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), origin);
        drawable.setCircular(true);
        ImageView iconView = findViewById(R.id.live_name_board_icon);
        iconView.setImageDrawable(drawable);
    }

    private void initData() {
        mVideoDimension = io.agora.openlive.Constants.VIDEO_DIMENSIONS[
                config().getVideoDimenIndex()];
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        RelativeLayout topLayout = findViewById(R.id.live_room_top_layout);
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) topLayout.getLayoutParams();
        params.height = mStatusBarHeight + topLayout.getMeasuredHeight();
        topLayout.setLayoutParams(params);
        topLayout.setPadding(0, mStatusBarHeight, 0, 0);
    }

    private void startBroadcast() {
        rtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        SurfaceView surface = prepareRtcVideo(0, true);
        mVideoGridContainer.addUserVideoSurface(0, surface, true);
        mMuteAudioBtn.setActivated(true);
    }

    private void stopBroadcast() {
        rtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        removeRtcVideo(0, true);
        mVideoGridContainer.removeUserVideo(0, true);
        mMuteAudioBtn.setActivated(false);
    }

    @Override
    public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {

        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (isBroadcaster && isRecorded) {

                    String fb_url = "rtmps://live-api-s.facebook.com:443/rtmp/3655315151170641?s_bl=1&s_psm=1&s_sc=3655315207837302&s_sw=0&s_vt=api-s&a=Abx9QQkdpCjhoq_J";
                    String youtube_url = "rtmp://a.rtmp.youtube.com/live2/528v-3bfd-qh3z-ze67-81z5";

                    // CDN transcoding settings.
                    LiveTranscoding config = new LiveTranscoding();
                    config.audioSampleRate = TYPE_44100;
                    config.audioChannels = 2;
                    config.audioBitrate = 48;
                    // Width of the video (px). The default value is 360.
                    config.width = 360;
                    // Height of the video (px). The default value is 640.
                    config.height = 640;
                    // Video bitrate of the video (Kbps). The default value is 400.
                    config.videoBitrate = 400;
                    // Video framerate of the video (fps). The default value is 15. Agora adjusts all values over 30 to 30.
                    config.videoFramerate = 15;
                    // If userCount > 1，set the layout for each user with transcodingUser.
                    config.userCount = 1;
                    // Video codec profile. Choose to set as Baseline (66), Main (77) or High (100). If you set this parameter to other values, Agora adjusts it to the default value 100.
                    config.videoCodecProfile = HIGH;

                    // Sets the output layout for each user.
                    LiveTranscoding transcoding = new LiveTranscoding();
                    LiveTranscoding.TranscodingUser user = new LiveTranscoding.TranscodingUser();
                    // The uid must be identical to the uid used in joinChannel().
                    user.uid = uid;
                    transcoding.addUser(user);
                    user.x = 0;
                    user.audioChannel = 0;
                    user.y = 0;
                    user.width = 640;
                    user.height = 720;

                    Log.i("StreamTest", " ---- uid - " + uid);

                    // CDN transcoding settings when using transcoding.
                    int setLiveTranscoding = rtcEngine().setLiveTranscoding(transcoding);

                    Log.i("StreamTest", " ---- setLiveTranscoding - " + setLiveTranscoding);

                    // Adds a URL to which the host pushes a stream. Set the transcodingEnabled parameter as true to enable the transcoding service. Once transcoding is enabled, you need to set the live transcoding configurations by calling the setLiveTranscoding method. We do not recommend transcoding in the case of a single host.
                    int addPublishStream = rtcEngine().addPublishStreamUrl(fb_url, true);
                    int addPublishStream2 = rtcEngine().addPublishStreamUrl(youtube_url, true);

                    Log.i("StreamTest", " ---- addPublishStreamUrl - " + addPublishStream);
                    Log.i("StreamTest", " ---- addPublishStreamUrl02 - " + addPublishStream2);

                }

            }
        });*/

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isBroadcaster && isRecorded) {

                    LiveInjectStreamConfig config = new LiveInjectStreamConfig();
                    config.width = 0;
                    config.height = 0;
                    config.videoGop = 30;
                    config.videoFramerate = 15;
                    config.videoBitrate = 400;
                    config.videoBitrate = 400;
                    config.audioSampleRate = LiveInjectStreamConfig.AudioSampleRateType.TYPE_48000;
                    config.audioBitrate = 48;
                    config.audioChannels = 1;

                    //final String urlPath = "http://content.jwplatform.com/manifests/vM7nH0Kl.m3u8";//working
                    final String urlPath = "http://radios-ec.cdn.nedmedia.io/radios/ec-alfa.m3u8";//working
                    //final String urlPath = "https://agoracr.s3.ap-south-1.amazonaws.com/cloudVideo07/28b0899f20463ea4f72dfc910c633865_agoracr.m3u8";//working
                    int a = rtcEngine().addInjectStreamUrl(urlPath, new LiveInjectStreamConfig());
                    Log.e("addInjectStreamUrl", "-- " + a);
                }
            }
        });

    }

    @Override
    public void onUserJoined(int uid, int elapsed) {
        // Do nothing at the moment
        Log.e("addInjectStreamUrl", "uid -- onUserJoined -- " + uid);
    }

    @Override
    public void onStreamInjectedStatus(String s, final int i, int i1) {

        if (isBroadcaster && isRecorded) {
            getChannelMemberList();
        }
        mStartQuizRl.setVisibility(View.VISIBLE);


        Log.e("addInjectStreamUrl", "url -- " + s);
        Log.e("addInjectStreamUrl", "User ID. -- " + i);
        Log.e("addInjectStreamUrl", "status -- " + i1);

    }

    @Override
    public void onUserOffline(final int uid, int reason) {
        Log.e("addInjectStreamUrl", "uid -- onUserOffline -- " + uid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeRemoteUser(uid);
            }
        });
    }

    @Override
    public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
        Log.e("addInjectStreamUrl", "uid -- onFirstRemoteVideoDecoded -- " + uid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isRecorded) {
                    if (uid == 666) {
                        renderRecordedForUser(uid);
                    }
                } else {
                    renderRemoteUser(uid);
                }
                //renderRemoteUser(uid);
            }
        });
    }

    private void renderRemoteUser(int uid) {
        SurfaceView surface = prepareRtcVideo(uid, false);
        mVideoGridContainer.addUserVideoSurface(uid, surface, false);
    }

    private void renderRecordedForUser(int uid) {
        SurfaceView surface = prepareRecordedRtcVideo(uid, false);
        mVideoGridContainer.addUserVideoSurface(uid, surface, false);
    }

    private void removeRemoteUser(int uid) {
        removeRtcVideo(uid, false);
        mVideoGridContainer.removeUserVideo(uid, false);
    }

    @Override
    public void onLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats stats) {
        if (!statsManager().isEnabled()) return;

        LocalStatsData data = (LocalStatsData) statsManager().getStatsData(0);
        if (data == null) return;

        data.setWidth(mVideoDimension.width);
        data.setHeight(mVideoDimension.height);
        data.setFramerate(stats.sentFrameRate);
    }

    @Override
    public void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
        if (!statsManager().isEnabled()) return;

        LocalStatsData data = (LocalStatsData) statsManager().getStatsData(0);
        if (data == null) return;

        data.setLastMileDelay(stats.lastmileDelay);
        data.setVideoSendBitrate(stats.txVideoKBitRate);
        data.setVideoRecvBitrate(stats.rxVideoKBitRate);
        data.setAudioSendBitrate(stats.txAudioKBitRate);
        data.setAudioRecvBitrate(stats.rxAudioKBitRate);
        data.setCpuApp(stats.cpuAppUsage);
        data.setCpuTotal(stats.cpuAppUsage);
        data.setSendLoss(stats.txPacketLossRate);
        data.setRecvLoss(stats.rxPacketLossRate);
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
        if (!statsManager().isEnabled()) return;

        StatsData data = statsManager().getStatsData(uid);
        if (data == null) return;

        data.setSendQuality(statsManager().qualityToString(txQuality));
        data.setRecvQuality(statsManager().qualityToString(rxQuality));
    }

    @Override
    public void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats) {
        if (!statsManager().isEnabled()) return;

        RemoteStatsData data = (RemoteStatsData) statsManager().getStatsData(stats.uid);
        if (data == null) return;

        data.setWidth(stats.width);
        data.setHeight(stats.height);
        data.setFramerate(stats.rendererOutputFrameRate);
        data.setVideoDelay(stats.delay);
    }

    @Override
    public void onRemoteAudioStats(IRtcEngineEventHandler.RemoteAudioStats stats) {
        if (!statsManager().isEnabled()) return;

        RemoteStatsData data = (RemoteStatsData) statsManager().getStatsData(stats.uid);
        if (data == null) return;

        data.setAudioNetDelay(stats.networkTransportDelay);
        data.setAudioNetJitter(stats.jitterBufferDelay);
        data.setAudioLoss(stats.audioLossRate);
        data.setAudioQuality(statsManager().qualityToString(stats.quality));
    }

    @Override
    public void finish() {
        super.finish();
        statsManager().clearAllData();
    }

    @Override
    protected void onDestroy() {
        if (isBroadcaster) {
            deletedChannelAttributeByKey();
        }
        leaveAndReleaseChannel();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (isBroadcaster) {

            deletedChannelAttributeByKey();
            clearCallBackAndFinish();

            /*List<String> list = new ArrayList<>();
            list.add(mChannelAttributeKey);
            ResultCallback<Void> callback = new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i("Loginissue", "deletedChannelAttributeByKey -- onSuccess");
                    clearCallBackAndFinish();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    Log.i("Loginissue", "  deletedChannelAttributeByKey -- onFailure");
                    Log.i("Loginissue", "  deletedChannelAttributeByKey -- onFailure" + errorInfo.getErrorDescription());
                    clearCallBackAndFinish();
                }
            };
            rtmClient().deleteChannelAttributesByKeys(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);*/

        } else {
            clearCallBackAndFinish();
        }
    }

    public void onLeaveClicked(View view) {

        if (isBroadcaster) {

            deletedChannelAttributeByKey();
            clearCallBackAndFinish();

            /*List<String> list = new ArrayList<>();
            list.add(mChannelAttributeKey);
            ResultCallback<Void> callback = new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i("Loginissue", "deletedChannelAttributeByKey -- onSuccess");
                    clearCallBackAndFinish();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    Log.i("Loginissue", "  deletedChannelAttributeByKey -- onFailure");
                    Log.i("Loginissue", "  deletedChannelAttributeByKey -- onFailure" + errorInfo.getErrorDescription());
                    clearCallBackAndFinish();
                }
            };
            rtmClient().deleteChannelAttributesByKeys(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);*/

        } else {
            clearCallBackAndFinish();
        }
    }

    public void onSwitchCameraClicked(View view) {
        rtcEngine().switchCamera();
    }

    public void onBeautyClicked(View view) {
        view.setActivated(!view.isActivated());
        rtcEngine().setBeautyEffectOptions(view.isActivated(),
                io.agora.openlive.Constants.DEFAULT_BEAUTY_OPTIONS);
    }

    public void onMoreClicked(View view) {
        // Do nothing at the moment
    }

    public void onPushStreamClicked(View view) {
        // Do nothing at the moment
    }

    public void onMuteAudioClicked(View view) {
        if (!mMuteVideoBtn.isActivated()) return;

        rtcEngine().muteLocalAudioStream(view.isActivated());
        view.setActivated(!view.isActivated());
    }

    public void onMuteVideoClicked(View view) {

        if (isRecorded) {
            return;
        }

        if (view.isActivated()) {
            stopBroadcast();
        } else {
            startBroadcast();
        }
        view.setActivated(!view.isActivated());
    }

    public void startQuizOnClick(View view) {

        String[] singleChoiceItems = {" 30 Seconds ", " 1 minute ", " 2 minutes "};
        itemSelected = 0;
        TOTAL_TIME = 90000;
        INTERVAL = 30000;
        new AlertDialog.Builder(this)
                .setTitle("Select Quiz Interval")
                .setSingleChoiceItems(singleChoiceItems, itemSelected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                        if (selectedIndex == 0) {
                            TOTAL_TIME = 90000;
                            INTERVAL = 30000;
                            itemSelected = 0;
                        } else if (selectedIndex == 1) {
                            TOTAL_TIME = 180000;
                            INTERVAL = 60000;
                            itemSelected = 1;
                        } else {
                            TOTAL_TIME = 360000;
                            INTERVAL = 120000;
                            itemSelected = 2;
                        }

                    }
                })
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        mStartQuizRl.setVisibility(View.GONE);
                        dialog.dismiss();

                        if (itemSelected == 0) {
                            forThirtySec();
                        } else if (itemSelected == 1) {
                            forOneMinute();
                        } else {
                            forTwoMinute();
                        }

                        /*long initTime = (System.currentTimeMillis() - (io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long a = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_ONE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long b = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_TWO - io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long c = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_THREE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long d = ((System.currentTimeMillis()) + (50000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long e = ((System.currentTimeMillis()) + (80000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long f = ((System.currentTimeMillis()) + (110000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;

                        long dd = ((System.currentTimeMillis()) + (50000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long bb = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_TWO + io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long ee = ((System.currentTimeMillis()) + (80000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
                        long cc = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_THREE + io.agora.openlive.Constants.ONE_SEC)) / 1000;

                        questionArrayList.get(0).setStartTime(a);
                        questionArrayList.get(1).setStartTime(b);
                        questionArrayList.get(2).setStartTime(c);
                        questionArrayList.get(0).setEndTime(d);
                        questionArrayList.get(1).setEndTime(e);
                        questionArrayList.get(2).setEndTime(f);
                        questionArrayList.get(0).setInitialTime(initTime);
                        questionArrayList.get(0).setClosingTime(a);
                        questionArrayList.get(1).setInitialTime(dd);
                        questionArrayList.get(1).setClosingTime(bb);
                        questionArrayList.get(2).setInitialTime(ee);
                        questionArrayList.get(2).setClosingTime(cc);*/

                        isWaiting = true;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                startQuizTimer(TOTAL_TIME, INTERVAL);

                            }
                        }, INTERVAL);

                        /*startQuizTimer(TOTAL_TIME, INTERVAL);
                        mStartQuizRl.setVisibility(View.GONE);
                        dialog.dismiss();*/
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

    }

    private void forThirtySec() {

        long initTime = (System.currentTimeMillis() - (io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long a = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_ONE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long b = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_TWO - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long c = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_THREE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long d = ((System.currentTimeMillis()) + (50000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long e = ((System.currentTimeMillis()) + (80000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long f = ((System.currentTimeMillis()) + (110000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        long dd = ((System.currentTimeMillis()) + (50000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long bb = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_TWO + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long ee = ((System.currentTimeMillis()) + (80000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long cc = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_THREE + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        questionArrayList.get(0).setStartTime(a);
        questionArrayList.get(1).setStartTime(b);
        questionArrayList.get(2).setStartTime(c);
        questionArrayList.get(0).setEndTime(d);
        questionArrayList.get(1).setEndTime(e);
        questionArrayList.get(2).setEndTime(f);
        questionArrayList.get(0).setInitialTime(initTime);
        questionArrayList.get(0).setClosingTime(a);
        questionArrayList.get(1).setInitialTime(dd);
        questionArrayList.get(1).setClosingTime(bb);
        questionArrayList.get(2).setInitialTime(ee);
        questionArrayList.get(2).setClosingTime(cc);

    }

    private void forOneMinute() {

        long initTime = (System.currentTimeMillis() - (io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long a = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.TWO_ONE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long b = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.TWO_TWO - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long c = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.TWO_THREE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long d = ((System.currentTimeMillis()) + (80000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long e = ((System.currentTimeMillis()) + (140000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long f = ((System.currentTimeMillis()) + (200000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        long dd = ((System.currentTimeMillis()) + (80000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long bb = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.TWO_TWO + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long ee = ((System.currentTimeMillis()) + (140000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long cc = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.TWO_THREE + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        questionArrayList.get(0).setStartTime(a);
        questionArrayList.get(1).setStartTime(b);
        questionArrayList.get(2).setStartTime(c);
        questionArrayList.get(0).setEndTime(d);
        questionArrayList.get(1).setEndTime(e);
        questionArrayList.get(2).setEndTime(f);
        questionArrayList.get(0).setInitialTime(initTime);
        questionArrayList.get(0).setClosingTime(a);
        questionArrayList.get(1).setInitialTime(dd);
        questionArrayList.get(1).setClosingTime(bb);
        questionArrayList.get(2).setInitialTime(ee);
        questionArrayList.get(2).setClosingTime(cc);

    }

    private void forTwoMinute() {

        long initTime = (System.currentTimeMillis() - (io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long a = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.THREE_ONE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long b = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.THREE_TWO - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long c = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.THREE_THREE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long d = ((System.currentTimeMillis()) + (140000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long e = ((System.currentTimeMillis()) + (260000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long f = ((System.currentTimeMillis()) + (380000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        long dd = ((System.currentTimeMillis()) + (140000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long bb = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.THREE_TWO + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long ee = ((System.currentTimeMillis()) + (260000 - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long cc = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.THREE_THREE + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        questionArrayList.get(0).setStartTime(a);
        questionArrayList.get(1).setStartTime(b);
        questionArrayList.get(2).setStartTime(c);
        questionArrayList.get(0).setEndTime(d);
        questionArrayList.get(1).setEndTime(e);
        questionArrayList.get(2).setEndTime(f);
        questionArrayList.get(0).setInitialTime(initTime);
        questionArrayList.get(0).setClosingTime(a);
        questionArrayList.get(1).setInitialTime(dd);
        questionArrayList.get(1).setClosingTime(bb);
        questionArrayList.get(2).setInitialTime(ee);
        questionArrayList.get(2).setClosingTime(cc);

    }

    private void startQuizTimer(int totalTime, int interval) {

        isWaiting = false;

        new CountDownTimer(totalTime, interval) {

            public void onTick(long millisUntilFinished) {

                isCount = isCount + 1;
                isStartQuiz = true;

                //For Broadcaster
               /* question.setQuestion(questionArrayList.get(isCount).getQuestion());
                question.setCount(questionArrayList.get(isCount).getCount());
                question.setOptionA(questionArrayList.get(isCount).getOptionA());
                question.setOptionB(questionArrayList.get(isCount).getOptionB());
                question.setOptionC(questionArrayList.get(isCount).getOptionC());
                question.setOptionD(questionArrayList.get(isCount).getOptionD());
                runOnUiThread(new Runnable() {
                    public void run() {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Do something after 100ms
                                showQuizDialog(20000);
                            }
                        }, 1000);
                    }
                });*/

                if (isCount == 2) {

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isStartQuiz = false;
                                    isCount = -1;
                                    mStartQuizRl.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }, 20000);

                }

            }

            public void onFinish() {
                /*isStartQuiz = false;
                isCount = -1;
                mStartQuizRl.setVisibility(View.VISIBLE);*/
            }

        }.start();
    }


    /**
     * API CALLBACK: rtm channel event listener
     */
    class MyAudienceChannelListener implements RtmChannelListener {
        @Override
        public void onMemberCountUpdated(int i) {

        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {

        }

        @Override
        public void onMessageReceived(final RtmMessage message, final RtmChannelMember fromMember) {

        }

        @Override
        public void onMemberJoined(final RtmChannelMember member) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("isBroadcaster", "--- >> onMemberJoined --->>  " + isBroadcaster);
                    if (isBroadcaster) {
                        Toast.makeText(LiveActivity.this, "Remote User Joined", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onMemberLeft(final RtmChannelMember member) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    /*Log.i("isBroadcaster", "--- >> onMemberJoined --->>  " + member.getChannelId());
                    if (isBroadcaster) {
                        Toast.makeText(LiveActivity.this, "Remote User Joined", Toast.LENGTH_SHORT).show();
                    }*/

                }
            });
        }

    }


    class MyChannelListener implements RtmChannelListener {
        @Override
        public void onMemberCountUpdated(int i) {

        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {

        }

        @Override
        public void onMessageReceived(final RtmMessage message, final RtmChannelMember fromMember) {

        }

        @Override
        public void onMemberJoined(final RtmChannelMember member) {

        }

        @Override
        public void onMemberLeft(final RtmChannelMember member) {

        }

    }

    private void joinAudienceChannel() {
        mRtmAudienceChannel = rtmClient().createChannel(getString(R.string.audience), new MyAudienceChannelListener());
        if (mRtmAudienceChannel == null) {
            finish();
            return;
        }
        mRtmAudienceChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("Loginissue", " --   mRtmChannel.joinAudienceChannel -- onSuccess");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i("Loginissue", " --   mRtmChannel.joinAudienceChannel -- errorInfo");
                Log.i("Loginissue", " --   " + errorInfo.getErrorDescription());
            }
        });
    }

    private void joinChannel() {

        getQuizQuestionsData();
        mStartQuizRl.setVisibility(View.VISIBLE);

        // step 1: create a channel instance
        mRtmChannel = rtmClient().createChannel(config().getSessionName(), new MyChannelListener());
        if (mRtmChannel == null) {
            finish();
            return;
        }

        Log.e("channel", mRtmChannel + "");

        // step 2: join the channel
        mRtmChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {

                Log.i("Loginissue", " --   mRtmChannel.join -- onSuccess");
                Log.e("mRtmChannel", "onSuccess --- ");

                if (isBroadcaster && !isRecorded) {
                    getChannelMemberList();
                }

                //getChannelMemberList();

                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        getChannelAttributes();

                    }
                });*/

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e("mRtmChannel", "onFailure --- " + errorInfo.getErrorDescription());
                Log.e("mRtmChannel", "onFailure --- " + errorInfo.getErrorCode());
                Log.i("Loginissue", " --   mRtmChannel.onFailure -- onFailure");
                Log.i("Loginissue", " --   " + errorInfo.getErrorDescription());
            }
        });
    }

    private void leaveAndReleaseChannel() {

        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            //mRtmChannel.release();
            mRtmChannel = null;
        }

        if (mRtmAudienceChannel != null) {
            mRtmAudienceChannel.leave(null);
            //mRtmChannel.release();
            mRtmAudienceChannel = null;
        }

    }

    private void clearCallBackAndFinish() {

        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            //mRtmChannel.release();
            mRtmChannel = null;
        }
        if (mRtmAudienceChannel != null) {
            mRtmAudienceChannel.leave(null);
            //mRtmChannel.release();
            mRtmAudienceChannel = null;
        }
        finish();

    }


    private void getChannelMemberList() {
        mRtmChannel.getMembers(new ResultCallback<List<RtmChannelMember>>() {
            @Override
            public void onSuccess(final List<RtmChannelMember> responseInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int min = 1000;
                        final int max = 99999;
                        final int random = new Random().nextInt((max - min) + 1) + min;
                        mChannelAttributeKey = "k_" + random;
                        int count = responseInfo.size();
                        Log.i("Loginissue", "   getChannelMemberList -- " + count);
                        if (count == 1) {
                            setChannelAttributes();
                        } else if (count > 1) {
                            updateChannelAttributes();
                        }

                    }
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e(TAG, "failed to get channel members, err: " + errorInfo.getErrorCode());
                Log.e("Loginissue", "failed to get channel members, err: " + errorInfo.getErrorCode());
                Log.e("Loginissue", "failed to get channel members, err: " + errorInfo.getErrorDescription());
            }
        });

    }


    private void updateChannelAttributes() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(getString(R.string.BROADCASTER_NAME), config().getBroadcasterName());
            jsonObject.put(getString(R.string.GROUP_NAME), config().getChannelName());
            jsonObject.put(getString(R.string.IS_RECORDED), isRecorded);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<RtmChannelAttribute> list = new ArrayList<>();
        //RtmChannelAttribute rtmChannelAttribute = new RtmChannelAttribute(mChannelAttributeKey, config().getChannelName());
        RtmChannelAttribute rtmChannelAttribute = new RtmChannelAttribute(mChannelAttributeKey, jsonObject.toString());
        list.add(rtmChannelAttribute);
        Log.i("Loginissue", "update -- >>> k_" + mChannelAttributeKey);
        ResultCallback<Void> callback = new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("Loginissue", "   updateChannelAttributes -- onSuccess");
               /* runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        joinAudienceChannel();
                    }
                });*/

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i("Loginissue", "   updateChannelAttributes -- onFailure");
                Log.i("Loginissue", "  " + errorInfo.getErrorDescription().toString());
                Log.i("Loginissue", "  " + errorInfo.getErrorCode());
            }
        };
        rtmClient().addOrUpdateChannelAttributes(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);

    }

    private void setChannelAttributes() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(getString(R.string.BROADCASTER_NAME), config().getBroadcasterName());
            jsonObject.put(getString(R.string.GROUP_NAME), config().getChannelName());
            jsonObject.put(getString(R.string.IS_RECORDED), isRecorded);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<RtmChannelAttribute> list = new ArrayList<>();
        //RtmChannelAttribute rtmChannelAttribute = new RtmChannelAttribute(mChannelAttributeKey, config().getChannelName());
        RtmChannelAttribute rtmChannelAttribute = new RtmChannelAttribute(mChannelAttributeKey, jsonObject.toString());
        list.add(rtmChannelAttribute);
        Log.i("Loginissue", "first insert k_" + mChannelAttributeKey);
        ResultCallback<Void> callback = new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("Loginissue", "  setChannelAttributes -- onSuccess");
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        joinAudienceChannel();
                    }
                });*/
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i("Loginissue", "  setChannelAttributes -- onFailure");
                Log.i("Loginissue", "  " + errorInfo.getErrorDescription().toString());
                Log.i("Loginissue", "  " + errorInfo.getErrorCode());
            }
        };
        rtmClient().setChannelAttributes(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);
    }

    void deletedChannelAttributeByKey() {
        List<String> list = new ArrayList<>();
        list.add(mChannelAttributeKey);
        ResultCallback<Void> callback = new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("Loginissue", "deletedChannelAttributeByKey -- onSuccess");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i("Loginissue", "  deletedChannelAttributeByKey -- onFailure");
                Log.i("Loginissue", "  deletedChannelAttributeByKey -- onFailure" + errorInfo.getErrorDescription());
            }
        };
        rtmClient().deleteChannelAttributesByKeys(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);
    }

    void getChannelAttributes() {

        rtmClient().getChannelAttributes(config().getSessionName(), new ResultCallback<List<RtmChannelAttribute>>() {
            @Override
            public void onSuccess(List<RtmChannelAttribute> rtmChannelAttributes) {
                if (rtmChannelAttributes != null && rtmChannelAttributes.size() > 0) {

                    for (RtmChannelAttribute data : rtmChannelAttributes) {

                        try {
                            JSONObject jsonObject = new JSONObject(data.getValue());
                            Log.i("Loginissue", " values -- " + jsonObject.get(getString(R.string.BROADCASTER_NAME)));
                            Log.i("Loginissue", " values -- " + jsonObject.get(getString(R.string.GROUP_NAME)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.i("Loginissue", " values -- " + data.getKey());
                        Log.i("Loginissue", " values -- " + data.getValue());
                    }

                } else {
                    Log.i("Loginissue", "  -- no values");
                }
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i("Loginissue", "  -- onFailure");
            }
        });

    }

    void getQuizQuestionsData() {

        InputStream is = getResources().openRawResource(R.raw.myjsonfile);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String jsonString = writer.toString();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObject.getJSONArray("questions");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = (JSONObject) jsonArray.get(i);
                Question question = new Question(object.getString("question"),
                        object.getString("option_a"),
                        object.getString("option_b"),
                        object.getString("option_c"),
                        object.getString("option_d"),
                        object.getInt("count"),
                        object.getBoolean("isShowQuiz"),
                        object.getLong("start_time"),
                        object.getLong("end_time"),
                        object.getString("q_no"),
                        object.getBoolean("is_final"),
                        object.getLong("initial_time"),
                        object.getLong("closing_time"));
                questionArrayList.add(question);
            }

            for (Question question : questionArrayList) {
                Log.e("Loginissue", question.getQuestion());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getMaxMetadataSize() {
        return 1024;
    }

    @Override
    public byte[] onReadyToSendMetadata(long l) {

        if (isStartQuiz && isCount >= 0) {

            long startTime = questionArrayList.get(isCount).getStartTime();
            long endTime = questionArrayList.get(isCount).getEndTime();
            long currentTime = (System.currentTimeMillis()) / 1000;
            long initialTime;
            long closingTime;

            if (isCount <= 1) {
                initialTime = questionArrayList.get(isCount + 1).getInitialTime();
                closingTime = questionArrayList.get(isCount + 1).getClosingTime();
            } else {
                initialTime = questionArrayList.get(isCount).getInitialTime();
                closingTime = questionArrayList.get(isCount).getClosingTime();
            }

            /*long initialTime = questionArrayList.get(isCount).getInitialTime();
            long closingTime = questionArrayList.get(isCount).getClosingTime();*/

            Calendar c_start = Calendar.getInstance(Locale.ENGLISH);
            c_start.setTimeInMillis(startTime * 1000);
            String date_start = DateFormat.format("hh:mm:ss", c_start).toString();

            Calendar c_end = Calendar.getInstance(Locale.ENGLISH);
            c_end.setTimeInMillis(endTime * 1000);
            String date_end = DateFormat.format("hh:mm:ss", c_end).toString();

            Calendar c_current = Calendar.getInstance(Locale.ENGLISH);
            c_current.setTimeInMillis(currentTime * 1000);
            String date_current = DateFormat.format("hh:mm:ss", c_current).toString();

            Calendar c_initial = Calendar.getInstance(Locale.ENGLISH);
            c_initial.setTimeInMillis(initialTime * 1000);
            String date_initial = DateFormat.format("hh:mm:ss", c_initial).toString();

            Calendar c_closing = Calendar.getInstance(Locale.ENGLISH);
            c_closing.setTimeInMillis(closingTime * 1000);
            String date_closing = DateFormat.format("hh:mm:ss", c_closing).toString();

            if (c_current.getTime().after(c_start.getTime()) && c_current.getTime().before(c_end.getTime())) {

                Log.i("time_con", "answer -- >> true");
                final long seconds = getDifferenceInSeconds(date_end, date_current);
                Log.i("time_con", "DifferenceInSeconds -- >> " + seconds);

                byte[] data = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(bos);
                    Question pojo = questionArrayList.get(isCount);
                    pojo.setInitialTime(initialTime);
                    pojo.setClosingTime(closingTime);
                    pojo.setShowQuiz(false);
                    oos.writeObject(pojo);
                    oos.flush();
                    data = bos.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (dialog == null && !isAnswered) {

                    isAnswered = true;

                    runOnUiThread(new Runnable() {
                        public void run() {

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms

                                    showQuizDialog((int) seconds);
                                }
                            }, 500);

                        }
                    });
                }

                //Log.i(TAG, " --- onReadyToSendMetadata -- Broadcaster");
                return data;

            } else if (c_current.getTime().after(c_initial.getTime()) && c_current.getTime().before(c_closing.getTime())) {

                //Log.i("time_con", "date_closing -- >> "+date_closing);
                //Log.i("time_con", "date_current -- >> true"+date_current);
                final long seconds = getDifferenceInSeconds(date_closing, date_current);
                Log.i("time_con", "DifferenceInSeconds -- >> " + seconds);


                byte[] data = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(bos);
                    Question pojo = questionArrayList.get(isCount);
                    pojo.setShowQuiz(false);
                    oos.writeObject(pojo);
                    oos.flush();
                    data = bos.toByteArray();
                    Log.i("remaining", "showquiz -- >> " + pojo.getShowQuiz());
                } catch (IOException e) {
                    e.printStackTrace();
                }


                if (isTimeSet) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Log.i("remaining", "DifferenceInSeconds -- >> " + seconds);
                            setStartTime((int) seconds, false);
                        }
                    });

                }

                return data;

            } else {

                Log.i("time_con", "Timer else if -- >> false");
                return new byte[0];

            }

            /*byte[] data = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(bos);
                Question pojo = questionArrayList.get(isCount);
                //pojo.setShowQuiz(true);
                oos.writeObject(pojo);
                oos.flush();
                data = bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Log.i(TAG, " --- onReadyToSendMetadata -- Broadcaster");
            return data;*/

        } else if (isWaiting) {

            long startTime = questionArrayList.get(0).getStartTime();
            long endTime = questionArrayList.get(0).getEndTime();
            long currentTime = (System.currentTimeMillis()) / 1000;
            long initialTime = questionArrayList.get(0).getInitialTime();
            long closingTime = questionArrayList.get(0).getClosingTime();

            Calendar c_start = Calendar.getInstance(Locale.ENGLISH);
            c_start.setTimeInMillis(startTime * 1000);
            String date_start = DateFormat.format("hh:mm:ss", c_start).toString();

            Calendar c_end = Calendar.getInstance(Locale.ENGLISH);
            c_end.setTimeInMillis(endTime * 1000);
            String date_end = DateFormat.format("hh:mm:ss", c_end).toString();

            Calendar c_current = Calendar.getInstance(Locale.ENGLISH);
            c_current.setTimeInMillis(currentTime * 1000);
            String date_current = DateFormat.format("hh:mm:ss", c_current).toString();

            Calendar c_initial = Calendar.getInstance(Locale.ENGLISH);
            c_initial.setTimeInMillis(initialTime * 1000);
            String date_initial = DateFormat.format("hh:mm:ss", c_initial).toString();

            Calendar c_closing = Calendar.getInstance(Locale.ENGLISH);
            c_closing.setTimeInMillis(closingTime * 1000);
            String date_closing = DateFormat.format("hh:mm:ss", c_closing).toString();

            if (c_current.getTime().after(c_initial.getTime()) && c_current.getTime().before(c_closing.getTime())) {

                //Log.i("time_con", "date_closing -- >> "+date_closing);
                //Log.i("time_con", "date_current -- >> true"+date_current);
                final long seconds = getDifferenceInSeconds(date_closing, date_current);
                Log.i("time_con", "DifferenceInSeconds -- >> " + seconds);

                byte[] data = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(bos);
                    Question pojo = questionArrayList.get(0);
                    pojo.setShowQuiz(true);
                    //pojo.setShowQuiz(true);
                    oos.writeObject(pojo);
                    oos.flush();
                    data = bos.toByteArray();
                    Log.i("remaining", "showquiz -- >> " + pojo.getShowQuiz());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (isTimeSet) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Log.i("remaining", "DifferenceInSeconds -- >> " + seconds);
                            setStartTime((int) seconds, true);
                        }
                    });

                }


                return data;

            } else {

                Log.i("time_con", "Timer else if -- >> false");
                return new byte[0];

            }

        } else {

            Log.i("time_con", "Timer else if -- >> count 0");
            return new byte[0];
        }

    }

    @Override
    public void onMetadataReceived(byte[] bytes, int i, long l) {

        if (bytes != null && bytes.length > 0) {

            Log.i(TAG, " --- onMetadataReceived uid --- >>>> Audiance" + i);

            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream is = null;
            try {

                is = new ObjectInputStream(in);
                Question object = (Question) is.readObject();
                question.setQuestion(object.getQuestion());
                question.setCount(object.getCount());
                question.setOptionA(object.getOptionA());
                question.setOptionB(object.getOptionB());
                question.setOptionC(object.getOptionC());
                question.setOptionD(object.getOptionD());
                question.setStartTime(object.getStartTime());
                question.setEndTime(object.getEndTime());
                question.setQuestionNumber(object.getQuestionNumber());
                question.setLastQuestion(object.isLastQuestion());
                question.setInitialTime(object.getInitialTime());
                question.setClosingTime(object.getClosingTime());
                question.setShowQuiz(object.getShowQuiz());

                long startTime = question.getStartTime();
                long endTime = question.getEndTime();
                long initialTime = question.getInitialTime();
                long closingTime = question.getClosingTime();
                long currentTime = (System.currentTimeMillis()) / 1000;
                final boolean showQuiz = question.getShowQuiz();

                Calendar c_start = Calendar.getInstance(Locale.ENGLISH);
                c_start.setTimeInMillis(startTime * 1000);

                Calendar c_end = Calendar.getInstance(Locale.ENGLISH);
                c_end.setTimeInMillis(endTime * 1000);
                String date_end = DateFormat.format("hh:mm:ss", c_end).toString();

                Calendar c_current = Calendar.getInstance(Locale.ENGLISH);
                c_current.setTimeInMillis(currentTime * 1000);
                String date_current = DateFormat.format("hh:mm:ss", c_current).toString();

                Calendar c_initial = Calendar.getInstance(Locale.ENGLISH);
                c_initial.setTimeInMillis(initialTime * 1000);

                Calendar c_closing = Calendar.getInstance(Locale.ENGLISH);
                c_closing.setTimeInMillis(closingTime * 1000);
                String date_closing = DateFormat.format("hh:mm:ss", c_closing).toString();

                if (c_current.getTime().after(c_start.getTime()) && c_current.getTime().before(c_end.getTime())) {

                    //Log.i("time_con", "answer -- >> true");
                    final long seconds = getDifferenceInSeconds(date_end, date_current);
                    //Log.i("time_con", "DifferenceInSeconds -- >> " + seconds);
                    Log.i("time_con", "DifferenceInSeconds -- >> " + seconds / 1000);

                    if (dialog == null && !isAnswered) {
                        runOnUiThread(new Runnable() {
                            public void run() {

                                isAnswered = true;
                                showQuizDialog((int) seconds);

                            }
                        });
                    } else {

                        if (dialog != null) {
                            Log.i("time_con", "answer -- >> not null");
                        } else {
                            Log.i("time_con", "answer -- >> isAnswered -- true");
                        }

                    }

                } else if (c_current.getTime().after(c_initial.getTime()) && c_current.getTime().before(c_closing.getTime())) {

                    //Log.i("time_con", "date_closing -- >> "+date_closing);
                    //Log.i("time_con", "date_current -- >> true"+date_current);
                    final long seconds = getDifferenceInSeconds(date_closing, date_current);
                    Log.i("time_con", "DifferenceInSeconds -- >> " + seconds);
                    if (isTimeSet) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Log.i("remaining", "DifferenceInSeconds -- >> " + seconds);

                                /*if (question.getShowQuiz()) {
                                    Log.i("remaining", "count 1 -- >> ");
                                    setStartTime((int) seconds, true);
                                } else {
                                    Log.i("remaining", "count else -- >> -- >> " + seconds);
                                    setStartTime((int) seconds, false);
                                }*/

                                Log.i("remaining", "showquiz -- >> " + showQuiz);
                                setStartTime((int) seconds, showQuiz);

                            }
                        });

                    }

                } else {
                    Log.i("time_con", "DifferenceInSeconds -- >>  else condition newww");
                    //Log.i("time_con", "answer -- >> false");
                }


                /*if (!object.getCount().equals(question.getCount())) {
                    question.setQuestion(object.getQuestion());
                    question.setCount(object.getCount());
                    question.setOptionA(object.getOptionA());
                    question.setOptionB(object.getOptionB());
                    question.setOptionC(object.getOptionC());
                    question.setOptionD(object.getOptionD());
                    question.setStartTime(object.getStartTime());
                    question.setEndTime(object.getEndTime());

                    if (dialog != null && dialog.isShowing()) {

                    }else {
                        runOnUiThread(new Runnable() {
                            public void run() {

                                showQuizDialog();

                            }
                        });
                    }


                }*/


                Log.i(TAG, " --- onMetadataReceived uid --- >>>> " + object.getQuestion());

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {

            Log.i(TAG, " --- onMetadataReceived uid --- >>>> size 0");

        }

    }


    private long getDifferenceInSeconds(String date_a, String date_d) {

        long diffInSec = 0;
        long diffInMs = 0;
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        try {
            Date date1 = format.parse(date_a);
            Date date2 = format.parse(date_d);
            if (date1 != null && date2 != null) {
                diffInMs = date1.getTime() - date2.getTime();
            }

            diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
            //Log.i("time_con", "diffInSec -- >> " + diffInSec);
        } catch (ParseException ex) {
            ex.printStackTrace();
            return 10000;
        }

        return Math.abs(diffInMs);
    }

    private void openQuizDialog() {

        new AlertDialog.Builder(LiveActivity.this)
                .setTitle(question.getQuestion())
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

    }


    public void showQuizDialog(final int remaining) {

        if (isBroadcaster) {
            question.setQuestion(questionArrayList.get(isCount).getQuestion());
            question.setCount(questionArrayList.get(isCount).getCount());
            question.setOptionA(questionArrayList.get(isCount).getOptionA());
            question.setOptionB(questionArrayList.get(isCount).getOptionB());
            question.setOptionC(questionArrayList.get(isCount).getOptionC());
            question.setOptionD(questionArrayList.get(isCount).getOptionD());
            question.setQuestionNumber(questionArrayList.get(isCount).getQuestionNumber());
            question.setLastQuestion(questionArrayList.get(isCount).isLastQuestion());
        }

        if (question.isLastQuestion()) {

            int time = remaining + 1000;

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    AlertDialog alertDialog = new AlertDialog.Builder(LiveActivity.this).create();
                    alertDialog.setTitle("kudos");
                    alertDialog.setMessage("Quiz completed successfully");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "GOT IT",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                }
            }, time);

        }

        Log.i("TAG", " --- onMetadataReceived uid --- >>>> " + "showQuizDialog -- start");
        Log.i("TAG", " --- onMetadataReceived uid --- >>>> " + "showQuizDialog -- dismiss");
        // create an alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // set the custom layout
        final View quizLayout = getLayoutInflater().inflate(R.layout.quiz_dialog_layout, null);
        builder.setView(quizLayout);
        dialog = builder.create();
        // create and show the alert dialog
        TextView questionNumberTv = (TextView) quizLayout.findViewById(R.id.questionNumberTv);
        final TextView remainingTimeTv = (TextView) quizLayout.findViewById(R.id.remainingTimeTv);
        TextView questionTv = (TextView) quizLayout.findViewById(R.id.questionTv);
        TextView option_a = (TextView) quizLayout.findViewById(R.id.option_a);
        TextView option_b = (TextView) quizLayout.findViewById(R.id.option_b);
        TextView option_c = (TextView) quizLayout.findViewById(R.id.option_c);
        TextView option_d = (TextView) quizLayout.findViewById(R.id.option_d);
        questionNumberTv.setText(question.getQuestionNumber());
        questionTv.setText(question.getQuestion());
        option_a.setText(question.getOptionA());
        option_b.setText(question.getOptionB());
        option_c.setText(question.getOptionC());
        option_d.setText(question.getOptionD());
        option_a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (timer != null) timer.cancel();
                if (dialog != null) dialog.hide();
            }
        });
        option_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (timer != null) timer.cancel();
                if (dialog != null) dialog.hide();
            }
        });
        option_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer != null) timer.cancel();
                if (dialog != null) dialog.hide();
            }
        });
        option_d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (timer != null) timer.cancel();
                if (dialog != null) dialog.hide();
            }
        });
        dialog.setCancelable(true);
        dialog.show();

        timer = null;
        timer = new CountDownTimer(remaining, 1000) {
            public void onTick(long millisUntilFinished) {

                Log.i("time_con", "DifferenceInSeconds -- >> " + millisUntilFinished / 1000);
                if (remainingTimeTv != null)
                    remainingTimeTv.setText("" + (millisUntilFinished / 1000));
                Log.i("time_con", "DifferenceInSeconds -- >> ");

            }

            public void onFinish() {
                if (remainingTimeTv != null) remainingTimeTv.setText("0");
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                    Log.i("time_con", "dialog -- >> null - false");
                }
            }

        }.start();

        Log.i("TAG", " --- onMetadataReceived uid --- >>>> " + "showQuizDialog -- show");

        int FIFTEEN_SEC = remaining + 3000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                isAnswered = false;
                // Do something after x seconds
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                    isAnswered = false;
                    Log.i("time_con", "dialog -- >> null - false");

                } else {
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                }

            }
        }, FIFTEEN_SEC);

    }

    /*void setRemainingTime(int totalTime) {

        Log.i("remaining", " -- >> " + totalTime);
        isTimeSet = false;
        mRemainingTimeRl.setVisibility(View.VISIBLE);

        new CountDownTimer(totalTime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                mTimeLeftTv.setText("" + millisUntilFinished / 1000);
                Log.i("remaining", "01 -- >> " + millisUntilFinished / 1000);
                Log.i("remaining", "02 -- >> running");


            }

            @Override
            public void onFinish() {
                mTimeLeftTv.setText("0");
                mRemainingTimeRl.setVisibility(View.GONE);
                isTimeSet = true;
                Log.i("remaining", "03 -- >> finished");
            }
        }.start();

    }*/

    void setStartTime(int totalTime, boolean isBegin) {

        mBeginTimeContentTv.setText(isBegin ? getString(R.string.begin_in) : getString(R.string.next_in));

        isTimeSet = false;
        mBeginTimeRl.setVisibility(View.VISIBLE);

        new CountDownTimer(totalTime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                mBeginTimeTv.setText("" + millisUntilFinished / 1000);

            }

            @Override
            public void onFinish() {
                mBeginTimeTv.setText("0");
                mBeginTimeRl.setVisibility(View.GONE);
                isTimeSet = true;
            }

        }.start();

    }

}

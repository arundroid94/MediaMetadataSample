package io.agora.openlive.activities;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.agora.openlive.R;
import io.agora.openlive.activities.pojo.Question;
import io.agora.openlive.stats.LocalStatsData;
import io.agora.openlive.stats.RemoteStatsData;
import io.agora.openlive.stats.StatsData;
import io.agora.openlive.ui.VideoGridContainer;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.rtm.ChannelAttributeOptions;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmMessage;

public class LiveActivity extends RtcBaseActivity {
    private static final String TAG = LiveActivity.class.getSimpleName();

    private VideoGridContainer mVideoGridContainer;
    private ImageView mMuteAudioBtn;
    private ImageView mMuteVideoBtn;
    private RelativeLayout mStartQuizRl;

    private VideoEncoderConfiguration.VideoDimensions mVideoDimension;
    private RtmChannel mRtmChannel;
    boolean isBroadcaster;
    private String mChannelAttributeKey;
    List<Question> questionArrayList = new ArrayList<>();
    private boolean isStartQuiz = false;
    private AlertDialog dialog;
    private int isCount = -1;
    private int TOTAL_TIME = 90000;
    private int INTERVAL = 30000;

    Question question = new Question();
    final Handler handler = new Handler();

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

        mStartQuizRl = findViewById(R.id.start_quiz_rl);
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
        if (isBroadcaster) startBroadcast();
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
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        // Do nothing at the moment
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {
        // Do nothing at the moment
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isBroadcaster) {
                    Toast.makeText(LiveActivity.this, "Joined isBroadcaster", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LiveActivity.this, "OnUser Audiance", Toast.LENGTH_SHORT).show();
                }
            }
        });*/
    }

    @Override
    public void onUserOffline(final int uid, int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeRemoteUser(uid);
            }
        });
    }

    @Override
    public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                renderRemoteUser(uid);
            }
        });
    }

    private void renderRemoteUser(int uid) {
        SurfaceView surface = prepareRtcVideo(uid, false);
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
            List<String> list = new ArrayList<>();
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
            rtmClient().deleteChannelAttributesByKeys(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);
        } else {
            finish();
        }

    }

    public void onLeaveClicked(View view) {

        if (isBroadcaster) {
            List<String> list = new ArrayList<>();
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
            rtmClient().deleteChannelAttributesByKeys(config().getSessionName(), list, new ChannelAttributeOptions(true), callback);
        } else {
            finish();
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
        if (view.isActivated()) {
            stopBroadcast();
        } else {
            startBroadcast();
        }
        view.setActivated(!view.isActivated());
    }

    public void startQuizOnClick(View view) {

        String[] singleChoiceItems = {" 30 Seconds ", " 1 minute ", " 2 minutes "};
        final int itemSelected = 0;
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
                        } else if (selectedIndex == 1) {
                            TOTAL_TIME = 180000;
                            INTERVAL = 60000;
                        } else {
                            TOTAL_TIME = 360000;
                            INTERVAL = 120000;
                        }

                    }
                })
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startQuizTimer(TOTAL_TIME, INTERVAL);
                        mStartQuizRl.setVisibility(View.GONE);
                        dialog.dismiss();
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

    private void startQuizTimer(int totalTime, int interval) {
        new CountDownTimer(totalTime, interval) {

            public void onTick(long millisUntilFinished) {
                isCount = isCount + 1;
                isStartQuiz = true;
            }

            public void onFinish() {
                isStartQuiz = false;
                isCount = -1;
                mStartQuizRl.setVisibility(View.VISIBLE);
            }

        }.start();
    }


    /**
     * API CALLBACK: rtm channel event listener
     */
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
                getChannelMemberList();
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

    }

    private void clearCallBackAndFinish() {

        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            //mRtmChannel.release();
            mRtmChannel = null;
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
            }
        });

    }


    private void updateChannelAttributes() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(getString(R.string.BROADCASTER_NAME), config().getBroadcasterName());
            jsonObject.put(getString(R.string.GROUP_NAME), config().getChannelName());
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
                        object.getBoolean("isShowQuiz"));
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
        if (isBroadcaster) {
            return 1024;
        } else {
            return 0;
        }
    }

    @Override
    public byte[] onReadyToSendMetadata(long l) {

        if (isBroadcaster && isStartQuiz && isCount >= 0) {

            byte[] data = null;
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

            Log.i(TAG, " --- onReadyToSendMetadata -- Broadcaster");
            return data;

        } else {

            Log.i(TAG, " --- onReadyToSendMetadata -- Audiance");
            Log.i(TAG, " --- onReadyToSendMetadata -- isBroadcaster  -- " + isBroadcaster);
            Log.i(TAG, " --- onReadyToSendMetadata -- isStartQuiz -- " + isStartQuiz);
            Log.i(TAG, " --- onReadyToSendMetadata -- isCount -- " + isCount);

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
                if (!object.getCount().equals(question.getCount())) {
                    question.setQuestion(object.getQuestion());
                    question.setCount(object.getCount());
                    question.setOptionA(object.getOptionA());
                    question.setOptionB(object.getOptionB());
                    question.setOptionC(object.getOptionC());
                    question.setOptionD(object.getOptionD());
                    runOnUiThread(new Runnable() {
                        public void run() {

                            showQuizDialog();

                        }
                    });


                }
                Log.i(TAG, " --- onMetadataReceived uid --- >>>> " + object.getQuestion());

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {

            Log.i(TAG, " --- onMetadataReceived uid --- >>>> size 0");

        }

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


    public void showQuizDialog() {

        dismissDialog();
        // create an alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // set the custom layout
        final View quizLayout = getLayoutInflater().inflate(R.layout.quiz_dialog_layout, null);
        builder.setView(quizLayout);
        dialog = builder.create();
        // create and show the alert dialog
        TextView questionTv = (TextView) quizLayout.findViewById(R.id.questionTv);
        TextView option_a = (TextView) quizLayout.findViewById(R.id.option_a);
        TextView option_b = (TextView) quizLayout.findViewById(R.id.option_b);
        TextView option_c = (TextView) quizLayout.findViewById(R.id.option_c);
        TextView option_d = (TextView) quizLayout.findViewById(R.id.option_d);
        questionTv.setText(question.getQuestion());
        option_a.setText(question.getOptionA());
        option_b.setText(question.getOptionB());
        option_c.setText(question.getOptionC());
        option_d.setText(question.getOptionD());
        option_a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });
        option_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismissDialog();
            }
        });
        option_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });
        option_d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });
        dialog.setCancelable(true);
        dialog.show();

        int FIFTEEN_SEC = 10000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after x seconds
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    dialog = null;
                } else {
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                }

            }
        }, FIFTEEN_SEC);

    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

}

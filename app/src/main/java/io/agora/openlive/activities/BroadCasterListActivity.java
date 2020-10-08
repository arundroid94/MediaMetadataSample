package io.agora.openlive.activities;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.agora.openlive.AgoraApplication;
import io.agora.openlive.R;
import io.agora.openlive.rtm.ChatManager;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;

import static io.agora.openlive.Constants.ACTIVITY_RESULT_CONN_ABORTED;

public class BroadCasterListActivity extends BaseActivity {

    private static final String TAG = BroadCasterListActivity.class.getSimpleName();

    //private ChatManager mChatManager;
    //private RtmClient mRtmClient;
    //private MyRtmClientListener mClientListener;
    private RtmChannel mRtmChannel;

    List<ChannelModel> dataSet = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private ChannelListAdapter mChannelListAdapter;

    public void onBackArrowPressed(View view) {
        onBackPressed();
    }

    /*class MyRtmClientListener implements RtmClientListener {

        @Override
        public void onConnectionStateChanged(final int state, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (state) {
                        case RtmStatusCode.ConnectionState.CONNECTION_STATE_RECONNECTING:
                            //showToast(getString(R.string.reconnecting));
                            break;
                        case RtmStatusCode.ConnectionState.CONNECTION_STATE_ABORTED:
                            //showToast(getString(R.string.account_offline));
                            setResult(ACTIVITY_RESULT_CONN_ABORTED);
                            finish();
                            break;
                    }
                }
            });
        }

        @Override
        public void onMessageReceived(final RtmMessage message, final String peerId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    *//*String content = message.getText();
                    if (peerId.equals(mPeerId)) {
                        MessageBean messageBean = new MessageBean(peerId, content, false);
                        messageBean.setBackground(getMessageColor(peerId));
                        mMessageBeanList.add(messageBean);
                        mMessageAdapter.notifyItemRangeChanged(mMessageBeanList.size(), 1);
                        mRecyclerView.scrollToPosition(mMessageBeanList.size() - 1);
                    } else {
                        MessageUtil.addMessageBean(peerId, content);
                    }*//*
                }
            });
        }

        @Override
        public void onTokenExpired() {

        }


    }*/

    /**
     * API CALLBACK: rtm channel event listener
     */
    class MyChannelListener implements RtmChannelListener {
        @Override
        public void onMemberCountUpdated(int i) {

        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {

            Log.i("onAttributesUpdated", " --- onAttributesUpdated");
            final List<ChannelModel> updatedList = new ArrayList<>();
            if (list != null) {
                for (RtmChannelAttribute data : list) {
                    try {
                        JSONObject jsonObject = new JSONObject(data.getValue());
                        String c_name = jsonObject.get(getString(R.string.GROUP_NAME)).toString();
                        String b_name = jsonObject.get(getString(R.string.BROADCASTER_NAME)).toString();
                        boolean is_recorded = jsonObject.getBoolean(getString(R.string.IS_RECORDED));
                        ChannelModel channelModel = new ChannelModel(c_name, b_name, is_recorded);
                        updatedList.add(channelModel);
                        Log.i("Attributes", " values -- " + c_name);
                        Log.i("Attributes", " values -- " + b_name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.i("Attributes", " values -- " + data.getKey());
                    Log.i("Attributes", " values -- " + data.getValue());
                }

                Log.i("Attributes", " values -- runOnUiThread");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChannelListAdapter.swap(updatedList);
                        Log.i("Attributes", " values -- runOnUiThread 22");
                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChannelListAdapter.swap(updatedList);
                    }
                });
                Log.i("Attributes", "  -- no values");
            }

        }

        @Override
        public void onMessageReceived(final RtmMessage message, final RtmChannelMember fromMember) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String account = fromMember.getUserId();
                    String msg = message.getText();
                   /* Log.i(LOG_TAG, "onMessageReceived account = " + account + " msg = " + msg);
                    MessageBean messageBean = new MessageBean(account, msg, false);
                    messageBean.setBackground(getMessageColor(account));
                    mMessageBeanList.add(messageBean);
                    mMessageAdapter.notifyItemRangeChanged(mMessageBeanList.size(), 1);
                    mRecyclerView.scrollToPosition(mMessageBeanList.size() - 1);*/
                }
            });
        }

        @Override
        public void onMemberJoined(final RtmChannelMember member) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /*userIdSet.add(member.getUserId());
                    mChannelMemberCount++;
                    refreshChannelTitle();*/

                }
            });
        }

        @Override
        public void onMemberLeft(final RtmChannelMember member) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    /*for (int i = 0; i < userIdSet.size(); i++) {
                        if (userIdSet.get(i).equalsIgnoreCase(member.getUserId())) {
                            userIdSet.remove(i);
                        }
                    }

                    mChannelMemberCount--;
                    refreshChannelTitle();*/
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audience_list);
        initUi();
        initAgoraEngine();

    }

    private void initUi() {
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mChannelListAdapter = new ChannelListAdapter(dataSet);
        mRecyclerView.setAdapter(mChannelListAdapter);
    }

    private void initAgoraEngine() {
        //config().setSessionName(getString(R.string.session));
        //mChatManager = AgoraApplication.the().getChatManager();
        //mRtmClient = mChatManager.getRtmClient();
        //mClientListener = new MyRtmClientListener();
        //mChatManager.registerListener(mClientListener);
        joinChannel();
    }

    private void loginAndJoinChannel() {

        /*final int min = 1000;
        final int max = 9999;
        List<RtmChannelAttribute> list = new ArrayList<>();
        final int random = new Random().nextInt((max - min) + 1) + min;

        //login in to channel
        mRtmClient.login(null, config().getSessionName() + random, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.i(TAG, "login success");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        joinChannel();

                    }
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i(TAG, "login failed: " + errorInfo.getErrorDescription());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });*/

    }

    private void joinChannel() {

        config().setSessionName(getString(R.string.session));
        // step 1: create a channel instance
        mRtmChannel = rtmClient().createChannel(config().getSessionName(), new MyChannelListener());
        if (mRtmChannel == null) {
            //showToast(getString(R.string.join_channel_failed));
            finish();
            return;
        }

        Log.e("channel", mRtmChannel + "");

        // step 2: join the channel
        mRtmChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {

                Log.e("mRtmChannel", "onSuccess --- ");
                getChannelMemberList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        getChannelAttributes();

                    }
                });


            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {

                Log.e("mRtmChannel", "onFailure --- " + errorInfo.getErrorDescription());
                Log.e("mRtmChannel", "onFailure --- " + errorInfo.getErrorCode());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //showToast(getString(R.string.join_channel_failed));
                        //finish();

                    }
                });
            }
        });
    }

    /*@Override
    public void onBackPressed() {

        if (mRtmChannel != null) {
            mRtmChannel.leave(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    finish();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {

                }
            });
            mRtmChannel.release();
            mRtmChannel = null;

        }
    }*/

    @Override
    protected void onDestroy() {
        leaveAndReleaseChannel();
        super.onDestroy();
    }

    private void leaveAndReleaseChannel() {
        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            //mRtmChannel.release();
            mRtmChannel = null;
        }
    }

    private void getChannelMemberList() {
        mRtmChannel.getMembers(new ResultCallback<List<RtmChannelMember>>() {
            @Override
            public void onSuccess(final List<RtmChannelMember> responseInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int count = responseInfo.size();
                    }
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e(TAG, "failed to get channel members, err: " + errorInfo.getErrorCode());
            }
        });

    }

    void getChannelAttributes() {

        rtmClient().getChannelAttributes(config().getSessionName(), new ResultCallback<List<RtmChannelAttribute>>() {
            @Override
            public void onSuccess(List<RtmChannelAttribute> rtmChannelAttributes) {

                final List<ChannelModel> list = new ArrayList<>();
                if (rtmChannelAttributes != null && rtmChannelAttributes.size() > 0) {
                    for (RtmChannelAttribute data : rtmChannelAttributes) {
                        try {
                            JSONObject jsonObject = new JSONObject(data.getValue());
                            String c_name = jsonObject.get(getString(R.string.GROUP_NAME)).toString();
                            String b_name = jsonObject.get(getString(R.string.BROADCASTER_NAME)).toString();
                            boolean is_recorded = jsonObject.getBoolean(getString(R.string.IS_RECORDED));
                            ChannelModel channelModel = new ChannelModel(c_name, b_name, is_recorded);
                            list.add(channelModel);
                            Log.i("Attributes", " values -- " + c_name);
                            Log.i("Attributes", " values -- " + b_name);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.i("Attributes", " values -- " + data.getKey());
                        Log.i("Attributes", " values -- " + data.getValue());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChannelListAdapter.swap(list);
                        }
                    });
                } else {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChannelListAdapter.swap(list);
                        }
                    });

                    Log.i("Attributes", "  -- no values");
                }

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i("Attributes", "  -- onFailure");
            }
        });

    }

    public class ChannelListAdapter extends RecyclerView.Adapter<ChannelListAdapter.MyViewHolder> {

        List<ChannelModel> chanelList;

        public ChannelListAdapter(List<ChannelModel> data) {
            this.chanelList = data;
        }

        public void refresh() {
            chanelList.clear();
            notifyDataSetChanged();
        }

        public void swap(List<ChannelModel> list) {
            chanelList.clear();
            chanelList.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lsit_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            final ChannelModel data = chanelList.get(position);
            holder.channelNameTv.setText(data.getChannelName());
            holder.broadcasterNameTv.setText("By - " + data.getBroadcasterName());
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mRtmChannel.leave(new ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent intent = new Intent(BroadCasterListActivity.this, LiveActivity.class);
                            String room = data.getChannelName();
                            config().setChannelName(room);
                            config().setBroadcasterName(data.getBroadcasterName() != null ? data.getBroadcasterName() : getString(R.string.guest));
                            boolean isRecorded = data.getRecorded();
                            intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE);
                            intent.putExtra("isRecorded", isRecorded);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(ErrorInfo errorInfo) {
                            Intent intent = new Intent(BroadCasterListActivity.this, LiveActivity.class);
                            String room = data.getChannelName();
                            config().setChannelName(room);
                            config().setBroadcasterName(data.getBroadcasterName() != null ? data.getBroadcasterName() : getString(R.string.guest));
                            boolean isRecorded = data.getRecorded();
                            intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE);
                            intent.putExtra("isRecorded", isRecorded);
                            startActivity(intent);
                            finish();
                        }
                    });

                    /*Intent intent = new Intent(BroadCasterListActivity.this, LiveActivity.class);
                    String room = data.getChannelName();
                    config().setChannelName(room);
                    config().setBroadcasterName(data.getBroadcasterName() != null ? data.getBroadcasterName() : getString(R.string.guest));
                    intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE);
                    startActivity(intent);*/

                }
            });

        }

        @Override
        public int getItemCount() {
            return chanelList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {

            CardView cardView;
            TextView channelNameTv;
            TextView broadcasterNameTv;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                this.cardView = (CardView) itemView.findViewById(R.id.card_view);
                this.channelNameTv = (TextView) itemView.findViewById(R.id.channelName);
                this.broadcasterNameTv = (TextView) itemView.findViewById(R.id.broadcasterName);
            }

        }
    }

    static class ChannelModel {

        String channelName;
        String broadcasterName;
        boolean isRecorded;


        public ChannelModel(String channelName, String broadcasterName, boolean isRecorded) {
            this.channelName = channelName;
            this.broadcasterName = broadcasterName;
            this.isRecorded = isRecorded;
        }

        public Boolean getRecorded() {
            return isRecorded;
        }

        public void setRecorded(boolean recorded) {
            isRecorded = recorded;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public String getBroadcasterName() {
            return broadcasterName;
        }

        public void setBroadcasterName(String broadcasterName) {
            this.broadcasterName = broadcasterName;
        }
    }


}
package io.agora.openlive.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.agora.openlive.R;
import io.agora.rtc.Constants;

public class RoleActivity extends BaseActivity {

    private int mRole = 0;
    private static final int PERMISSION_REQ_CODE = 1 << 4;
    private String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    long startTime = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_ONE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
    long endTime = ((System.currentTimeMillis()) + (50000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);
    }


    private void checkPermission() {
        boolean granted = true;
        for (String per : PERMISSIONS) {
            if (!permissionGranted(per)) {
                granted = false;
                break;
            }
        }

        if (granted) {

            if (mRole == Constants.CLIENT_ROLE_BROADCASTER) {
                goToMainActivity();
            } else if (mRole == Constants.CLIENT_ROLE_AUDIENCE) {
                goToAudienceListActivity();
            } else {
                Toast.makeText(this, "do nothing", Toast.LENGTH_SHORT).show();
            }

        } else {
            requestPermissions();
        }
    }

    private void goToMainActivity() {

        /*Intent intent = new Intent(getIntent());
        intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, Constants.CLIENT_ROLE_BROADCASTER);
        intent.setClass(getApplicationContext(), MainActivity.class);
        startActivity(intent);*/

        Intent intent1 = new Intent(RoleActivity.this, MainActivity.class);
        intent1.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, Constants.CLIENT_ROLE_BROADCASTER);
        startActivity(intent1);
    }

    private void goToAudienceListActivity() {

        /*Intent intent = new Intent(getIntent());
        intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, Constants.CLIENT_ROLE_AUDIENCE);
        intent.setClass(getApplicationContext(), AudienceListActivity.class);
        startActivity(intent);*/

        Intent intent = new Intent(RoleActivity.this, BroadCasterListActivity.class);
        intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, Constants.CLIENT_ROLE_AUDIENCE);
        startActivity(intent);
    }

    private boolean permissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(
                this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                granted = (result == PackageManager.PERMISSION_GRANTED);
                if (!granted) break;
            }

            if (granted) {
                if (mRole == Constants.CLIENT_ROLE_BROADCASTER) {
                    goToMainActivity();
                } else if (mRole == Constants.CLIENT_ROLE_AUDIENCE) {
                    goToAudienceListActivity();
                } else {
                    Toast.makeText(this, "do nothing", Toast.LENGTH_SHORT).show();
                }
            } else {
                toastNeedPermissions();
            }
        }
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        RelativeLayout layout = findViewById(R.id.role_title_layout);
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) layout.getLayoutParams();
        params.height += mStatusBarHeight;
        layout.setLayoutParams(params);

        layout = findViewById(R.id.role_content_layout);
        params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
        params.topMargin = (mDisplayMetrics.heightPixels -
                layout.getMeasuredHeight()) * 3 / 7;
        layout.setLayoutParams(params);
    }

    public void onJoinAsBroadcaster(View view) {
        mRole = Constants.CLIENT_ROLE_BROADCASTER;
        checkPermission();
        //gotoLiveActivity(Constants.CLIENT_ROLE_BROADCASTER);
    }

    public void onJoinAsAudience(View view) {
        mRole = Constants.CLIENT_ROLE_AUDIENCE;
        checkPermission();
        //gotoLiveActivity(Constants.CLIENT_ROLE_AUDIENCE);
    }

    private void gotoLiveActivity(int role) {
        Intent intent = new Intent(getIntent());
        intent.putExtra(io.agora.openlive.Constants.KEY_CLIENT_ROLE, role);
        intent.setClass(getApplicationContext(), LiveActivity.class);
        startActivity(intent);
    }

    public void onBackArrowPressed(View view) {
        finish();
    }

    private void toastNeedPermissions() {
        Toast.makeText(this, R.string.need_necessary_permissions, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();

        /*new CountDownTimer(60000, 1000) {

            public void onTick(long millisUntilFinished) {

                newMethod();
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {



            }

        }.start();*/

        //calculateTimeIntervals();


    }


    void newMethod() {

        long currentTime = (System.currentTimeMillis()) / 1000;

        Calendar c_start = Calendar.getInstance(Locale.ENGLISH);
        c_start.setTimeInMillis(startTime * 1000);
        String date_start = DateFormat.format("hh:mm:ss", c_start).toString();

        Calendar c_end = Calendar.getInstance(Locale.ENGLISH);
        c_end.setTimeInMillis(endTime * 1000);
        String date_end = DateFormat.format("hh:mm:ss", c_end).toString();

        Calendar c_current = Calendar.getInstance(Locale.ENGLISH);
        c_current.setTimeInMillis(currentTime * 1000);
        String date_current = DateFormat.format("hh:mm:ss", c_current).toString();

        Log.i("time_con", "date_start -- >> " + date_start);
        Log.i("time_con", "date_end -- >> " + date_end);
        Log.i("time_con", "date_current -- >> " + date_current);

        if (c_current.getTime().after(c_start.getTime()) && c_current.getTime().before(c_end.getTime())) {

            Log.i("time_con", "answer -- >> true");
            long seconds = getDifferenceInSeconds(date_end, date_current);
            Log.i("time_con", "DifferenceInSeconds -- >> " + seconds);

        } else {

            Log.i("time_con", "answer -- >> false");

        }


    }

    void calculateTimeIntervals() {

        Date currentTime = Calendar.getInstance().getTime();
        Log.i("time_con", "currentTime -- >> " + currentTime);

        int value = 30000;

        long a = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_ONE - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long b = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_TWO - io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long c = ((System.currentTimeMillis()) + (io.agora.openlive.Constants.INTERVAL_THREE - io.agora.openlive.Constants.ONE_SEC)) / 1000;

        long d = ((System.currentTimeMillis()) + (50000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long e = ((System.currentTimeMillis()) + (80000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;
        long f = ((System.currentTimeMillis()) + (110000 + io.agora.openlive.Constants.ONE_SEC)) / 1000;

        Calendar cal_a = Calendar.getInstance(Locale.ENGLISH);
        cal_a.setTimeInMillis(a * 1000);
        String date_a = DateFormat.format("hh:mm:ss", cal_a).toString();

        Calendar cal_b = Calendar.getInstance(Locale.ENGLISH);
        cal_b.setTimeInMillis(b * 1000);
        String date_b = DateFormat.format("hh:mm:ss", cal_b).toString();

        Calendar cal_c = Calendar.getInstance(Locale.ENGLISH);
        cal_c.setTimeInMillis(c * 1000);
        String date_c = DateFormat.format("hh:mm:ss", cal_c).toString();


        Calendar cal_d = Calendar.getInstance(Locale.ENGLISH);
        cal_d.setTimeInMillis(d * 1000);
        String date_d = DateFormat.format("hh:mm:ss", cal_d).toString();
        Calendar cal_e = Calendar.getInstance(Locale.ENGLISH);
        cal_e.setTimeInMillis(e * 1000);
        String date_e = DateFormat.format("hh:mm:ss", cal_e).toString();
        Calendar cal_f = Calendar.getInstance(Locale.ENGLISH);
        cal_f.setTimeInMillis(f * 1000);
        String date_f = DateFormat.format("hh:mm:ss", cal_f).toString();

        getDifferenceInSeconds(date_a, date_d);


        Log.i("time_con", "date_a -- >> " + date_a);
        Log.i("time_con", "date_d -- >> " + date_d);
        Log.i("time_con", "date_b -- >> " + date_b);
        Log.i("time_con", "date_e -- >> " + date_e);
        Log.i("time_con", "date_c -- >> " + date_c);
        Log.i("time_con", "date_f -- >> " + date_f);

        long tsLong = (System.currentTimeMillis() - 1000) / 1000;
        long ts_30 = (System.currentTimeMillis() + (30000 + 1000)) / 1000;
        long ts_20 = (System.currentTimeMillis() + 30000) / 1000;

        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(tsLong * 1000);
        String date = DateFormat.format("hh:mm:ss", cal).toString();

        Calendar cal_20 = Calendar.getInstance(Locale.ENGLISH);
        cal_20.setTimeInMillis(ts_20 * 1000);
        String date_20 = DateFormat.format("hh:mm:ss", cal_20).toString();

        Calendar cal_30 = Calendar.getInstance(Locale.ENGLISH);
        cal_30.setTimeInMillis(ts_30 * 1000);
        String date_30 = DateFormat.format("hh:mm:ss", cal_30).toString();

        Log.i("time_con", "date -- >> " + date);
        Log.i("time_con", "date_30 -- >> " + date_30);
        Log.i("time_con", "date_20 -- >> " + date_20);


        Date x = cal_20.getTime();
        if (x.after(cal.getTime()) && x.before(cal_30.getTime())) {
            //checkes whether the current time is between 14:49:00 and 20:11:13.
            Log.i("time_con", "answer -- >> true");
        } else {
            Log.i("time_con", "answer -- >> false");
        }


    }

    private long getDifferenceInSeconds(String date_a, String date_d) {

        long diffInSec = 0;
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        try {
            Date date1 = format.parse(date_a);
            Date date2 = format.parse(date_d);
            long diffInMs = 0;
            if (date1 != null && date2 != null) {
                diffInMs = date1.getTime() - date2.getTime();
            }

            diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
            Log.i("time_con", "diffInSec -- >> " + diffInSec);
        } catch (ParseException ex) {
            ex.printStackTrace();
            return 10;
        }

        return Math.abs(diffInSec);
    }
}

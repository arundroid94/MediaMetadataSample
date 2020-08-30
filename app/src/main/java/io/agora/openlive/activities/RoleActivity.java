package io.agora.openlive.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import io.agora.openlive.R;
import io.agora.openlive.activities.pojo.Question;
import io.agora.rtc.Constants;

public class RoleActivity extends BaseActivity {

    private int mRole = 0;
    private static final int PERMISSION_REQ_CODE = 1 << 4;
    private String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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
        //showQuizDialog();
    }
}

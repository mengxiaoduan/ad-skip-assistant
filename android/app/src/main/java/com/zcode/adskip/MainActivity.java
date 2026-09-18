package com.zcode.adskip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView tvStatus, tvCount, tvTip;
    private Switch swWorking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("adskip", MODE_PRIVATE);

        tvStatus = findViewById(R.id.tvStatus);
        tvCount = findViewById(R.id.tvCount);
        tvTip = findViewById(R.id.tvTip);
        swWorking = findViewById(R.id.swWorking);
        MaterialButton btnSettings = findViewById(R.id.btnSettings);
        MaterialButton btnTest = findViewById(R.id.btnTest);

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                Toast.makeText(MainActivity.this,
                        "在列表中找到「广告跳过助手」并开启开关", Toast.LENGTH_LONG).show();
            }
        });
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MockAdActivity.class));
            }
        });
        swWorking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                prefs.edit().putBoolean("working", checked).apply();
                tvTip.setText(checked
                        ? "工作中：检测到广告「跳过」按钮会自动点击"
                        : "已暂停：不会自动点击任何按钮");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = isServiceEnabled();
        tvStatus.setText(enabled ? "● 无障碍服务已开启" : "○ 无障碍服务未开启");
        tvStatus.setTextColor(getResources().getColor(enabled ? R.color.ok_green : R.color.sub));
        swWorking.setChecked(prefs.getBoolean("working", true));
        int count = prefs.getInt("count", 0);
        long last = prefs.getLong("last_at", 0);
        String extra = count > 0 ? "（最近一次 " + formatTime(last) + "）" : "";
        tvCount.setText("已自动跳过 " + count + " 次广告" + extra);
    }

    private boolean isServiceEnabled() {
        String setting = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return setting != null
                && setting.contains(getPackageName())
                && setting.contains(SkipAdService.class.getSimpleName());
    }

    private String formatTime(long ts) {
        return android.text.format.DateFormat.format("MM-dd HH:mm", ts).toString();
    }
}

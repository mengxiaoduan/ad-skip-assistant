package com.zcode.adskip;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 模拟开屏广告页：右上角有一个「跳过」按钮，用于自测服务的自动点击效果。
 * 服务点击与手动点击都会进入这里，通过标记区分并给出不同的反馈。
 */
public class MockAdActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int countdown = 3;
    private boolean done = false;
    private TextView tvStatus;
    private Button btnSkip;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (done) return;
            countdown--;
            if (countdown > 0) {
                btnSkip.setText("跳过 " + countdown);
                handler.postDelayed(tick, 1000);
            } else {
                btnSkip.setText("跳过");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_ad);
        tvStatus = findViewById(R.id.tvAdStatus);
        btnSkip = findViewById(R.id.btnSkip);
        SkipAdService.clearSelfTest();
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAd();
            }
        });
        handler.postDelayed(tick, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!done) finishAd();
            }
        }, 9000);
    }

    private void finishAd() {
        if (done) return;
        done = true;
        boolean byService = SkipAdService.isSelfTest();
        SkipAdService.clearSelfTest();
        if (byService) {
            tvStatus.setText("✓ 被无障碍服务自动点击了！真实广告的「跳过」按钮也会这样被自动点掉");
            tvStatus.setTextColor(0xFF7FE8B5);
        } else {
            tvStatus.setText("这是你手动点的。开启无障碍服务后，这一步会自动完成");
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 2400);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}

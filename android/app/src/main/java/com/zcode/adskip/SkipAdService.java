package com.zcode.adskip;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

/**
 * 无障碍服务：扫描当前窗口，找到开屏广告上的「跳过」按钮并自动点击。
 * 只点击文本匹配「跳过 / skip」（或 viewId 含 skip）且尺寸合理
 * （不会超过半屏宽、四分之一屏高）的可点击节点，最大限度避免误触。
 */
public class SkipAdService extends AccessibilityService {

    private static final long SCAN_THROTTLE_MS = 150;
    private static final long SCAN_DEBOUNCE_MS = 60;
    private static final long TOAST_THROTTLE_MS = 5000;

    /** 自测标记：服务即将点击时置位，模拟广告页据此显示"自动"还是"手动" */
    private static final boolean[] SELF_TEST_FLAG = new boolean[1];

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            scanNow();
        }
    };

    private SharedPreferences prefs;
    private long lastScanAt = 0;
    private long lastToastAt = 0;

    public static void markSelfTest() {
        synchronized (SELF_TEST_FLAG) {
            SELF_TEST_FLAG[0] = true;
        }
    }

    public static void clearSelfTest() {
        synchronized (SELF_TEST_FLAG) {
            SELF_TEST_FLAG[0] = false;
        }
    }

    public static boolean isSelfTest() {
        synchronized (SELF_TEST_FLAG) {
            return SELF_TEST_FLAG[0];
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("adskip", MODE_PRIVATE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        if (prefs == null) {
            prefs = getSharedPreferences("adskip", MODE_PRIVATE);
        }
        if (!prefs.getBoolean("working", true)) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastScanAt < SCAN_THROTTLE_MS) return;
        lastScanAt = now;
        handler.removeCallbacks(scanTask);
        handler.postDelayed(scanTask, SCAN_DEBOUNCE_MS);
    }

    private void scanNow() {
        if (prefs == null || !prefs.getBoolean("working", true)) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        AccessibilityNodeInfo target = findTarget(root, screenW, screenH, 0);
        if (target != null) {
            clickTarget(target);
        }
    }

    private AccessibilityNodeInfo findTarget(AccessibilityNodeInfo node, int screenW, int screenH, int depth) {
        if (node == null || depth > 60) return null;
        AccessibilityNodeInfo hit = asSkipTarget(node, screenW, screenH);
        if (hit != null) return hit;
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findTarget(child, screenW, screenH, depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * 判断节点是否是「跳过」按钮；是则返回真正可点击的节点（自身或 4 层以内祖先），否则返回 null。
     */
    private AccessibilityNodeInfo asSkipTarget(AccessibilityNodeInfo node, int screenW, int screenH) {
        CharSequence textCs = node.getText();
        CharSequence descCs = node.getContentDescription();
        String s = textCs != null ? textCs.toString().trim()
                : (descCs != null ? descCs.toString().trim() : "");
        String viewId = node.getViewIdResourceName();
        boolean textHit = !s.isEmpty() && s.length() <= 12
                && (s.contains("跳过") || s.toLowerCase().contains("skip"));
        boolean idHit = viewId != null && viewId.contains("skip");
        if (!textHit && !idHit) return null;

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() <= 0 || bounds.height() <= 0) return null;
        if (bounds.width() > screenW * 0.5f || bounds.height() > screenH * 0.25f) return null;

        AccessibilityNodeInfo n = node;
        for (int i = 0; i < 4 && n != null; i++) {
            if (n.isClickable()) return n;
            n = n.getParent();
        }
        return null;
    }

    private void clickTarget(AccessibilityNodeInfo target) {
        markSelfTest();
        boolean ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        clearSelfTestDelay();
        if (!ok) return;
        long now = System.currentTimeMillis();
        int count = prefs.getInt("count", 0) + 1;
        prefs.edit().putInt("count", count).putLong("last_at", now).apply();
        if (now - lastToastAt > TOAST_THROTTLE_MS) {
            lastToastAt = now;
            Toast.makeText(this, "已自动跳过广告 ✓ 第 " + count + " 次", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearSelfTestDelay() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearSelfTest();
            }
        }, 600);
    }

    @Override
    public void onInterrupt() {
    }
}

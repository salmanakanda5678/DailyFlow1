package com.dailyroutine.util;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.dailyroutine.database.DatabaseHelper;

public class ReelsBlockerService extends AccessibilityService {

    private long lastBlockTime = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        PreferenceManager prefs = PreferenceManager.getInstance(this);
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        boolean isBlocking = prefs.isReelsBlockerEnabled() || db.getActiveFocusTask() != null;
        if (!isBlocking) return;

        CharSequence pkgChar = event.getPackageName();
        if (pkgChar == null) return;
        String packageName = pkgChar.toString();

        if (!packageName.contains("youtube") && !packageName.contains("facebook")
                && !packageName.contains("instagram") && !packageName.contains("musically")
                && !packageName.contains("trill")) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            root = event.getSource();
        }
        if (root == null) return;

        try {
            boolean isReels = isReelsOrShortsActive(root, packageName, event);
            if (isReels) {
                long now = System.currentTimeMillis();
                if (now - lastBlockTime > 500) {
                    lastBlockTime = now;
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    mainHandler.post(() -> Toast.makeText(getApplicationContext(),
                            "🚫 Reels & Shorts দেখা নিষেধ! কাজে মনোযোগ দিন। 🚀",
                            Toast.LENGTH_SHORT).show());
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { root.recycle(); } catch (Exception ignored) {}
        }
    }

    private boolean isReelsOrShortsActive(AccessibilityNodeInfo root, String pkgName, AccessibilityEvent event) {
        CharSequence eventClass = event.getClassName();
        String className = eventClass != null ? eventClass.toString().toLowerCase() : "";

        // Direct Activity/Fragment class name match for Reels/Shorts
        if (className.contains("fbshorts") || className.contains("reelviewer")
                || className.contains("watchreel") || className.contains("clipsviewer")
                || className.contains("shortsactivity") || className.contains("reelsactivity")) {
            return true;
        }

        ScanState state = new ScanState();
        scanTree(root, pkgName, state, 0);

        // Rule 1: If Reels/Shorts bottom tab is selected, IT IS REELS!
        if (state.isReelsTabSelected) {
            return true;
        }

        // Rule 2: App specific check (Ignore regular post comment section)
        if (pkgName.contains("facebook")) {
            return state.hasFbReelViewer || (state.hasReelAudioText && !state.isHomeTabSelected && !state.isCommentSection);
        } else if (pkgName.contains("youtube")) {
            return state.hasYoutubeShortsPlayer || (state.hasShortsText && !state.isHomeTabSelected && !state.isCommentSection);
        } else if (pkgName.contains("instagram")) {
            return state.hasIgClipsViewer || (state.hasReelAudioText && !state.isHomeTabSelected && !state.isCommentSection);
        } else if (pkgName.contains("musically") || pkgName.contains("trill")) {
            return state.hasTikTokFeed;
        }

        return false;
    }

    private static class ScanState {
        boolean isHomeTabSelected = false;
        boolean isReelsTabSelected = false;
        boolean isCommentSection = false;
        boolean hasFbReelViewer = false;
        boolean hasYoutubeShortsPlayer = false;
        boolean hasShortsText = false;
        boolean hasIgClipsViewer = false;
        boolean hasTikTokFeed = false;
        boolean hasReelAudioText = false;
    }

    private void scanTree(AccessibilityNodeInfo node, String pkgName, ScanState state, int depth) {
        if (node == null || depth > 35) return;

        CharSequence viewIdChar = node.getViewIdResourceName();
        CharSequence textChar = node.getText();
        CharSequence descChar = node.getContentDescription();

        String id = viewIdChar != null ? viewIdChar.toString().toLowerCase() : "";
        String text = textChar != null ? textChar.toString().toLowerCase() : "";
        String desc = descChar != null ? descChar.toString().toLowerCase() : "";
        boolean isSelected = node.isSelected() || node.isFocused();

        // 1. Check if Home tab is selected (signals normal Home Feed)
        if (isSelected || id.contains("home_tab") || id.contains("feed_tab")) {
            if (desc.contains("home") || text.contains("home") || desc.contains("হোম") || desc.contains("news feed")) {
                state.isHomeTabSelected = true;
            }
        }

        // 2. Check if Reels / Shorts tab is selected (signals bottom Reels/Shorts button was clicked)
        if (desc.contains("reels") || text.contains("reels") || desc.contains("shorts") || text.contains("shorts")
                || id.contains("reels_tab") || id.contains("shorts_tab") || id.contains("clips_tab")) {
            if (isSelected || id.contains("selected") || desc.contains("selected")) {
                state.isReelsTabSelected = true;
            }
        }

        // 3. Check for Post Comment Section (Regular Facebook / YouTube / Instagram Comments)
        if (id.contains("comment_list") || id.contains("comments_sheet") || id.contains("comment_flyout")
                || id.contains("comments_container") || id.contains("comment_dialog")
                || text.equalsIgnoreCase("comments") || desc.equalsIgnoreCase("comments")
                || text.contains("most relevant") || desc.contains("most relevant")
                || text.contains("write a public comment") || text.contains("write a comment")) {
            state.isCommentSection = true;
        }

        // 4. App-specific Reel Player & Viewer checks
        if (pkgName.contains("facebook")) {
            if (id.contains("reel_viewer") || id.contains("reels_viewer") || id.contains("fb_shorts")
                    || id.contains("reel_video_player") || id.contains("reels_video_view")
                    || id.contains("reels_media_view") || id.contains("reels_full_screen")
                    || id.contains("reels_tab_viewer") || id.contains("clips_viewer")) {
                state.hasFbReelViewer = true;
            }
            if (text.contains("original audio") || text.contains("মূল অডিও") || desc.contains("remix")
                    || desc.contains("original audio") || text.contains("create with this audio")
                    || text.contains("watch reels") || desc.contains("watch reels") || desc.contains("reels viewer")
                    || desc.contains("send in messenger") || text.contains("send in messenger")) {
                state.hasReelAudioText = true;
            }
        } else if (pkgName.contains("youtube")) {
            if (id.contains("shorts_player") || id.contains("reel_player") || id.contains("shorts_container")
                    || id.contains("shorts_video_cell") || id.contains("shorts_surface_view")
                    || id.contains("reel_viewer") || id.contains("reel_watch") || id.contains("reel_recycler")) {
                state.hasYoutubeShortsPlayer = true;
            }
            if (desc.contains("shorts player") || desc.equals("shorts") || text.equals("shorts")
                    || desc.contains("shorts tab") || text.equalsIgnoreCase("remix") || desc.contains("remix")
                    || desc.contains("create a short") || text.contains("create a short")) {
                state.hasShortsText = true;
            }
        } else if (pkgName.contains("instagram")) {
            if (id.contains("clips_viewer") || id.contains("clips_video_container")
                    || id.contains("clips_video_player") || id.contains("reels_viewer")
                    || id.contains("clips_swipe_refresh_layout")) {
                state.hasIgClipsViewer = true;
            }
            if (text.contains("original audio") || desc.contains("original audio") || text.contains("use audio")
                    || desc.contains("reels viewer")) {
                state.hasReelAudioText = true;
            }
        } else if (pkgName.contains("musically") || pkgName.contains("trill")) {
            if (id.contains("view_pager") || id.contains("feed_view") || id.contains("main_page_fragment")) {
                state.hasTikTokFeed = true;
            }
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                scanTree(child, pkgName, state, depth + 1);
                try { child.recycle(); } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        String service = context.getPackageName() + "/" + ReelsBlockerService.class.getCanonicalName();
        try {
            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    return settingValue.contains(service);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public void onInterrupt() {
    }
}

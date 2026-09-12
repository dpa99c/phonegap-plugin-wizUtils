package jp.wizcorp.phonegap.plugin.wizUtilsPlugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.Display;
import android.webkit.WebView;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.File;
import java.util.List;

/**
 *
 * @author Wizcorp Inc. [ Incorporated Wizards ] Copyright 2014
 * file WizUtilsPlugin.java for PhoneGap
 *
 */
public class WizUtilsPlugin extends CordovaPlugin {

    private Activity act;
    private WebView _webView;
    private int sdk;

    @Override
    public void initialize(org.apache.cordova.CordovaInterface cordova, org.apache.cordova.CordovaWebView webView) {
        act = cordova.getActivity();
        _webView = (WebView) webView.getView();
        super.initialize(cordova, webView);
        sdk = android.os.Build.VERSION.SDK_INT;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("getAppFileName")) {
            String label = "(unknown)";
            PackageManager pm = act.getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(act.getPackageName(), 0);
                String fullDir = ai.publicSourceDir;
                label = fullDir.split(File.separator)[fullDir.split(File.separator).length - 1];
            } catch (PackageManager.NameNotFoundException e) {
            }
            callbackContext.success(label);
            return true;

        } else if (action.equals("getBundleVersion")) {
            PackageInfo pInfo = null;
            try {
                pInfo = act.getPackageManager().getPackageInfo(act.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
            String bundleVersion = "0";
            if (pInfo != null) {
                bundleVersion = pInfo.versionName;
            }
            callbackContext.success(bundleVersion);
            return true;

        } else if (action.equals("getBundleDisplayName")) {
            PackageManager pm = act.getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo( act.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                ai = null;
            }
            // If unknown we set to "(unknown)"
            String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
            callbackContext.success(applicationName);
            return true;

        } else if (action.equals("getBundleIdentifier")) {

            String identifier;
            try {
                PackageManager manager = act.getPackageManager();
                PackageInfo info = manager.getPackageInfo(act.getPackageName(), 0);
                identifier = info.packageName;
            } catch (Exception e) {
                identifier = null;
            }

            callbackContext.success(identifier);
            return true;

        } else if (action.equals("getDeviceHeight")) {

            Display mDisplay = act.getWindowManager().getDefaultDisplay();
            int appHeight = mDisplay.getHeight();
            callbackContext.success(appHeight);
            return true;

        } else if (action.equals("getDeviceWidth")) {

            Display mDisplay = act.getWindowManager().getDefaultDisplay();
            int appWidth = mDisplay.getWidth();
            callbackContext.success(appWidth);
            return true;

        } else if (action.equals("getText")) {

            String pasteText;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) cordova.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                pasteText = clipboard.getText().toString();
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) cordova.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item;
                if (clipboard.getPrimaryClip().getItemCount() > 0) {
                    item = clipboard.getPrimaryClip().getItemAt(0);
                    pasteText = item.getText().toString();
                } else {
                    // clipboard was empty
                    pasteText = "";
                }
            }
            callbackContext.success(pasteText);
            return true;

        } else if (action.equals("setText")) {

            String text2save = args.getString(0);
            if (text2save.length() > 0) {
                if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) cordova.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(text2save);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) cordova.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Clip", text2save);
                    clipboard.setPrimaryClip(clip);
                }
            }
            callbackContext.success();
            return true;

        } else if (action.equals("restart")) {

            // TODO: check for displaying the splash screen
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _webView.reload();
                }
            });
            return true;
        } else if (action.equals("getPhysicalCameraBounds")) {
            this.getPhysicalCameraBounds(callbackContext);
            return true;
        }
        return false;
    }

    private void getPhysicalCameraBounds(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            try {
                JSONObject result = new JSONObject();

                // Cutout APIs were introduced in Android 9 (API 28 / Pie)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    View decorView = cordova.getActivity().getWindow().getDecorView();
                    WindowInsets insets = decorView.getRootWindowInsets();

                    if (insets != null) {
                        DisplayCutout cutout = insets.getDisplayCutout();

                        if (cutout != null) {
                            List<Rect> boundingRects = cutout.getBoundingRects();
                            Rect topCutout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                    ? cutout.getBoundingRectTop()
                                    : null;

                            if ((topCutout == null || topCutout.isEmpty()) && boundingRects != null) {
                                for (Rect rect : boundingRects) {
                                    if (rect.top == 0 || rect.top < 100) {
                                        topCutout = rect;
                                        break;
                                    }
                                }
                            }

                            if (topCutout != null && !topCutout.isEmpty()) {
                                DisplayMetrics metrics = cordova.getActivity()
                                        .getResources().getDisplayMetrics();
                                float density = metrics.density;

                                RectF cameraBounds = new RectF(topCutout);

                                // The cutout path is closer to the physical camera shape than its enclosing rect.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Path cutoutPath = cutout.getCutoutPath();
                                    if (cutoutPath != null) {
                                        Path topCutoutPath = new Path();
                                        Path topRegion = new Path();
                                        topRegion.addRect(new RectF(topCutout), Path.Direction.CW);
                                        if (topCutoutPath.op(cutoutPath, topRegion, Path.Op.INTERSECT)) {
                                            RectF pathBounds = new RectF();
                                            topCutoutPath.computeBounds(pathBounds, true);
                                            if (!pathBounds.isEmpty()) {
                                                cameraBounds = pathBounds;
                                            }
                                        }
                                    }
                                }

                                float topPx = cameraBounds.top;
                                float heightPx = cameraBounds.height();
                                float leftPx = cameraBounds.left;
                                float widthPx = cameraBounds.width();

                                // Returns measurements in DP for CSS/JS layout consumption
                                result.put("top", topPx / density);
                                result.put("height", heightPx / density);
                                result.put("left", leftPx / density);
                                result.put("width", widthPx / density);
                                result.put("bottom", (topPx + heightPx) / density);
                                result.put("hasCutout", true);

                                callbackContext.success(result);
                                return;
                            }
                        }
                    }
                }

                // Fallback for devices prior to API 28 or those without a cutout
                result.put("top", 0);
                result.put("height", 0);
                result.put("left", 0);
                result.put("width", 0);
                result.put("bottom", 0);
                result.put("hasCutout", false);

                callbackContext.success(result);

            } catch (Exception e) {
                callbackContext.error("Failed to query camera bounds: " + e.getMessage());
            }
        });
    }
}

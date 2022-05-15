package com.reactnativeaccessibilitymanagerplugin;

import static com.reactnativeaccessibilitymanagerplugin.AccessibilityManagerPluginModule.*;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.facebook.react.bridge.ReactApplicationContext;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WhatsappAccessibilityService extends AccessibilityService {
  public String signature = "";
  private static ReactApplicationContext reactContext;
  public static String AccessibilityPreference = "AccessibilityPreference";
  public static String isServiceBinded = "isServiceBinded";

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {

    if (getRootInActiveWindow() == null) {
      return;
    }

    AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(getRootInActiveWindow());

    // Whatsapp Message EditText id
    List<AccessibilityNodeInfoCompat> messageNodeList2 = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/caption");
    List<AccessibilityNodeInfoCompat> messageNodeList = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry");

    if ((messageNodeList2 == null || messageNodeList2.isEmpty()) && (messageNodeList == null || messageNodeList.isEmpty())) {
      return;
    }

    if (messageNodeList == null) {
      // check if the whatsapp message EditText field is filled with text and ending with your suffix (explanation above)
      AccessibilityNodeInfoCompat messageField = messageNodeList.get(0);
      if (messageField.getText() == null || messageField.getText().length() == 0
        || !messageField.getText().toString().endsWith(signature)) { // So your service doesn't process any message, but the ones ending your apps suffix
        return;
      }
    }

    if (messageNodeList2 == null) {
      // check if the whatsapp message EditText field is filled with text and ending with your suffix (explanation above)
      AccessibilityNodeInfoCompat messageField = messageNodeList2.get(0);
      if (messageField.getText() == null || messageField.getText().length() == 0
        || !messageField.getText().toString().endsWith(signature)) { // So your service doesn't process any message, but the ones ending your apps suffix
        return;
      }
    }

    // Whatsapp send button id
    List<AccessibilityNodeInfoCompat> sendMessageNodeInfoList = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
    if (sendMessageNodeInfoList == null || sendMessageNodeInfoList.isEmpty()) {
      return;
    }

    AccessibilityNodeInfoCompat sendMessageButton = sendMessageNodeInfoList.get(0);
    if (!sendMessageButton.isVisibleToUser()) {
      return;
    }

    try {
      Thread.sleep(600);
      sendMessageButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      Thread.sleep(600);
      reactContext = AccessibilityManagerPluginModule.getReactContext();
      String packageName = reactContext.getPackageName();
      Intent launchIntent = reactContext.getPackageManager().getLaunchIntentForPackage(packageName);
      String className = launchIntent.getComponent().getClassName();
      Class<?> activityClass = Class.forName(className);
      Intent activityIntent = new Intent(reactContext, activityClass);
      activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      reactContext.startActivity(activityIntent);
    } catch (InterruptedException | ClassNotFoundException ie) {
      Thread.currentThread().interrupt();
    }
  }

  @SuppressLint("LongLogTag")
  @Override
  public void onInterrupt() {

  }

  @Override
  public void onServiceConnected() {
    AccessibilityServiceInfo info = getServiceInfo();
    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
    info.packageNames = new String[]{"com.whatsapp"};
    info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
    setServiceInfo(info);
    SharedPreferences p = getSharedPreferences(AccessibilityPreference, Context.MODE_PRIVATE);
    SharedPreferences.Editor e = p.edit();
    e.putBoolean(isServiceBinded, true);
    e.apply();
    super.onServiceConnected();
  }

  @Override
  public boolean onUnbind(Intent intent) {
    SharedPreferences p = getSharedPreferences(AccessibilityPreference, Context.MODE_PRIVATE);
    SharedPreferences.Editor e = p.edit();
    e.putBoolean(isServiceBinded, false);
    e.apply();
    return super.onUnbind(intent);
  }

  @Override
  public void onRebind(Intent intent) {
    SharedPreferences p = getSharedPreferences(AccessibilityPreference, Context.MODE_PRIVATE);
    SharedPreferences.Editor e = p.edit();
    e.putBoolean(isServiceBinded, true);
    e.apply();
    super.onRebind(intent);
  }
}


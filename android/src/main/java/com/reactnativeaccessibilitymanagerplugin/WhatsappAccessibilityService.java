package com.reactnativeaccessibilitymanagerplugin;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WhatsappAccessibilityService extends AccessibilityService {
  public String signature = "";

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {

    if (getRootInActiveWindow() == null) {
      return;
    }

    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException ignored) {

    }

    AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(getRootInActiveWindow());

    // Whatsapp Message EditText id
    List<AccessibilityNodeInfoCompat> messageNodeList2 = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/caption");
    List<AccessibilityNodeInfoCompat> messageNodeList = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry");

    if ((messageNodeList2 == null || messageNodeList2.isEmpty()) && (messageNodeList == null || messageNodeList.isEmpty())) {
      return;
    }

    if (messageNodeList == null){
      // check if the whatsapp message EditText field is filled with text and ending with your suffix (explanation above)
      AccessibilityNodeInfoCompat messageField = messageNodeList.get(0);
      if (messageField.getText() == null || messageField.getText().length() == 0
        || !messageField.getText().toString().endsWith(signature)) { // So your service doesn't process any message, but the ones ending your apps suffix
        return;
      }
    }

    if (messageNodeList2 == null){
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

    // Now fire a click on the send button
    sendMessageButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);

    // Now go back to your app by clicking on the Android back button twice:
    // First one to leave the conversation screen
    // Second one to leave whatsapp
    try {
      TimeUnit.SECONDS.sleep(1);
      performGlobalAction(GLOBAL_ACTION_BACK);
      TimeUnit.SECONDS.sleep(1);
      performGlobalAction(GLOBAL_ACTION_BACK);
    } catch (InterruptedException ignored) {

    }
  }

  @SuppressLint("LongLogTag")
  @Override
  public void onInterrupt() {
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public void onServiceConnected() {
    AccessibilityServiceInfo info = getServiceInfo();
    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;

    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
    info.packageNames = new String[]{"com.whatsapp"};
    info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
    setServiceInfo(info);
    super.onServiceConnected();
  }
}

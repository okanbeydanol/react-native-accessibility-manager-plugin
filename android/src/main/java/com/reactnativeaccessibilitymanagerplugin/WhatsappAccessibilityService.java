package com.reactnativeaccessibilitymanagerplugin;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

public class WhatsappAccessibilityService extends AccessibilityService {
  public boolean hasListener = false;
  public String signature = "";

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {

    if (getRootInActiveWindow() == null) {
      return;
    }

    AccessibilityNodeInfoCompat rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(getRootInActiveWindow());

    // Whatsapp Message EditText id
    List<AccessibilityNodeInfoCompat> messageNodeList = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry");
    if (messageNodeList == null || messageNodeList.isEmpty()) {
      return;
    }

    // check if the whatsapp message EditText field is filled with text and ending with your suffix (explanation above)
    AccessibilityNodeInfoCompat messageField = messageNodeList.get(0);
    if (messageField.getText() == null || messageField.getText().length() == 0
      || !messageField.getText().toString().endsWith(signature)) { // So your service doesn't process any message, but the ones ending your apps suffix
      return;
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
      Thread.sleep(500); // hack for certain devices in which the immediate back click is too fast to handle
      performGlobalAction(GLOBAL_ACTION_BACK);
      Thread.sleep(500);  // same hack as above
      if (hasListener) {
        WritableMap data = Arguments.createMap();
        data.putBoolean("sent", true);
        AccessibilityManagerPluginModule.sendData("wpsent", data);
      }
    } catch (InterruptedException ignored) {
    }
  }

  @SuppressLint("LongLogTag")
  @Override
  public void onInterrupt() {
  }

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
    info.flags = AccessibilityServiceInfo.DEFAULT;
    info.packageNames = new String[]{"com.whatsapp"};
    info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
    setServiceInfo(info);
  }
}

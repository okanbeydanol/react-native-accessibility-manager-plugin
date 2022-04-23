package com.reactnativeaccessibilitymanagerplugin;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;
import java.util.function.Function;

@ReactModule(name = AccessibilityManagerPluginModule.NAME)
public class AccessibilityManagerPluginModule extends ReactContextBaseJavaModule {
  public static final String NAME = "AccessibilityManagerPlugin";
  private static ReactApplicationContext reactContext;
  public static final String LOG_TAG = "RNInvokeApp";
  private static Bundle bundle = null;
  private Promise mPromise;
  private final int DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE = 1222;
  private final WhatsappAccessibilityService mConnectivityReceiver = new WhatsappAccessibilityService();

  public AccessibilityManagerPluginModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  public void multiply(int a, int b, Promise promise) {
    promise.resolve(a * b);
  }

  public static native int nativeMultiply(int a, int b);

  @ReactMethod
  public void invokeApp(ReadableMap params) {
    ReadableMap data = params.hasKey("data") ? params.getMap("data") : null;

    if (data != null) {
      bundle = Arguments.toBundle(data);
    }

    String packageName = reactContext.getPackageName();
    Intent launchIntent = reactContext.getPackageManager().getLaunchIntentForPackage(packageName);
    String className = launchIntent.getComponent().getClassName();

    try {
      Class<?> activityClass = Class.forName(className);
      Intent activityIntent = new Intent(reactContext, activityClass);

      activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      reactContext.startActivity(activityIntent);
    } catch (Exception e) {
      Log.e(LOG_TAG, "Class not found", e);
      return;
    }

    if (isAppOnForeground(reactContext)) {
      sendEvent("appInvoked");
    }
  }

  public static void sendEvent(String eventName) {
    if (bundle != null) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, Arguments.fromBundle(bundle));
      bundle = null;
    }
  }

  public static void sendData(String eventName, WritableMap event) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, event);
  }
  private boolean isAppOnForeground(ReactApplicationContext context) {
    /**
     * We need to check if app is in foreground otherwise the app will crash.
     * http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
     **/
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
    if (appProcesses == null) {
      return false;
    }
    final String packageName = context.getPackageName();
    for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
      if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        && appProcess.processName.equals(packageName)) {
        return true;
      }
    }
    return false;
  }

  @SuppressLint("LongLogTag")
  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void openDisplayOverOtherAppsPermissionSettings(Promise promise) {
    mPromise = promise;
    if (!Settings.canDrawOverlays(reactContext)) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + reactContext.getPackageName()));
      reactContext.startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE, null);
    }
    promise.resolve(true);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void canDisplayOverOtherApps(Promise promise) {
    if (Settings.canDrawOverlays(reactContext)) {
      promise.resolve(true);
    } else {
      promise.resolve(false);
    }

  }

  @SuppressLint("LongLogTag")
  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void openAccessibilitySettings(Promise promise) {
    int accessibilityEnabled = 0;
    try {
      accessibilityEnabled = Settings.Secure.getInt(reactContext.getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
      if (accessibilityEnabled == 0){
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
          | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        reactContext.startActivity(intent);
      }
      promise.resolve(accessibilityEnabled);
    } catch (Settings.SettingNotFoundException ignored) {
      promise.resolve(accessibilityEnabled);
    }


  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void isAccessibilityOn(Promise promise) {
    int accessibilityEnabled = 0;
    try {
      accessibilityEnabled = Settings.Secure.getInt(reactContext.getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
      if (accessibilityEnabled == 0) {
        promise.resolve(false);
      }else{
        promise.resolve(true);
      }
    } catch (Settings.SettingNotFoundException ignored) {
      promise.resolve(false);
    }
  }

  @SuppressLint("LongLogTag")
  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void addEventListener(String eventName) {
    mConnectivityReceiver.hasListener = true;
  }

  @SuppressLint("LongLogTag")
  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void removeEventListener(String eventName) {
    mConnectivityReceiver.hasListener = false;
  }
}


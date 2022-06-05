package com.reactnativeaccessibilitymanagerplugin;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.loader.content.CursorLoader;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import java.io.File;
import java.util.List;

@ReactModule(name = AccessibilityManagerPluginModule.NAME)
public class AccessibilityManagerPluginModule extends ReactContextBaseJavaModule implements ActivityEventListener {
  public static final String NAME = "AccessibilityManagerPlugin";
  private static ReactApplicationContext reactContext;
  public static final String LOG_TAG = "RNInvokeApp";
  private static Bundle bundle = null;
  private final int DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE = 1222;

  public AccessibilityManagerPluginModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
    reactContext.addActivityEventListener(this);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  public static ReactApplicationContext getReactContext() {
    return reactContext;
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

  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  public static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }

  public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
    Cursor cursor = null;
    final String column = MediaStore.MediaColumns.DATA;
    final String[] projection = {column};

    try {
      CursorLoader loader = new CursorLoader(context, uri, projection, selection, selectionArgs, null);
      cursor = loader.loadInBackground();
      if (cursor != null && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(index);
      }
    } finally {
      if (cursor != null) cursor.close();
    }
    return null;
  }

  public static String getRealPathFromURI(final ReactContext context, final Uri uri, Boolean useInternalStorage) {

    String filePrefix = "";
    // DocumentProvider
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type) || "0".equalsIgnoreCase(type)) {
          File cacheDir = useInternalStorage ? context.getCacheDir() : context.getExternalCacheDir();
          return filePrefix + cacheDir + "/" + split[1];
        } else if ("raw".equalsIgnoreCase(type)) {
          return filePrefix + split[1];
        } else if (!TextUtils.isEmpty(type)) {
          return filePrefix + "/storage/" + type + "/" + split[1];
        }

        // TODO handle non-primary volumes
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {

        final String id = DocumentsContract.getDocumentId(uri);
        if (id.startsWith("raw:")) {
          return filePrefix + id.replaceFirst("raw:", "");
        }
        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return filePrefix + getDataColumn(context, contentUri, null, null);
      }
      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if ("raw".equalsIgnoreCase(type)) {
          return filePrefix + split[1];
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[]{
          split[1]
        };

        return filePrefix + getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {

      // Return the remote address
      if (isGooglePhotosUri(uri))
        return uri.getLastPathSegment();

      return filePrefix + getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode == 1) {
      WritableMap data2 = Arguments.createMap();
      data2.putBoolean("sented", true);
      AccessibilityManagerPluginModule.sendData("wpsented", data2);
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void openDisplayOverOtherAppsPermissionSettings(Promise promise) {
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

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void openAutoStartSettings(Promise promise) {
    for (Intent intent : POWERMANAGER_INTENTS)
      if (reactContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
        try {
          intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
          reactContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
          Log.e(LOG_TAG, "Failed to launch AutoStart activity: " + e.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
          Log.e(LOG_TAG, "Failed to launch AutoStart activity: " + e.getMessage());
        }
        break;
      }
  }

  @SuppressLint("LongLogTag")
  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void openAccessibilitySettings(Promise promise) {
    int accessibilityEnabled = 0;
    try {
      accessibilityEnabled = Settings.Secure.getInt(reactContext.getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
      Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      reactContext.startActivity(intent);
      promise.resolve(accessibilityEnabled);
    } catch (Settings.SettingNotFoundException ignored) {
      promise.resolve(accessibilityEnabled);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void isAccessibilityOn(Promise promise) {
    AccessibilityManager am = (AccessibilityManager) reactContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
    List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

    for (AccessibilityServiceInfo enabledService : enabledServices) {
      ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
      if (enabledServiceInfo.packageName.equals(reactContext.getPackageName()) && enabledServiceInfo.name.equals(WhatsappAccessibilityService.class.getName()))
        promise.resolve(true);
    }
    promise.resolve(false);
  }

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

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void sendText(String phone, String text, String signature, Promise promise) {
    if (phone == null || phone == "" || text == null || text == "") {
      promise.resolve(false);
    }
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setPackage("com.whatsapp");
    intent.setType("text/plain");
    intent.putExtra("jid", phone + "@s.whatsapp.net");
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(Intent.EXTRA_TEXT, text + "\n" + ((signature != null && signature != "") ? signature : "#.~!#"));
    WhatsappAccessibilityService.signature = ((signature != null && signature != "") ? signature : "#.~!#");
    reactContext.startActivityForResult(intent, 1, null);
    promise.resolve(true);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void sendImage(String filepath, String phone, String text, String signature, Promise promise) {
    if (filepath == null || filepath == "" | phone == null || phone == "") {
      promise.resolve(false);
    }
    Uri uri = Uri.parse(filepath);
    String realPath = getRealPathFromURI(reactContext, uri, true);
    File file = new File(realPath);
    if (!file.exists()) {
      promise.resolve(false);
    }
    Uri imageUri = FileProvider.getUriForFile(reactContext, reactContext.getPackageName() + ".provider", file);
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setPackage("com.whatsapp");
    intent.setType("image/*");
    intent.putExtra("jid", phone + "@s.whatsapp.net");
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(Intent.EXTRA_STREAM, imageUri);
    intent.putExtra(Intent.EXTRA_TEXT, text + "\n" + ((signature != null && signature != "") ? signature : "#.~!#"));
    WhatsappAccessibilityService.signature = ((signature != null && signature != "") ? signature : "#.~!#");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    reactContext.startActivityForResult(intent, 1, null);
    promise.resolve(true);
  }

  private static final Intent[] POWERMANAGER_INTENTS = {
    new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
    new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
    new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
    new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
    new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
    new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
    new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
    new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
    new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
    new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
    new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
    new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
    new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
  };
}


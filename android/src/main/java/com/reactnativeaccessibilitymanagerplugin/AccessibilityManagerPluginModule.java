package com.reactnativeaccessibilitymanagerplugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.loader.content.CursorLoader;

import com.facebook.react.HeadlessJsTaskService;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ReactModule(name = AccessibilityManagerPluginModule.NAME)
public class AccessibilityManagerPluginModule extends ReactContextBaseJavaModule  implements ActivityEventListener {
  public static final String NAME = "AccessibilityManagerPlugin";
  private static ReactApplicationContext reactContext;
  public static final String LOG_TAG = "RNInvokeApp";
  public static final String TAG = "dfgsdfgsdgsdfgsdf";
  public static final String TAG2 = "dfgsdfgsdgsdfgsdf222";
  private static Bundle bundle = null;
  private Promise mPromise;
  private final int DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE = 1222;
  private final WhatsappAccessibilityService mConnectivityReceiver = new WhatsappAccessibilityService();

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

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void sendMedia(String filepath, Promise promise) throws IOException {

    Uri uri2 = Uri.parse("smsto:" + "05418581704");

    Uri uri = Uri.parse(filepath);
    String realPath= getRealPathFromURI(reactContext, uri,true);
    File file = new File(realPath);
    Log.w(TAG, "sendMedia: " + reactContext.getPackageName() );
    Uri imageUri = FileProvider.getUriForFile(reactContext,reactContext.getPackageName() + ".provider", file);
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setPackage("com.whatsapp");
    intent.setType("image/*");
    intent.putExtra("jid",  "905418581704@s.whatsapp.net");
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(Intent.EXTRA_STREAM, imageUri);
    intent.putExtra(Intent.EXTRA_TEXT, "My sample image text");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    reactContext.startActivityForResult(intent, 1, null );
    promise.resolve("");
    //intentShareFile.putExtra(Intent.EXTRA_STREAM, new File(filepath).toURI());
    // intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
    //   "Share");
    //intentShareFile.putExtra("jid",  "905418581704@s.whatsapp.net");
    // intentShareFile.setPackage("com.whatsapp");
    //intentShareFile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    // intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


    //Uri uri = Uri.parse("smsto:" + "05418581704");
    //Intent shareIntent = new Intent(Intent.ACTION_SEND);
    //shareIntent.setPackage("com.whatsapp");
    // shareIntent.putExtra(Intent.EXTRA_TEXT, "My sample image text");
    //shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
    // shareIntent.setType("image/jpeg");
    //shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    // shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

   try {
       //reactContext.startActivity(intentShareFile);
     } catch (ActivityNotFoundException ex) {
       Log.w(TAG, ex.getMessage());
    }
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
    final String[] projection = { column };

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
    Log.w(TAG, "requestCode" + String.valueOf(requestCode));
    Log.w(TAG, "resultCode" + String.valueOf(resultCode));
    Log.w(TAG, "data" + String.valueOf(data));
    Log.w(TAG, String.valueOf("onActivityResult"));
    if (requestCode == 1){
      WritableMap data2 = Arguments.createMap();
      data2.putBoolean("sented", true);
      AccessibilityManagerPluginModule.sendData("wpsented", data2);
    }


  }

  @Override
  public void onNewIntent(Intent intent) {
    Log.w(TAG, String.valueOf("onNewIntent"));
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @ReactMethod
  public void sendText(String phone, String text) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://api.whatsapp.com/send?phone="+phone+"=&text="+text+""));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
      | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    intent.setPackage("com.whatsapp");

    try {
      reactContext.startActivity(intent);
    } catch (android.content.ActivityNotFoundException ex) {
      Log.w(TAG, ex.getMessage());
    }
  }
}


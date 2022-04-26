import { NativeModules, Platform, DeviceEventEmitter } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-accessibility-manager-plugin' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';
const AccessibilityManagerPlugin = NativeModules.AccessibilityManagerPlugin
  ? NativeModules.AccessibilityManagerPlugin
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function invokeApp(data = {}): Promise<number> {
  return AccessibilityManagerPlugin.invokeApp(
    typeof data !== 'object' ? {} : data
  );
}
export function openDisplayOverOtherAppsPermissionSettings(): Promise<boolean> {
  return AccessibilityManagerPlugin.openDisplayOverOtherAppsPermissionSettings();
}
export function canDisplayOverOtherApps(): Promise<boolean> {
  return AccessibilityManagerPlugin.canDisplayOverOtherApps();
}

export function isAccessibilityOn(): Promise<boolean> {
  return AccessibilityManagerPlugin.isAccessibilityOn();
}

export function openAccessibilitySettings(): Promise<boolean> {
  return AccessibilityManagerPlugin.openAccessibilitySettings();
}

let listenWpSentEvent: any = null;
export function listenWpSent(eventName: string, callback: Function): void {
  if (listenWpSentEvent !== null) {
    const test = (event: any) => {
      callback(event);
    };
    listenWpSentEvent = DeviceEventEmitter.addListener(eventName, test);
    return AccessibilityManagerPlugin.addEventListener(eventName);
  }
}

export function removeListenWpSent(): void {
  DeviceEventEmitter.removeAllListeners();
  return AccessibilityManagerPlugin.removeEventListener();
}

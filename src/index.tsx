import { NativeModules, Platform } from 'react-native';

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

export function sendImage(
  filePath: string,
  phoneNumber: string,
  text?: string
): Promise<boolean> {
  return AccessibilityManagerPlugin.sendImage(filePath, phoneNumber, text);
}
export function sendText(phoneNumber: string, text: string): Promise<boolean> {
  return AccessibilityManagerPlugin.sendText(phoneNumber, text);
}

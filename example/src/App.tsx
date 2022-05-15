import React, { useEffect } from 'react';
import { TouchableOpacity } from 'react-native';
import { Text } from 'react-native';
import { sendText } from 'react-native-accessibility-manager-plugin';
import BackgroundService from 'react-native-background-actions';
import { DeviceEventEmitter } from 'react-native';
import {
  isAccessibilityOn,
  openAccessibilitySettings,
} from 'react-native-accessibility-manager-plugin';
const App = () => {
  // You can do anything in your task such as network requests, timers and so on,
  // as long as it doesn't touch UI. Once your task completes (i.e. the promise is resolved),
  // React Native will go into "paused" mode (unless there are other tasks running,
  // or there is a foreground app).
  const veryIntensiveTask = async () => {
    DeviceEventEmitter.addListener('wpsented', async () => {
      setTimeout(() => {
        onPress();
      }, 2000);
    });
  };
  const options = {
    taskName: 'Example',
    taskTitle: 'ExampleTask title',
    taskDesc: 'ExampleTask description',
    taskIcon: {
      name: 'ic_launcher',
      type: 'mipmap',
    },
    color: '#ff00ff',
    linkingURI: 'peoplesapp', // See Deep Linking for more info
    parameters: {
      delay: 1000,
    },
  };
  BackgroundService.start(veryIntensiveTask, options);
  useEffect(() => {
    isAccessibilityOn().then((d) => {
      console.log('%c IZIN GELDIIIIII:::', d);
      if (!d) {
        openAccessibilitySettings();
      }
    });
    return () => {};
  }, []);

  setTimeout(async () => {}, 10);
  const onPress = () => {
    // sendMedia(
    //   'file:///data/user/0/com.example.reactnativeaccessibilitymanagerplugin/cache/rn_image_picker_lib_temp_1fe1a570-0f6d-48e2-bc38-fe90cc6ebef3.jpg'
    // ).then(async (a: any) => {
    //   console.log('%c burası', 'background: #222; color: #bada55', a);
    // });
    sendText('905432962305', 'Merhaba nasılsunnnnnnn').then((asd: any) => {
      console.log('%c a', 'background: #222; color: #bada55', asd);
    });
  };
  return (
    <TouchableOpacity onPress={onPress}>
      <Text>Bsdfasdfadsfsadfas</Text>
    </TouchableOpacity>
  );
};

export default App;

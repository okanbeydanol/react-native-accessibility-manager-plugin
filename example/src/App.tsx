import React, { useState } from 'react';
import { TouchableOpacity } from 'react-native';
import { Text } from 'react-native';
import { sendMedia } from 'react-native-accessibility-manager-plugin';
import {
  ImagePickerResponse,
  launchImageLibrary,
} from 'react-native-image-picker';
import BackgroundService from 'react-native-background-actions';
import { DeviceEventEmitter } from 'react-native';

const App = () => {
  const [first, setFirst] = useState('');
  // You can do anything in your task such as network requests, timers and so on,
  // as long as it doesn't touch UI. Once your task completes (i.e. the promise is resolved),
  // React Native will go into "paused" mode (unless there are other tasks running,
  // or there is a foreground app).
  const veryIntensiveTask = async (taskDataArguments: any) => {
    DeviceEventEmitter.addListener('wpsented', async (asd: any) => {
      console.log('%c asd', 'background: #222; color: #bada55', asd);
      console.log('merhabaaaaaaaa');
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
  setTimeout(async () => {}, 10);
  const onPress = () => {
    sendMedia(
      'file:///data/user/0/com.example.reactnativeaccessibilitymanagerplugin/cache/rn_image_picker_lib_temp_1fe1a570-0f6d-48e2-bc38-fe90cc6ebef3.jpg'
    ).then(async (a: any) => {
      console.log('%c burası', 'background: #222; color: #bada55', a);
    });
  };
  return (
    <TouchableOpacity onPress={onPress}>
      <Text>Bsdfasdfadsfsadfas</Text>
    </TouchableOpacity>
  );
};

export default App;

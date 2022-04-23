import * as React from 'react';

import { StyleSheet, View, Text, AppState, NativeModules, Platform, TouchableOpacity, DeviceEventEmitter } from 'react-native';
import { invokeApp, openAccessibilitySettings, listenWpSent } from 'react-native-accessibility-manager-plugin';
import BackgroundService from 'react-native-background-actions';
export default function App() {

const LINKING_ERROR =
`The package 'react-native-accessibility-manager' doesn't seem to be linked. Make sure: \n\n` +
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
  console.log('%c AccessibilityManagerPlugin', 'background: #222; color: #bada55',AccessibilityManagerPlugin);
  const sleep = (time: any) => new Promise(resolve => setTimeout(() => resolve(true), time));
  console.log(
    '%c çalışşş amcıııkkkk',
    'background: #222; color: #bada55',
  );
  const veryIntensiveTask = async (taskDataArguments: any) => {
    // Example of an infinite loop task
    const {delay} = taskDataArguments;
    await new Promise(async resolve => {
      for (let i = 0; BackgroundService.isRunning(); i++) {
        console.log(i);
        if (i === 6) {
          console.log(
            '%c çalışşş amcıııkkkk',
            'background: #222; color: #bada55',
          );
         invokeApp();
        }
        await sleep(delay);
      }
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
    linkingURI: 'peoplesapp://', // See Deep Linking for more info
    parameters: {
      delay: 1000,
    },
  };
  React.useEffect(() => {
    AppState.addEventListener('change', async state => {
      console.log('state', state);
      if (state === 'background') {
        // setTimeout(() => {
        //   console.log('%c çalıştı', 'background: #222; color: #bada55');
        //   invokeApp();
        // }, 2000);
        // await BackgroundService.start(veryIntensiveTask, options);
      } else if (state === 'active') {
      }
    });
  }, []);

  return (

    <TouchableOpacity style={{ marginTop: 100, height: '100%' }} onPress={()=>{
      openAccessibilitySettings().then((a: any)=>{
        listenWpSent('wpsent',(e: any)=>{
console.log('%c allahımmmmm', 'background: #222; color: #bada55',e);
        })
console.log('%c ahgsducyfasuykdcfasukydfcuyksafcdukyafsuydcfsauky', 'background: #222; color: #bada55',a);
      }).catch((err:any)=>{
        console.log('%c false', 'background: #222; color: #bada55',err);

      });
    }}>
    <View style={styles.container}>
    <Text>Result: 27</Text>
  </View>
    </TouchableOpacity>

  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});

import { AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';
import { openAccessibilitySettings } from 'react-native-accessibility-manager-plugin';

// const addddd = async (taskData) => {
//   setInterval(() => {
//     console.log('%c a', 'background: #222; color: #bada55');
//   });
// };

AppRegistry.registerComponent(appName, () => App);

openAccessibilitySettings();

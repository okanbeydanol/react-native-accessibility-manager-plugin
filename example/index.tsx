import { AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';

// const addddd = async (taskData) => {
//   setInterval(() => {
//     console.log('%c a', 'background: #222; color: #bada55');
//   });
// };

AppRegistry.registerComponent(appName, () => App);

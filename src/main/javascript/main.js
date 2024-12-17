import Vue from 'vue';
import store from './store/index';
import { EmojiConvertor } from 'emoji-js';
import EmojiPicker from 'rm-emoji-picker';
import pickersheet_apple from "../../../node_modules/rm-emoji-picker/sheets/sheet_apple_64_indexed_128.png";
import pickersheet_google from "../../../node_modules/rm-emoji-picker/sheets/sheet_google_64_indexed_128.png";
import pickersheet_twitter from "../../../node_modules/rm-emoji-picker/sheets/sheet_twitter_64_indexed_128.png";
import convsheet_apple from "./assets/sheet_apple_64.png";
import convsheet_google from "./assets/sheet_google_64.png";
import convsheet_twitter from "./assets/sheet_twitter_64.png";

const App = () => import(
  /* webpackPrefetch: true */
  /* webpackChunkName: "root" */
  './App.vue');

var emoji = new EmojiConvertor();
emoji.replace_mode = 'css';
emoji.use_sheet = true;
emoji.img_sets.apple.sheet = convsheet_apple;
emoji.img_sets.google.sheet = convsheet_google;
emoji.img_sets.twitter.sheet = convsheet_twitter;
emoji.allow_caps = true;
emoji.allow_native = true;
emoji.addAliases({
'thumbs_up' : '1f44d',
'thumbs-up' : '1f44d'
});

var picker = picker = new EmojiPicker({
 sheets: {
   apple   : pickersheet_apple,
   google  : pickersheet_google,
   twitter : pickersheet_twitter
 },
 positioning: function(tip) {
   if (typeof(tip.element_rect) != 'undefined') {
     let coordinate = {
       top: tip.centered_coordinate.top - (tip.element_rect.height + tip.tooltip_height) / 2,
       left: tip.centered_coordinate.left
     };
     tip._applyPosition(coordinate)('TooltipAbove');
   } else {
     tip.above();
   }
 },
 search_icon : '🔍',
 categories: [
   {
     title: "People",
     icon : '😀'
   },
   {
     title: "Nature",
     icon : '🌳'
   },
   {
     title: "Foods",
     icon : '🍉'
   },
   {
     title: "Activity",
     icon : '⚽'
   },
   {
     title: "Places",
     icon : '🧭'
   },
   {
     title: "Symbols",
     icon : '➗'
   },
   {
     title: "Flags",
     icon : '🏳'
   }
 ]
});

//console.log("window.myGlobalEmoji=" + emoji);
Vue.prototype.$emoji = emoji;
Vue.prototype.$emojipicker = picker;

new Vue({
  el: '#app',
  store,
  render: h => h(App)
});

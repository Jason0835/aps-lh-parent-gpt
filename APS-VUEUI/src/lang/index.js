/*
 * @Author: TenderFlow
 * @Date: 2023-12-25 11:41:22
 * @LastEditTime: 2023-12-26 14:07:02
 * @LastEditors: TenderFlow
 * @FilePath: \telecom-ui\src\lang\index.js
 * @Description:
 *
 */
import Vue from "vue";

import VueI18n from "vue-i18n";
// import elementEnLocale from "element-ui/lib/locale/lang/en"; // element-ui lang
// import elementZhLocale from "element-ui/lib/locale/lang/zh-CN"; // element-ui lang
// import elementViLocale from "element-ui/lib/locale/lang/vi";
// import tltZhLocale from "tlt-ui/src/locale/lang/zh-CN"
// import tltEnLocale from "tlt-ui/src/locale/lang/en"
// import tltViLocale from "tlt-ui/src/locale/lang/vi"
// import enLocale from "./en";
// import zhLocale from "./zh";
// import viLocale from "./vi";
Vue.use(VueI18n);

// const messages = {
//   en_US: {
//     ...enLocale,
//     ...elementEnLocale,
//     ...tltEnLocale
//   },
//   zh_CN: {
//     ...zhLocale,
//     ...elementZhLocale,
//     ...tltZhLocale
//   },
//   vi_VN: {
//     ...viLocale,
//     ...elementViLocale,
//     ...tltViLocale
//   }
// };


const i18n = new VueI18n();

export default i18n;

import Vue from "vue";
import axios from "axios";
import i18n from "./lang";

import Cookies from "js-cookie";

import Element from "element-ui";
import "./assets/styles/element-variables.scss";

import "@/assets/styles/index.scss"; // global css
import "@/assets/styles/ruoyi.scss"; // ruoyi css
// import "tlt-ui/lib/tlt-ui.css"

import App from "./App";
import store from "./store";
import router from "./router";
import directive from "./directive"; // directive
import plugins from "./plugins"; // plugins
import { download } from "@/utils/request";

import "./mock";

import "./assets/icons"; // icon
import "./permission"; // permission control
import { getDicts } from "@/api/system/dict/data";
import { getConfigKey } from "@/api/system/config";
import {
  parseTime,
  resetForm,
  addDateRange,
  selectDictLabel,
  selectDictLabels,
  handleTree,
  isEmpty,
} from "@/utils/ruoyi";

// 分页组件
import Pagination from "@/components/Pagination";
// 自定义表格工具组件
import RightToolbar from "@/components/RightToolbar";
// 富文本组件
import Editor from "@/components/Editor";
// 文件上传组件
import FileUpload from "@/components/FileUpload";
// 图片上传组件
import ImageUpload from "@/components/ImageUpload";
// 图片预览组件
import ImagePreview from "@/components/ImagePreview";
// 字典标签组件
import DictTag from "@/components/DictTag";
// 基础数据组件
import BaseData from "@/components/BaseData";
// 字典下拉框组件
import DictSelect from "@/components/DictSelect";
// 头部标签组件
import VueMeta from "vue-meta";
// 字典数据组件
import DictData from "@/components/DictData";
import LangSelect from "@/components/LangSelect";
// 基础数据组件
import BaseDataPlugin from '@/components/BaseData/plugin'


import TltUI from "tlt-ui";
Vue.use(TltUI, {
  base: process.env.VUE_APP_BASE_API,
  i18n: (key, value) => i18n.t(key, value),
});

import PageTable from "@/components/Table/PageTable";
import TextButton from "@/components/Table/TextButton";
import HeaderContainer from "@/components/Container/HeaderContainer";
import BasicContainer from "@/components/Container/BasicContainer";
import InputAmount from "@/components/Input/InputAmount";
Vue.component("PageTable", PageTable);
Vue.component("TextButton", TextButton);
Vue.component("BasicContainer", BasicContainer);
Vue.component("HeaderContainer", HeaderContainer);
Vue.component("InputAmount", InputAmount);

// 全局方法挂载
Vue.prototype.getDicts = getDicts;
Vue.prototype.getConfigKey = getConfigKey;
Vue.prototype.parseTime = parseTime;
Vue.prototype.resetForm = resetForm;
Vue.prototype.addDateRange = addDateRange;
Vue.prototype.selectDictLabel = selectDictLabel;
Vue.prototype.selectDictLabels = selectDictLabels;
Vue.prototype.isEmpty = isEmpty;
Vue.prototype.download = download;
Vue.prototype.handleTree = handleTree;
/**
 *
 * @param {String} str
 * @param {Number} defaultWidth
 * @returns {Number}
 */
Vue.prototype.computeWidth = function (str = "",defaultWidth = 0) {
  let tempWidth = parseInt(defaultWidth);
  defaultWidth = parseInt(defaultWidth)
  let charWidth = 14
  if(i18n.locale == "zh_CN") {
    charWidth = 14
  }else{
    charWidth = 7
  };
  tempWidth = str.length * charWidth;
  tempWidth += 40;
  // console.log(str, Math.max(tempWidth,defaultWidth));
  return Math.max(tempWidth,defaultWidth)
}

Vue.prototype.$axios = axios;

// use添加i18n
Vue.use(Element, {
  i18n: (key, value) => i18n.t(key, value),
  size: "mini",
});
// 全局组件挂载
Vue.component("LangSelect", LangSelect);
Vue.component("DictTag", DictTag);
Vue.component("BaseData", BaseData);
Vue.component("DictSelect", DictSelect);
Vue.component("Pagination", Pagination);
Vue.component("RightToolbar", RightToolbar);
Vue.component("Editor", Editor);
Vue.component("FileUpload", FileUpload);
Vue.component("ImageUpload", ImageUpload);
Vue.component("ImagePreview", ImagePreview);

Vue.use(directive);
Vue.use(plugins);
Vue.use(VueMeta);
Vue.use(BaseDataPlugin);
DictData.install();

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

// Vue.use(Element, {
//   size: Cookies.get("size") || "medium", // set element-ui default size
// });

Vue.config.productionTip = false;

new Vue({
  el: "#app",
  i18n,
  router,
  store,
  render: (h) => h(App),
});

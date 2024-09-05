import axios from 'axios'
import i18n from '@/lang'
import store from '@/store'
import elementEnLocale from "element-ui/lib/locale/lang/en"; // element-ui lang
import elementZhLocale from "element-ui/lib/locale/lang/zh-CN";
import elementViLocale from "element-ui/lib/locale/lang/vi"
import tltZhLocale from "tlt-ui/src/locale/lang/zh-CN"
import tltEnLocale from "tlt-ui/src/locale/lang/en"
import tltViLocale from "tlt-ui/src/locale/lang/vi"
import enLocale from "./en";
import zhLocale from "./zh";
// import zhLocale from "./zh/web_zh_CN.json"
// import zhUILocale from "./zh/ui_zh_CN.json"
import viLocale from "./vi";

import { pageJson } from "@/api/bd/i18nChange"

// vue组件默认语言
let messages = {
  en_US: {
    ...elementEnLocale,
    ...tltEnLocale,
  },
  zh_CN: {
    ...elementZhLocale,
    ...tltZhLocale,
  },
  vi_VN: {
    ...elementViLocale,
    ...tltViLocale,
  }
};

if (process.env.NODE_ENV === "development") {
  messages = {
    en_US: {
      ...elementEnLocale,
      ...tltEnLocale,
      ...enLocale,
    },
    zh_CN: {
      ...elementZhLocale,
      ...tltZhLocale,
      ...zhLocale,
    },
    vi_VN: {
      ...elementViLocale,
      ...tltViLocale,
      ...viLocale,
    }
  };
}

// 别名
const langKey = {
  en: 'en_US',
  en_US: 'en_US',
  zh: 'zh_CN',
  zh_CN: 'zh_CN',
  vi: 'vi_VN',
  vi_VN: 'vi_VN',
}
function setI18nLanguage(lang) {
  i18n.locale = langKey[lang]
  axios.defaults.headers.common['Accept-Language'] = langKey[lang]
  document.querySelector('html').setAttribute('lang', langKey[lang])
  store.dispatch('app/setLanguage', langKey[lang])
  store.dispatch("dict/cleanDict");
  return langKey[lang]
}


export async function loadLanguageAsync(lang) {
  lang = lang || store.getters.language || 'zh_CN'
  if (i18n.locale !== langKey[lang] || !i18n.messages[lang]) {
    if (process.env.NODE_ENV === "development") {
      i18n.setLocaleMessage(langKey[lang], {
        ...messages[lang]
      })
      setI18nLanguage(langKey[lang])

    } else {

      // const response = await axios.get(`${process.env.VUE_APP_LANGUAGE_PATH}/${langKey[lang]}.json`)
      const response = await pageJson({
        locale: langKey[lang]
      })
      i18n.setLocaleMessage(langKey[lang], {
        ...response,
        ...messages[lang]
      })
      setI18nLanguage(langKey[lang])

      return true
    }

  }
  return false
}

import axios from 'axios'
import i18n from '@/lang'
import store from '@/store'
import elementEnLocale from "element-ui/lib/locale/lang/en"; // element-ui lang
import elementZhLocale from "element-ui/lib/locale/lang/zh-CN";
import elementViLocale from "element-ui/lib/locale/lang/vi"
import tltZhLocale from "tlt-ui/src/locale/lang/zh-CN"
import tltEnLocale from "tlt-ui/src/locale/lang/en"
import tltViLocale from "tlt-ui/src/locale/lang/vi"
// import enLocale from "./en";
import zhLocale from "./zh";
// import zhLocale from "./zh/web_zh_CN.json"
// import zhUILocale from "./zh/ui_zh_CN.json"
// import viLocale from "./vi";
import pako from "pako"
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
    ...zhLocale,
  },
  vi_VN: {
    ...elementViLocale,
    ...tltViLocale,
  }
};

// if (process.env.NODE_ENV === "development") {
//   messages = {
//     en_US: {
//       ...elementEnLocale,
//       ...tltEnLocale,
//       ...enLocale,
//     },
//     zh_CN: {
//       ...elementZhLocale,
//       ...tltZhLocale,
//       ...zhLocale,
//     },
//     vi_VN: {
//       ...elementViLocale,
//       ...tltViLocale,
//       ...viLocale,
//     }
//   };
// }

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
    // if (process.env.NODE_ENV === "development") {
    //   i18n.setLocaleMessage(langKey[lang], {
    //     ...messages[lang]
    //   })
    //   setI18nLanguage(langKey[lang])

    // } else {
    //   i18n.setLocaleMessage(langKey[lang], {
    //     ...messages[lang]
    //   })
    //   setI18nLanguage(langKey[lang])
    //   // let data = getLanguage(lang);

    //   // if (data == false) {
    //   //   const response = await pageJson({
    //   //     locale: langKey[lang]
    //   //   });

    //   //   data = response;
    //   //   saveLanguage(data, lang);
    //   // }

    //   // Object.keys(data).map(key => {
    //   //   data[key] = unzipDataBase64(data[key])
    //   // });

    //   // i18n.setLocaleMessage(langKey[lang], {
    //   //   ...messages[lang],
    //   //   ...data,
    //   // })
    //   // setI18nLanguage(langKey[lang])

    //   // return true


    // }
     if (process.env.NODE_ENV === "development") {
      i18n.setLocaleMessage(langKey[lang], {
        ...messages[lang]
      })
      setI18nLanguage(langKey[lang])

    }else{
      let data = getLanguage(lang);
      console.log(data)
      if (data == false) {
        const response = await pageJson({
          locale: langKey[lang]
        });


        data = response;
        saveLanguage(data, lang);
      }

      Object.keys(data).map(key => {
        data[key] = unzipDataBase64(data[key])
      });
      console.log(data)

      i18n.setLocaleMessage(langKey[lang], {
        ...messages[lang],
        ...data,
      })
      setI18nLanguage(langKey[lang])

      return true
    }


  }
  return false
}
function base64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding)
    .replace(/\-/g, '+') // 将'-'转换为'+'，因为Base64URL编码中可能使用'-'代替'+'
    .replace(/_/g, '/'); // 将'_'转换为'/'，因为Base64URL编码中可能使用'_'代替'/'

  const binaryString = atob(base64); // 解码Base64字符串为二进制字符串
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

function unzipDataBase64(content) {
  const compressed = base64ToUint8Array(content);
  let result = pako.inflate(compressed, { to: 'string' });
  return JSON.parse(result);
}

// 保存缓存
function saveLanguage(data, lang) {
  const key = `i18n-lang`;

  let obj = {
    lang: lang,
    createTime: Date.now(),
    data: data
  }

  window.localStorage.setItem(key, JSON.stringify(obj));

}
function getLanguage(lang) {
  const key = `i18n-lang`;

  let data = window.localStorage.getItem(key);
  if (data) {
    data = JSON.parse(data);

    // 判断是否为语言变更
    if (data.lang !== lang) {
      return false;
    }

    // 设置超时时间
    let expireTime = 7 * 24 * 60 * 60 * 1000;
    if (!data.createTime) {
      return false;
    }

    if (Date.now() - data.createTime > expireTime) {
      return false;
    }

    return data.data ? data.data : false;

  }

  return false;


}
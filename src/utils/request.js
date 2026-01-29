import axios from 'axios'
import { Notification, MessageBox, Message, Loading } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import { serialize } from '@/utils/object-to-formdata'
import i18n from '@/lang'
import cos from 'highlight.js/lib/languages/cos'

let downloadLoadingInstance
// 是否显示重新登录
export const isRelogin = { show: false }

axios.defaults.headers['Content-Type'] =
  'application/x-www-form-urlencoded;charset=UTF-8'
// axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: process.env.VUE_APP_BASE_API,
  // 超时
  timeout: 60000 * 10
})

// request拦截器
service.interceptors.request.use(
  function (config) {
    if (
      config.headers?.['Content-Type'] ===
      'application/x-www-form-urlencoded;charset=UTF-8' &&
      config.data
    ) {

      let dataParams = null;
      if (config.data.params) {
        let params = {
          params: { ...config.data.params }
        }
        delete config.data.params;
        // dataParams = JSON.stringify(params)
        dataParams = serialize(
          params,
          {
            indices: true,
            nullsAsUndefineds: true,
            booleansAsIntegers: true,
            allowEmptyArrays: false,
            noAttributesWithArrayNotation: false,
            noFilesWithArrayNotation: false,
            dotsForObjectNotation: false
          }
        )
      }

      const options = {
        /**
         * include array indices in FormData keys
         * defaults to false
         */
        indices: true,

        /**
         * treat null values like undefined values and ignore them
         * defaults to false
         */
        nullsAsUndefineds: true,

        /**
         * convert true or false to 1 or 0 respectively
         * defaults to false
         */
        booleansAsIntegers: true,

        /**
         * store arrays even if they're empty
         * defaults to false
         */
        allowEmptyArrays: false,

        /**
         * don't include array notation in FormData keys for any Attributes excepted Files in arrays
         * defaults to false
         */
        noAttributesWithArrayNotation: false,

        /**
         * don't include array notation in FormData keys for Files in arrays
         * defaults to false
         */
        noFilesWithArrayNotation: false,

        /**
         * use dots instead of brackets for object notation in FormData keys
         * defaults to false
         */
        dotsForObjectNotation: true
      }
      // API请求批量新增lang字段
      // if (config.data && config.data.lang === undefined) {
      //   if (typeof config.data !== 'object') config.data = JSON.parse(config.data)
      //   config.data.lang = store.getters.language
      // }
      const formData = serialize(
        config.data,
        options // optional
        // existingFormData, // optional
        // keyPrefix // optional
      )
      // const formData = new FormData();
      // Object.keys(config.data).forEach((key) => {
      //   const p = config.data[key];
      //   if (
      //     Object.prototype.toString.call(p) !== "[object Null]" &&
      //     Object.prototype.toString.call(p) !== "[object Undefined]"
      //   ) {
      //     if (typeof p === "object") {
      //       formData.append(key, JSON.stringify(p));
      //     } else {
      //       formData.append(key, p);
      //     }
      //   }
      // });
      if (dataParams) {
        for (let pair of dataParams.entries()) {
          formData.append(pair[0], pair[1]);
        }
      }

      config.data = formData
    }
    // 是否需要设置 token
    // const isToken = (config.headers || {}).isToken === false
    // // 是否需要防止数据重复提交
    const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
    // if (getToken() && !isToken) {
    //   config.headers['Authorization'] = 'Bearer ' + getToken() // 让每个请求携带自定义token 请根据实际情况自行修改
    // }
    config.params = config.params ? config.params : {}
    // get请求映射params参数
    if (config.method === 'get' && config.params) {
      // // API请求批量新增lang字段
      // if (config.params.lang === undefined) {
      //   if (typeof config.params !== 'object') config.params = JSON.parse(config.params)
      //   config.params.lang = store.getters.language
      // }
      let url = config.url + '?' + tansParams(config.params)
      url = url.slice(0, -1)
      config.params = {}
      config.url = url
    }
    if (
      !isRepeatSubmit &&
      (config.method === 'post' || config.method === 'put')
    ) {
      const requestObj = {
        url: config.url,
        data:
          typeof config.data === 'object'
            ? JSON.stringify(config.data)
            : config.data,
        time: new Date().getTime()
      }
      const requestSize = Object.keys(JSON.stringify(requestObj)).length // 请求数据大小
      const limitSize = 5 * 1024 * 1024 // 限制存放数据5M
      if (requestSize >= limitSize) {
        console.warn(
          `[${config.url}]: ` +
          '请求数据大小超出允许的5M限制，无法进行防重复提交验证。'
        )
        return config
      }
      const sessionObj = cache.session.getJSON('sessionObj')
      if (
        sessionObj === undefined ||
        sessionObj === null ||
        sessionObj === ''
      ) {
        cache.session.setJSON('sessionObj', requestObj)
      } else {
        const s_url = sessionObj.url // 请求地址
        const s_data = sessionObj.data // 请求数据
        const s_time = sessionObj.time // 请求时间
        const interval = 200 // 间隔时间(ms)，小于此时间视为重复提交
        if (
          s_data === requestObj.data &&
          requestObj.time - s_time < interval &&
          s_url === requestObj.url
        ) {
          console.count(s_url);
          const message = i18n.t("common.duplicateRequest")//'数据正在处理，请勿重复提交'
          console.warn(`[${s_url}]: ` + message)
          return Promise.reject(new Error(message))
        } else {
          cache.session.setJSON('sessionObj', requestObj)
        }
      }
    }
    return config
  },
  (error) => {
    console.log(error)
    Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (res) => {
    if (res.request?.responseURL?.endsWith(`${location.host}/login`)) {
      location.href = '/login'
      return Promise.reject('redirect')
    }


    // 未设置状态码则默认成功状态
    const code = res.data.code !== undefined ? res.data.code : 200
    // 获取错误信息
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    // 二进制数据则直接返回
    if (
      res.request.responseType === 'blob' ||
      res.request.responseType === 'arraybuffer'
    ) {
      return res.data
    }
    if (code == 1 || code === 401) {
      if (!isRelogin.show) {
        isRelogin.show = true
        MessageBox.confirm(
          '登录状态已过期，请重新登录',
          '系统提示',
          {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            showCancelButton: false,
            type: 'warning',
            closeOnClickModal: false,
            closeOnPressEscape: false,
          }
        ).then(() => {
          isRelogin.show = false
          // store.dispatch('LogOut').then(() => {
          //   location.href = '/login'
          // })
          if (location.href.endsWith(`${location.host}/login`)) {
            return
          }
          location.href = '/login'
        })
          .catch(() => {
            isRelogin.show = false
          })
      }
      return Promise.reject('redirect')
    } else if (code === 500) {
      if (res.config.url.indexOf('/monthplan/supplyOrderPool/checkOverdue') != -1) {
        return Promise.reject(msg)
      } else {
        Message({ message: msg, type: 'error' ,dangerouslyUseHTMLString:true})
        return Promise.reject(new Error(msg))
      }

    } else if (code === 601) {
      Message({ message: msg, type: 'warning' })
      return Promise.reject('error')
    } else if (code === 301) {
      return res.data
    } else if (code !== 200) {
      Notification.error({ title: msg })
      return Promise.reject('error')
    } else {
      if (res.data.data == '0' || res.data.data == '1') {
        return res.data;
      }
      if (res.data?.data) {
        return res.data.data
      }
      return res.data
    }
  },
  (error) => {
    console.log('err' + error)
    let { message } = error
    if (message == 'Network Error') {
      message = '后端接口连接异常'
    } else if (message.includes('timeout')) {
      message = '系统接口请求超时'
    } else if (message.includes('Request failed with status code')) {
      message = '系统接口' + message.substr(message.length - 3) + '异常'
    }
    if (message != "redirect") {
      console.log('ssss')
      Message({ message: message, type: 'error', duration: 5 * 1000, dangerouslyUseHTMLString: true })

    }
    return Promise.reject(error)
  }
)

// 通用下载方法
export function download(url, params, filename, config) {
  downloadLoadingInstance = Loading.service({
    text: '正在下载数据，请稍候',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  return service
    .post(url, params, {
      transformRequest: [
        (params) => {
          return tansParams(params)
        }
      ],
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      responseType: 'blob',
      ...config
    })
    .then(async (data) => {
      const isBlob = blobValidate(data)
      if (isBlob) {
        const blob = new Blob([data])
        saveAs(blob, filename)
      } else {
        const resText = await data.text()
        const rspObj = JSON.parse(resText)
        const errMsg =
          errorCode[rspObj.code] || rspObj.msg || errorCode['default']
        Message.error(errMsg)
      }
      downloadLoadingInstance.close()
    })
    .catch((r) => {
      console.error(r)
      Message.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance.close()
    })
}

export function downloadGet(url, params, filename, config) {
  downloadLoadingInstance = Loading.service({
    text: "正在下载数据，请稍候",
    spinner: "el-icon-loading",
    background: "rgba(0, 0, 0, 0.7)",
  });
  return service
    .get(url, params, {
      transformRequest: [
        (params) => {
          return tansParams(params);
        },
      ],
      // headers: { "Content-Type": "application/x-www-form-urlencoded" },
      responseType: "blob",
      ...config,
    })
    .then(async (data) => {
      const isBlob = blobValidate(data);
      if (isBlob) {
        const blob = new Blob([data]);
        let href = window.URL.createObjectURL(blob)
        let downloadDom = document.createElement('a')
        downloadDom.href = href
        // downloadDom.download=fileName //--不是必须 若需要【前端重命名文件夹】的话这句代码就需要
        downloadDom.download = "生产排程版本.xlsx"
        document.body.appendChild(downloadDom)
        downloadDom.click()
        // document.body.removeChild(downloadDom)
        // window.URL.revokeObjectURL(href)
        // saveAs(blob, filename);

      } else {
        const resText = await data.text();
        const rspObj = JSON.parse(resText);
        const errMsg =
          errorCode[rspObj.code] || rspObj.msg || errorCode["default"];
        Message.error(errMsg);
      }
      downloadLoadingInstance.close();
    })
    .catch((r) => {
      console.error(r);
      Message.error("下载文件出现错误，请联系管理员！");
      downloadLoadingInstance.close();
    });
}

export function downloadLink(url, params = null, filename = null) {
  downloadLoadingInstance = Loading.service({
    text: "正在下载数据，请稍候",
    spinner: "el-icon-loading",
    background: "rgba(0, 0, 0, 0.7)",
  });

  if (params) {
    if (params.pageNum) {
      params.pageNum = undefined;
    }
    if (params.pageSize) {
      params.pageSize = undefined
    }
    const paramsStr = tansParams(params)
    if (paramsStr) {
      url += `?${paramsStr}`
    }
  }

  url = process.env.VUE_APP_BASE_API + url

  fetch(url)
    .then((response) => {
      if (!response.ok) {
        if (!response.ok) {
          return response.json();
        }
      }

      if (response.headers.get('Content-type') == "application/json") {
        return response.json();
      }

      if (!filename) {
        const disposition = response.headers.get('Content-Disposition');
        if (disposition && disposition.indexOf('attachment') !== -1) {
          const matches = disposition.replace("attachment;filename=", "");
          filename = decodeURIComponent(matches);

        }
      }

      return response.blob(); // 将响应体读取为 Blob 对象
    })
    .then((blob) => {
      if (blob instanceof Blob) {
        saveAs(blob, filename);
        downloadLoadingInstance.close()
      } else {
        console.log(blob);
        Message.error(blob.msg || "下载文件出现错误，请联系管理员！");
        downloadLoadingInstance.close()
      }
    })
    .catch((error) => {
      downloadLoadingInstance.close()
    });
}
service.send = function (url, data = {}, config = { headers: {} }) {
  if (!config.headers) {
    config.headers = {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  }
  if (!config.headers['Content-Type']) {
    config.headers['Content-Type'] = 'application/json;charset=UTF-8'
  }
  data = JSON.stringify(data)
  return service.post(url, data, config)
}

export default service

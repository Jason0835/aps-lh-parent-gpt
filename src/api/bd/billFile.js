/**
 * @Description:  页面
 * @Author: qy
 * @Date: 2024/2/22
 **/
import request from '@/utils/request'
import axios from 'axios'

// 通过单据类型与单id查找单据和文件模板关系及单据相关上传数据
export function selectTempAndBillFile(data) {
  return request({
    url: '/bd/billFileTemplate/selectTempAndBillFile',
    method: 'post',
    data: {
      ...data,
      isSettle: true
    }
  })
}
// 根据条件查询单据和文件模板关系数据
export function billFileTemplateList(data) {
  return request({
    url: '/bd/billFileTemplate/list',
    method: 'post',
    data: {
      ...data,
      isSettle: true
    }
  })
}
// 附件上传
export function uploadFileSync(data) {
  return request({
    url: '/common/fileUpload/uploadFileSync',
    method: 'post',
    data: {
      ...data,
      isSettle: true
    }
  })

  // return axios.request({
  //   method: 'post',
  //   url: process.env.VUE_APP_BASE_API + '/common/fileUpload/uploadFileSync',
  //   withCredentials: false,
  //   headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
  //   validateStatus: validateStatus,
  //   onUploadProgress: onUploadProgress,
  //   data: data,
  // })
}
// 删除上传
export function deleteFileSync(data) {
  return request({
    url: '/common/scmFiles/del',
    method: 'post',
    data: {
      ...data,
      isSettle: true
    }
  })
}
// 动态path请求
export function baseRequest(url, type, data) {
  return request({
    url: url,
    method: type,
    data: {
      ...data,
      isSettle: true
    }
  })
}

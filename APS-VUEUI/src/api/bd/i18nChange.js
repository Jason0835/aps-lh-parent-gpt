import request from '@/utils/request'

// 查询1021.基础_商品类目列表

export function listLanguage(data) {
  return request({
    url: '/bd/i18nChange/list',
    method: 'post',
    data
  })
}
export function saveLanguage(data) {
  return request({
    url: '/bd/i18nChange/save',
    method: 'post',
    data
  })
}
export function exportLanguage(data) {
  return request({
    url: '/bd/i18nChange/save',
    method: 'post',
    data
  })
}
export function pageJson(data) {
  return request({
    url: '/common/bd/i18nChange/pageJson',
    method: 'post',
    data
  })
}

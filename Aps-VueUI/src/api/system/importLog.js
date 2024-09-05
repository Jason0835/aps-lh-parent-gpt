import request from '@/utils/request'

/**
 * 查询导入日志管理列表
 * @param {*} data
 * @returns
 */
export function list(data) {
  return request({
    url: '/system/importLog/list',
    method: 'post',
    data: data
  })
}

/**
 * 错误日志详情列表
 * @param {*} data
 * @returns
 */
export function errorDietailList(data) {
  return request({
    url: '/system/importLog/errorDetailList',
    method: 'post',
    data: data,
  })
}

// 清空操作日志
export function cleanOperlog() {
  return request({
    url: '/system/importLog/clean',
    method: 'post'
  })
}

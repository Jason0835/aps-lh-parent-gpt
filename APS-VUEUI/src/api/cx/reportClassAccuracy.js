import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询班次完成统计列表
 * @param {*} query
 * @returns
 */
export function listReportClassAccuracy(query) {
  return request({
    url: '/cx/reportClassAccuracy/list',
    method: 'post',
    data: query
  })
}

/**
 * 导出班次完成统计列表
 * @param {*} params
 * @returns
 */
export function exportReportClassAccuracy(params) {
  return downloadLink("/cx/reportClassAccuracy/export", params);
}

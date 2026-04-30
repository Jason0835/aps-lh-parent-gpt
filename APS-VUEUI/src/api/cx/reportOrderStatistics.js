import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询工单完成情况统计报表列表
 * @param {*} query
 * @returns
 */
export function listReportOrderStatistics(query) {
  return request({
    url: '/cx/reportOrderStatistics/list',
    method: 'post',
    data: query
  })
}

/**
 * 导出工单完成情况统计报表列表
 * @param {*} params
 * @returns
 */
export function exportReportOrderStatistics(params) {
  return downloadLink("/cx/reportOrderStatistics/export", params);
}

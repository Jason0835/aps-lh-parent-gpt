import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询报表统计列表
 * @param {*} query
 * @returns
 */
export function listReportStatistics(query) {
  return request({
    url: '/cx/reportStatistics/list',
    method: 'post',
    data: query
  })
}

/**
 * 导出报表统计列表
 * @param {*} params
 * @returns
 */
export function exportReportStatistics(params) {
  return downloadLink("/cx/reportStatistics/export", params);
}

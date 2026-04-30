import request,{ downloadLink } from '@/utils/request'

/**
 * 预生产库存倍数列表
 * @param {Object} query
 * @returns
 */
export function listReserveStock(query) {
  return request({
    url: 'gdyy/reserveStock/list',
    method: 'post',
    data: query
  })
}
/**
 * 编辑预生产库存倍数
 * @param {Object} query
 * @returns
 */
export function editReserveStock(query) {
  return request({
    url: 'gdyy/reserveStock/edit',
    method: 'post',
    data: query
  })
}
/**
 * 删除预生产库存倍数
 * @param {Object} query
 * @returns
 */
export function removeReserveStock(query) {
  return request({
    url: 'gdyy/reserveStock/remove',
    method: 'post',
    data: query
  })
}

/**
 * 导出预生产库存倍数
 * @param {*} params
 * @returns
 */
export function exportData(params) {
  return downloadLink("/gdyy/reserveStock/export", params);
}

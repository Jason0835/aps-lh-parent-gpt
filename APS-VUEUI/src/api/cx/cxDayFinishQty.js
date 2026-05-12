import request from '@/utils/request'

/**
 * 查询成型排程日完成量列表
 */
export function listCxDayFinishQty(query) {
  return request({
    url: '/cx/cxDayFinishQty/list',
    method: 'post',
    data: query
  })
}

/**
 * 根据ID获取成型排程日完成量详情
 */
export function getCxDayFinishQty(id) {
  return request({
    url: `/cx/cxDayFinishQty/${id}`,
    method: 'get'
  })
}

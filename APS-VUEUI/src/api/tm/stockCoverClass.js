import request from '@/utils/request'

// 查询备库班数配置列表
export function listStockCoverClass(query) {
  return request({
    url: '/tm/tmStockCoverClass/list',
    method: 'post',
    data: query
  })
}

// 获取备库班数配置详细信息
export function getStockCoverClass(id) {
  return request({
    url: '/tm/tmStockCoverClass/' + id,
    method: 'get'
  })
}

// 新增/修改备库班数配置
export function saveStockCoverClass(data) {
  return request({
    url: '/tm/tmStockCoverClass/save',
    method: 'post',
    data: data
  })
}

// 删除备库班数配置
export function removeStockCoverClass(ids) {
  return request({
    url: '/tm/tmStockCoverClass/remove',
    method: 'post',
    data: ids
  })
}

// 导出备库班数配置
export function exportStockCoverClass(query) {
  return request({
    url: '/tm/tmStockCoverClass/export',
    method: 'get',
    params: query
  })
}

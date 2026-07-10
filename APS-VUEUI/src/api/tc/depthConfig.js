import request from '@/utils/request'

// 查询胎侧备库班数配置列表
export function listDepthConfig(query) {
  return request({
    url: '/tc/depthConfig/list',
    method: 'post',
    data: query
  })
}

// 获取胎侧备库班数配置详细信息
export function getDepthConfig(id) {
  return request({
    url: '/tc/depthConfig/' + id,
    method: 'get'
  })
}

// 新增/修改胎侧备库班数配置
export function saveDepthConfig(data) {
  return request({
    url: '/tc/depthConfig/save',
    method: 'post',
    data: data
  })
}

// 删除胎侧备库班数配置
export function removeDepthConfig(ids) {
  return request({
    url: '/tc/depthConfig/remove',
    method: 'post',
    data: ids
  })
}

// 导出胎侧备库班数配置
export function exportDepthConfig(query) {
  return request({
    url: '/tc/depthConfig/export',
    method: 'get',
    params: query
  })
}

import request from '@/utils/request'

// 查询备库班数配置列表
export function listDepthConfig(query) {
  return request({
    url: '/tm/depthConfig/list',
    method: 'post',
    data: query
  })
}

// 获取备库班数配置详细信息
export function getDepthConfig(id) {
  return request({
    url: '/tm/depthConfig/' + id,
    method: 'get'
  })
}

// 新增/修改备库班数配置
export function saveDepthConfig(data) {
  return request({
    url: '/tm/depthConfig/save',
    method: 'post',
    data: data
  })
}

// 删除备库班数配置
export function removeDepthConfig(ids) {
  return request({
    url: '/tm/depthConfig/remove',
    method: 'post',
    data: ids
  })
}

// 导出备库班数配置
export function exportDepthConfig(query) {
  return request({
    url: '/tm/depthConfig/export',
    method: 'get',
    params: query
  })
}

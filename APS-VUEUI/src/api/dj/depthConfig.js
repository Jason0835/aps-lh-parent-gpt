import request from '@/utils/request'

// 查询垫胶备库班数与供成型机数配置列表
export function listDepthConfig(query) {
  return request({
    url: '/dj/depthConfig/list',
    method: 'post',
    data: query
  })
}

// 获取垫胶备库班数与供成型机数配置详细信息
export function getDepthConfig(id) {
  return request({
    url: '/dj/depthConfig/' + id,
    method: 'get'
  })
}

// 新增/修改垫胶备库班数与供成型机数配置
export function saveDepthConfig(data) {
  return request({
    url: '/dj/depthConfig/save',
    method: 'post',
    data: data
  })
}

// 删除垫胶备库班数与供成型机数配置
export function removeDepthConfig(ids) {
  return request({
    url: '/dj/depthConfig/remove',
    method: 'post',
    data: ids
  })
}

// 导出垫胶备库班数与供成型机数配置
export function exportDepthConfig(query) {
  return request({
    url: '/dj/depthConfig/export',
    method: 'get',
    params: query
  })
}

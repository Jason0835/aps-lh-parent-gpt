import request from '@/utils/request'

// 查询钢丝圈排程参数配置列表
export function listParams(query) {
  return request({
    url: '/gsq/params/list',
    method: 'post',
    data: query
  })
}

// 保存钢丝圈排程参数配置
export function saveParams(data) {
  return request({
    url: '/gsq/params/save',
    method: 'post',
    data: data
  })
}

// 删除钢丝圈排程参数配置
export function removeParams(query) {
  return request({
    url: '/gsq/params/remove',
    method: 'delete',
    data: query
  })
}

// 获取钢丝圈排程参数配置详细信息
export function getInfo(id) {
  return request({
    url: '/gsq/params/' + id,
    method: 'get'
  })
}

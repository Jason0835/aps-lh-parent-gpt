import request from '@/utils/request'

// 查询垫胶参数信息列表
export function listParams(query) {
  return request({
    url: '/dj/params/list',
    method: 'post',
    data: query
  })
}

// 获取垫胶参数信息详细信息
export function getParams(id) {
  return request({
    url: '/dj/params/' + id,
    method: 'get'
  })
}

// 新增垫胶参数信息
export function addParams(data) {
  return request({
    url: '/dj/params/add',
    method: 'post',
    data: data
  })
}

// 修改垫胶参数信息
export function editParams(data) {
  return request({
    url: '/dj/params/edit',
    method: 'post',
    data: data
  })
}

// 删除垫胶参数信息
export function removeParams(ids) {
  return request({
    url: '/dj/params/' + ids,
    method: 'delete'
  })
}

// 校验参数代码唯一性
export function checkParamsCodeUnique(paramCode) {
  return request({
    url: '/dj/params/checkUnique',
    method: 'post',
    data: { paramCode }
  })
}
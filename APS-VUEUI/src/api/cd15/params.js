import request, { downloadLink } from '@/utils/request'

// 列表查询
export function listParams(query) {
  return request({
    url: '/cd15/cd15Params/list',
    method: 'post',
    data: query
  })
}

// 获取详情
export function getParams(id) {
  return request({
    url: `/cd15/cd15Params/getInfo/${id}`,
    method: 'get'
  })
}

// 新增
export function addParams(data) {
  return request({
    url: '/cd15/cd15Params/add',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

// 编辑
export function editParams(data) {
  return request({
    url: '/cd15/cd15Params/edit',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

// 删除
export function delParams(data) {
  return request({
    url: '/cd15/cd15Params/remove',
    method: 'post',
    data
  })
}

// 校验唯一性
export function checkUnique(data) {
  return request({
    url: '/cd15/cd15Params/checkUnique',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

// 导出
export function exportParams(query) {
  return downloadLink('/cd15/cd15Params/export', query)
}

// 导入模板下载
export function importTemplate() {
  return downloadLink('/cd15/cd15Params/importTemplate', {})
}
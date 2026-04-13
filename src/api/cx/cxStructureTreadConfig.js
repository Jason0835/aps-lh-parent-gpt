import request from '@/utils/request'

export function listCxStructureTreadConfig(query) {
  return request({
    url: '/cx/cxStructureTreadConfig/list',
    method: 'post',
    data: query
  })
}

export function getCxStructureTreadConfig(id) {
  return request({
    url: `/cx/cxStructureTreadConfig/${id}`,
    method: 'get'
  })
}

export function saveCxStructureTreadConfig(data) {
  return request({
    url: '/cx/cxStructureTreadConfig/save',
    method: 'post',
    data
  })
}

export function removeCxStructureTreadConfig(ids) {
  return request({
    url: '/cx/cxStructureTreadConfig/remove',
    method: 'post',
    params: { ids: ids.join(',') }
  })
}

export function checkUniqueCxStructureTreadConfig(data) {
  return request({
    url: '/cx/cxStructureTreadConfig/checkUnique',
    method: 'post',
    data
  })
}

export function exportCxStructureTreadConfig(data) {
  return request({
    url: '/cx/cxStructureTreadConfig/export',
    method: 'get',
    params: data,
    responseType: 'blob'
  })
}

export function importCxStructureTreadConfig(formData) {
  return request({
    url: '/cx/cxStructureTreadConfig/importData',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importTemplateCxStructureTreadConfig() {
  return request({
    url: '/cx/cxStructureTreadConfig/importTemplate',
    method: 'get',
    responseType: 'blob'
  })
}

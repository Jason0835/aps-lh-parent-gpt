import request from '@/utils/request'

export function listMdmStructureTreadConfig(query) {
  return request({
    url: '/cx/mdmStructureTreadConfig/list',
    method: 'post',
    data: query
  })
}

export function getMdmStructureTreadConfig(id) {
  return request({
    url: `/cx/mdmStructureTreadConfig/${id}`,
    method: 'get'
  })
}

export function saveMdmStructureTreadConfig(data) {
  return request({
    url: '/cx/mdmStructureTreadConfig/save',
    method: 'post',
    data
  })
}

export function removeMdmStructureTreadConfig(ids) {
  return request({
    url: '/cx/mdmStructureTreadConfig/remove',
    method: 'post',
    params: { ids: ids.join(',') }
  })
}

export function checkUniqueMdmStructureTreadConfig(data) {
  return request({
    url: '/cx/mdmStructureTreadConfig/checkUnique',
    method: 'post',
    data
  })
}

export function exportMdmStructureTreadConfig(data) {
  return request({
    url: '/cx/mdmStructureTreadConfig/export',
    method: 'get',
    params: data,
    responseType: 'blob'
  })
}

export function importMdmStructureTreadConfig(formData) {
  return request({
    url: '/cx/mdmStructureTreadConfig/importData',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importTemplateMdmStructureTreadConfig() {
  return request({
    url: '/cx/mdmStructureTreadConfig/importTemplate',
    method: 'get',
    responseType: 'blob'
  })
}

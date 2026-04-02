import request from '@/utils/request'

export function listMdmStructureTreadConfig(query) {
  const { pageNum, pageSize, ...filters } = query
  return request({
    url: '/cx/mdmStructureTreadConfig/list',
    method: 'post',
    params: { pageNum, pageSize },
    data: filters
  })
}

export function getMdmStructureTreadConfig(id) {
  return request({
    url: `/cx/mdmStructureTreadConfig/${id}`,
    method: 'get'
  })
}

export function addMdmStructureTreadConfig(data) {
  return request({
    url: '/cx/mdmStructureTreadConfig/add',
    method: 'post',
    data
  })
}

export function editMdmStructureTreadConfig(data) {
  return request({
    url: '/cx/mdmStructureTreadConfig/edit',
    method: 'put',
    data
  })
}

export function removeMdmStructureTreadConfig(params) {
  return request({
    url: '/cx/mdmStructureTreadConfig/remove',
    method: 'post',
    params
  })
}

export function exportMdmStructureTreadConfig(data) {
  return request({
    url: '/cx/mdmStructureTreadConfig/export',
    method: 'post',
    data,
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

import request from '@/utils/request'

export function listCxMachineOnlineInfo(query) {
  return request({
    url: '/cx/cxMachineOnlineInfo/list',
    method: 'post',
    data: query
  })
}

export function getCxMachineOnlineInfo(id) {
  return request({
    url: `/cx/cxMachineOnlineInfo/${id}`,
    method: 'get'
  })
}

export function saveCxMachineOnlineInfo(data) {
  return request({
    url: '/cx/cxMachineOnlineInfo/save',
    method: 'post',
    data
  })
}

export function removeCxMachineOnlineInfo(ids) {
  return request({
    url: '/cx/cxMachineOnlineInfo/remove',
    method: 'post',
    params: { ids: ids.join(',') }
  })
}

export function checkUniqueCxMachineOnlineInfo(data) {
  return request({
    url: '/cx/cxMachineOnlineInfo/checkUnique',
    method: 'post',
    data
  })
}

export function exportCxMachineOnlineInfo(data) {
  return request({
    url: '/cx/cxMachineOnlineInfo/export',
    method: 'get',
    params: data,
    responseType: 'blob'
  })
}

export function importCxMachineOnlineInfo(formData) {
  return request({
    url: '/cx/cxMachineOnlineInfo/importData',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importTemplateCxMachineOnlineInfo() {
  return request({
    url: '/cx/cxMachineOnlineInfo/importTemplate',
    method: 'get',
    responseType: 'blob'
  })
}


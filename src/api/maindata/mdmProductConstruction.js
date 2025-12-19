import request, { downloadLink } from '@/utils/request'

// =
export function listMdmProductConstruction(query) {
  return request({
    url: '/monthplan/mdmSkuConstructionRef/list',
    method: 'post',
    data: query
  })
}
export function editMdmProductConstruction(query) {
  return request({
    url: '/maindata/mdmProductConstruction/save',
    method: 'post',
    data: query
  })
}
export function removeMdmProductConstruction(query) {
  return request({
    url: '/maindata/mdmProductConstruction/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/maindata/mdmProductConstruction/export', query)
}
export function mesCapture(query) {
  return request({
    url: '/monthplan/mdmSkuConstructionRef/mesCapture',
    method: 'post',
    data: query
  })
}
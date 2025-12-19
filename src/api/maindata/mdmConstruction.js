import request, { downloadLink } from '@/utils/request'

// =
export function listMdmProductConstruction(query) {
  return request({
    url: '/monthplan/mdmConstructionInfo/list',
    method: 'post',
    data: query
  })
}
export function editMdmProductConstruction(query) {
  return request({
    url: '/monthplan/mdmConstructionInfo/save',
    method: 'post',
    data: query
  })
}
export function removeMdmProductConstruction(query) {
  return request({
    url: '/monthplan/mdmConstructionInfo/remove',
    method: 'post',
    data: query
  })
}


export function mesCapture(query) {
  return request({
    url: '/monthplan/mdmConstructionInfo/mesCapture',
    method: 'post',
    data: query
  })
}
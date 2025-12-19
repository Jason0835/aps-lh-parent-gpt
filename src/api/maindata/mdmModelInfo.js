import request, { downloadLink } from '@/utils/request'

// =
export function listMdmModelInfo(query) {
  return request({
    url: '/maindata/mdmModelInfo/list',
    method: 'post',
    data: query
  })
}
export function editMdmModelInfo(query) {
  return request({
    url: '/maindata/mdmModelInfo/save',
    method: 'post',
    data: query
  })
}
export function removeMdmModelInfo(query) {
  return request({
    url: '/maindata/mdmModelInfo/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/maindata/mdmModelInfo/export', query)
}
export function mesCapture(query) {
  return request({
    url: '/maindata/mdmModelInfo/mesCapture',
    method: 'post',
    data: query
  })
}
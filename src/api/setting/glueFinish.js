import request, { downloadLink } from '@/utils/request'


export function listGlueFinish(query) {
  return request({
    url: '/setting/glueFinish/list',
    method: 'post',
    data: query
  })
}
export function removeGlueFinish(query) {
  return request({
    url: '/setting/glueFinish/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueFinish(query) {
  return request({
    url: '/setting/glueFinish/save',
    method: 'post',
    data: query
  })
}
export function exportData(query) {
  downloadLink('/setting/glueFinish/export', query)
}

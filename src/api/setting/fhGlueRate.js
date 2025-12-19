import request, {downloadLink} from '@/utils/request'


export function listReturnedRate(query) {
  return request({
    url: '/setting/fhGlueRate/list',
    method: 'post',
    data: query
  })
}
export function removeReturnedRate(query) {
  return request({
    url: '/setting/fhGlueRate/remove',
    method: 'post',
    data: query
  })
}
export function saveReturnedRate(query) {
  return request({
    url: '/setting/fhGlueRate/save',
    method: 'post',
    data: query
  })
}
export function exportData(params) {
  return downloadLink("/setting/fhGlueRate/export", params);
}

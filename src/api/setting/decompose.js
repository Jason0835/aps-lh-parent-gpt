import request, {downloadLink} from '@/utils/request'

// =
export function listDecompose(query) {
  return request({
    url: '/setting/decompose/list',
    method: 'post',
    data: query
  })
}
export function removeDecompose(query) {
  return request({
    url: '/setting/decompose/remove',
    method: 'post',
    data: query
  })
}
export function saveDecompose(query) {
  return request({
    url: '/setting/decompose/save',
    method: 'post',
    data: query
  })
}
export function checkGlueDecomposeUnique(query) {
  return request({
    url: '/setting/decompose/checkGlueDecomposeUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/decompose/checkComplete',
    method: 'post',
    data: query
  })
}
export function exportData(params) {
  return downloadLink("/setting/decompose/export", params);
}

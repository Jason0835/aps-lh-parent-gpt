import request, {downloadLink} from '@/utils/request'

// =
export function listMaterBatch(query) {
  return request({
    url: '/setting/mlstock/list',
    method: 'post',
    data: query
  })
}
export function removeMaterBatch(query) {
  return request({
    url: '/setting/mlstock/remove',
    method: 'post',
    data: query
  })
}
export function saveMaterBatch(query) {
  return request({
    url: '/setting/mlstock/save',
    method: 'post',
    data: query
  })
}
export function checkGlueMaterBatchUnique(query) {
  return request({
    url: '/setting/mlstock/checkGlueMaterBatchUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/mlstock/checkComplete',
    method: 'post',
    data: query
  })
}
export function exportData(params) {
  return downloadLink("/setting/mlstock/export", params);
}

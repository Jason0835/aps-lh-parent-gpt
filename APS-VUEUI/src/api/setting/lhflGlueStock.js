import request, { downloadLink } from '@/utils/request'


export function listLhflGlueStock(query) {
  return request({
    url: '/setting/lhflGlueStock/list',
    method: 'post',
    data: query
  })
}
export function removeLhflGlueStock(query) {
  return request({
    url: '/setting/lhflGlueStock/remove',
    method: 'post',
    data: query
  })
}
export function saveLhflGlueStock(query) {
  return request({
    url: '/setting/lhflGlueStock/save',
    method: 'post',
    data: query
  })
}


export function exportData(query) {
  return downloadLink('/setting/lhflGlueStock/export', query);
}

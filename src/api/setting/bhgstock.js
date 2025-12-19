import request, { downloadLink } from '@/utils/request'

// =
export function listBelowStandard(query) {
  return request({
    url: '/setting/bhgstock/list',
    method: 'post',
    data: query
  })
}
export function removeBelowStandard(query) {
  return request({
    url: '/setting/bhgstock/remove',
    method: 'post',
    data: query
  })
}
export function saveBelowStandard(query) {
  return request({
    url: '/setting/bhgstock/save',
    method: 'post',
    data: query
  })
}
export function checkGlueBelowStandardUnique(query) {
  return request({
    url: '/setting/bhgstock/checkGlueBelowStandardUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/bhgstock/checkComplete',
    method: 'post',
    data: query
  })
}


export function exportData(query) {
  return downloadLink('/setting/bhgstock/export', query);
}


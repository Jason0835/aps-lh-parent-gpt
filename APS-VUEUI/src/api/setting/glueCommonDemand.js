import request, { downloadLink } from '@/utils/request'


export function listGlueCommonDemand(query) {
  return request({
    url: '/setting/glueCommonDemand/list',
    method: 'post',
    data: query
  })
}
export function removeGlueCommonDemand(query) {
  return request({
    url: '/setting/glueCommonDemand/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueCommonDemand(query) {
  return request({
    url: '/setting/glueCommonDemand/save',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/glueCommonDemand/export', query);
}

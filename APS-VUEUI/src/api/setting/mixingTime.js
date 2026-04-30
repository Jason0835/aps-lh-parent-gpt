import request, {downloadLink} from '@/utils/request'


export function listMixingTime(query) {
  return request({
    url: '/setting/mixingTime/list',
    method: 'post',
    data: query
  })
}
export function removeMixingTime(query) {
  return request({
    url: '/setting/mixingTime/remove',
    method: 'post',
    data: query
  })
}
export function saveMixingTime(query) {
  return request({
    url: '/setting/mixingTime/save',
    method: 'post',
    data: query
  })
}
export function exportData(params) {
  return downloadLink("/setting/mixingTime/export", params);
}

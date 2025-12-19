import request, {downloadLink} from '@/utils/request'


export function listGlueSpanSetting(query) {
  return request({
    url: '/setting/glueSpanSetting/list',
    method: 'post',
    data: query
  })
}
export function removeGlueSpanSetting(query) {
  return request({
    url: '/setting/glueSpanSetting/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueSpanSetting(query) {
  return request({
    url: '/setting/glueSpanSetting/save',
    method: 'post',
    data: query
  })
}
export function exportData(params) {
  return downloadLink("/setting/glueSpanSetting/export", params);
}

import request, {downloadLink} from '@/utils/request'

// =
export function listRemindSetting(query) {
  return request({
    url: '/setting/remindSetting/list',
    method: 'post',
    data: query
  })
}
export function removeRemindSetting(query) {
  return request({
    url: '/setting/remindSetting/remove',
    method: 'post',
    data: query
  })
}
export function saveRemindSetting(query) {
  return request({
    url: '/setting/remindSetting/save',
    method: 'post',
    data: query
  })
}

export function exportData(params) {
  return downloadLink("/setting/remindSetting/export", params);
}

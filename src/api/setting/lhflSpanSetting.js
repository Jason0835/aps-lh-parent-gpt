import request from '@/utils/request'

// =
export function listLhflSpanSetting(query) {
  return request({
    url: '/setting/lhflSpanSetting/list',
    method: 'post',
    data: query
  })
}
export function removeLhflSpanSetting(query) {
  return request({
    url: '/setting/lhflSpanSetting/remove',
    method: 'post',
    data: query
  })
}
export function saveLhflSpanSetting(query) {
  return request({
    url: '/setting/lhflSpanSetting/save',
    method: 'post',
    data: query
  })
}

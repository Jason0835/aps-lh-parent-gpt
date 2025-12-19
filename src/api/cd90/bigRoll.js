import request,{ downloadLink } from '@/utils/request'

/**
 * 根据条件查询帘布大卷列表
 * @param {Object} query
 * @returns
 */
export function listBigRoll(query) {
  return request({
    url: 'cd90/bigRoll/list',
    method: 'post',
    data: query
  })
}
/**
 * 修改帘布大卷
 * @param {Object} query
 * @returns
 */
export function editBigRoll(query) {
  return request({
    url: 'cd90/bigRoll/save',
    method: 'post',
    data: query
  })
}
export function removeBigRoll(query) {
  return request({
    url: '/cd90/bigRoll/remove',
    method: 'post',
    data: query
  })
}
export function checkBigRollCodeUnique(query) {
  return request({
    url: '/cd90/bigRoll/checkBigRollCodeUnique',
    method: 'post',
    data: query
  })
}

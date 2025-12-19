import request,{ downloadLink } from '@/utils/request'

/**
 * 口型板列表
 * @param {Object} query
 * @returns
 */
export function listMouthPlate(query) {
  return request({
    url: 'xwyy/mouthPlate/list',
    method: 'post',
    data: query
  })
}
/**
 * 编辑成型机台
 * @param {Object} query
 * @returns
 */
export function editMouthPlate(query) {
  return request({
    url: 'xwyy/mouthPlate/edit',
    method: 'post',
    data: query
  })
}
export function removeMouthPlate(query) {
  return request({
    url: 'xwyy/mouthPlate/remove',
    method: 'post',
    data: query
  })
}

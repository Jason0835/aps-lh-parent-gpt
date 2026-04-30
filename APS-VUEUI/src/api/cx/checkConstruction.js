import request from '@/utils/request'

// 查询月度检测施工
export function listCheckConstruction(query) {
  return request({
    url: '/cx/checkConstruction/list',
    method: 'post',
    data: query
  })
}

/**
 * 检测施工
 * @param {*} query
 * @returns
 */
export function checkConstruction(query) {
  return request({
    url: '/cx/checkConstruction/checkConstruction',
    method: 'post',
    data: query
  })
}

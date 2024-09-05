import request from '@/utils/request'

// 根据条件查询施工信息列表
export function listConstructionInfo(query) {
  return request({
    url: '/cx/constructionInfo/list',
    method: 'post',
    data: query
  })
}

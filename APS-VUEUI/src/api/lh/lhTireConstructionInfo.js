import request from '@/utils/request'

// 不良数
export function listLhTireConstructionInfo(query) {
  return request({
    url: '/lh/lhTireConstructionInfo/list',
    method: 'post',
    data: query
  })
}



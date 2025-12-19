import request from '@/utils/request'

// 根据条件查询施工信息列表
export function listConstructionInfo(query) {
  return request({
    url: '/maindata/mdmBomInfo/list',
    method: 'post',
    data: query
  })
}

export function mesCapture(query) {
  return request({
    url: '/maindata/mdmBomInfo/mesCapture',
    method: 'post',
    data: query
  })
}

import request from '@/utils/request'

// 查询在线用户列表
export function list(data) {
  return request({
    url: '/monitor/online/list',
    method: 'post',
    data: data
  })
}

// 强退用户
export function forceLogout(tokenId) {
  return request({
    url: '/monitor/online/batchForceLogout',
    method: 'post',
    data: {
      ids: tokenId
    }
  })
}

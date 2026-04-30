import request from '@/utils/request'

// 查询公告列表
export function listNotice(data) {
  return request({
    url: '/system/notice/list',
    method: 'post',
    data: data
  })
}

// 查询公告详细
export function getNotice(noticeId) {
  return request({
    url: '/system/notice/detail/' + noticeId,
    method: 'get'
  })
}

// 新增公告
export function addNotice(data) {
  return request({
    url: '/system/notice/add',
    method: 'post',
    data: data
  })
}

// 修改公告
export function updateNotice(data) {
  return request({
    url: '/system/notice/edit',
    method: 'post',
    data: data
  })
}

// 删除公告
export function delNotice(noticeId) {
  return request({
    url: '/system/notice/remove',
    method: 'post',
    data: {
      ids: noticeId.join(',')
    }
  })
}

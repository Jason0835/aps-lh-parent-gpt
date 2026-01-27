import request from '@/utils/request'
export function listTemplate(query) {
  return request({
    url: '/message/templateList/list ',
    method: 'post',
    data: query,
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },

  })
}
export function editTemplate(query) {
  return request({
    url: '/message/templateList/edit',
    method: 'post',
    data: query,

  })
}
export function removeTemplate(query) {
  return request({
    url: '/message/templateList/remove',
    method: 'post',
    data: query,

  })
}

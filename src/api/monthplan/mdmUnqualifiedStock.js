import request from '@/utils/request'

// =
export function listMdmUnqualifiedStock(query) {
  return request({
    url: '/monthplan/mdmUnqualifiedStock/list',
    method: 'post',
    data: query
  })
}
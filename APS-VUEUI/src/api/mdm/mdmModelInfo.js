import request from '@/utils/request'

export function listMdmModelInfo(query) {
  return request({
    url: '/mdm/mdmModelInfo/list',
    method: 'post',
    data: query
  })
}

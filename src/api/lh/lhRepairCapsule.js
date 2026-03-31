import request from '@/utils/request'

export function listLhRepairCapsule(query) {
  return request({
    url: '/lh/lhRepairCapsule/list',
    method: 'post',
    data: query
  })
}

export function getLhRepairCapsuleInfo(id) {
  return request({
    url: '/lh/lhRepairCapsule/getInfo/' + id,
    method: 'get'
  })
}

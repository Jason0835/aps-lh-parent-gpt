import request from '@/utils/request'
export function listRawMaterialInfo(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/list',
    method: 'post',
    data: query
  })
}
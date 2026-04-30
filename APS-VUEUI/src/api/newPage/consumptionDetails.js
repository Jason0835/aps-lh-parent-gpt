import request from '@/utils/request'
export function listConsumptionDetails(query) {
  return request({
    url: '/mdm/mdmMaterialConsumeDetail/list ',
    method: 'post',
    data: query,
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },

  })
}
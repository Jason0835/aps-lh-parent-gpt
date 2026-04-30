import request from '@/utils/request'

// conversion
export function listConversion(query) {
  return request({
    url: '/cx/conversion/list',
    method: 'post',
    data: query
  })
}
export function batchSaveConversion(query) {
  return request({
    url: '/cx/conversion/batchSave',
    method: 'post',
    data: query
  })
}

export function batchPublishConversion(query) {
  return request({
    url: '/cx/conversion/batchPublish',
    method: 'post',
    data: query
  })
}
export function getMachineInfoListByHalfPartType(query) {
  return request({
    url: '/cx/conversion/getMachineInfoListByHalfPartType',
    method: 'post',
    data: query
  })
}



import request, { downloadLink } from '@/utils/request'

// =
export function listMdmProductConstruction(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/list',
    method: 'post',
    data: query
  })
}
// =
export function generateMdmProductConstruction(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/generate',
    method: 'post',
    data: query
  })
}

export function saveMdmProductConstruction(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/save',
    method: 'post',
    data: query
  })
}
export function removeMdmProductConstruction(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/remove',
    method: 'post',
    data: query
  })
}
export function generateVersion(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/generateVersion',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
export function getMdmProductVersion(query) {
  return request({
    url: '/maindata/rawMaterialRequirePlan/getVersionList',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}
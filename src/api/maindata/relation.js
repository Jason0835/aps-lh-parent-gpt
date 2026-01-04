import request, { downloadLink } from '@/utils/request'

// =
export function listRelation(query) {
  return request({
    url: '/maindata/relation/list',
    method: 'post',
    data: query
  })
}
export function editRelation(query) {
  return request({
    url: '/maindata/relation/save',
    method: 'post',
    data: query
  })
}
export function removeRelation(query) {
  return request({
    url: '/maindata/relation/remove',
    method: 'post',
    data: query
  })
}
export function matchMouldConfiguration(query) {
  return request({
    url: '/maindata/relation/matchMouldConfiguration',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function exportData(query) {
  downloadLink('/maindata/relation/export', query)
}
export function mesCapture(query) {
  return request({
    url: '/maindata/relation/mesCapture',
    method: 'post',
    data: query
  })
}

export function updateMaterial(query) {
  return request({
    url: '/maindata/relation/updateMainPatternToMaterial',
    method: 'post',
    data: query
  })
}
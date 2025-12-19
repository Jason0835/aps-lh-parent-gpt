import request from '@/utils/request'

// =
export function listBigRollRubberCarRelation(query) {
  return request({
    url: 'xwyy/bigRollRubberCarRelation/list',
    method: 'post',
    data: query
  })
}
export function editBigRollRubberCarRelation(query) {
  return request({
    url: 'xwyy/bigRollRubberCarRelation/edit',
    method: 'post',
    data: query
  })
}
export function removeBigRollRubberCarRelation(query) {
  return request({
    url: 'xwyy/bigRollRubberCarRelation/remove',
    method: 'post',
    data: query
  })
}



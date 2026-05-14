import request, { downloadLink } from '@/utils/request'

export function listSharedMouldPat(query) {
  return request({
    url: '/lh/lhSharedMouldPat/list',
    method: 'post',
    data: query
  })
}

export function getSharedMouldPat(id) {
  return request({
    url: '/lh/lhSharedMouldPat/' + id,
    method: 'get'
  })
}

export function editSharedMouldPat(data) {
  return request({
    url: '/lh/lhSharedMouldPat/save',
    method: 'post',
    data: data
  })
}

export function removeSharedMouldPat(ids) {
  let idList = ids;
  if (typeof ids === 'string') {
    idList = ids.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
  }
  return request({
    url: '/lh/lhSharedMouldPat/remove',
    method: 'post',
    data: idList
  })
}

export function exportSharedMouldPat(query) {
  return downloadLink('/lh/lhSharedMouldPat/export', query)
}

export function checkUniqueSharedMouldPat(data) {
  return request({
    url: '/lh/lhSharedMouldPat/checkUnique',
    method: 'post',
    data: data
  })
}

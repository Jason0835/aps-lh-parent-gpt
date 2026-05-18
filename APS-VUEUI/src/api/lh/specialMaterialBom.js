import request, { downloadLink } from '@/utils/request'

export function listSpecialMaterialBom(query) {
  return request({
    url: '/lh/lhSpecialMaterialBom/list',
    method: 'post',
    data: query
  })
}

export function getSpecialMaterialBom(id) {
  return request({
    url: '/lh/lhSpecialMaterialBom/' + id,
    method: 'get'
  })
}

export function editSpecialMaterialBom(data) {
  return request({
    url: '/lh/lhSpecialMaterialBom/save',
    method: 'post',
    data: data
  })
}

export function removeSpecialMaterialBom(ids) {
  let idList = ids;
  if (typeof ids === 'string') {
    idList = ids.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
  }
  return request({
    url: '/lh/lhSpecialMaterialBom/remove',
    method: 'post',
    data: idList
  })
}

export function exportSpecialMaterialBom(query) {
  return downloadLink('/lh/lhSpecialMaterialBom/export', query)
}

import request, {downloadLink} from '@/utils/request'


export function listFactoryGlueAreaRelation(query) {
  return request({
    url: '/setting/factoryGlueAreaRelation/list',
    method: 'post',
    data: query
  })
}
export function removeFactoryGlueAreaRelation(query) {
  return request({
    url: '/setting/factoryGlueAreaRelation/remove',
    method: 'post',
    data: query
  })
}
export function saveFactoryGlueAreaRelation(query) {
  return request({
    url: '/setting/factoryGlueAreaRelation/save',
    method: 'post',
    data: query
  })
}

export function exportData(params) {
  return downloadLink("/setting/factoryGlueAreaRelation/export", params);
}

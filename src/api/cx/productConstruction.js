import request from '@/utils/request'

export function listProductConstruction(query) {
  return request({
    url: 'cx/productConstruction/list',
    method: 'post',
    data: query
  })
}

export function getEmbryoVersions(query) {
  return request({
    url: 'cx/productConstruction/getEmbryoVersions',
    method: 'post',
    data: query
  })
}

export function getVersionsByEmbryoCode(query) {
  return request({
    url: 'cx/productConstruction/getVersionsByEmbryoCode',
    method: 'post',
    data: query
  })
}

export function editProductConstruction(query) {
  return request({
    url: 'cx/productConstruction/edit',
    method: 'post',
    data: query
  })
}
export function edit1ProductConstruction(query) {
  return request({
    url: 'cx/productConstruction/edit1',
    method: 'post',
    data: query
  })
}
export function getVersions(query) {
  return request({
    url: 'cx/productConstruction/edit1/info',
    method: 'post',
    data: query
  })
}
export function updateProductionStage(query) {
  return request({
    url: 'cx/productConstruction/updateProductionStage',
    method: 'post',
    data: query
  })
}






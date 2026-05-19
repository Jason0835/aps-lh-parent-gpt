import request, { downloadLink } from '@/utils/request'

export function listProductinfo(query) {
  return request.send('/lean/productinfo/list', query)
}
export function editProductinfo(query) {
  return request.send('/lean/productinfo/edit', query)
}
export function removeProductinfo(query) {
  return request.send('/lean/productinfo/remove', query)
}
export function checkMouldUseStatusUnique(query) {
  return request.send('/lean/productinfo/checkMouldUseStatusUnique', query)
}

export function exportData(query) {
  downloadLink('/lean/productinfo/export', query)
}

export function updateQualityState(query) {
  return request.send('/lean/productinfo/updateQualityStateCodeName', query)
}

export function listEmbryoCode(query) {
  return request.send('/lean/productinfo/listEmbryoCode', query)
}

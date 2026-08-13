import request, { downloadLink } from '@/utils/request'

export function listAdjustPlanRequireInfo(query) {
  return request({
    url: '/monthplan/adjustPlanRequireInfo/list',
    method: 'post',
    data: query
  })
}

export function getAdjustPlanRequireInfo(id) {
  return request({
    url: `/monthplan/adjustPlanRequireInfo/getInfo/${id}`,
    method: 'get'
  })
}

export function addAdjustPlanRequireInfo(data) {
  return request({
    url: '/monthplan/adjustPlanRequireInfo/add',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateAdjustPlanRequireInfo(data) {
  return request({
    url: '/monthplan/adjustPlanRequireInfo/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delAdjustPlanRequireInfo(data) {
  return request({
    url: '/monthplan/adjustPlanRequireInfo/remove',
    method: 'post',
    data
  })
}

export function exportAdjustPlanRequireInfo(query) {
  return downloadLink('/monthplan/adjustPlanRequireInfo/export', query)
}

/** 产品结构下拉数据（本功能 UI 层接口，来源 mdmSkuStructureRef 去重） */
export function listAdjustPlanStructureOptions(query) {
  return request({
    url: '/monthplan/adjustPlanRequireInfo/structureOptions',
    method: 'get',
    params: query
  })
}

/** 物料编码下拉数据（本功能 UI 层接口，含物料描述反显） */
export function listAdjustPlanMaterialOptions(query) {
  return request({
    url: '/monthplan/adjustPlanRequireInfo/materialOptions',
    method: 'get',
    params: query
  })
}

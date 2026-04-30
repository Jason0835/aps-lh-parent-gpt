import request, { downloadLink } from '@/utils/request'

/**
 * 根据条件查询硫化工序模具变动单APS列表
 * @param {*} query
 * @returns
 */
export function listLhMoldChangePlan(query) {
  return request({
    url: '/lh/lhMoldChangePlan/list',
    method: 'post',
    data: query
  })
}


export function editLhMoldChangePlan(query) {
  return request({
    url: '/lh/lhMoldChangePlan/save',
    method: 'post',
    data: query
  })
}

/**
 * 修改或新增硫化工序模具变动单APS
 * @param {*} query
 * @returns
 */
export function removeLhMoldChangePlan(query) {
  return request({
    url: '/lh/lhMoldChangePlan/remove',
    method: 'post',
    data: query
  })
}


/**
 * 生成换模计划
 * @param {*} query 
 * @returns 
 */
export function generateMoldReplacementPlan(query) {
  return request({
    url: '/lh/lhMoldChangePlan/generateMoldReplacementPlan',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

export function exportLhMoldChangePlan(params) {
  downloadLink('/lh/lhMoldChangePlan/export', params)
}

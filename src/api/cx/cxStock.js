import request from '@/utils/request'
import { saveAs } from 'file-saver'

// 查询成型库存列表
export function listCxStock(query) {
  return request({
    url: '/cx/cxStock/list',
    method: 'post',
    data: query
  })
}

// 新增/修改成型库存
export function saveCxStock(data) {
  return request({
    url: '/cx/cxStock/save',
    method: 'post',
    data: data
  })
}

// 删除成型库存
export function removeCxStock(ids) {
  return request({
    url: '/cx/cxStock/remove',
    method: 'post',
    params: { ids: ids }
  })
}

// 获取成型库存详情
export function getCxStock(id) {
  return request({
    url: `/cx/cxStock/${id}`,
    method: 'get'
  })
}

// 校验唯一性
export function checkCxStockUnique(data) {
  return request({
    url: '/cx/cxStock/checkUnique',
    method: 'post',
    data: data
  })
}

// 导出成型库存
export function exportCxStock(query, fileName) {
  return request({
    url: `/cx/cxStock/exportData/${encodeURIComponent(fileName)}`,
    method: 'post',
    data: query,
    responseType: 'blob'
  }).then(data => {
    const blob = new Blob([data])
    saveAs(blob, fileName + '.xlsx')
  })
}

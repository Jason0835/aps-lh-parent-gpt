import request from '@/utils/request'

// 不良数
export function listLhUnscheduledResult(query) {
  return request({
    url: '/lh/lhUnscheduledResult/list',
    method: 'post',
    data: query
  })
}

/**
 * 查询硫化未排结果
 * @param {*} query
 * @returns
 */
export function listCuringUnscheduleResult(query) {
  return request({
    url: '/lh/lhUnscheduledResult/listResult',
    method: 'post',
    data: query
  });
}



import request, {downloadLink} from '@/utils/request'

/**
 * 查询胎侧调度员操作日志列表
 * @param {Object} query
 * @returns
 */
export function listDispatcherLog(query) {
  return request({
    url: '/tc/tcDispatcherLog/list',
    method: 'post',
    data: query
  })
}

/**
 * 导出胎侧调度员操作日志
 * @param {Object} query
 */
export function exportDispatcherLog(query) {
  return downloadLink("/tc/tcDispatcherLog/export", query);
}

import request, {downloadLink} from '@/utils/request'

export function listDeviceMaintenancePlan(type, query) {
  return request({
    url: '/fac/docDeviceMaintenancePlan/' + type + '/list',
    method: 'post',
    data: query
  })
}

export function editDeviceMaintenancePlan(type, query) {
  return request({
    url: '/fac/docDeviceMaintenancePlan/' + type + '/edit',
    method: 'post',
    data: query
  })
}

export function removeDeviceMaintenancePlan(type, query) {
  return request({
    url: '/fac/docDeviceMaintenancePlan/' + type + '/remove',
    method: 'post',
    data: query
  })
}

export function exportData(type, query) {
  downloadLink('/fac/docDeviceMaintenancePlan/' + type + '/export', query)
}

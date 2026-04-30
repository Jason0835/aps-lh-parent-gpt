import request from '@/utils/request'

export function halfYcImportBakImportData(query) {
  return request({
    url: 'tm/halfYcImportBak/importData',
    method: 'post',
    data: query
  })
}

export function halfYcImportBakImportExcelToListAndExport(query) {
  return request({
    url: 'tm/halfYcImportBak/importExcelToListAndExport',
    method: 'post',
    data: query
  })
}


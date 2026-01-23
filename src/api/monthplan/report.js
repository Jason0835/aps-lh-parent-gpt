import request, { downloadLink } from '@/utils/request'

// =
export function listMonthFinishRate(query) {
  return request({
    url: '/monthPlan/report/listMonthFinishRate',
    method: 'post',
    data: query
  })
}
export function exportMonthFinishRate(query) {
  downloadLink('/monthPlan/report/exportMonthFinishRate', query)
}


export function listMonthFinishRateBrand(query) {
  return request({
    url: '/monthPlan/report/listMonthFinishRateBrand',
    method: 'post',
    data: query
  })
}
export function exportMonthFinishRateBrand(query) {
  downloadLink('/monthPlan/report/exportMonthFinishRateBrand', query)
}


export function listMonthFinishRateBrandProSize(query) {
  return request({
    url: '/monthPlan/report/listMonthFinishRateBrandProSize',
    method: 'post',
    data: query
  })
}
export function exportMonthFinishRateBrandProSize(query) {
  downloadLink('/monthPlan/report/exportMonthFinishRateBrandProSize', query)
}

// --------------- SKU汇总 ---------------- //
export function listSkuSummary(query) {
  return request({
    url: '/monthPlan/report/listSkuSummary',
    method: 'post',
    data: query
  })
}
export function exportSkuSummary(query) {
  downloadLink('/monthPlan/report/exportSkuSummary', query)
}

// --------------- 投产SKU汇总 ---------------- //
export function listSkuSummaryProduce(query) {
  return request({
    url: '/monthPlan/report/listSkuSummaryProduce',
    method: 'post',
    data: query
  })
}
export function exportSkuSummaryProduce(query) {
  downloadLink('/monthPlan/report/exportSkuSummaryProduce', query)
}

// --------------- 试制sku汇总分析 ---------------- //
export function listSkuSummaryTrial(query) {
  return request({
    url: '/monthPlan/report/listSkuSummaryTrial',
    method: 'post',
    data: query
  })
}
export function exportSkuSummaryTrial(query) {
  downloadLink('/monthPlan/report/exportSkuSummaryTrial', query)
}


// --------------- 品牌汇总分析 ---------------- //
export function listBrandSummary(query) {
  return request({
    url: '/monthPlan/report/listBrandSummary',
    method: 'post',
    data: query
  })
}
export function exportBrandSummary(query) {
  downloadLink('/monthPlan/report/exportBrandSummary', query)
}

// --------------- 寸口汇总分析 ---------------- //
export function listProSizeSummary(query) {
  return request({
    url: '/monthPlan/report/listProSizeSummary',
    method: 'post',
    data: query
  })
}
export function exportProSizeSummary(query) {
  downloadLink('/monthPlan/report/exportProSizeSummary', query)
}

// --------------- 生产销售计划数据 ---------------- //
export function listProduceSalePlan(query) {
  return request({
    url: '/monthPlan/report/listProduceSalePlanList',
    method: 'post',
    data: query
  })
}
export function exportProduceSalePlan(query) {
  downloadLink('/monthPlan/report/exportProduceSalePlan', query)
}

// --------------- 胎类区分及缺口汇总-排产受限影响满足率 ---------------- //
export function listTireTypeSatisfyRate(query) {
  return request({
    url: '/monthPlan/report/listTireTypeSatisfyRateList',
    method: 'post',
    data: query
  })
}
export function exportTireTypeSatisfyRate(query) {
  downloadLink('/monthPlan/report/exportTireTypeSatisfyRate', query)
}
// --------------- 胎类区分及缺口汇总-排产受限影响满足率 ---------------- //
export function listMonthTireTypeList(query) {
  return request({
    url: '/monthPlan/report/listMonthTireTypeList',
    method: 'post',
    data: query
  })
}
export function exportMonthTireType(query) {
  downloadLink('/monthPlan/report/exportMonthTireType', query)
}
// --------------- 渠道分类缺口差异 ---------------- //
export function listChannelClassification(query) {
  return request({
    url: '/monthPlan/report/listChannelClassification',
    method: 'post',
    data: query
  })
}
export function exportChannelClassification(query) {
  downloadLink('/monthPlan/report/exportChannelClassification', query)
}

// --------------- 品牌分类缺口差异 ---------------- //
export function listBrandClassification(query) {
  return request({
    url: '/monthPlan/report/listBrandClassification',
    method: 'post',
    data: query
  })
}
export function exportBrandClassification(query) {
  downloadLink('/monthPlan/report/exportBrandClassification', query)
}
// --------------- 品牌库位分类缺口差异 ---------------- //
export function listBrandLocationClassification(query) {
  return request({
    url: '/monthPlan/report/listBrandLocationClassification',
    method: 'post',
    data: query
  })
}
export function exportBrandLocationClassification(query) {
  downloadLink('/monthPlan/report/exportBrandLocationClassification', query)
}
// --------------- 查询寸别分类缺口差异 ---------------- //
export function listProSizeClassification(query) {
  return request({
    url: '/monthPlan/report/listProSizeClassification',
    method: 'post',
    data: query
  })
}
export function exportProSizeClassification(query) {
  downloadLink('/monthPlan/report/exportProSizeClassification', query)
}
// --------------- 查询寸别渠道分类缺口差异 ---------------- //
export function listProSizeChannelClassification(query) {
  return request({
    url: '/monthPlan/report/listProSizeChannelClassification',
    method: 'post',
    data: query
  })
}
export function exportProSizeChannelClassification(query) {
  downloadLink('/monthPlan/report/exportProSizeChannelClassification', query)
}



export function listProduceVersionList(query) {
  return request({
    url: '/monthPlan/report/listProduceVersionList',
    method: 'post',
    data: query
  })
}

export function homePage4Machine(query) {
  return request({
    url: '/monthPlan/report/homePage4Machine',
    method: 'post',
    data: query
  })
}
export function homePage4ProductionProcesses(query) {
  return request({
    url: '/monthPlan/report/homePage4ProductionProcesses',
    method: 'post',
    data: query
  })
}
export function homePage4Plan(query) {
  return request({
    url: '/monthPlan/report/homePage4Plan',
    method: 'post',
    data: query
  })
}
export function homePage4Order(query) {
  return request({
    url: '/monthPlan/report/homePage4Order',
    method: 'post',
    data: query
  })
}
export function selectSkuSummary4BigScreen(query) {
  return request({
    url: '/monthPlan/report/selectSkuSummary4BigScreen',
    method: 'post',
    data: query
  })
}
export function selectMachineGantt(query) {
  return request({
    url: '/lh/lhScheduleResult/selectMachineGantt',
    method: 'post',
    data: query
  })
}
export function selectProductionProcessesByDate7(query) {
  return request({
    url: '/monthPlan/report/selectProductionProcessesByDate7',
    method: 'post',
    data: query
  })
}


export function selectSystemRunReport(query) {
  return request({
    url: '/monthPlan/report/selectSystemRunReport',
    method: 'post',
    data: query
  })
}

//库存库龄分析
export function inventoryAgeAnalysisReport(query) {
  return request({
    url: '/report/inventoryAgeAnalysis',
    method: 'post',
    data: query
  })
}
export function exportSelectSystemRunReport(query) {
  downloadLink('/monthPlan/report/exportSystemRunReport', query)
}

//单胎总重报表
export function totalWeightReport(query) {
  return request({
    url: '/report/singleTireTotalWeight',
    method: 'post',
    data: query
  })
}
//成型机数据报表
export function moldingMachineReport(query) {
  return request({
    url: '/report/factoryMoldingMachine',
    method: 'post',
    data: query
  })
}
//硫化机数据报表
export function vulcanizingMachineReport(query) {
  return request({
    url: '/report/factoryVulcanizingMachine',
    method: 'post',
    data: query
  })
}
//结构在机数据报表
export function productionStructureReport(query) {
  return request({
    url: '/report/productionStructure',
    method: 'post',
    data: query
  })
}


//报表统一处理接口
export function reportUrt(query) {
  return request({
    url: '/report/reportView',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}

//订单冲减分配
export function listOrderOffsetDetail(query) {
  return request({
    url: '/maindata/dpOrderOffsetDetail/list',
    method: 'post',
    data: query
  })
}

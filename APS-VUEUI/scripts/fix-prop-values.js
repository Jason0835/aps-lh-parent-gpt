/**
 * 修复被错误替换的 prop 值
 * prop 应该是中文字段名，label 才使用翻译 key
 */

const fs = require('fs');
const path = require('path');

// 定义反向映射 - 将错误的 key 映射回中文
const reverseMapping = {
  '"newPage.common.factory"': '"工厂"',
  '"newPage.common.year"': '"年份"',
  '"newPage.common.month"': '"月份"',
  '"newPage.common.yearMonth"': '"年月"',
  '"newPage.common.date"': '"日期"',
  '"newPage.common.startDate"': '"开始日期"',
  '"newPage.common.endDate"': '"结束日期"',
  '"newPage.common.startTime"': '"开始时间"',
  '"newPage.common.endTime"': '"结束时间"',
  '"newPage.common.code"': '"编码"',
  '"newPage.common.name"': '"名称"',
  '"newPage.common.quantity"': '"数量"',
  '"newPage.common.unit"': '"单位"',
  '"newPage.common.version"': '"版本号"',
  '"newPage.common.createTime"': '"创建时间"',
  '"newPage.common.updateTime"': '"更新时间"',
  '"newPage.common.operation"': '"操作"',
  '"newPage.common.edit"': '"编辑"',
  '"newPage.common.add"': '"新增"',
  '"newPage.common.delete"': '"删除"',
  '"newPage.common.search"': '"搜索"',
  '"newPage.common.reset"': '"重置"',
  '"newPage.common.save"': '"保存"',
  '"newPage.common.cancel"': '"取消"',
  '"newPage.common.confirm"': '"确定"',
  '"newPage.common.import"': '"导入"',
  '"newPage.common.export"': '"导出"',
  '"newPage.common.template"': '"模板"',
  '"newPage.common.batchDelete"': '"批量删除"',
  '"newPage.common.remark"': '"备注"',
  '"newPage.common.status"': '"状态"',
  '"newPage.common.type"': '"类型"',

  '"newPage.machine.machineCode"': '"机台编号"',
  '"newPage.machine.machineName"': '"机台名称"',
  '"newPage.machine.machineType"': '"机台类型"',
  '"newPage.machine.structure"': '"结构"',
  '"newPage.machine.moldingDate"': '"成型日期"',
  '"newPage.machine.curingDate"': '"硫化日期"',
  '"newPage.machine.curingMachine"': '"硫化机台"',
  '"newPage.machine.moldingMachine"': '"成型机台"',
  '"newPage.machine.scheduleMachine"': '"排产机台"',
  '"newPage.machine.productionMachine"': '"排产机台"',
  '"newPage.machine.abnormalMachine"': '"异常机台"',

  '"newPage.material.materialCode"': '"物料编码"',
  '"newPage.material.materialName"': '"物料名称"',
  '"newPage.material.materialDesc"': '"物料描述"',
  '"newPage.material.materialGroup"': '"物料组"',
  '"newPage.material.materialVersion"': '"物料版本"',
  '"newPage.material.materialPriority"': '"物料优先"',
  '"newPage.material.rawMaterialCode"': '"原材料物料编号"',
  '"newPage.material.rawMaterialDesc"': '"原材料物料描述"',
  '"newPage.material.rawMaterialVersion"': '"原材料物料版本"',
  '"newPage.material.specDesc"': '"规格"',
  '"newPage.material.specification"': '"规格"',
  '"newPage.material.usage"': '"用量"',
  '"newPage.material.glueName"': '"胶料名称"',
  '"newPage.material.drugName"': '"药品名称"',
  '"newPage.material.productStructure"': '"产品结构"',
  '"newPage.material.structurePriority"': '"结构优先级"',
  '"newPage.material.materialStructure"': '"产品结构"',
  '"newPage.material.previousSpecDesc"': '"前规格物料描述"',
  '"newPage.material.previousSpecCode"': '"前规格物料编码"',
  '"newPage.material.afterSpecDesc"': '"后规格物料描述"',
  '"newPage.material.afterSpecCode"': '"后规格物料编码"',

  '"newPage.mold.moldCode"': '"模具编号"',
  '"newPage.mold.moldName"': '"模具名称"',
  '"newPage.mold.moldType"': '"模具类型"',
  '"newPage.mold.moldQuantity"': '"模具数"',
  '"newPage.mold.cavityCode"': '"型腔模号"',
  '"newPage.mold.patternCode"': '"花纹代号"',
  '"newPage.mold.mainPattern"': '"主花纹"',
  '"newPage.mold.shellStandard"': '"模壳标准"',
  '"newPage.mold.availableStatus"': '"可用状态"',
  '"newPage.mold.logisticsStatus"': '"物流状态"',
  '"newPage.mold.moldPlanStartTime"': '"模具计划上机时间"',
  '"newPage.mold.moldChangeBatchNo"': '"模具变动单批次号"',

  '"newPage.plan.planTime"': '"计划时间"',
  '"newPage.plan.actualTime"': '"实际时间"',
  '"newPage.plan.planQuantity"': '"计划量"',
  '"newPage.plan.actualQuantity"': '"实际量"',
  '"newPage.plan.scheduledQuantity"': '"已排数量"',
  '"newPage.plan.unscheduledQuantity"': '"未排数量"',
  '"newPage.plan.planVersion"': '"排产版本"',
  '"newPage.plan.planVersionNo"': '"排产版本号"',
  '"newPage.plan.planType"': '"排产类型"',
  '"newPage.plan.planCategory"': '"排产分类"',
  '"newPage.plan.netDemand"': '"排产净需求"',
  '"newPage.plan.isSchedule"': '"是否排产"',
  '"newPage.plan.isPublished"': '"是否发布"',
  '"newPage.plan.isJoinSchedule"': '"是否参与排产"',
  '"newPage.plan.completionStatus"': '"完成情况"',
  '"newPage.plan.dueDate"': '"到期日"',
  '"newPage.plan.planStartTime"': '"计划开始时间"',
  '"newPage.plan.planEndTime"': '"计划结束时间"',
  '"newPage.plan.estimatedStartTime"': '"预计开始时间"',
  '"newPage.plan.estimatedEndTime"': '"预计结束时间"',
  '"newPage.plan.originalPlanQuantity"': '"原计划量"',
  '"newPage.plan.adjustedPlanQuantity"': '"调整后计划量"',
  '"newPage.plan.adjustedStartDate"': '"调整后开始日期"',
  '"newPage.plan.adjustedEndDate"': '"调整后结束日期"',
  '"newPage.plan.currentNetDemand"': '"当前净需求量"',
  '"newPage.plan.previousNetDemand"': '"调整前净需求量（上周）"',
  '"newPage.plan.netDemandChange"': '"净需求变动"',
  '"newPage.plan.monthPlanScheduledQty"': '"月计划已排产量"',
  '"newPage.plan.monthPlanProducedQty"': '"月计划已生产量"',
  '"newPage.plan.pendingAdjustment"': '"待调整量"',
  '"newPage.plan.confirmedAdjustment"': '"确认调整量"',
  '"newPage.plan.actualAdjustment"': '"实际调整"',
  '"newPage.plan.adjustmentPriority"': '"调整优先级"',
  '"newPage.plan.adjustmentReason"': '"调整原因"',
  '"newPage.plan.adjustmentType"': '"调整类型"',
  '"newPage.plan.adjustmentDetail"': '"调整明细"',
  '"newPage.plan.adjustmentVersion"': '"调整版本"',
  '"newPage.plan.adjustmentEndDate"': '"调整结束日期"',
  '"newPage.plan.lockMountingDate"': '"锁定上机日期"',
  '"newPage.plan.scheduledMountingDate"': '"计划上机日期"',
  '"newPage.plan.planSurplus"': '"计划余量"',
  '"newPage.plan.monthEndSurplus"': '"月底余量"',
  '"newPage.plan.monthPlanStartTime"': '"月计划开产时间"',
  '"newPage.plan.estimatedStartProductionTime"': '"预计开产时间"',
  '"newPage.plan.estimatedOutput"': '"预计实际产量"',
  '"newPage.plan.planDeliveryDate"': '"计划发货日期"',
  '"newPage.plan.scheduleDate"': '"排程日期"',
  '"newPage.plan.planMonthProduction"': '"月计划"',
  '"newPage.plan.T1MonthScheduledQty"': '"T+1月排产量"',
  '"newPage.plan.T2MonthScheduledQty"': '"T+2月排产量"',
  '"newPage.plan.TnMonthScheduledQty"': '"T+n月排产量"',

  '"newPage.shift.midClass"': '"中班"',
  '"newPage.shift.nightClass"': '"夜班"',
  '"newPage.shift.dayClass"': '"白班"',
  '"newPage.shift.midClass1422"': '"中班（14:00-22:00)"',
  '"newPage.shift.midPlanQuantity"': '"中班计划量"',
  '"newPage.shift.midCompletedQuantity"': '"中班完成量"',
  '"newPage.shift.midCompletionRate"': '"中班完成率"',
  '"newPage.shift.midProductionOrder"': '"中班生产顺序"',
  '"newPage.shift.midRemark"': '"中班备注"',
  '"newPage.shift.midFinishRate"': '"中班完成率"',
  '"newPage.shift.midFinishQty"': '"中班完成量"',
  '"newPage.shift.midPlanQty"': '"中班计划量"',
  '"newPage.shift.midProduceOrder"': '"中班生产顺序"',
  '"newPage.shift.midExpectFinishTime"': '"中班预计完成时间"',

  '"newPage.maintenance.maintenanceType"': '"维修类型"',
  '"newPage.maintenance.maintenanceDate"': '"维修日期"',
  '"newPage.maintenance.maintenanceStartTime"': '"维修开始时间"',
  '"newPage.maintenance.maintenanceEndTime"': '"维修结束时间"',
  '"newPage.maintenance.maintenanceItem"': '"维修项目"',
  '"newPage.maintenance.accuracyType"': '"精度类型"',
  '"newPage.maintenance.shutdownType"': '"停机类型"',
  '"newPage.maintenance.abnormalStartTime"': '"异常开始时间"',
  '"newPage.maintenance.abnormalReason"': '"异常原因"',
  '"newPage.maintenance.abnormalImpactQty"': '"异常影响量"',
  '"newPage.maintenance.abnormalStructure"': '"异常结构"',
  '"newPage.maintenance.estimatedRecoveryTime"': '"预计恢复时间"',
  '"newPage.maintenance.alternateType"': '"交替类型"',
  '"newPage.maintenance.alternateStartTime"': '"交替开始时间"',
  '"newPage.maintenance.alternateEndTime"': '"交替结束时间"',

  '"newPage.cleaning.cleaningType"': '"清洗类型"',
  '"newPage.cleaning.cleaningDate"': '"清洗日期"',
  '"newPage.cleaning.cleaningStartTime"': '"清洗开始时间"',
  '"newPage.cleaning.cleaningEndTime"': '"清洗结束时间"',

  '"newPage.capsule.capsuleCode"': '"胶囊编号"',
  '"newPage.capsule.capsuleName"': '"胶囊名称"',
  '"newPage.capsule.capsuleChangeCount"': '"换胶囊次数"',
  '"newPage.capsule.newChuck"': '"新卡盘"',
  '"newPage.capsule.chuckType"': '"卡盘类型"',

  '"newPage.embryo.embryoCode"': '"胎胚代码"',
  '"newPage.embryo.embryoVersion"': '"胎胚版本"',
  '"newPage.embryo.embryoNo"': '"胎胚号"',
  '"newPage.embryo.moldingCode"': '"成型编号"',
  '"newPage.embryo.updateEmbryoDesc"': '"更新胎胚描述到物料信息"',
  '"newPage.embryo.wholeTireTread"': '"整车胎面条"',
  '"newPage.embryo.wholeTireEmbryo"': '"整车胎胚（条）"',
  '"newPage.embryo.wholeTireTreadCount"': '"整车胎面（条）"',
  '"newPage.embryo.tireCount"': '"折合轮胎条数"',
  '"newPage.embryo.adjustedTireCount"': '"调整的轮胎条数"',

  '"newPage.recipe.recipeName"': '"配方名称"',
  '"newPage.recipe.recipeType"': '"配方类型"',
  '"newPage.recipe.recipeTypeName"': '"配方类型名称"',
  '"newPage.recipe.recipeVersion"': '"配方版本"',
  '"newPage.recipe.recipeStage"': '"配方阶段"',
  '"newPage.recipe.recipeNeedWashing"': '"需要洗车的配方名称"',
  '"newPage.recipe.preProductionMaterial"': '"生产前物料名称"',

  '"newPage.inventory.stock"': '"库存"',
  '"newPage.inventory.totalStock"': '"总库存"',
  '"newPage.inventory.workshopStock"': '"车间库存"',
  '"newPage.inventory.intermediateStock"': '"中间库库存"',
  '"newPage.inventory.rawMaterialStock"': '"原材料库存"',
  '"newPage.inventory.stockDate"': '"库存日期"',
  '"newPage.inventory.safetyStock"': '"安全库存"',
  '"newPage.inventory.currentStock"': '"当前库存"',
  '"newPage.inventory.WMSUnusedProductCode"': '"WMS未用产品编码"',
  '"newPage.inventory.boxQuantity"': '"显示箱数"',

  '"newPage.schedule.autoSchedule"': '"自动排产"',
  '"newPage.schedule.autoAdjust"': '"自动调整"',
  '"newPage.schedule.singleAdjust"': '"单选结构调整"',
  '"newPage.schedule.addStructure"': '"新增结构"',
  '"newPage.schedule.checkOrderSimulate"': '"查单模拟排产"',
  '"newPage.schedule.viewStructureSchedule"': '"查看结构排产"',
  '"newPage.schedule.generateCurrentCycle"': '"生成当前周期排产"',
  '"newPage.schedule.getAdjustOrders"': '"获取调整订单"',
  '"newPage.schedule.cyclicSchedule"': '"周期排产"',
  '"newPage.schedule.scheduleClass"': '"班别"',
  '"newPage.schedule.cyclicReserve"': '"周期排产储备"',
  '"newPage.schedule.conventionalReserve"': '"常规储备"',
  '"newPage.schedule.netDemandWithoutPostpone"': '"净需求(不含暂缓)"',
  '"newPage.schedule.netDemandWithPostpone"': '"净需求(含暂缓)"',
  '"newPage.schedule.minProductionQty"': '"最小投产量值"',
  '"newPage.schedule.isMinProductionQtyMet"': '"是否满足最小投产量"',
  '"newPage.schedule.isSpecialMaterial"': '"是否含特殊材料"',
  '"newPage.schedule.specialMaterial"': '"特殊材料"',
  '"newPage.schedule.specialMaterialStockAndPlan"': '"特殊材料今日库存+今日计划"',
  '"newPage.schedule.specialMaterialProduction"': '"特殊材料生产情况"',
  '"newPage.schedule.highPriorityDemand"': '"高优先级需求(含损耗)"',
  '"newPage.schedule.delayedOrder"': '"暂缓订单"',
  '"newPage.schedule.schedulePriority"': '"供应链优先级"',
  '"newPage.schedule.forecastScheduleVersion"': '"预测排产计划版"',
  '"newPage.schedule.unscheduleReason"': '"未排原因"',

  '"newPage.weighing.weighingStation"': '"称重工位"',
  '"newPage.weighing.weighingStationShort"': '"称重工位.short"',
  '"newPage.weighing.drugPackageCount"': '"药品包数"',

  '"newPage.product.productCategory"': '"产品分类"',
  '"newPage.product.productType"': '"产品品类"',
  '"newPage.product.productStatus"': '"产品状态"',
  '"newPage.product.brand"': '"品牌"',
  '"newPage.product.inch"': '"英寸"',

  '"newPage.priority.high"': '"高优先级"',
  '"newPage.priority.medium"': '"中优先级"',
  '"newPage.priority.low"': '"低优先级"',

  '"newPage.adjust.followUpShift"': '"后续平移"',
  '"newPage.adjust.temporaryMeasure"': '"临时调整措施"',
  '"newPage.adjust.changeMachine"': '"转机台"',
  '"newPage.adjust.publish"': '"发布"',
  '"newPage.adjust.sendCrossRegional"': '"发送跨区域"',
  '"newPage.adjust.receiveCrossRegional"': '"接收跨区域"',
  '"newPage.adjust.statistics"': '"统计"',
  '"newPage.adjust.supplement"': '"补单"',

  '"newPage.warehouse.warehouseNo"': '"库排号"',
  '"newPage.warehouse.locationType"': '"库位类型"',

  '"newPage.batch.batchNo"': '"批次号"',
  '"newPage.batch.curingResultBatchNo"': '"硫化结果批次号"',
  '"newPage.batch.yearWeekNo"': '"年周号"',
  '"newPage.batch.DOT"': '"DOT"',
  '"newPage.batch.Y0DOT"': '"Y-0+(DOT)"',
  '"newPage.batch.Y1DOT"': '"Y-1+(DOT)"',
  '"newPage.batch.Y2DOT"': '"Y-2+(DOT)"',
  '"newPage.batch.seqNo"': '"序号"',
  '"newPage.batch.sortNo"': '"顺位"',

  '"newPage.production.balance"': '"动平衡"',
  '"newPage.production.uniformity"': '"均匀性"',

  '"newPage.confirm.pushSCMMES"': '"确定推送调整后的月计划到SCM/MES？"',
  '"newPage.confirm.joinSchedule"': '"确定参与排产"',
  '"newPage.confirm.cancelJoinSchedule"': '"确定取消参与排产"',
  '"newPage.confirm.generateOverdueSKU"': '"生成超期SKU"',

  '"newPage.message.loading"': '"正在加载中，请稍候"',
  '"newPage.message.autoAdjusting"': '"正在自动调整中，请稍候"',
  '"newPage.message.autoAdjustingPleaseWait"': '"正在自动调整，请稍候"',
  '"newPage.message.gettingOrders"': '"正在获取订单中"',
  '"newPage.message.gettingAdjustOrders"': '"正在获取调整订单，请稍候"',
  '"newPage.message.inputPositiveInteger"': '"请输入大于0的正整数"',
  '"newPage.message.finishTimeWithin4Days"': '"收尾时间（4天以内）"',

  '"newPage.other.total"': '"总合计"',
  '"newPage.other.from"': '"从"',
  '"newPage.other.to"': '"到"',
  '"newPage.other.post"': '" post"',
  '"newPage.other.carCount"': '"车数"',
  '"newPage.other.progress"': '"进度"',
  '"newPage.other.componentName"': '"部件名称"',
  '"newPage.other.departmentName"': '"部门名称"',
  '"newPage.other.relatedUser"': '"关联用户"',
  '"newPage.other.fixedStructure"': '"固定结构"',
  '"newPage.other.fixedStructure1"': '"固定结构1"',
  '"newPage.other.fixedStructure2"': '"固定结构2"',
  '"newPage.other.fixedStructure3"': '"固定结构3"',
  '"newPage.other.unworkableStructure"': '"不可作业结构"',
  '"newPage.other.VNFactoryStructure"': '"越南工厂结构"',
  '"newPage.other.JYVietnam"': '"金宇越南"',
  '"newPage.other.capacityPerShift1Machine"': '"一台的班产（3个班）"',
  '"newPage.other.capacityPerShift2Machine"': '"二台的班产（3个班）"',
  '"newPage.other.capacityPerShift3Machine"': '"三台的班产（3个班）"',
  '"newPage.other.capacityPerShift4Machine"': '"四台的班产（3个班）"',
  '"newPage.other.dailyStandardOutput"': '"日标准产量"',
  '"newPage.changeType"': '"变更类型"',
  '"newPage.standardLength"': '"标准长"',
  '"newPage.MESGrab"': '"MES抓取"',
  '"newPage.SCMGrab"': '"SCM抓取"',
  '"newPage.MESDailyCuringQty"': '"MES日硫化量"',
  '"newPage.NCMaterialCode"': '"NC物料编码"',
  '"newPage.EUDR"': '"EUDR"',
  '"newPage.drumManufacturer"': '"成型鼓厂家"',
  '"newPage.warningData"': '"预警数据"',
};

// 递归获取所有 Vue 文件
function getVueFiles(dir, files = []) {
  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      getVueFiles(fullPath, files);
    } else if (item.endsWith('.vue')) {
      files.push(fullPath);
    }
  }

  return files;
}

// 修复文件中的 prop 值
function fixFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  let hasChanges = false;
  let changeCount = 0;

  // 按长度降序排序
  const sortedKeys = Object.keys(reverseMapping).sort((a, b) => b.length - a.length);

  // 只替换 prop: "xxx" 模式中的 key
  for (const englishKey of sortedKeys) {
    const chineseKey = reverseMapping[englishKey];
    // 匹配 prop: "englishKey" 或 prop:"englishKey"
    const regex = new RegExp(`(prop\\s*:\\s*)${englishKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`, 'g');

    const matches = content.match(regex);
    if (matches) {
      content = content.replace(regex, `$1${chineseKey}`);
      hasChanges = true;
      changeCount += matches.length;
    }
  }

  if (hasChanges) {
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`✓ ${filePath} (${changeCount} 处修复)`);
    return true;
  }

  return false;
}

// 主函数
function main() {
  const targetDir = path.resolve(__dirname, '../src/views/newPage');

  console.log('开始修复 Vue 文件中的 prop 值...\n');

  const vueFiles = getVueFiles(targetDir);
  console.log(`找到 ${vueFiles.length} 个 Vue 文件\n`);

  let processedCount = 0;
  let modifiedCount = 0;

  for (const file of vueFiles) {
    processedCount++;
    const modified = fixFile(file);
    if (modified) {
      modifiedCount++;
    }
  }

  console.log(`\n=================================`);
  console.log(`处理完成: ${processedCount} 个文件`);
  console.log(`修复文件: ${modifiedCount} 个`);
  console.log(`=================================`);
}

main();

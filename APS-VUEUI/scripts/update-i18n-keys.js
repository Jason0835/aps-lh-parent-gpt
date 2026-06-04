/**
 * 批量替换 newPage 目录下的 Vue 文件中的中文 i18n key
 * 将中文 key 替换为规范的英文 key
 */

const fs = require('fs');
const path = require('path');

// 定义中文到英文 key 的映射
const keyMapping = {
  // 通用
  '"备注"': '"newPage.common.remark"',
  '"状态"': '"newPage.common.status"',
  '"类型"': '"newPage.common.type"',
  '"工厂"': '"newPage.common.factory"',
  '"区域"': '"newPage.common.area"',
  '"年份"': '"newPage.common.year"',
  '"月份"': '"newPage.common.month"',
  '"年月"': '"newPage.common.yearMonth"',
  '"日期"': '"newPage.common.date"',
  '"开始日期"': '"newPage.common.startDate"',
  '"结束日期"': '"newPage.common.endDate"',
  '"开始时间"': '"newPage.common.startTime"',
  '"结束时间"': '"newPage.common.endTime"',
  '"编码"': '"newPage.common.code"',
  '"名称"': '"newPage.common.name"',
  '"数量"': '"newPage.common.quantity"',
  '"单位"': '"newPage.common.unit"',
  '"版本号"': '"newPage.common.version"',
  '"创建时间"': '"newPage.common.createTime"',
  '"更新时间"': '"newPage.common.updateTime"',
  '"操作"': '"newPage.common.operation"',
  '"编辑"': '"newPage.common.edit"',
  '"新增"': '"newPage.common.add"',
  '"删除"': '"newPage.common.delete"',
  '"搜索"': '"newPage.common.search"',
  '"重置"': '"newPage.common.reset"',
  '"保存"': '"newPage.common.save"',
  '"取消"': '"newPage.common.cancel"',
  '"确定"': '"newPage.common.confirm"',
  '"导入"': '"newPage.common.import"',
  '"导出"': '"newPage.common.export"',
  '"模板"': '"newPage.common.template"',
  '"批量删除"': '"newPage.common.batchDelete"',
  '"是"': '"newPage.common.yes"',
  '"否"': '"newPage.common.no"',

  // 机台
  '"机台"': '"newPage.machine.machineCode"',
  '"机台名称"': '"newPage.machine.machineName"',
  '"机台类型"': '"newPage.machine.machineType"',
  '"结构"': '"newPage.machine.structure"',
  '"成型日期"': '"newPage.machine.moldingDate"',
  '"硫化日期"': '"newPage.machine.curingDate"',
  '"硫化机台"': '"newPage.machine.curingMachine"',
  '"成型机台"': '"newPage.machine.moldingMachine"',
  '"排产机台"': '"newPage.machine.scheduleMachine"',
  '"机台编号"': '"newPage.machine.machineCode"',
  '"排产分类"': '"newPage.plan.planCategory"',

  // 物料
  '"物料编码"': '"newPage.material.materialCode"',
  '"物料名称"': '"newPage.material.materialName"',
  '"物料描述"': '"newPage.material.materialDesc"',
  '"物料组"': '"newPage.material.materialGroup"',
  '"物料版本"': '"newPage.material.materialVersion"',
  '"物料优先"': '"newPage.material.materialPriority"',
  '"规格"': '"newPage.material.specDesc"',
  '"胶料名称"': '"newPage.material.glueName"',
  '"药品名称"': '"newPage.material.drugName"',
  '"产品结构"': '"newPage.material.productStructure"',
  '"前规格物料描述"': '"newPage.material.previousSpecDesc"',
  '"前规格物料编码"': '"newPage.material.previousSpecCode"',
  '"后规格物料描述"': '"newPage.material.afterSpecDesc"',
  '"后规格物料编码"': '"newPage.material.afterSpecCode"',
  '"用量"': '"newPage.material.usage"',
  '"原材料物料编号"': '"newPage.material.rawMaterialCode"',
  '"原材料物料描述"': '"newPage.material.rawMaterialDesc"',
  '"原材料物料版本"': '"newPage.material.rawMaterialVersion"',

  // 模具
  '"模具编号"': '"newPage.mold.moldCode"',
  '"模具名称"': '"newPage.mold.moldName"',
  '"模具类型"': '"newPage.mold.moldType"',
  '"模具数"': '"newPage.mold.moldQuantity"',
  '"型腔模号"': '"newPage.mold.cavityCode"',
  '"花纹代号"': '"newPage.mold.patternCode"',
  '"主花纹"': '"newPage.mold.mainPattern"',
  '"模壳标准"': '"newPage.mold.shellStandard"',
  '"可用状态"': '"newPage.mold.availableStatus"',
  '"物流状态"': '"newPage.mold.logisticsStatus"',
  '"模具计划上机时间"': '"newPage.mold.moldPlanStartTime"',

  // 计划
  '"计划时间"': '"newPage.plan.planTime"',
  '"实际时间"': '"newPage.plan.actualTime"',
  '"计划量"': '"newPage.plan.planQuantity"',
  '"实际量"': '"newPage.plan.actualQuantity"',
  '"已排数量"': '"newPage.plan.scheduledQuantity"',
  '"未排数量"': '"newPage.plan.unscheduledQuantity"',
  '"排产版本"': '"newPage.plan.planVersion"',
  '"排产版本号"': '"newPage.plan.planVersionNo"',
  '"排产类型"': '"newPage.plan.planType"',
  '"排产净需求"': '"newPage.plan.netDemand"',
  '"是否排产"': '"newPage.plan.isSchedule"',
  '"是否发布"': '"newPage.plan.isPublished"',
  '"是否参与排产"': '"newPage.plan.isJoinSchedule"',
  '"完成情况"': '"newPage.plan.completionStatus"',
  '"到期日"': '"newPage.plan.dueDate"',
  '"计划开始时间"': '"newPage.plan.planStartTime"',
  '"计划结束时间"': '"newPage.plan.planEndTime"',
  '"预计开始时间"': '"newPage.plan.estimatedStartTime"',
  '"预计结束时间"': '"newPage.plan.estimatedEndTime"',
  '"原计划量"': '"newPage.plan.originalPlanQuantity"',
  '"调整后计划量"': '"newPage.plan.adjustedPlanQuantity"',
  '"调整后开始日期"': '"newPage.plan.adjustedStartDate"',
  '"调整后结束日期"': '"newPage.plan.adjustedEndDate"',
  '"当前净需求量"': '"newPage.plan.currentNetDemand"',
  '"调整前净需求量（上周）"': '"newPage.plan.previousNetDemand"',
  '"净需求变动"': '"newPage.plan.netDemandChange"',
  '"月计划已排产量"': '"newPage.plan.monthPlanScheduledQty"',
  '"月计划已生产量"': '"newPage.plan.monthPlanProducedQty"',
  '"待调整量"': '"newPage.plan.pendingAdjustment"',
  '"确认调整量"': '"newPage.plan.confirmedAdjustment"',
  '"实际调整"': '"newPage.plan.actualAdjustment"',
  '"调整优先级"': '"newPage.plan.adjustmentPriority"',
  '"调整原因"': '"newPage.plan.adjustmentReason"',
  '"调整类型"': '"newPage.plan.adjustmentType"',
  '"调整明细"': '"newPage.plan.adjustmentDetail"',
  '"调整版本"': '"newPage.plan.adjustmentVersion"',
  '"调整结束日期"': '"newPage.plan.adjustmentEndDate"',
  '"锁定上机日期"': '"newPage.plan.lockMountingDate"',
  '"计划上机日期"': '"newPage.plan.scheduledMountingDate"',
  '"计划余量"': '"newPage.plan.planSurplus"',
  '"月底余量"': '"newPage.plan.monthEndSurplus"',
  '"月计划开产时间"': '"newPage.plan.monthPlanStartTime"',
  '"预计开产时间"': '"newPage.plan.estimatedStartProductionTime"',
  '"预计实际产量"': '"newPage.plan.estimatedOutput"',
  '"计划发货日期"': '"newPage.plan.planDeliveryDate"',
  '"排程日期"': '"newPage.plan.scheduleDate"',
  '"月计划"': '"newPage.plan.planMonthProduction"',
  '"T+1月排产量"': '"newPage.plan.T1MonthScheduledQty"',
  '"T+2月排产量"': '"newPage.plan.T2MonthScheduledQty"',
  '"T+n月排产量"': '"newPage.plan.TnMonthScheduledQty"',

  // 班次
  '"中班"': '"newPage.shift.midClass"',
  '"夜班"': '"newPage.shift.nightClass"',
  '"白班"': '"newPage.shift.dayClass"',
  '"中班（14:00-22:00)"': '"newPage.shift.midClass1422"',
  '"中班计划量"': '"newPage.shift.midPlanQty"',
  '"中班完成量"': '"newPage.shift.midFinishQty"',
  '"中班完成率"': '"newPage.shift.midFinishRate"',
  '"中班生产顺序"': '"newPage.shift.midProduceOrder"',
  '"中班备注"': '"newPage.shift.midRemark"',
  '"中班预计完成时间"': '"newPage.shift.midExpectFinishTime"',

  // 维修
  '"维修类型"': '"newPage.maintenance.maintenanceType"',
  '"维修日期"': '"newPage.maintenance.maintenanceDate"',
  '"维修开始时间"': '"newPage.maintenance.maintenanceStartTime"',
  '"维修结束时间"': '"newPage.maintenance.maintenanceEndTime"',
  '"维修项目"': '"newPage.maintenance.maintenanceItem"',
  '"精度类型"': '"newPage.maintenance.accuracyType"',
  '"停机类型"': '"newPage.maintenance.shutdownType"',
  '"异常开始时间"': '"newPage.maintenance.abnormalStartTime"',
  '"异常原因"': '"newPage.maintenance.abnormalReason"',
  '"异常影响量"': '"newPage.maintenance.abnormalImpactQty"',
  '"异常结构"': '"newPage.maintenance.abnormalStructure"',
  '"预计恢复时间"': '"newPage.maintenance.estimatedRecoveryTime"',
  '"交替类型"': '"newPage.maintenance.alternateType"',
  '"交替开始时间"': '"newPage.maintenance.alternateStartTime"',
  '"交替结束时间"': '"newPage.maintenance.alternateEndTime"',

  // 清洗
  '"清洗类型"': '"newPage.cleaning.cleaningType"',
  '"清洗日期"': '"newPage.cleaning.cleaningDate"',
  '"清洗开始时间"': '"newPage.cleaning.cleaningStartTime"',
  '"清洗结束时间"': '"newPage.cleaning.cleaningEndTime"',

  // 胶囊
  '"胶囊编号"': '"newPage.capsule.capsuleCode"',
  '"胶囊名称"': '"newPage.capsule.capsuleName"',
  '"换胶囊次数"': '"newPage.capsule.capsuleChangeCount"',
  '"新卡盘"': '"newPage.capsule.newChuck"',
  '"卡盘类型"': '"newPage.capsule.chuckType"',

  // 胎胚
  '"胎胚代码"': '"newPage.embryo.embryoCode"',
  '"胎胚版本"': '"newPage.embryo.embryoVersion"',
  '"胎胚号"': '"newPage.embryo.embryoNo"',
  '"成型编号"': '"newPage.embryo.moldingCode"',
  '"更新胎胚描述到物料信息"': '"newPage.embryo.updateEmbryoDesc"',
  '"整车胎面条"': '"newPage.embryo.wholeTireTread"',
  '"整车胎胚（条）"': '"newPage.embryo.wholeTireEmbryo"',
  '"整车胎面（条）"': '"newPage.embryo.wholeTireTreadCount"',
  '"折合轮胎条数"': '"newPage.embryo.tireCount"',
  '"调整的轮胎条数"': '"newPage.embryo.adjustedTireCount"',

  // 配方
  '"配方名称"': '"newPage.recipe.recipeName"',
  '"配方类型"': '"newPage.recipe.recipeType"',
  '"配方类型名称"': '"newPage.recipe.recipeTypeName"',
  '"配方版本"': '"newPage.recipe.recipeVersion"',
  '"配方阶段"': '"newPage.recipe.recipeStage"',
  '"需要洗车的配方名称"': '"newPage.recipe.recipeNeedWashing"',
  '"生产前物料名称"': '"newPage.recipe.preProductionMaterial"',

  // 库存
  '"库存"': '"newPage.inventory.stock"',
  '"总库存"': '"newPage.inventory.totalStock"',
  '"车间库存"': '"newPage.inventory.workshopStock"',
  '"中间库库存"': '"newPage.inventory.intermediateStock"',
  '"原材料库存"': '"newPage.inventory.rawMaterialStock"',
  '"库存日期"': '"newPage.inventory.stockDate"',
  '"安全库存"': '"newPage.inventory.safetyStock"',
  '"当前库存"': '"newPage.inventory.currentStock"',
  '"显示箱数"': '"newPage.inventory.boxQuantity"',

  // 排产
  '"自动排产"': '"newPage.schedule.autoSchedule"',
  '"自动调整"': '"newPage.schedule.autoAdjust"',
  '"单选结构调整"': '"newPage.schedule.singleAdjust"',
  '"新增结构"': '"newPage.schedule.addStructure"',
  '"查单模拟排产"': '"newPage.schedule.checkOrderSimulate"',
  '"查看结构排产"': '"newPage.schedule.viewStructureSchedule"',
  '"生成当前周期排产"': '"newPage.schedule.generateCurrentCycle"',
  '"获取调整订单"': '"newPage.schedule.getAdjustOrders"',
  '"周期排产"': '"newPage.schedule.cyclicSchedule"',
  '"班别"': '"newPage.schedule.scheduleClass"',
  '"周期排产储备"': '"newPage.schedule.cyclicReserve"',
  '"常规储备"': '"newPage.schedule.conventionalReserve"',
  '"净需求(不含暂缓)"': '"newPage.schedule.netDemandWithoutPostpone"',
  '"净需求(含暂缓)"': '"newPage.schedule.netDemandWithPostpone"',
  '"最小投产量值"': '"newPage.schedule.minProductionQty"',
  '"是否满足最小投产量"': '"newPage.schedule.isMinProductionQtyMet"',
  '"是否含特殊材料"': '"newPage.schedule.isSpecialMaterial"',
  '"特殊材料"': '"newPage.schedule.specialMaterial"',
  '"特殊材料今日库存+今日计划"': '"newPage.schedule.specialMaterialStockAndPlan"',
  '"特殊材料生产情况"': '"newPage.schedule.specialMaterialProduction"',
  '"高优先级需求(含损耗)"': '"newPage.schedule.highPriorityDemand"',
  '"暂缓订单"': '"newPage.schedule.delayedOrder"',
  '"供应链优先级"': '"newPage.schedule.schedulePriority"',
  '"预测排产计划版"': '"newPage.schedule.forecastScheduleVersion"',
  '"未排原因"': '"newPage.schedule.unscheduleReason"',

  // 称量
  '"称重工位"': '"newPage.weighing.weighingStation"',
  '"称重工位.short"': '"newPage.weighing.weighingStationShort"',
  '"药品包数"': '"newPage.weighing.drugPackageCount"',

  // 产品
  '"产品分类"': '"newPage.product.productCategory"',
  '"产品品类"': '"newPage.product.productType"',
  '"产品状态"': '"newPage.product.productStatus"',
  '"品牌"': '"newPage.product.brand"',
  '"英寸"': '"newPage.product.inch"',

  // 优先级
  '"高优先级"': '"newPage.priority.high"',
  '"中优先级"': '"newPage.priority.medium"',
  '"低优先级"': '"newPage.priority.low"',

  // 调整
  '"后续平移"': '"newPage.adjust.followUpShift"',
  '"临时调整措施"': '"newPage.adjust.temporaryMeasure"',
  '"转机台"': '"newPage.adjust.changeMachine"',
  '"发布"': '"newPage.adjust.publish"',
  '"发送跨区域"': '"newPage.adjust.sendCrossRegional"',
  '"接收跨区域"': '"newPage.adjust.receiveCrossRegional"',
  '"统计"': '"newPage.adjust.statistics"',
  '"补单"': '"newPage.adjust.supplement"',

  // 仓库
  '"库排号"': '"newPage.warehouse.warehouseNo"',
  '"库位类型"': '"newPage.warehouse.locationType"',

  // 批次
  '"批次号"': '"newPage.batch.batchNo"',
  '"硫化结果批次号"': '"newPage.batch.curingResultBatchNo"',
  '"年周号"': '"newPage.batch.yearWeekNo"',
  '"序号"': '"newPage.batch.seqNo"',
  '"顺位"': '"newPage.batch.sortNo"',

  // 生产
  '"动平衡"': '"newPage.production.balance"',
  '"均匀性"': '"newPage.production.uniformity"',

  // 确认消息
  '"确定推送调整后的月计划到SCM/MES？"': '"newPage.confirm.pushSCMMES"',
  '"确定参与排产"': '"newPage.confirm.joinSchedule"',
  '"确定取消参与排产"': '"newPage.confirm.cancelJoinSchedule"',
  '"生成超期SKU"': '"newPage.confirm.generateOverdueSKU"',

  // 提示消息
  '"正在加载中，请稍候"': '"newPage.message.loading"',
  '"正在自动调整中，请稍候"': '"newPage.message.autoAdjusting"',
  '"正在自动调整，请稍候"': '"newPage.message.autoAdjustingPleaseWait"',
  '"正在获取订单中"': '"newPage.message.gettingOrders"',
  '"正在获取调整订单，请稍候"': '"newPage.message.gettingAdjustOrders"',
  '"请输入大于0的正整数"': '"newPage.message.inputPositiveInteger"',
  '"收尾时间（4天以内）"': '"newPage.message.finishTimeWithin4Days"',

  // 其他
  '"总合计"': '"newPage.other.total"',
  '"从"': '"newPage.other.from"',
  '"到"': '"newPage.other.to"',
  '"车数"': '"newPage.other.carCount"',
  '"进度"': '"newPage.other.progress"',
  '"部件名称"': '"newPage.other.componentName"',
  '"部门名称"': '"newPage.other.departmentName"',
  '"关联用户"': '"newPage.other.relatedUser"',
  '"固定结构"': '"newPage.other.fixedStructure"',
  '"固定结构1"': '"newPage.other.fixedStructure1"',
  '"固定结构2"': '"newPage.other.fixedStructure2"',
  '"固定结构3"': '"newPage.other.fixedStructure3"',
  '"不可作业结构"': '"newPage.other.unworkableStructure"',
  '"越南工厂结构"': '"newPage.other.VNFactoryStructure"',
  '"金宇越南"': '"newPage.other.JYVietnam"',
  '"一台的班产（3个班）"': '"newPage.other.capacityPerShift1Machine"',
  '"二台的班产（3个班）"': '"newPage.other.capacityPerShift2Machine"',
  '"三台的班产（3个班）"': '"newPage.other.capacityPerShift3Machine"',
  '"四台的班产（3个班）"': '"newPage.other.capacityPerShift4Machine"',
  '"日标准产量"': '"newPage.other.dailyStandardOutput"',
  '"变更类型"': '"newPage.changeType"',
  '"标准长"': '"newPage.standardLength"',
  '"MES抓取"': '"newPage.MESGrab"',
  '"SCM抓取"': '"newPage.SCMGrab"',
  '"MES日硫化量"': '"newPage.MESDailyCuringQty"',
  '"NC物料编码"': '"newPage.NCMaterialCode"',
  '"EUDR"': '"newPage.EUDR"',
  '"成型鼓厂家"': '"newPage.drumManufacturer"',
  '"预警数据"': '"newPage.warningData"',
};

// 需要跳过的文件模式
const skipPatterns = [
  /node_modules/,
];

// 递归获取所有 Vue 文件
function getVueFiles(dir, files = []) {
  const items = fs.readdirSync(dir);

  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);

    if (stat.isDirectory()) {
      if (!skipPatterns.some(pattern => pattern.test(fullPath))) {
        getVueFiles(fullPath, files);
      }
    } else if (item.endsWith('.vue')) {
      files.push(fullPath);
    }
  }

  return files;
}

// 处理单个文件
function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8');
  let hasChanges = false;
  let changeCount = 0;

  // 按长度降序排序，避免短key替换影响长key
  const sortedKeys = Object.keys(keyMapping).sort((a, b) => b.length - a.length);

  for (const chineseKey of sortedKeys) {
    const englishKey = keyMapping[chineseKey];
    const regex = new RegExp(chineseKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g');

    const matches = content.match(regex);
    if (matches) {
      content = content.replace(regex, englishKey);
      hasChanges = true;
      changeCount += matches.length;
    }
  }

  if (hasChanges) {
    fs.writeFileSync(filePath, content, 'utf-8');
    console.log(`✓ ${filePath} (${changeCount} 处替换)`);
    return true;
  }

  return false;
}

// 主函数
function main() {
  const targetDir = path.resolve(__dirname, '../src/views/newPage');

  console.log('开始扫描 Vue 文件...\n');

  const vueFiles = getVueFiles(targetDir);
  console.log(`找到 ${vueFiles.length} 个 Vue 文件\n`);

  let processedCount = 0;
  let modifiedCount = 0;

  for (const file of vueFiles) {
    processedCount++;
    const modified = processFile(file);
    if (modified) {
      modifiedCount++;
    }
  }

  console.log(`\n=================================`);
  console.log(`处理完成: ${processedCount} 个文件`);
  console.log(`修改文件: ${modifiedCount} 个`);
  console.log(`=================================`);
}

main();

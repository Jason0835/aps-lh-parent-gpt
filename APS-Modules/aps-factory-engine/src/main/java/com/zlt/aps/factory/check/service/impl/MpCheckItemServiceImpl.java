package com.zlt.aps.factory.check.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.enums.CheckItemTypeEnums;
import com.zlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.basedataassemble.history.ProductionHistoryHandler;
import com.zlt.aps.factory.check.service.IMpCheckItemRecordService;
import com.zlt.aps.factory.check.service.IMpCheckItemService;
import com.zlt.aps.factory.daylimit.CapsuleChuckInfoVo;
import com.zlt.aps.factory.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.factory.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.factory.daylimit.TireDrumInfoVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.AbstractDataLoaderService;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.factory.service.DpRequireDataService;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import com.zlt.aps.monthplan.api.domain.vo.MpCheckItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检测项检测Service服务
 *
 * @author hsc
 * @since 2026/01/30
 */
@Service
@Slf4j
public class MpCheckItemServiceImpl extends AbstractDataLoaderService implements IMpCheckItemService {

    private final IMpCheckItemRecordService iMpCheckItemRecordService;

    public MpCheckItemServiceImpl(ProductionMdmDataService dataService,
                                  DpRequireDataService dpRequireDataService,
                                  MonthProductionDataService monthProductionDataService,
                                  ProductionHistoryHandler productionHistoryHandler,
                                  IMpCheckItemRecordService iMpCheckItemRecordService) {
        super(dataService, dpRequireDataService, monthProductionDataService, productionHistoryHandler);
        this.iMpCheckItemRecordService = iMpCheckItemRecordService;
    }

    @Override
    public List<MpCheckItemVo> check(Context context) {
        List<MpCheckItemVo> mpCheckItemVos = new ArrayList<>();
        List<MpCheckItemRecord> mpCheckItemRecords = new ArrayList<>();
        try {

            TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);

            // 1. 初始化数据检测 (Phase 1)
            // 【修改点】接收返回值，判断核心数据是否有效
            boolean isPhase1CoreDataValid = checkInitializationData(productionContext, mpCheckItemVos, mpCheckItemRecords);

            // 如果核心数据无效（查不到或报错），则后续所有检测都无意义，直接标记全部失败并返回
            if (!isPhase1CoreDataValid) {
                log.warn("初始化核心数据无效，终止后续检测流程，标记所有检测项为失败");
                saveRecords(mpCheckItemRecords, context);
                return mpCheckItemVos;
            }

            // 3. 排产前数据检测 (Phase 2)
            try {
                checkBeforeSchedulingData(productionContext, mpCheckItemVos, mpCheckItemRecords);
            } catch (Exception e) {
                log.error("排产前数据检测(Phase 2)发生异常", e);
                // 【核心调用】这里会处理：保留1、2，失败3~8，并新增99记录
                markPhase2AsFailed(mpCheckItemVos, mpCheckItemRecords, e);
            }

            // 4. 保存并返回
            saveRecords(mpCheckItemRecords, context);
            return mpCheckItemVos;

        } catch (Exception e) {
            // 记录系统级异常
            log.error("检测过程发生异常", e);
            MpCheckItemRecord errorRecord = new MpCheckItemRecord();
            errorRecord.setCheckItem("99");
            errorRecord.setCheckContent("系统检测异常: " + e.getMessage());
            mpCheckItemRecords.add(errorRecord);

            MpCheckItemVo errorVo = new MpCheckItemVo();
            errorVo.setCheckItem("系统检测");
            errorVo.setPass(false);
            mpCheckItemVos.add(errorVo);

            saveRecords(mpCheckItemRecords, context);
            return mpCheckItemVos;
        }
    }

    private void addSystemError(List<MpCheckItemVo> vos, List<MpCheckItemRecord> records, String msg) {
        MpCheckItemRecord errorRecord = new MpCheckItemRecord();
        errorRecord.setCheckItem(CheckItemTypeEnums.INIT_DATA.getCode());
        errorRecord.setCheckContent(msg);
        records.add(errorRecord);

        MpCheckItemVo errorVo = new MpCheckItemVo();
        errorVo.setCheckItem(CheckItemTypeEnums.INIT_DATA.getCode());
        errorVo.setPass(false);
        vos.add(errorVo);
    }

    private void markPhase2AsFailed(List<MpCheckItemVo> vos, List<MpCheckItemRecord> records, Exception e) {
        // 1. 分析异常，获取具体的报错检查项 Code
        String errorItemCode = getFailedCheckItemCode(e);
        String errorMsg = "数据检测异常中断";

        // 2. 插入 99 类型的系统异常记录
        // 如果定位到了具体项，将 checkItem 设置为该项的 Code；否则设置为 "99"
        String recordItemCode = StringUtils.isNotBlank(errorItemCode) ? errorItemCode : "99";
        String recordContent = StringUtils.isNotBlank(errorItemCode)
                ? ("检测项[" + errorItemCode + "]执行异常: " + e.getMessage())
                : ("系统检测异常: " + e.getMessage());

        MpCheckItemRecord errorRecord = new MpCheckItemRecord();
        errorRecord.setCheckItem(recordItemCode);
        errorRecord.setCheckContent(recordContent);
        records.add(errorRecord);

        MpCheckItemVo errorVo = new MpCheckItemVo();
        errorVo.setCheckItem(StringUtils.isNotBlank(errorItemCode) ? errorItemCode : "99");
        errorVo.setPass(false);
        vos.add(errorVo);

        // 3. 遍历 8 个检测项，统一设置为 false
        // 注意：这里依然依赖 addCheckResult 的幂等性来保留 1、2 项
        addCheckResult(false, CheckItemTypeEnums.SPECIAL_RAW_MATERIAL_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.PRODUCTION_CALENDAR_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.BASIC_DATA_OF_MOLDING_MACHINE, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.EQUIPMENT_LEDGER_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.MOLD_ALLOCATION_RATIO_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.MOLD_SHELL_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.CAPSULE_CHUCK_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.SULFURIZATION_RATIO_DATA, errorMsg, vos, records);
        addCheckResult(false, CheckItemTypeEnums.OTHER_PARAMS_CONFIG, errorMsg, vos, records);
    }

    /**
     * 根据异常堆栈的行号，判断是哪个检查项报错了
     * 【注意】你需要根据代码实际的行号修改下面的数字范围
     */
    private String getFailedCheckItemCode(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            // 只关心本类中的 checkBeforeSchedulingData 方法
            if ("checkBeforeSchedulingData".equals(element.getMethodName())
                    && element.getClassName().equals(this.getClass().getName())) {

                int lineNumber = element.getLineNumber();

                // --- 以下行号范围仅为示例，请打开 IDE 查看代码实际行号并修改 ---

                // 检查 1: 特殊原材料数据 (假设代码在 150-155 行)
                if (lineNumber >= 295 && lineNumber <= 300) {
                    return CheckItemTypeEnums.SPECIAL_RAW_MATERIAL_DATA.getCode();
                }
                // 检查 2: 生产日历数据 (假设代码在 158-163 行)
                else if (lineNumber >= 301 && lineNumber <= 305) {
                    return CheckItemTypeEnums.PRODUCTION_CALENDAR_DATA.getCode();
                }
                // 检查 3: 成型机基础数据 (假设代码在 166-171 行)
                else if (lineNumber >= 306 && lineNumber <= 310) {
                    return CheckItemTypeEnums.BASIC_DATA_OF_MOLDING_MACHINE.getCode();
                }
                // 检查 4: 工装台账数据
                else if (lineNumber >= 311 && lineNumber <= 315) {
                    return CheckItemTypeEnums.EQUIPMENT_LEDGER_DATA.getCode();
                }
                // 检查 5: 模具分配比例配置
                else if (lineNumber >= 316 && lineNumber <= 320) {
                    return CheckItemTypeEnums.MOLD_ALLOCATION_RATIO_DATA.getCode();
                }
                // 检查 6: 模壳数据
                else if (lineNumber >= 321 && lineNumber <= 325) {
                    return CheckItemTypeEnums.MOLD_SHELL_DATA.getCode();
                }
                // 检查 7: 胶囊卡盘数据
                else if (lineNumber >= 326 && lineNumber <= 330) {
                    return CheckItemTypeEnums.CAPSULE_CHUCK_DATA.getCode();
                }
                // 检查 8: 结构成型硫化配比数据
                else if (lineNumber >= 331 && lineNumber <= 335) {
                    return CheckItemTypeEnums.SULFURIZATION_RATIO_DATA.getCode();
                }

                break; // 找到对应方法后即可跳出循环
            }
        }
        return null; // 未定位到具体行号
    }

    /**
     * 初始化数据检测
     * 优化点：优先赋值 Context，分阶段捕获异常
     */
    private boolean checkInitializationData(TbrProductionContext productionContext, List<MpCheckItemVo> mpCheckItemVos, List<MpCheckItemRecord> mpCheckItemRecords) {

        List<MonthPlanProductionRequirePlanVo> requirePlanList = null;

        // 阶段 1: 数据查询与 Context 赋值
        try {
            // 1. 获取计划列表
            requirePlanList = getMonthPlanRequirePlan(productionContext);

            // 【关键】如果查不到数据，直接返回 false
            if (CollectionUtils.isEmpty(requirePlanList)) {
                markAllChecksAsFailed(mpCheckItemVos, mpCheckItemRecords, "未查询到月度排产需求计划");
                return false;
            }

            // 查到数据，立即放入 Context
            Map<String, List<MonthPlanProductionRequirePlanVo>> allSkuMap = requirePlanList.stream()
                    .filter(plan -> StringUtils.isNotBlank(plan.getMaterialDesc()))
                    .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
            productionContext.setAllSkuProductionPlan(allSkuMap);

            Map<Long, MonthPlanProductionRequirePlanVo> allPlanMap = requirePlanList.stream()
                    .collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
            productionContext.setAllProductionPlan(allPlanMap);

        } catch (Exception e) {
            // 【关键】如果查询阶段报错，记录错误并返回 false
            log.error("获取月度排产计划数据异常", e);
            markAllChecksAsFailed(mpCheckItemVos, mpCheckItemRecords, "获取计划数据异常: " + e.getMessage());
            return false;
        }

        // 阶段 2: 数据补全与校验逻辑
        // 即使这里校验失败（比如物料缺失），只要查到了计划，对于后续流程来说“基础数据是有的”，所以这里返回 true
        // 但需要在 vo 里标记 INIT_DATA 为 false
        try {
            ProductionInitParamConfiguration paramConfiguration = createInitParamConfiguration(productionContext);
            Map<String, ProductBaseInfoVo> productBaseInfoMap = getMaterialInfo(productionContext);
            Map<String, List<MonthPlanProductConstructionInfoVo>> constructionInfoMap = getProductionConstructionInfo(productionContext);
            Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = getProductionMouldInfo(productionContext);
            Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = getProductLhCapacityInfo(productionContext, paramConfiguration.getDayVulcanizationQtyConfiguration());

            requirePlanList.forEach(requirePlan -> {
                String materialCode = requirePlan.getMaterialCode();
                String materialDesc = requirePlan.getMaterialDesc();
                requirePlan.setProductBaseInfo(productBaseInfoMap.get(materialDesc));
                requirePlan.setConstructionInfo(constructionInfoMap.get(materialCode));
                requirePlan.setMouldInfo(mouldInfoMap.get(materialDesc));
                requirePlan.setVulcanizationInfo(lhCapacityMap.get(materialDesc));
            });

            List<MonthPlanProductionRequirePlanVo> validCheckList = requirePlanList.stream()
                    .filter(plan -> YesOrNoEnum.YES.getCode().equals(plan.getIsProduction()))
                    .filter(plan -> plan.getPlanNeedProductionQty() > BigDecimal.ZERO.intValue())
                    .collect(Collectors.toList());

            boolean isInitDataPass = true;
            String failReason = null;

            Map<String, List<MonthPlanProductionRequirePlanVo>> monthPlanProductionRequirePlanVoMaps = validCheckList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialCode));
            for (Map.Entry<String, List<MonthPlanProductionRequirePlanVo>> monthPlanProductionRequirePlanVoMap : monthPlanProductionRequirePlanVoMaps.entrySet()) {
                MonthPlanProductionRequirePlanVo plan = monthPlanProductionRequirePlanVoMap.getValue().get(0);
                plan.checkProductionConditionByBase();
                if (YesOrNoEnum.NO.getCode().equals(plan.getIsProduction())) {
                    isInitDataPass = false;
                    failReason = StringUtils.isBlank(plan.getNoProductionReason()) ? "未知原因导致无法排产" : plan.getNoProductionReason();
                    addErrorRecord(mpCheckItemRecords, CheckItemTypeEnums.INIT_DATA.getCode(), failReason, plan.getMaterialDesc());
                }
            }
            addCheckItemResult(mpCheckItemVos, CheckItemTypeEnums.INIT_DATA, isInitDataPass, failReason);

            // 即使校验不通过，也返回 true，允许后续流程继续跑（可能会查出其他配置缺失的问题）
            return true;

        } catch (Exception e) {
            log.error("初始化数据校验逻辑异常", e);
            addErrorRecord(mpCheckItemRecords, CheckItemTypeEnums.INIT_DATA.getCode(), "数据校验异常: " + e.getMessage(), null);
            addCheckItemResult(mpCheckItemVos, CheckItemTypeEnums.INIT_DATA, false, "数据校验异常: " + e.getMessage());
            // 校验异常通常视为流程可继续，但数据不完整
            return true;
        }
    }

    /**
     * 【新增】批量标记所有检测项为失败
     * 用于在初始化阶段崩溃时，统一设置所有后续依赖检测项的状态
     */
    private void markAllChecksAsFailed(List<MpCheckItemVo> vos, List<MpCheckItemRecord> records, String failReason) {
        // 定义所有受影响的检测项枚举
        CheckItemTypeEnums[] checkItems = {
                CheckItemTypeEnums.INIT_DATA,
                CheckItemTypeEnums.SPECIAL_RAW_MATERIAL_DATA,
                CheckItemTypeEnums.PRODUCTION_CALENDAR_DATA,
                CheckItemTypeEnums.BASIC_DATA_OF_MOLDING_MACHINE,
                CheckItemTypeEnums.EQUIPMENT_LEDGER_DATA,
                CheckItemTypeEnums.MOLD_ALLOCATION_RATIO_DATA,
                CheckItemTypeEnums.MOLD_SHELL_DATA,
                CheckItemTypeEnums.CAPSULE_CHUCK_DATA,
                CheckItemTypeEnums.SULFURIZATION_RATIO_DATA,
                CheckItemTypeEnums.OTHER_PARAMS_CONFIG
        };

        for (CheckItemTypeEnums item : checkItems) {
            addErrorRecord(records, item.getCode(), failReason, null);
            addCheckItemResult(vos, item, false, failReason);
        }
    }

    /**
     * 排产前数据检测
     * 整合逻辑：
     * 1. 调用父类 initProductionBaseData 加载所有排产所需数据（包含环境数据）
     * 2. 检查关键 Map 是否为空
     */
    private void checkBeforeSchedulingData(TbrProductionContext productionContext, List<MpCheckItemVo> mpCheckItemVos, List<MpCheckItemRecord> mpCheckItemRecords) {
        List<MonthPlanProductionRequirePlanVo> requirePlanList;

        // 【安全检查】即使 Phase 1 报错跳过了，这里如果 Context 里没数据，尝试补救查询
        if (productionContext.getAllSkuProductionPlan() != null && !productionContext.getAllSkuProductionPlan().isEmpty()) {
            requirePlanList = productionContext.getAllSkuProductionPlan().values().stream().flatMap(List::stream).collect(Collectors.toList());
            log.info("检测: 复用上下文中的初始化计划数据，条数: {}", requirePlanList.size());
        } else {
            log.warn("检测: 上下文中未找到计划数据，尝试重新查询数据库");
            try {
                requirePlanList = getMonthPlanRequirePlan(productionContext);
                if (CollectionUtils.isEmpty(requirePlanList)) {
                    // 如果重试查询也没数据，Phase 2 的后续逻辑无法进行，直接返回（保留前面的失败状态）
                    return;
                }
            } catch (Exception e) {
                log.error("重试查询计划数据失败", e);
                // 查询失败，无法继续
                return;
            }
        }
        //基础数据容器存储
        productionContext.setBaseDataContainer(new BaseDataContainer());
        // 调用父类方法加载数据
        super.initProductionBaseDataWithExceptions(productionContext, requirePlanList, mpCheckItemVos, mpCheckItemRecords);

        // 2. 开始从 BaseDataContainer 中检查关键数据
        // 检查 1: 特殊原材料数据
        Map<String, Map<String, BigDecimal>> specialMaterialMap = productionContext.getBaseDataContainer().getEmbryoSpecialMaterialInfoMap();
        boolean hasSpecialMaterial = !specialMaterialMap.isEmpty();
        addCheckResult(hasSpecialMaterial, CheckItemTypeEnums.SPECIAL_RAW_MATERIAL_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.SPECIAL_RAW_MATERIAL_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 2: 生产日历数据
        Map<Integer, Integer> capacityRatioMap = productionContext.getCapacityRatioMap();
        boolean hasCalendar = !capacityRatioMap.isEmpty();
        addCheckResult(hasCalendar, CheckItemTypeEnums.PRODUCTION_CALENDAR_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.PRODUCTION_CALENDAR_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 3: 成型机基础数据
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        boolean hasMoldingMachine = !cxMachineBaseInfo.isEmpty();
        addCheckResult(hasMoldingMachine, CheckItemTypeEnums.BASIC_DATA_OF_MOLDING_MACHINE, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.MOLD_MACHINE_BASEDATA_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 4: 工装台账数据
        Map<String, Map<String, TireDrumInfoVo>> mouldInfoMap = productionContext.getBaseDataContainer().getTireDrumInfoMap();
        boolean hasEquipmentLedger = !mouldInfoMap.isEmpty();
        addCheckResult(hasEquipmentLedger, CheckItemTypeEnums.EQUIPMENT_LEDGER_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.WORKWEAR_INVENTORY_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 5: 模具分配比例配置
        Map<String, MouldAllocationInfoVo> mouldAllocationMap = productionContext.getBaseDataContainer().getGroupMainPatternAllocationLimitMap();
        boolean hasMouldRatio = !mouldAllocationMap.isEmpty();
        addCheckResult(hasMouldRatio, CheckItemTypeEnums.MOLD_ALLOCATION_RATIO_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.MOLD_ALLOCATION_RATIO_CONFIG_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 6: 模壳数据
        Map<String, MouldShellBaseInfoVo> mouldShellMap = productionContext.getBaseDataContainer().getMouldShellMap();
        boolean hasMoldShell = !mouldShellMap.isEmpty();
        addCheckResult(hasMoldShell, CheckItemTypeEnums.MOLD_SHELL_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.MOLD_SHELL_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 7: 胶囊卡盘数据
        Map<String, CapsuleChuckInfoVo> capsuleChuckInfoMap = productionContext.getBaseDataContainer().getCapsuleChuckInfoMap();
        boolean hasCapsuleChuck = !capsuleChuckInfoMap.isEmpty();
        addCheckResult(hasCapsuleChuck, CheckItemTypeEnums.CAPSULE_CHUCK_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.CAPSULE_CHUCK_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 检查 8: 结构成型硫化配比数据
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = productionContext.getBaseDataContainer().getStructureLhRatioList();
        boolean hasSulfurizationRatio = CollectionUtils.isNotEmpty(structureLhRatioList);
        addCheckResult(hasSulfurizationRatio, CheckItemTypeEnums.SULFURIZATION_RATIO_DATA, NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.STRUCTURE_FORMING_VULCANIZATION_RATIO_NOTEMPTY), mpCheckItemVos, mpCheckItemRecords);

        // 确保 OTHER_PARAMS_CONFIG 检测项被正确处理
        // 检查 OTHER_PARAMS_CONFIG 是否已经存在，如果不存在则添加成功状态
        boolean isOtherParamsConfigChecked = mpCheckItemVos.stream()
                .anyMatch(vo -> CheckItemTypeEnums.OTHER_PARAMS_CONFIG.getCode().equals(vo.getCheckItem()));

        if (!isOtherParamsConfigChecked) {
            addCheckResult(true, CheckItemTypeEnums.OTHER_PARAMS_CONFIG, null, mpCheckItemVos, mpCheckItemRecords);
        }
    }

    /**
     * 辅助方法：添加检测结果
     */
    private void addCheckResult(boolean isPass, CheckItemTypeEnums checkItemType, String failReason, List<MpCheckItemVo> mpCheckItemVos, List<MpCheckItemRecord> mpCheckItemRecords) {
        // 先判断该类型的检测是否已经存在结果
        boolean isAlreadyChecked = mpCheckItemVos.stream()
                .anyMatch(vo -> checkItemType.getCode().equals(vo.getCheckItem()));

        // 如果已经检测过，直接返回，保留原来的结果（保留第1、2项的成功状态）
        if (isAlreadyChecked) {
            return;
        }
        if (!isPass) {
            addErrorRecord(mpCheckItemRecords, checkItemType.getCode(), failReason, null);
        }
        addCheckItemResult(mpCheckItemVos, checkItemType, isPass, isPass ? null : failReason);
    }

    /**
     * 辅助方法：添加单个检测项VO
     */
    private void addCheckItemResult(List<MpCheckItemVo> list, CheckItemTypeEnums checkItemType, boolean isPass, String reason) {
        MpCheckItemVo vo = new MpCheckItemVo();
        vo.setCheckItem(checkItemType.getCode());
        vo.setPass(isPass);
        list.add(vo);
    }

    /**
     * 添加错误记录
     */
    private void addErrorRecord(List<MpCheckItemRecord> records, String checkItem, String errorReason, String materialDesc) {
        MpCheckItemRecord record = new MpCheckItemRecord();
        record.setCheckItem(checkItem);
        record.setCheckContent(errorReason);
        record.setMaterialDesc(materialDesc);
        records.add(record);
    }

    /**
     * 保存记录
     */
    private void saveRecords(List<MpCheckItemRecord> records, Context context) {
        LambdaQueryWrapper<MpCheckItemRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpCheckItemRecord::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(MpCheckItemRecord::getYear, context.getYear());
        wrapper.eq(MpCheckItemRecord::getMonth, context.getMonth());
        wrapper.eq(MpCheckItemRecord::getFactoryCode, context.getFactoryCode());
        wrapper.eq(MpCheckItemRecord::getProductTypeCode, context.getProductType().getValue());
        wrapper.eq(MpCheckItemRecord::getMonthPlanVersion, context.getMonthPlanVersion());
        iMpCheckItemRecordService.remove(wrapper);

        if (CollectionUtils.isNotEmpty(records)) {
            records.forEach(record -> {
                record.setYear(context.getYear());
                record.setMonth(context.getMonth());
                record.setFactoryCode(context.getFactoryCode());
                record.setProductTypeCode(context.getProductType().getValue());
                record.setMonthPlanVersion(context.getMonthPlanVersion());
            });
            iMpCheckItemRecordService.saveBatch(records);
        }
    }


    @Override
    public void run(Context context, Object userObj) {
        // 接口默认空实现
    }
}

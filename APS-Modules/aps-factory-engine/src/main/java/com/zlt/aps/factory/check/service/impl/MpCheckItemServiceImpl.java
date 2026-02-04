package com.zlt.aps.factory.check.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tlt.aps.enums.CheckItemTypeEnums;
import com.tlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.basedataassemble.history.ProductionHistoryHandler;
import com.zlt.aps.factory.check.service.IMpCheckItemRecordService;
import com.zlt.aps.factory.check.service.IMpCheckItemService;
import com.zlt.aps.factory.daylimit.CapsuleChuckInfoVo;
import com.zlt.aps.factory.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.factory.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.factory.service.DpRequireDataService;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import com.zlt.aps.monthplan.api.domain.vo.MpCheckItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MpCheckItemServiceImpl extends AbstractProductionBusinessService implements IMpCheckItemService {


    @Autowired
    private IMpCheckItemRecordService iMpCheckItemRecordService;

    public MpCheckItemServiceImpl(ProductionSchedulingDataService dataService,
                                  DpRequireDataService dpRequireDataService,
                                  ProductionHistoryHandler productionHistoryHandler) {
        super(dataService, dpRequireDataService);
        super.setProductionHistoryHandler(productionHistoryHandler);
    }

    @Override
    public List<MpCheckItemVo> check(Context context) {
        List<MpCheckItemVo> mpCheckItemVos = new ArrayList<>();
        List<MpCheckItemRecord> mpCheckItemRecords = new ArrayList<>();
        try {
//            if (null == context.getInsertNewProductionVersion()) {
//                context.setInsertNewProductionVersion(Boolean.FALSE);
//            }
            TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);

            // 2. 初始化数据检测 (Phase 1)
            checkInitializationData(productionContext, mpCheckItemVos, mpCheckItemRecords);

            // 3. 排产前数据检测 (Phase 2)
            checkBeforeSchedulingData(productionContext, mpCheckItemVos, mpCheckItemRecords);

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

    /**
     * 初始化数据检测
     * 负责加载基础数据（物料、施工、模具）并校验计划有效性
     */
    private void checkInitializationData(TbrProductionContext productionContext, List<MpCheckItemVo> mpCheckItemVos, List<MpCheckItemRecord> mpCheckItemRecords) {

        // 1. 获取计划列表
        List<MonthPlanProductionRequirePlanVo> requirePlanList = getMonthPlanRequirePlan(productionContext);

        if (CollectionUtils.isEmpty(requirePlanList)) {
            addErrorRecord(mpCheckItemRecords, CheckItemTypeEnums.INIT_DATA.getCode(), "未查询到月度排产需求计划");
            addCheckItemResult(mpCheckItemVos, CheckItemTypeEnums.INIT_DATA, false, "未查询到月度排产需求计划");
            return;
        }

        // 2. 获取初始化辅助配置数据
        ProductionInitParamConfiguration paramConfiguration = createInitParamConfiguration(productionContext);
        Map<String, ProductBaseInfoVo> productBaseInfoMap = getMaterialInfo(productionContext);
        Map<String, List<MonthPlanProductConstructionInfoVo>> constructionInfoMap = getProductionConstructionInfo(productionContext);
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = getProductionMouldInfo(productionContext);
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = getProductLhCapacityInfo(productionContext, paramConfiguration.getDayVulcanizationQtyConfiguration());

        // 3. 数据赋值与预处理
        requirePlanList.forEach(requirePlan -> {
            String materialCode = requirePlan.getMaterialCode();
            String materialDesc = requirePlan.getMaterialDesc();
            requirePlan.setProductBaseInfo(productBaseInfoMap.get(materialDesc));
            requirePlan.setConstructionInfo(constructionInfoMap.get(materialCode));
            requirePlan.setMouldInfo(mouldInfoMap.get(materialDesc));
            requirePlan.setVulcanizationInfo(lhCapacityMap.get(materialDesc));
        });

        // 4. 过滤有效数据
        List<MonthPlanProductionRequirePlanVo> validCheckList = requirePlanList.stream().filter(plan -> YesOrNoEnum.YES.getCode().equals(plan.getIsProduction())).filter(plan -> plan.getPlanNeedProductionQty() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());

        // 5. 执行检测逻辑
        boolean isInitDataPass = true;
        String failReason = null;

        for (MonthPlanProductionRequirePlanVo plan : validCheckList) {
            plan.checkProductionConditionByBase();
            if (YesOrNoEnum.NO.getCode().equals(plan.getIsProduction())) {
                isInitDataPass = false;
                failReason = StringUtils.isBlank(plan.getNoProductionReason()) ? "未知原因导致无法排产" : plan.getNoProductionReason();
                addErrorRecord(mpCheckItemRecords, CheckItemTypeEnums.INIT_DATA.getCode(), failReason);
                break;
            }
        }

        // 6. 添加结果
        addCheckItemResult(mpCheckItemVos, CheckItemTypeEnums.INIT_DATA, isInitDataPass, failReason);

        // 将处理过的 requirePlanList 存入 Context，供排产前检测复用
        if (true) {
            // 按物料描述分组 Map
            Map<String, List<MonthPlanProductionRequirePlanVo>> allSkuMap = requirePlanList.stream().filter(plan -> StringUtils.isNotBlank(plan.getMaterialDesc())).collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));

            productionContext.setAllSkuProductionPlan(allSkuMap);

            // 按ID分组 Map
            Map<Long, MonthPlanProductionRequirePlanVo> allPlanMap = requirePlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity()));
            productionContext.setAllProductionPlan(allPlanMap);
        }
    }

    /**
     * 排产前数据检测
     * 整合逻辑：
     * 1. 调用父类 initProductionBaseData 加载所有排产所需数据（包含环境数据）
     * 2. 检查关键 Map 是否为空
     */
    private void checkBeforeSchedulingData(TbrProductionContext productionContext, List<MpCheckItemVo> mpCheckItemVos, List<MpCheckItemRecord> mpCheckItemRecords) {
        try {
            List<MonthPlanProductionRequirePlanVo> requirePlanList;

            // 【整合点】优先尝试从 Context 获取已经初始化过的列表
            if (!productionContext.getAllSkuProductionPlan().isEmpty()) {
                // 将 Map 还原为 List
                requirePlanList = productionContext.getAllSkuProductionPlan().values().stream().flatMap(List::stream).collect(Collectors.toList());
                log.info("检测: 复用上下文中的初始化计划数据，条数: {}", requirePlanList.size());
            } else {
                // 容错：如果 Context 中没有（可能逻辑分支没走到），则重新查询
                log.warn("检测: 上下文中未找到计划数据，重新查询数据库");
                requirePlanList = getDataService().getFactoryMonthPlanManufacturing(productionContext);
            }
            //基础数据容器存储
            productionContext.setBaseDataContainer(new BaseDataContainer());
            // 调用父类方法加载数据
            super.initProductionBaseData(productionContext, requirePlanList);

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
            Map<String, ProductionMouldInfoVo> mouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
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

        } catch (Exception e) {
            log.error("排产前数据检测失败", e);
            addErrorRecord(mpCheckItemRecords, "99", "数据加载异常: " + e.getMessage());
            addCheckItemResult(mpCheckItemVos, CheckItemTypeEnums.SPECIAL_RAW_MATERIAL_DATA, false, "数据加载异常: " + e.getMessage());
        }
    }

    /**
     * 辅助方法：添加检测结果
     */
    private void addCheckResult(boolean isPass, CheckItemTypeEnums checkItemType, String failReason, List<MpCheckItemVo> mpCheckItemVos, List<MpCheckItemRecord> mpCheckItemRecords) {
        if (!isPass) {
            addErrorRecord(mpCheckItemRecords, checkItemType.getCode(), failReason);
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
    private void addErrorRecord(List<MpCheckItemRecord> records, String checkItem, String errorReason) {
        MpCheckItemRecord record = new MpCheckItemRecord();
        record.setCheckItem(checkItem);
        record.setCheckContent(errorReason);
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

package com.zlt.aps.lh.handler;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.LhInsertOrderValidateResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.mapper.CxStockMapper;
import com.zlt.aps.lh.mapper.LhMachineInfoMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.lh.mapper.MdmModelInfoMapper;
import com.zlt.aps.lh.mapper.MdmMonthSurplusMapper;
import com.zlt.aps.lh.mapper.MdmSkuLhCapacityMapper;
import com.zlt.aps.lh.mapper.MdmSkuMouldRelMapper;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化插单校验处理器
 * <p>负责插单前的所有校验逻辑，包括：</p>
 * <ul>
 *   <li>物料编码校验</li>
 *   <li>物料编码月度计划校验</li>
 *   <li>SKU月度计划及生产中校验</li>
 *   <li>班次计划量校验</li>
 *   <li>重复插单校验</li>
 *   <li>历史班次校验</li>
 *   <li>机台可用性校验及产能提示</li>
 *   <li>硫化余量超产提示</li>
 *   <li>模具可用性提示</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class LhInsertOrderValidateHandler {

    @Resource
    private MdmMaterialInfoMapper mdmMaterialInfoMapper;

    @Resource
    private LhMachineInfoMapper lhMachineInfoMapper;

    @Resource
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Resource
    private MdmSkuLhCapacityMapper mdmSkuLhCapacityMapper;

    @Resource
    private MdmMonthSurplusMapper mdmMonthSurplusMapper;

    @Resource
    private MdmSkuMouldRelMapper mdmSkuMouldRelMapper;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

    @Resource
    private FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Resource
    private MpFactoryProductionVersionMapper mpFactoryProductionVersionMapper;

    @Resource
    private CxStockMapper cxStockMapper;

    @Resource
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;

    @Resource
    private MdmModelInfoMapper mdmModelInfoMapper;

    /** 排产版本已定稿 */
    private static final String PRODUCTION_VERSION_IS_FINAL = "1";

    /** 生产状态：生产中 */
    private static final String PRODUCTION_STATUS_IN_PRODUCTION = "1";

    /** 模具状态：可用 */
    private static final int MOULD_STATUS_AVAILABLE = 1;

    /**
     * 执行插单校验
     *
     * @param dto 插单请求数据
     * @return 校验结果
     */
    public LhInsertOrderValidateResultDTO validateInsertOrder(LhOrderInsertDTO dto) {
        LhInsertOrderValidateResultDTO result = new LhInsertOrderValidateResultDTO();
        result.setValid(true);

        validateMaterialCode(dto, result);
        validateMaterialCodeInMonthPlan(dto, result);
        validateSkuInMonthPlanAndNotInProduction(dto, result);
        validateShiftPlanQty(dto, result);
        validateDuplicateInsert(dto, result);
        validateHistoricalShift(dto, result);
        checkMachineAvailability(dto, result);
        checkMouldSurplus(dto, result);
        checkMouldAvailability(dto, result);
        fillSkuRelatedData(dto, result);

        return result;
    }

    /**
     * 获取SKU关联数据（硫化余量/胎胚库存/硫化班产/示方类型）
     * <p>用于插单页面选择新物料时实时获取关联信息，不进行业务校验</p>
     *
     * @param dto 包含factoryCode、materialCode、scheduleDate的请求对象
     * @return SKU关联数据（仅包含mouldSurplusQty、embryoStock、machineShiftCapacity、trialStatus、leftRightMould等字段）
     */
    public LhInsertOrderValidateResultDTO getSkuRelatedData(LhOrderInsertDTO dto) {
        LhInsertOrderValidateResultDTO result = new LhInsertOrderValidateResultDTO();
        result.setValid(true);
        checkMachineAvailability(dto, result);
        checkMouldSurplus(dto, result);
        checkMouldAvailability(dto, result);
        fillSkuRelatedData(dto, result);
        return result;
    }

    /**
     * 获取物料编码（优先使用productCode，其次使用materialCode）
     *
     * @param dto 插单数据
     * @return 物料编码
     */
    private String resolveMaterialCode(LhOrderInsertDTO dto) {
        return StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();
    }

    /**
     * 校验物料编码是否存在
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void validateMaterialCode(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode)) {
            result.addError(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.materialCodeBlank"));
            return;
        }
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMaterialInfo::getMaterialCode, materialCode);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(MdmMaterialInfo::getFactoryCode, dto.getFactoryCode());
        }
        wrapper.last("LIMIT 1");
        MdmMaterialInfo materialInfo = mdmMaterialInfoMapper.selectOne(wrapper);
        if (Objects.isNull(materialInfo)) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.materialCodeNotFound"), materialCode));
        }
    }

    /**
     * 校验物料编码是否在当前排程月对应的月度计划中
     * <p>根据排程日期确定所属年月，查找定稿排产版本，再校验物料编码是否存在于月度计划定稿表中</p>
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void validateMaterialCodeInMonthPlan(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode) || dto.getScheduleDate() == null) {
            return;
        }

        Date scheduleDate = dto.getScheduleDate();
        int year = DateUtil.year(scheduleDate);
        int month = DateUtil.month(scheduleDate) + 1;
        String yearMonthText = String.format("%04d-%02d", year, month);

        MpFactoryProductionVersion finalVersion = getFinalProductionVersion(dto.getFactoryCode(), year, month);
        if (finalVersion == null) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.monthPlanVersionNotFound"), yearMonthText));
            return;
        }

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, dto.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (StringUtils.isNotBlank(finalVersion.getProductionVersion())) {
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalVersion.getProductionVersion());
        }
        wrapper.last("LIMIT 1");
        Long count = monthPlanMapper.selectCount(wrapper);
        if (count == null || count == 0) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.materialCodeNotInMonthPlan"), materialCode, yearMonthText));
        }
    }

    /**
     * 校验SKU是否在当前月度计划表中，并且过滤掉当前月已经正在生产的SKU
     * <p>1. 校验SKU（物料编码）是否存在于当前排程月对应的月度计划定稿表中</p>
     * <p>2. 校验SKU在当前月是否已经正在生产（productionStatus=1），若正在生产则不允许重复插单</p>
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void validateSkuInMonthPlanAndNotInProduction(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode) || dto.getScheduleDate() == null) {
            return;
        }

        Date scheduleDate = dto.getScheduleDate();
        int year = DateUtil.year(scheduleDate);
        int month = DateUtil.month(scheduleDate) + 1;

        MpFactoryProductionVersion finalVersion = getFinalProductionVersion(dto.getFactoryCode(), year, month);
        if (finalVersion == null) {
            return;
        }

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, dto.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (StringUtils.isNotBlank(finalVersion.getProductionVersion())) {
            planWrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalVersion.getProductionVersion());
        }
        planWrapper.last("LIMIT 1");
        Long planCount = monthPlanMapper.selectCount(planWrapper);
        if (planCount == null || planCount == 0) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.skuNotInMonthPlan"), materialCode));
            return;
        }

        Date startDate = DateUtil.beginOfMonth(scheduleDate);
        Date endDate = DateUtil.offsetMonth(startDate, 1);

        LambdaQueryWrapper<LhScheduleResult> productionWrapper = new LambdaQueryWrapper<>();
        productionWrapper.eq(LhScheduleResult::getMaterialCode, materialCode)
                .eq(LhScheduleResult::getProductionStatus, PRODUCTION_STATUS_IN_PRODUCTION)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .ge(LhScheduleResult::getScheduleDate, startDate)
                .lt(LhScheduleResult::getScheduleDate, endDate);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            productionWrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        Long productionCount = lhScheduleResultMapper.selectCount(productionWrapper);
        if (productionCount != null && productionCount > 0) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.skuAlreadyInProduction"), materialCode));
        }
    }

    /**
     * 获取定稿排产版本
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return 定稿排产版本，不存在返回null
     */
    private MpFactoryProductionVersion getFinalProductionVersion(String factoryCode, int year, int month) {
        if (StringUtils.isBlank(factoryCode)) {
            return null;
        }
        LambdaQueryWrapper<MpFactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpFactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(MpFactoryProductionVersion::getYear, year)
                .eq(MpFactoryProductionVersion::getMonth, month)
                .eq(MpFactoryProductionVersion::getIsFinal, PRODUCTION_VERSION_IS_FINAL)
                .eq(MpFactoryProductionVersion::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                .orderByDesc(MpFactoryProductionVersion::getId)
                .last("LIMIT 1");
        return mpFactoryProductionVersionMapper.selectOne(wrapper);
    }

    /**
     * 校验8个班次中至少有一个班的计划量有值
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void validateShiftPlanQty(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        boolean hasAnyPlanQty = false;
        if (dto.getClass1PlanQty() != null && dto.getClass1PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass2PlanQty() != null && dto.getClass2PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass3PlanQty() != null && dto.getClass3PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass4PlanQty() != null && dto.getClass4PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass5PlanQty() != null && dto.getClass5PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass6PlanQty() != null && dto.getClass6PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass7PlanQty() != null && dto.getClass7PlanQty() > 0) hasAnyPlanQty = true;
        if (dto.getClass8PlanQty() != null && dto.getClass8PlanQty() > 0) hasAnyPlanQty = true;

        if (!hasAnyPlanQty) {
            result.addError(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.shiftPlanQtyEmpty"));
        }
    }

    /**
     * 重复插单校验：检查是否存在相同（物料编码+硫化机台）的排程结果
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void validateDuplicateInsert(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode) || StringUtils.isBlank(dto.getLhMachineCode())) {
            return;
        }
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getMaterialCode, materialCode)
                .eq(LhScheduleResult::getLhMachineCode, dto.getLhMachineCode())
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (dto.getScheduleDate() != null) {
            wrapper.eq(LhScheduleResult::getScheduleDate, dto.getScheduleDate());
        }
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        Long count = lhScheduleResultMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.duplicateInsert"), materialCode, dto.getLhMachineCode()));
        }
    }

    /**
     * 历史班次校验：只能往当前班次或后续班次插单，不能往历史班次插单
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void validateHistoricalShift(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        if (dto.getScheduleDate() == null) {
            return;
        }

        Date now = new Date();
        Date scheduleDate = dto.getScheduleDate();

        Date today = DateUtil.beginOfDay(now);
        Date scheduleDay = DateUtil.beginOfDay(scheduleDate);

        if (scheduleDay.after(today)) {
            return;
        }

        if (scheduleDay.before(today)) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.historicalDate"), DateUtil.formatDate(scheduleDate)));
            return;
        }

        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.buildDefaultScheduleShifts(null, scheduleDate);
        int currentShiftIndex = resolveCurrentShiftIndex(shifts, now);

        if (currentShiftIndex < 0) {
            return;
        }

        for (int i = 1; i < currentShiftIndex; i++) {
            Integer planQty = getPlanQtyByShiftIndex(dto, i);
            if (planQty != null && planQty > 0) {
                LhShiftConfigVO shift = findShiftByIndex(shifts, i);
                String shiftName = shift != null ? shift.getShiftName() : String.format("第%d班", i);
                result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.historicalShift"), shiftName));
            }
        }
    }

    /**
     * 检查硫化机台是否可用，填充机台班产，校验各班次剩余产能
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void checkMachineAvailability(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        if (StringUtils.isBlank(dto.getLhMachineCode())) {
            return;
        }

        LambdaQueryWrapper<LhMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
        machineWrapper.eq(LhMachineInfo::getMachineCode, dto.getLhMachineCode());
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            machineWrapper.eq(LhMachineInfo::getFactoryCode, dto.getFactoryCode());
        }
        machineWrapper.last("LIMIT 1");
        LhMachineInfo machineInfo = lhMachineInfoMapper.selectOne(machineWrapper);

        if (Objects.isNull(machineInfo)) {
            LambdaQueryWrapper<LhMachineInfo> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.likeRight(LhMachineInfo::getMachineCode, dto.getLhMachineCode());
            if (StringUtils.isNotBlank(dto.getFactoryCode())) {
                likeWrapper.eq(LhMachineInfo::getFactoryCode, dto.getFactoryCode());
            }
            List<LhMachineInfo> likeMachines = lhMachineInfoMapper.selectList(likeWrapper);
            if (CollectionUtils.isNotEmpty(likeMachines)) {
                List<String> machineCodes = likeMachines.stream()
                        .map(LhMachineInfo::getMachineCode)
                        .collect(Collectors.toList());
                result.addError(String.format(
                        I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.machineNotExist"),
                        dto.getLhMachineCode() + "，相似机台：" + String.join("、", machineCodes) + "，请选择具体机台"));
            } else {
                result.addError(String.format(
                        I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.machineNotExist"),
                        dto.getLhMachineCode()));
            }
            return;
        }

        if (!MachineStatusUtil.isEnabled(machineInfo.getStatus())) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.machineUnavailable"), dto.getLhMachineCode()));
        }

        if (machineInfo.getMaxMoldNum() != null && machineInfo.getMaxMoldNum() == 1) {
            String lrMould = resolveLeftRightMould(dto.getLhMachineCode());
            if (StringUtils.isNotBlank(lrMould)) {
                result.setLeftRightMould(lrMould);
            }
        }

        Integer machineQuota = machineInfo.getQuota();
        if (machineQuota == null || machineQuota <= 0) {
            String materialCode = resolveMaterialCode(dto);
            LambdaQueryWrapper<MdmSkuLhCapacity> capacityWrapper = new LambdaQueryWrapper<>();
            capacityWrapper.eq(MdmSkuLhCapacity::getMaterialCode, materialCode);
            if (StringUtils.isNotBlank(dto.getFactoryCode())) {
                capacityWrapper.eq(MdmSkuLhCapacity::getFactoryCode, dto.getFactoryCode());
            }
            capacityWrapper.last("LIMIT 1");
            MdmSkuLhCapacity skuCapacity = mdmSkuLhCapacityMapper.selectOne(capacityWrapper);
            if (skuCapacity != null && skuCapacity.getClassCapacity() != null) {
                machineQuota = skuCapacity.getClassCapacity();
            }
        }

        if (machineQuota != null && machineQuota > 0) {
            int singleMouldShiftQty = machineQuota;
            if (machineInfo.getMaxMoldNum() != null && machineInfo.getMaxMoldNum() == 1) {
                singleMouldShiftQty = machineQuota / 2;
            }
            result.setMachineShiftCapacity(machineQuota);
            result.setSingleMouldShiftQty(singleMouldShiftQty);
            calculateRemainingCapacity(dto, result, machineQuota);
        }
    }

    /**
     * 计算各班次剩余产能
     *
     * @param dto          插单数据
     * @param result       校验结果
     * @param shiftCapacity 班产
     */
    private void calculateRemainingCapacity(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result,
                                            Integer shiftCapacity) {
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getLhMachineCode, dto.getLhMachineCode())
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (dto.getScheduleDate() != null) {
            wrapper.eq(LhScheduleResult::getScheduleDate, dto.getScheduleDate());
        }
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        List<LhScheduleResult> existingResults = lhScheduleResultMapper.selectList(wrapper);

        int[] scheduledQtyByShift = new int[LhScheduleConstant.MAX_SHIFT_SLOT_COUNT + 1];
        for (LhScheduleResult existing : existingResults) {
            for (int i = 1; i <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(existing, i);
                scheduledQtyByShift[i] += (planQty != null ? planQty : 0);
            }
        }

        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.buildDefaultScheduleShifts(null, dto.getScheduleDate());

        for (int i = 1; i <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
            int scheduledQty = scheduledQtyByShift[i];
            int remaining = shiftCapacity - scheduledQty;
            Integer insertQty = getPlanQtyByShiftIndex(dto, i);
            if (insertQty != null && insertQty > 0 && remaining < insertQty) {
                String shiftName = getShiftName(shifts, i);
                result.addWarning(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.capacityInsufficient"), shiftName, shiftCapacity, scheduledQty, remaining, insertQty));
            }

            LhInsertOrderValidateResultDTO.ShiftCapacityInfo capacityInfo =
                    new LhInsertOrderValidateResultDTO.ShiftCapacityInfo(
                            i,
                            getShiftName(shifts, i),
                            shiftCapacity,
                            scheduledQty,
                            Math.max(remaining, 0)
                    );
            result.getRemainingCapacityByShift().add(capacityInfo);
        }
    }

    /**
     * 检查硫化余量，进行超产提示
     * <p>硫化余量 = 月计划量 - 已完成量（从月计划定稿表计算）</p>
     * <p>胎胚库存按共用胎胚分摊公式计算：SKU分配的胎胚库存量 = (SKU日硫化量 / 同胎胚的所有SKU日硫化量汇总) * 胎胚库存量</p>
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void checkMouldSurplus(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode) || dto.getScheduleDate() == null) {
            return;
        }

        Date scheduleDate = dto.getScheduleDate();
        int year = DateUtil.year(scheduleDate);
        int month = DateUtil.month(scheduleDate) + 1;

        MpFactoryProductionVersion finalVersion = getFinalProductionVersion(dto.getFactoryCode(), year, month);
        if (finalVersion == null) {
            return;
        }

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, dto.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (StringUtils.isNotBlank(finalVersion.getProductionVersion())) {
            planWrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalVersion.getProductionVersion());
        }
        planWrapper.last("LIMIT 1");
        FactoryMonthPlanProductionFinalResult monthPlan = monthPlanMapper.selectOne(planWrapper);
        if (Objects.isNull(monthPlan)) {
            return;
        }

        int totalPlanQty = monthPlan.getTotalQty() != null ? monthPlan.getTotalQty() : 0;
        int finishedQty = calculateFinishedQty(dto, materialCode, scheduleDate);
        int surplusQty = Math.max(0, totalPlanQty - finishedQty);
        result.setMouldSurplusQty(surplusQty);

        fillEmbryoStock(dto, result, monthPlan, year, month, finalVersion);

        int totalInsertQty = calculateTotalInsertQty(dto);
        if (totalInsertQty > surplusQty) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.mouldSurplusExceeded"), surplusQty, totalInsertQty, (totalInsertQty - surplusQty)));
        }
    }

    /**
     * 计算指定物料在当月的已完成量
     * <p>已完成量 = 当月排程结果中该物料各班次计划量汇总</p>
     *
     * @param dto          插单数据
     * @param materialCode 物料编码
     * @param scheduleDate 排程日期
     * @return 已完成量
     */
    private int calculateFinishedQty(LhOrderInsertDTO dto, String materialCode, Date scheduleDate) {
        Date startDate = DateUtil.beginOfMonth(scheduleDate);
        Date endDate = DateUtil.offsetMonth(startDate, 1);

        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getMaterialCode, materialCode)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .ge(LhScheduleResult::getScheduleDate, startDate)
                .lt(LhScheduleResult::getScheduleDate, endDate);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        List<LhScheduleResult> existingResults = lhScheduleResultMapper.selectList(wrapper);

        int finishedQty = 0;
        for (LhScheduleResult existing : existingResults) {
            for (int i = 1; i <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(existing, i);
                finishedQty += (planQty != null ? planQty : 0);
            }
        }
        return finishedQty;
    }

    /**
     * 填充胎胚库存（按共用胎胚分摊公式计算）
     * <p>SKU分配的胎胚库存量 = (SKU日硫化量 / 同胎胚的所有SKU日硫化量汇总) * 胎胚库存量</p>
     * <p>分摊权重优先使用SKU标准产能（standardCapacity），缺失时回退到日硫化量（dayVulcanizationQty）</p>
     *
     * @param dto          插单数据
     * @param result       校验结果
     * @param monthPlan    当前SKU的月计划
     * @param year         年份
     * @param month        月份
     * @param finalVersion 定稿版本
     */
    private void fillEmbryoStock(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result,
                                 FactoryMonthPlanProductionFinalResult monthPlan,
                                 int year, int month, MpFactoryProductionVersion finalVersion) {
        String embryoCode = monthPlan.getEmbryoCode();
        if (StringUtils.isBlank(embryoCode)) {
            return;
        }

        LambdaQueryWrapper<CxStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.eq(CxStock::getEmbryoCode, embryoCode);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            stockWrapper.eq(CxStock::getFactoryCode, dto.getFactoryCode());
        }
        if (dto.getScheduleDate() != null) {
            stockWrapper.eq(CxStock::getStockDate, dto.getScheduleDate());
        }
        List<CxStock> stockList = cxStockMapper.selectList(stockWrapper);
        int embryoTotalStock = stockList.stream()
                .mapToInt(s -> s.getStockNum() != null ? s.getStockNum() : 0)
                .sum();
        if (embryoTotalStock <= 0) {
            result.setEmbryoStock(0);
            return;
        }

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> sameEmbryoWrapper = new LambdaQueryWrapper<>();
        sameEmbryoWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, dto.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                .eq(FactoryMonthPlanProductionFinalResult::getEmbryoCode, embryoCode)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (StringUtils.isNotBlank(finalVersion.getProductionVersion())) {
            sameEmbryoWrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalVersion.getProductionVersion());
        }
        List<FactoryMonthPlanProductionFinalResult> sameEmbryoPlans = monthPlanMapper.selectList(sameEmbryoWrapper);

        int allocationWeight = resolveEmbryoAllocationWeight(dto.getFactoryCode(), monthPlan);
        int embryoWeightSum = 0;
        for (FactoryMonthPlanProductionFinalResult plan : sameEmbryoPlans) {
            embryoWeightSum += resolveEmbryoAllocationWeight(dto.getFactoryCode(), plan);
        }

        if (allocationWeight <= 0 || embryoWeightSum <= 0) {
            result.setEmbryoStock(embryoTotalStock);
            return;
        }

        int allocatedStock = (int) ((long) embryoTotalStock * allocationWeight / embryoWeightSum);
        log.debug("同胎胚库存按分摊权重分摊, materialCode: {}, embryoCode: {}, allocationWeight: {}, "
                        + "embryoWeightSum: {}, embryoTotalStock: {}, allocatedStock: {}",
                monthPlan.getMaterialCode(), embryoCode, allocationWeight,
                embryoWeightSum, embryoTotalStock, allocatedStock);
        result.setEmbryoStock(allocatedStock);
    }

    /**
     * 解析胎胚库存分摊权重
     * <p>优先使用SKU标准产能（standardCapacity），缺失时回退到日硫化量（dayVulcanizationQty）</p>
     *
     * @param factoryCode 工厂编码
     * @param plan        月计划
     * @return 分摊权重
     */
    private int resolveEmbryoAllocationWeight(String factoryCode, FactoryMonthPlanProductionFinalResult plan) {
        if (plan == null || StringUtils.isBlank(plan.getMaterialCode())) {
            return 0;
        }
        LambdaQueryWrapper<MdmSkuLhCapacity> capacityWrapper = new LambdaQueryWrapper<>();
        capacityWrapper.eq(MdmSkuLhCapacity::getMaterialCode, plan.getMaterialCode());
        if (StringUtils.isNotBlank(factoryCode)) {
            capacityWrapper.eq(MdmSkuLhCapacity::getFactoryCode, factoryCode);
        }
        capacityWrapper.last("LIMIT 1");
        MdmSkuLhCapacity capacity = mdmSkuLhCapacityMapper.selectOne(capacityWrapper);
        if (capacity != null && capacity.getStandardCapacity() != null && capacity.getStandardCapacity() > 0) {
            return capacity.getStandardCapacity();
        }
        return plan.getDayVulcanizationQty() != null ? plan.getDayVulcanizationQty() : 0;
    }

    /**
     * 检查物料编码使用模具是否可用
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void checkMouldAvailability(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode)) {
            return;
        }

        LambdaQueryWrapper<MdmSkuMouldRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmSkuMouldRel::getMaterialCode, materialCode);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(MdmSkuMouldRel::getFactoryCode, dto.getFactoryCode());
        }
        List<MdmSkuMouldRel> mouldRelList = mdmSkuMouldRelMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(mouldRelList)) {
            result.addWarning(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.mouldRelNotConfigured"), materialCode));
            return;
        }

        List<String> unavailableMoulds = mouldRelList.stream()
                .map(MdmSkuMouldRel::getMouldCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .filter(mouldCode -> !isMouldEnabled(mouldCode))
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(unavailableMoulds)) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.mouldUnavailable"), materialCode, String.join(",", unavailableMoulds)));
        }
    }

    /**
     * 判断模具是否可用
     * <p>查询模具台账表（T_MDM_MOULD_INFO），根据mouldStatus判断模具是否可用</p>
     * <p>mouldStatus=1表示可用，其他值表示不可用</p>
     *
     * @param mouldCode 模具编号
     * @return true-可用，false-不可用
     */
    private boolean isMouldEnabled(String mouldCode) {
        if (StringUtils.isBlank(mouldCode)) {
            return false;
        }
        LambdaQueryWrapper<MdmModelInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmModelInfo::getMouldCode, mouldCode)
                .eq(MdmModelInfo::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1");
        MdmModelInfo mouldInfo = mdmModelInfoMapper.selectOne(wrapper);
        if (mouldInfo == null) {
            return false;
        }
        return mouldInfo.getMouldStatus() != null && mouldInfo.getMouldStatus() == MOULD_STATUS_AVAILABLE;
    }

    /**
     * 计算插单总量
     *
     * @param dto 插单数据
     * @return 插单总量
     */
    private int calculateTotalInsertQty(LhOrderInsertDTO dto) {
        int total = 0;
        if (dto.getClass1PlanQty() != null) total += dto.getClass1PlanQty();
        if (dto.getClass2PlanQty() != null) total += dto.getClass2PlanQty();
        if (dto.getClass3PlanQty() != null) total += dto.getClass3PlanQty();
        if (dto.getClass4PlanQty() != null) total += dto.getClass4PlanQty();
        if (dto.getClass5PlanQty() != null) total += dto.getClass5PlanQty();
        if (dto.getClass6PlanQty() != null) total += dto.getClass6PlanQty();
        if (dto.getClass7PlanQty() != null) total += dto.getClass7PlanQty();
        if (dto.getClass8PlanQty() != null) total += dto.getClass8PlanQty();
        return total;
    }

    /**
     * 根据班次索引获取计划量
     *
     * @param dto        插单数据
     * @param shiftIndex 班次索引（1-8）
     * @return 计划量
     */
    private Integer getPlanQtyByShiftIndex(LhOrderInsertDTO dto, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return dto.getClass1PlanQty();
            case 2: return dto.getClass2PlanQty();
            case 3: return dto.getClass3PlanQty();
            case 4: return dto.getClass4PlanQty();
            case 5: return dto.getClass5PlanQty();
            case 6: return dto.getClass6PlanQty();
            case 7: return dto.getClass7PlanQty();
            case 8: return dto.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 解析当前时间所在的班次索引
     *
     * @param shifts 班次列表
     * @param now    当前时间
     * @return 当前班次索引，未匹配返回-1
     */
    private int resolveCurrentShiftIndex(List<LhShiftConfigVO> shifts, Date now) {
        for (LhShiftConfigVO shift : shifts) {
            Date start = shift.getShiftStartDateTime();
            Date end = shift.getShiftEndDateTime();
            if (start != null && end != null && !now.before(start) && now.before(end)) {
                return shift.getShiftIndex();
            }
        }
        return -1;
    }

    /**
     * 根据班次索引查找班次配置
     *
     * @param shifts     班次列表
     * @param shiftIndex 班次索引
     * @return 班次配置
     */
    private LhShiftConfigVO findShiftByIndex(List<LhShiftConfigVO> shifts, int shiftIndex) {
        for (LhShiftConfigVO shift : shifts) {
            if (shift.getShiftIndex() != null && shift.getShiftIndex() == shiftIndex) {
                return shift;
            }
        }
        return null;
    }

    /**
     * 获取班次名称
     *
     * @param shifts     班次列表
     * @param shiftIndex 班次索引
     * @return 班次名称
     */
    private String getShiftName(List<LhShiftConfigVO> shifts, int shiftIndex) {
        LhShiftConfigVO shift = findShiftByIndex(shifts, shiftIndex);
        if (shift != null && StringUtils.isNotBlank(shift.getShiftName())) {
            return shift.getShiftName();
        }
        return String.format("第%d班", shiftIndex);
    }

    /**
     * 根据机台编码解析左右模标识
     * 机台编码以L结尾为左模，以R结尾为右模
     *
     * @param machineCode 机台编码
     * @return 左右模标识（L-左模，R-右模），无法解析时返回null
     */
    private String resolveLeftRightMould(String machineCode) {
        if (StringUtils.isBlank(machineCode) || machineCode.length() < 2) {
            return null;
        }
        String lastChar = machineCode.substring(machineCode.length() - 1).toUpperCase();
        if ("L".equals(lastChar)) {
            return "L";
        }
        if ("R".equals(lastChar)) {
            return "R";
        }
        return null;
    }

    /**
     * 填充SKU关联数据（示方类型等）
     * <p>在所有校验完成后统一填充，避免校验中断导致数据缺失</p>
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void fillSkuRelatedData(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        fillConstructionStage(dto, result);
    }

    /**
     * 填充示方类型
     * <p>根据SKU与施工（示方书）关系表，查询硫化示方类型（lhType），直接使用字典 lh_trial_status 的值</p>
     * <p>字典值：S-正规示方，T-量试示方，X-试验示方</p>
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void fillConstructionStage(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isAnyBlank(dto.getFactoryCode(), materialCode)) {
            return;
        }
        MdmSkuConstructionRef skuConstructionRef = Optional.ofNullable(mdmSkuConstructionRefEntityMapper.selectList(
                        new LambdaQueryWrapper<MdmSkuConstructionRef>()
                                .eq(MdmSkuConstructionRef::getFactoryCode, dto.getFactoryCode())
                                .eq(MdmSkuConstructionRef::getMaterialCode, materialCode)
                                .eq(MdmSkuConstructionRef::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .orderByAsc(MdmSkuConstructionRef::getId)))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
        if (Objects.nonNull(skuConstructionRef) && StringUtils.isNotBlank(skuConstructionRef.getLhType())) {
            result.setTrialStatus(skuConstructionRef.getLhType());
        }
    }
}

package com.zlt.aps.lh.handler;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.LhInsertOrderValidateResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.mapper.CxStockMapper;
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.mapper.LhMachineInfoMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhScheFinishQtyMapper;
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
import java.math.BigDecimal;
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

    @Resource
    private LhDayFinishQtyMapper lhDayFinishQtyMapper;

    @Resource
    private LhScheFinishQtyMapper lhScheFinishQtyMapper;

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
     * 获取SKU关联数据（硫化余量/胎胚库存/硫化班产/示方类型/胎胚描述等）
     * <p>用于插单页面选择新物料时实时获取关联信息，不进行业务校验</p>
     *
     * @param dto 包含factoryCode、materialCode、scheduleDate的请求对象
     * @return SKU关联数据（包含mouldSurplusQty、embryoStock、machineShiftCapacity、trialStatus、leftRightMould、embryoCode等字段）
     */
    public LhInsertOrderValidateResultDTO getSkuRelatedData(LhOrderInsertDTO dto) {
        LhInsertOrderValidateResultDTO result = new LhInsertOrderValidateResultDTO();
        result.setValid(true);
        checkMachineAvailability(dto, result);
        checkMouldSurplus(dto, result);
        checkMouldAvailability(dto, result);
        fillSkuRelatedData(dto, result);
        fillEmbryoRelatedFields(dto, result);
        // 左右模处理逻辑：
        // 1. 前端传了leftRightMold且非空，优先使用前端值（用户手动选择或原值带入）
        // 2. checkMachineAvailability已设置（单模机台场景），保留该值
        // 3. 前端未传且checkMachineAvailability未设置，从机台编码解析
        // 4. 机台编码也无法解析时，查询机台信息判断是否双模机台，双模默认"LR"
        if (StringUtils.isNotBlank(dto.getLeftRightMold())) {
            result.setLeftRightMould(dto.getLeftRightMold());
        } else if (StringUtils.isBlank(result.getLeftRightMould())) {
            String lrMould = resolveLeftRightMould(dto.getLhMachineCode());
            if (StringUtils.isNotBlank(lrMould)) {
                result.setLeftRightMould(lrMould);
            } else {
                LhMachineInfo machineInfo = getMachineInfoByCode(dto.getLhMachineCode(), dto.getFactoryCode());
                if (machineInfo != null && machineInfo.getMaxMoldNum() != null && machineInfo.getMaxMoldNum() > 1) {
                    result.setLeftRightMould("LR");
                }
            }
        }
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
            // 当前时间不在任何班次范围内（班次间隙），取最近已结束的班次索引作为参考
            // 最近已结束的班次 = 结束时间 <= 当前时间 且 结束时间最晚的班次
            currentShiftIndex = resolveLastEndedShiftIndex(shifts, now);
            if (currentShiftIndex < 0) {
                return;
            }
        }

        for (int i = 1; i < currentShiftIndex; i++) {
            Integer planQty = getPlanQtyByShiftIndex(dto, i);
            if (planQty != null && planQty > 0) {
                LhShiftConfigVO shift = findShiftByIndex(shifts, i);
                String shiftName = shift != null ? shift.getShiftName() : String.format("第%d班", i);
                result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.historicalShift"), shiftName));
            }
        }

        // 当前班次允许插单，但需校验插单量是否超过剩余产能
        Integer currentPlanQty = getPlanQtyByShiftIndex(dto, currentShiftIndex);
        if (currentPlanQty != null && currentPlanQty > 0 && StringUtils.isNotBlank(dto.getLhMachineCode())) {
            int remainingCapacity = calculateShiftRemainingCapacity(dto, currentShiftIndex);
            if (remainingCapacity < currentPlanQty) {
                LhShiftConfigVO shift = findShiftByIndex(shifts, currentShiftIndex);
                String shiftName = shift != null ? shift.getShiftName() : String.format("第%d班", currentShiftIndex);
                result.addError(String.format(
                        I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.currentShiftOverCapacity"),
                        shiftName, remainingCapacity, currentPlanQty));
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
     * <p>硫化余量 = 生产实际排产量(totalQty) - 硫化产量今天夜班完成量</p>
     * <p>硫化产量今天夜班完成量 = 本月1日至昨天的日完成量(LhDayFinishQty.DAY_FINISH_QTY) + 今天的夜班完成量(LhScheFinishQty.CLASS1_FINISH_QTY)</p>
     * <p>注意：页面排程日期为T+2，"今天"指当前实际日期，而非排程日期</p>
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

        int totalQty = monthPlan.getTotalQty() != null ? monthPlan.getTotalQty() : 0;
        int todayNightFinishQty = calculateTodayNightFinishQty(dto.getFactoryCode(), materialCode, scheduleDate);
        // 上月超欠产量：仅当有效标志为"1"时参与计算，否则按0处理
        int lastMonthOverdue = 0;
        if ("1".equals(monthPlan.getLastMonthValidFlag())
                && Objects.nonNull(monthPlan.getLastMonthOverdueQty())) {
            lastMonthOverdue = monthPlan.getLastMonthOverdueQty();
        }
        // 硫化余量 = MAX(月计划总量 - 已完成量 + 上月超欠产量, 0)
        int surplusQty = Math.max(0, totalQty - todayNightFinishQty + lastMonthOverdue);
        result.setMouldSurplusQty(surplusQty);

        fillEmbryoStock(dto, result, monthPlan, year, month, finalVersion);

        int totalInsertQty = calculateTotalInsertQty(dto);
        if (totalInsertQty > surplusQty) {
            result.addError(String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.insertOrder.mouldSurplusExceeded"), surplusQty, totalInsertQty, (totalInsertQty - surplusQty)));
        }
    }

    /**
     * 计算硫化产量今天夜班完成量
     * <p>计算逻辑：从本月1日到昨天(含)的日完成量(LhDayFinishQty.DAY_FINISH_QTY)汇总
     * + 今天的夜班完成量(LhScheFinishQty.CLASS1_FINISH_QTY)汇总</p>
     * <p>注意：页面排程日期为T+2，"今天"指当前实际日期，而非排程日期。
     * 例如排程日期为5月27日，今天是5月25日，则"昨天"是5月24日，"今天"是5月25日。</p>
     *
     * @param factoryCode  工厂编码
     * @param materialCode 物料编码
     * @param scheduleDate 排程日期（用于确定所属月份，计算月初起点）
     * @return 今天夜班完成量
     */
    private int calculateTodayNightFinishQty(String factoryCode, String materialCode, Date scheduleDate) {
        Date today = DateUtil.beginOfDay(new Date());
        Date yesterday = DateUtil.offsetDay(today, -1);
        Date monthStart = DateUtil.beginOfMonth(scheduleDate);
        Date nextDayOfToday = DateUtil.offsetDay(today, 1);

        int dayFinishSum = 0;
        if (!yesterday.before(monthStart)) {
            LambdaQueryWrapper<LhDayFinishQty> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.eq(LhDayFinishQty::getFactoryCode, factoryCode)
                    .eq(LhDayFinishQty::getMaterialCode, materialCode)
                    .ge(LhDayFinishQty::getFinishDate, monthStart)
                    .lt(LhDayFinishQty::getFinishDate, today);
            List<LhDayFinishQty> dayFinishList = lhDayFinishQtyMapper.selectList(dayWrapper);
            for (LhDayFinishQty item : dayFinishList) {
                dayFinishSum += item.getDayFinishQty() != null ? item.getDayFinishQty().intValue() : 0;
            }
        }

        int scheClass1Sum = 0;
        LambdaQueryWrapper<LhScheFinishQty> scheWrapper = new LambdaQueryWrapper<>();
        scheWrapper.eq(LhScheFinishQty::getFactoryCode, factoryCode)
                .eq(LhScheFinishQty::getMaterialCode, materialCode)
                .ge(LhScheFinishQty::getScheduleDate, today)
                .lt(LhScheFinishQty::getScheduleDate, nextDayOfToday);
        List<LhScheFinishQty> scheFinishList = lhScheFinishQtyMapper.selectList(scheWrapper);
        for (LhScheFinishQty item : scheFinishList) {
            scheClass1Sum += item.getClass1FinishQty() != null ? item.getClass1FinishQty().intValue() : 0;
        }

        int totalFinishQty = dayFinishSum + scheClass1Sum;
        log.debug("计算硫化产量今天夜班完成量, factoryCode: {}, materialCode: {}, 今天: {}, "
                        + "日完成量汇总(月初~昨天): {}, 今天夜班完成量: {}, 合计: {}",
                factoryCode, materialCode, DateUtil.formatDate(today),
                dayFinishSum, scheClass1Sum, totalFinishQty);
        return totalFinishQty;
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
     * 填充胎胚库存（按共用胎胚分摊公式计算，使用最大余额法确保分配总和等于总量）
     * <p>SKU分配的胎胚库存量 = (SKU日硫化量 / 同胎胚的所有SKU日硫化量汇总) * 胎胚库存量</p>
     * <p>分摊权重优先使用日硫化量（dayVulcanizationQty），缺失时回退到SKU标准产能（standardCapacity）</p>
     * <p>最大余额法：先按比例取整数部分，再将未分配完的余量按余数从大到小依次补1，保证分配总和等于胎胚库存总量</p>
     * <p>注意：页面排程日期为T+2，胎胚库存查询使用T日（当前实际日期），而非排程日期</p>
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
        Date today = DateUtil.beginOfDay(new Date());
        Date todayEnd = DateUtil.endOfDay(new Date());
        stockWrapper.ge(CxStock::getStockDate, today)
                    .le(CxStock::getStockDate, todayEnd);
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

        int currentWeight = resolveEmbryoAllocationWeight(dto.getFactoryCode(), monthPlan);
        int embryoWeightSum = 0;
        for (FactoryMonthPlanProductionFinalResult plan : sameEmbryoPlans) {
            embryoWeightSum += resolveEmbryoAllocationWeight(dto.getFactoryCode(), plan);
        }

        if (currentWeight <= 0 || embryoWeightSum <= 0) {
            result.setEmbryoStock(embryoTotalStock);
            return;
        }

        int allocatedStock = allocateByLargestRemainderMethod(
                embryoTotalStock, sameEmbryoPlans, monthPlan.getMaterialCode(), dto.getFactoryCode());
        log.debug("同胎胚库存按最大余额法分摊, materialCode: {}, embryoCode: {}, currentWeight: {}, "
                        + "embryoWeightSum: {}, embryoTotalStock: {}, allocatedStock: {}",
                monthPlan.getMaterialCode(), embryoCode, currentWeight,
                embryoWeightSum, embryoTotalStock, allocatedStock);
        result.setEmbryoStock(allocatedStock);
    }

    /**
     * 最大余额法分配胎胚库存
     * <p>步骤：</p>
     * <ol>
     *   <li>按权重比例计算每个SKU的整数分配量（截断）</li>
     *   <li>计算每个SKU的余数（未分配的小数部分）</li>
     *   <li>将未分配完的余量（总量 - 整数分配总和）按余数从大到小依次补1</li>
     * </ol>
     * <p>保证所有SKU分配量之和等于胎胚库存总量</p>
     *
     * @param embryoTotalStock 胎胚库存总量
     * @param sameEmbryoPlans  同胎胚的所有月计划SKU列表
     * @param targetMaterialCode 当前查询的物料编码
     * @param factoryCode      工厂编码
     * @return 当前SKU分配的胎胚库存量
     */
    private int allocateByLargestRemainderMethod(int embryoTotalStock,
                                                  List<FactoryMonthPlanProductionFinalResult> sameEmbryoPlans,
                                                  String targetMaterialCode,
                                                  String factoryCode) {
        int weightSum = 0;
        List<AllocationItem> items = new ArrayList<>(sameEmbryoPlans.size());
        for (FactoryMonthPlanProductionFinalResult plan : sameEmbryoPlans) {
            int weight = resolveEmbryoAllocationWeight(factoryCode, plan);
            weightSum += weight;
            items.add(new AllocationItem(plan.getMaterialCode(), weight));
        }

        if (weightSum <= 0) {
            return embryoTotalStock;
        }

        int allocatedSum = 0;
        for (AllocationItem item : items) {
            item.integerPart = (int) ((long) embryoTotalStock * item.weight / weightSum);
            item.remainder = (long) embryoTotalStock * item.weight % weightSum;
            allocatedSum += item.integerPart;
        }

        int remainder = embryoTotalStock - allocatedSum;
        items.sort((a, b) -> Long.compare(b.remainder, a.remainder));
        for (int i = 0; i < remainder && i < items.size(); i++) {
            items.get(i).integerPart += 1;
        }

        int result = embryoTotalStock;
        for (AllocationItem item : items) {
            if (StringUtils.equals(item.materialCode, targetMaterialCode)) {
                result = item.integerPart;
                break;
            }
        }
        return result;
    }

    /**
     * 胎胚库存分配项（最大余额法内部使用）
     */
    private static class AllocationItem {
        String materialCode;
        int weight;
        int integerPart;
        long remainder;

        AllocationItem(String materialCode, int weight) {
            this.materialCode = materialCode;
            this.weight = weight;
            this.integerPart = 0;
            this.remainder = 0;
        }
    }

    /**
     * 解析胎胚库存分摊权重
     * <p>优先使用日硫化量（dayVulcanizationQty），缺失时回退到SKU标准产能（standardCapacity）</p>
     *
     * @param factoryCode 工厂编码
     * @param plan        月计划
     * @return 分摊权重
     */
    private int resolveEmbryoAllocationWeight(String factoryCode, FactoryMonthPlanProductionFinalResult plan) {
        if (plan == null || StringUtils.isBlank(plan.getMaterialCode())) {
            return 0;
        }
        if (plan.getDayVulcanizationQty() != null && plan.getDayVulcanizationQty() > 0) {
            return plan.getDayVulcanizationQty();
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
        return 0;
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
     * 计算指定班次在当前机台上的剩余产能
     * <p>剩余产能 = 机台班产 - 该班次已排计划量</p>
     *
     * @param dto        插单数据
     * @param shiftIndex 班次索引（1-8）
     * @return 剩余产能
     */
    private int calculateShiftRemainingCapacity(LhOrderInsertDTO dto, int shiftIndex) {
        Integer shiftCapacity = getMachineShiftCapacity(dto);
        if (shiftCapacity == null || shiftCapacity <= 0) {
            return 0;
        }

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

        int scheduledQty = 0;
        for (LhScheduleResult existing : existingResults) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(existing, shiftIndex);
            scheduledQty += (planQty != null ? planQty : 0);
        }

        return Math.max(0, shiftCapacity - scheduledQty);
    }

    /**
     * 获取机台班产
     * <p>优先从机台主数据获取quota，若为空则从SKU硫化产能主数据获取classCapacity</p>
     *
     * @param dto 插单数据
     * @return 机台班产
     */
    private Integer getMachineShiftCapacity(LhOrderInsertDTO dto) {
        if (StringUtils.isBlank(dto.getLhMachineCode())) {
            return null;
        }

        LambdaQueryWrapper<LhMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
        machineWrapper.eq(LhMachineInfo::getMachineCode, dto.getLhMachineCode());
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            machineWrapper.eq(LhMachineInfo::getFactoryCode, dto.getFactoryCode());
        }
        machineWrapper.last("LIMIT 1");
        LhMachineInfo machineInfo = lhMachineInfoMapper.selectOne(machineWrapper);

        if (machineInfo != null && machineInfo.getQuota() != null && machineInfo.getQuota() > 0) {
            return machineInfo.getQuota();
        }

        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isNotBlank(materialCode)) {
            LambdaQueryWrapper<MdmSkuLhCapacity> capacityWrapper = new LambdaQueryWrapper<>();
            capacityWrapper.eq(MdmSkuLhCapacity::getMaterialCode, materialCode);
            if (StringUtils.isNotBlank(dto.getFactoryCode())) {
                capacityWrapper.eq(MdmSkuLhCapacity::getFactoryCode, dto.getFactoryCode());
            }
            capacityWrapper.last("LIMIT 1");
            MdmSkuLhCapacity skuCapacity = mdmSkuLhCapacityMapper.selectOne(capacityWrapper);
            if (skuCapacity != null && skuCapacity.getClassCapacity() != null) {
                return skuCapacity.getClassCapacity();
            }
        }

        return null;
    }

    /**
     * 解析最近已结束的班次索引（用于班次间隙场景）
     * <p>当当前时间不在任何班次范围内时，找到结束时间 <= 当前时间且最晚的班次，
     * 该班次之后（不含）的班次为历史班次，不允许插单</p>
     *
     * @param shifts 班次列表
     * @param now    当前时间
     * @return 最近已结束的班次索引，无匹配返回-1
     */
    private int resolveLastEndedShiftIndex(List<LhShiftConfigVO> shifts, Date now) {
        int lastEndedIndex = -1;
        Date latestEndTime = null;
        for (LhShiftConfigVO shift : shifts) {
            Date end = shift.getShiftEndDateTime();
            if (end != null && !now.before(end)) {
                if (latestEndTime == null || end.after(latestEndTime)) {
                    latestEndTime = end;
                    lastEndedIndex = shift.getShiftIndex();
                }
            }
        }
        // 返回下一个班次索引，因为最近已结束的班次本身也不应再插单
        return lastEndedIndex >= 0 ? lastEndedIndex + 1 : -1;
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
     * 根据机台编码查询机台信息
     *
     * @param machineCode 机台编码
     * @param factoryCode 工厂编码
     * @return 机台信息
     */
    private LhMachineInfo getMachineInfoByCode(String machineCode, String factoryCode) {
        if (StringUtils.isBlank(machineCode)) {
            return null;
        }
        LambdaQueryWrapper<LhMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhMachineInfo::getMachineCode, machineCode);
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(LhMachineInfo::getFactoryCode, factoryCode);
        }
        wrapper.last("LIMIT 1");
        return lhMachineInfoMapper.selectOne(wrapper);
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
        } else {
            // 新SKU没有示方类型时，显式设置为空字符串，确保前端能区分"后端未返回"和"新SKU无示方类型"
            result.setTrialStatus("");
        }
    }

    /**
     * 填充胎胚关联字段（胎胚代码/胎胚描述/需求计划版本号/排产版本号/规格/结构/模具号）
     * <p>从月计划定稿表中根据工厂+年月+排产版本+物料编码查询关联字段</p>
     *
     * @param dto    插单数据
     * @param result 校验结果
     */
    private void fillEmbryoRelatedFields(LhOrderInsertDTO dto, LhInsertOrderValidateResultDTO result) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isAnyBlank(dto.getFactoryCode(), materialCode) || dto.getScheduleDate() == null) {
            return;
        }

        cn.hutool.core.date.DateTime scheduleDate = cn.hutool.core.date.DateUtil.date(dto.getScheduleDate());
        int year = cn.hutool.core.date.DateUtil.year(scheduleDate);
        int month = cn.hutool.core.date.DateUtil.month(scheduleDate) + 1;

        MpFactoryProductionVersion finalVersion = getFinalProductionVersion(dto.getFactoryCode(), year, month);

        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, dto.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        if (finalVersion != null && StringUtils.isNotBlank(finalVersion.getProductionVersion())) {
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalVersion.getProductionVersion());
        }
        wrapper.last("LIMIT 1");
        FactoryMonthPlanProductionFinalResult monthPlan = monthPlanMapper.selectOne(wrapper);

        if (monthPlan != null) {
            if (StringUtils.isNotBlank(monthPlan.getEmbryoCode())) {
                result.setEmbryoCode(monthPlan.getEmbryoCode());
            }
            if (StringUtils.isNotBlank(monthPlan.getMainMaterialDesc())) {
                result.setMainMaterialDesc(monthPlan.getMainMaterialDesc());
            }
            if (StringUtils.isNotBlank(monthPlan.getMonthPlanVersion())) {
                result.setMonthPlanVersion(monthPlan.getMonthPlanVersion());
            }
            if (StringUtils.isNotBlank(monthPlan.getProductionVersion())) {
                result.setProductionVersion(monthPlan.getProductionVersion());
            }
            if (StringUtils.isNotBlank(monthPlan.getSpecifications())) {
                result.setSpecCode(monthPlan.getSpecifications());
            }
            if (StringUtils.isNotBlank(monthPlan.getStructureName())) {
                result.setStructureName(monthPlan.getStructureName());
            }
        }

        String mouldCode = resolveMouldCodeForRelatedData(dto);
        if (StringUtils.isNotBlank(mouldCode)) {
            result.setMouldCode(mouldCode);
        }
    }

    /**
     * 根据物料编码查询模具号（用于getSkuRelatedData场景）
     *
     * @param dto 插单数据
     * @return 模具号，多个以逗号分隔
     */
    private String resolveMouldCodeForRelatedData(LhOrderInsertDTO dto) {
        String materialCode = resolveMaterialCode(dto);
        if (StringUtils.isBlank(materialCode)) {
            return null;
        }
        LambdaQueryWrapper<MdmSkuMouldRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmSkuMouldRel::getMaterialCode, materialCode);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(MdmSkuMouldRel::getFactoryCode, dto.getFactoryCode());
        }
        List<MdmSkuMouldRel> mouldRelList = mdmSkuMouldRelMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(mouldRelList)) {
            return null;
        }
        return mouldRelList.stream()
                .map(MdmSkuMouldRel::getMouldCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }
}

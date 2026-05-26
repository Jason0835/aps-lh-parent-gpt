package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.domain.dto.LhInsertOrderValidateResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.LhBatchNoRedisGenerator;
import com.zlt.aps.lh.handler.LhInsertOrderValidateHandler;
import com.zlt.aps.lh.mapper.CxLhScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.MdmSkuMouldRelMapper;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.lh.service.ILhScheduleResultService;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import cn.hutool.core.date.DateUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 硫化排程结果服务实现
 *
 * @author APS
 */
@Slf4j
@Service
public class LhScheduleResultServiceImpl implements ILhScheduleResultService {

    @Resource
    private LhScheduleResultMapper mapper;

    @Resource
    private CxLhScheduleResultMapper cxLhScheduleResultMapper;

    @Resource
    private LhBatchNoRedisGenerator batchNoRedisGenerator;

    @Resource
    private LhInsertOrderValidateHandler insertOrderValidateHandler;

    @Resource
    private LhMouldChangePlanEntityMapper mouldChangePlanMapper;

    @Resource
    private MdmSkuMouldRelMapper mdmSkuMouldRelMapper;

    @Resource
    private FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Resource
    private MpFactoryProductionVersionMapper mpFactoryProductionVersionMapper;

    private static final AtomicInteger INSERT_ORDER_SEQ = new AtomicInteger(0);

    @Override
    public List<LhScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode) {
        return mapper.selectList(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(StringUtils.isNotEmpty(factoryCode), LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    @Override
    public List<LhScheduleResult> selectPreviousSchedule(Date scheduleDate, String factoryCode) {
        return mapper.selectList(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    @Override
    public int deleteByDateAndFactory(Date scheduleDate, String factoryCode) {
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        return mapper.delete(wrapper);
    }

    @Override
    public int insertBatch(List<LhScheduleResult> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return mapper.insertBatch(list);
    }

    @Override
    public int countReleasedByDate(Date scheduleDate, String factoryCode) {
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getFactoryCode, factoryCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .eq(LhScheduleResult::getIsRelease, ReleaseStatusEnum.RELEASED.getCode())
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public String generateNextBatchNo(Date scheduleDate, String factoryCode) {
        return batchNoRedisGenerator.nextBatchNo(scheduleDate, factoryCode);
    }

    @Override
    public void updateReleaseStatus(LhScheduleResult item) {
        LhScheduleResult updateEntity = new LhScheduleResult();
        updateEntity.setId(item.getId());
        updateEntity.setIsRelease(item.getIsRelease());
        mapper.updateById(updateEntity);
    }

    @Override
    public List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> getCxLhScheduleResultList(Date scheduleDate) {
        LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, scheduleDate);
        return cxLhScheduleResultMapper.selectList(wrapper);
    }

    @Override
    public List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> getCxLhScheduleResultListByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getId, ids);
        return cxLhScheduleResultMapper.selectList(wrapper);
    }

    @Override
    public LhInsertOrderValidateResultDTO validateInsertOrder(LhOrderInsertDTO dto) {
        return insertOrderValidateHandler.validateInsertOrder(dto);
    }

    @Override
    public LhInsertOrderValidateResultDTO getSkuRelatedData(LhOrderInsertDTO dto) {
        return insertOrderValidateHandler.getSkuRelatedData(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertOrder(LhOrderInsertDTO dto) {
        log.info("执行插单操作, 工厂: {}, 机台: {}, 物料: {}, 排程日期: {}",
                dto.getFactoryCode(), dto.getLhMachineCode(), dto.getProductCode(), dto.getScheduleDate());

        LhInsertOrderValidateResultDTO validateResult = insertOrderValidateHandler.validateInsertOrder(dto);

        // 先查询前规格物料信息（必须在插入新排程结果之前查询，否则会查到刚插入的自身记录）
        String beforeMaterialCode = null;
        String beforeMaterialDesc = null;
        LhScheduleResult prevResult = queryPrevScheduleResult(dto);
        if (prevResult != null) {
            beforeMaterialCode = prevResult.getMaterialCode();
            beforeMaterialDesc = prevResult.getMaterialDesc();
        }

        String batchNo = generateNextBatchNo(dto.getScheduleDate(), dto.getFactoryCode());
        String orderNo = generateInsertOrderNo(dto.getScheduleDate());

        LhScheduleResult result = buildInsertOrderResult(dto, batchNo, orderNo, validateResult);
        mapper.insert(result);

        generateInsertMouldChangePlan(dto, batchNo, beforeMaterialCode, beforeMaterialDesc);

        // TODO 同步触发成型机台均衡
        log.info("插单操作完成, 工单号: {}, 批次号: {}", orderNo, batchNo);
    }

    /**
     * 查询插单机台在当前排程日期下的前规格物料信息（作为模具交替计划的前规格参考）
     * <p>查询优先级：</p>
     * <ol>
     *   <li>同机台+同排程日期下，非插单来源（dataSource != '1'）的排程结果，取最新一条</li>
     *   <li>若无非插单记录，则取同机台+同排程日期下所有排程结果中最新的一条</li>
     *   <li>若当前排程日期无任何记录，则查询前一天该机台最后一条排程结果</li>
     * </ol>
     *
     * @param dto 插单请求数据
     * @return 前规格排程结果，不存在返回null
     */
    private LhScheduleResult queryPrevScheduleResult(LhOrderInsertDTO dto) {
        // 优先查询非插单来源的排程结果，确保前规格是机台原始排程的规格
        LhScheduleResult originalResult = queryLatestScheduleResult(dto, false);
        if (originalResult != null) {
            return originalResult;
        }

        // 当前排程日期无非插单记录，查询所有记录（含插单）中最新的一条
        LhScheduleResult anyResult = queryLatestScheduleResult(dto, true);
        if (anyResult != null) {
            return anyResult;
        }

        // 当前排程日期无任何记录，查询前一天该机台最后一条排程结果
        return queryPrevDayLastScheduleResult(dto);
    }

    /**
     * 查询同机台+同排程日期下最新的排程结果
     *
     * @param dto          插单请求数据
     * @param includeInsert 是否包含插单来源的记录
     * @return 最新的排程结果，不存在返回null
     */
    private LhScheduleResult queryLatestScheduleResult(LhOrderInsertDTO dto, boolean includeInsert) {
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getLhMachineCode, dto.getLhMachineCode())
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .eq(LhScheduleResult::getScheduleDate, dto.getScheduleDate());
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        if (!includeInsert) {
            wrapper.ne(LhScheduleResult::getDataSource, "1");
        }
        wrapper.orderByDesc(LhScheduleResult::getCreateTime);
        wrapper.last("LIMIT 1");
        return mapper.selectOne(wrapper);
    }

    /**
     * 查询前一天该机台最后一条排程结果（作为前规格的兜底参考）
     *
     * @param dto 插单请求数据
     * @return 前一天最后的排程结果，不存在返回null
     */
    private LhScheduleResult queryPrevDayLastScheduleResult(LhOrderInsertDTO dto) {
        if (dto.getScheduleDate() == null) {
            return null;
        }
        Date prevDay = DateUtil.offsetDay(dto.getScheduleDate(), -1);
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhScheduleResult::getLhMachineCode, dto.getLhMachineCode())
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .eq(LhScheduleResult::getScheduleDate, prevDay);
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            wrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        wrapper.orderByDesc(LhScheduleResult::getCreateTime);
        wrapper.last("LIMIT 1");
        return mapper.selectOne(wrapper);
    }

    /**
     * 构建插单排程结果实体
     *
     * @param dto            插单请求数据
     * @param batchNo        批次号
     * @param orderNo        工单号
     * @param validateResult 校验结果（用于回填硫化余量/胎胚库存/班产/示方类型等字段）
     * @return 排程结果实体
     */
    private LhScheduleResult buildInsertOrderResult(LhOrderInsertDTO dto, String batchNo, String orderNo,
                                                    LhInsertOrderValidateResultDTO validateResult) {
        LhScheduleResult result = new LhScheduleResult();
        result.setFactoryCode(dto.getFactoryCode());
        result.setBatchNo(batchNo);
        result.setOrderNo(orderNo);
        result.setLhMachineCode(dto.getLhMachineCode());
        result.setLhMachineName(dto.getLhMachineName());
        String materialCode = StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();
        result.setMaterialCode(materialCode);
        result.setMaterialDesc(dto.getMaterialDesc());
        result.setSpecCode(dto.getSpecCode());
        result.setScheduleDate(dto.getScheduleDate());
        result.setLeftRightMould(dto.getLeftRightMold());
        result.setIsDelivery(dto.getIsDelivery());
        result.setDataSource("1");
        result.setIsRelease(ReleaseStatusEnum.PENDING_RELEASE.getCode());
        result.setIsDelete(DeleteFlagEnum.NORMAL.getCode());
        result.setScheduleType("02");
        result.setIsChangeMould("1");

        if (validateResult != null) {
            if (validateResult.getMouldSurplusQty() != null) {
                result.setMouldSurplusQty(validateResult.getMouldSurplusQty());
            }
            if (validateResult.getEmbryoStock() != null) {
                result.setEmbryoStock(validateResult.getEmbryoStock());
            }
            if (validateResult.getSingleMouldShiftQty() != null) {
                result.setSingleMouldShiftQty(validateResult.getSingleMouldShiftQty());
            }
            if (StringUtils.isNotBlank(validateResult.getTrialStatus())) {
                result.setTrialStatus(validateResult.getTrialStatus());
                result.setChangedTrialStatus(validateResult.getTrialStatus());
            } else if (StringUtils.isNotBlank(dto.getOriginalTrialStatus())) {
                result.setTrialStatus(dto.getOriginalTrialStatus());
                result.setChangedTrialStatus(dto.getOriginalTrialStatus());
            }
            if (StringUtils.isNotBlank(validateResult.getLeftRightMould())) {
                result.setLeftRightMould(validateResult.getLeftRightMould());
            }
        }

        fillEmbryoRelatedFields(result, dto);

        result.setClass1PlanQty(dto.getClass1PlanQty());
        result.setClass1StartTime(dto.getClass1StartTime());
        result.setClass1EndTime(dto.getClass1EndTime());
        result.setClass1Analysis(dto.getClass1Analysis());

        result.setClass2PlanQty(dto.getClass2PlanQty());
        result.setClass2StartTime(dto.getClass2StartTime());
        result.setClass2EndTime(dto.getClass2EndTime());
        result.setClass2Analysis(dto.getClass2Analysis());

        result.setClass3PlanQty(dto.getClass3PlanQty());
        result.setClass3StartTime(dto.getClass3StartTime());
        result.setClass3EndTime(dto.getClass3EndTime());
        result.setClass3Analysis(dto.getClass3Analysis());

        result.setClass4PlanQty(dto.getClass4PlanQty());
        result.setClass4StartTime(dto.getClass4StartTime());
        result.setClass4EndTime(dto.getClass4EndTime());
        result.setClass4Analysis(dto.getClass4Analysis());

        result.setClass5PlanQty(dto.getClass5PlanQty());
        result.setClass5StartTime(dto.getClass5StartTime());
        result.setClass5EndTime(dto.getClass5EndTime());
        result.setClass5Analysis(dto.getClass5Analysis());

        result.setClass6PlanQty(dto.getClass6PlanQty());
        result.setClass6StartTime(dto.getClass6StartTime());
        result.setClass6EndTime(dto.getClass6EndTime());
        result.setClass6Analysis(dto.getClass6Analysis());

        result.setClass7PlanQty(dto.getClass7PlanQty());
        result.setClass7StartTime(dto.getClass7StartTime());
        result.setClass7EndTime(dto.getClass7EndTime());
        result.setClass7Analysis(dto.getClass7Analysis());

        result.setClass8PlanQty(dto.getClass8PlanQty());
        result.setClass8StartTime(dto.getClass8StartTime());
        result.setClass8EndTime(dto.getClass8EndTime());
        result.setClass8Analysis(dto.getClass8Analysis());

        ShiftFieldUtil.syncDailyPlanQty(result);

        fillShiftStartEndTimes(result, dto);

        return result;
    }

    /**
     * 填充插单关联字段（胎胚代码/胎胚描述/需求计划版本号/排产版本号/规格/结构/模具号）
     * <p>从月计划定稿表中根据工厂+年月+排产版本+物料编码查询关联字段</p>
     * <p>班次开始/结束时间由 {@link #fillShiftStartEndTimes} 单独填充，需在班次计划量设置后调用</p>
     *
     * @param result 排程结果实体
     * @param dto    插单请求数据
     */
    private void fillEmbryoRelatedFields(LhScheduleResult result, LhOrderInsertDTO dto) {
        String materialCode = StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();
        if (StringUtils.isBlank(materialCode) || StringUtils.isBlank(dto.getFactoryCode()) || dto.getScheduleDate() == null) {
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

        String mouldCode = resolveMouldCode(dto);
        if (StringUtils.isNotBlank(mouldCode)) {
            result.setMouldCode(mouldCode);
        }
    }

    /**
     * 填充有计划量班次的开始/结束时间
     * <p>根据排程日期和班次配置计算各班次时间，仅对有计划量的班次填充</p>
     *
     * @param result 排程结果实体
     * @param dto    插单请求数据
     */
    private void fillShiftStartEndTimes(LhScheduleResult result, LhOrderInsertDTO dto) {
        if (dto.getScheduleDate() == null) {
            return;
        }
        List<com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO> shifts =
                LhScheduleTimeUtil.buildDefaultScheduleShifts(null, dto.getScheduleDate());
        for (int i = 1; i <= com.zlt.aps.lh.api.constant.LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; i++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, i);
            if (planQty != null && planQty > 0) {
                com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO shift = findShiftByIndex(shifts, i);
                if (shift != null) {
                    ShiftFieldUtil.setShiftPlanQty(result, i, planQty,
                            shift.getShiftStartDateTime(), shift.getShiftEndDateTime());
                }
            }
        }
    }

    /**
     * 根据班次索引从班次配置列表中查找对应班次
     *
     * @param shifts     班次配置列表
     * @param shiftIndex 班次索引（1-8）
     * @return 班次配置，未找到返回null
     */
    private com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO findShiftByIndex(
            List<com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO> shifts, int shiftIndex) {
        if (shifts == null || shiftIndex < 1 || shiftIndex > shifts.size()) {
            return null;
        }
        return shifts.get(shiftIndex - 1);
    }

    /**
     * 生成插单工单号：LHGD+yyyyMMdd+3位流水号
     *
     * @param scheduleDate 排程日期
     * @return 工单号
     */
    @Override
    public String generateInsertOrderNo(Date scheduleDate) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(scheduleDate);
        int seq = INSERT_ORDER_SEQ.incrementAndGet() % 1000;
        return String.format("LHGD%s%03d", dateStr, seq);
    }

    /**
     * 生成插单对应的模具交替计划
     * <p>若同机台+同排程日期+同后规格物料已存在交替计划，则覆盖更新；否则新增。</p>
     *
     * @param dto                 插单请求数据
     * @param batchNo             批次号
     * @param beforeMaterialCode  前规格物料编码（在插入排程结果之前查询获得）
     * @param beforeMaterialDesc  前规格物料描述（在插入排程结果之前查询获得）
     */
    private void generateInsertMouldChangePlan(LhOrderInsertDTO dto, String batchNo,
                                               String beforeMaterialCode, String beforeMaterialDesc) {
        String mouldCode = resolveMouldCode(dto);
        String afterMaterialCode = StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();

        // 查询是否已存在同机台+同排程日期+同后规格物料的模具交替计划（避免删除插单后再次插单产生重复数据）
        LhMouldChangePlan existingPlan = mouldChangePlanMapper.selectOne(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, dto.getFactoryCode())
                        .eq(LhMouldChangePlan::getScheduleDate, dto.getScheduleDate())
                        .eq(LhMouldChangePlan::getLhMachineCode, dto.getLhMachineCode())
                        .eq(LhMouldChangePlan::getAfterMaterialCode, afterMaterialCode)
                        .last("LIMIT 1"));

        if (existingPlan != null) {
            // 覆盖更新已有记录，避免重复
            existingPlan.setLhResultBatchNo(batchNo);
            existingPlan.setBeforeMaterialCode(beforeMaterialCode);
            existingPlan.setBeforeMaterialDesc(beforeMaterialDesc);
            existingPlan.setMouldCode(mouldCode);
            existingPlan.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(dto.getLeftRightMold(), dto.getLhMachineCode()));
            existingPlan.setChangeMouldType("01");
            existingPlan.setIsRelease(ReleaseStatusEnum.NOT_RELEASED.getCode());
            existingPlan.setMouldStatus("0");
            mouldChangePlanMapper.updateById(existingPlan);
            log.info("插单覆盖更新模具交替计划, ID: {}, 机台: {}, 前规格: {}, 后规格: {}",
                    existingPlan.getId(), dto.getLhMachineCode(), beforeMaterialCode, afterMaterialCode);
            return;
        }

        // 新增模具交替计划
        LhMouldChangePlan plan = new LhMouldChangePlan();
        plan.setFactoryCode(dto.getFactoryCode());
        plan.setLhResultBatchNo(batchNo);
        plan.setOrderNo(generateChangePlanOrderNo(dto.getScheduleDate()));
        plan.setScheduleDate(dto.getScheduleDate());
        plan.setPlanDate(dto.getScheduleDate());
        plan.setPlanOrder(1);
        plan.setLhMachineCode(dto.getLhMachineCode());
        plan.setLhMachineName(dto.getLhMachineName());
        plan.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(dto.getLeftRightMold(), dto.getLhMachineCode()));
        plan.setClassIndex("03");
        plan.setChangeTime(LhScheduleTimeUtil.getAfternoonShiftStart(null, dto.getScheduleDate()));
        plan.setBeforeMaterialCode(beforeMaterialCode);
        plan.setBeforeMaterialDesc(beforeMaterialDesc);
        plan.setAfterMaterialCode(afterMaterialCode);
        plan.setChangeMouldType("01");
        plan.setMouldCode(mouldCode);
        plan.setIsRelease(ReleaseStatusEnum.NOT_RELEASED.getCode());
        plan.setMouldStatus("0");
        plan.setIsDelete(DeleteFlagEnum.NORMAL.getCode());

        mouldChangePlanMapper.insert(plan);
        log.info("插单生成模具交替计划, 工单号: {}, 机台: {}, 前规格: {}, 后规格: {}",
                plan.getOrderNo(), dto.getLhMachineCode(), beforeMaterialCode, afterMaterialCode);
    }

    /**
     * 生成模具交替计划工单号：CHG+yyyyMMdd+3位流水号
     *
     * @param scheduleDate 排程日期
     * @return 工单号
     */
    private String generateChangePlanOrderNo(Date scheduleDate) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(scheduleDate);
        int seq = INSERT_ORDER_SEQ.incrementAndGet() % 1000;
        return String.format("CHG%s%03d", dateStr, seq);
    }

    /**
     * 根据物料编码查询模具号
     *
     * @param dto 插单数据
     * @return 模具号，多个以逗号分隔
     */
    private String resolveMouldCode(LhOrderInsertDTO dto) {
        String materialCode = StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();
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
                .eq(MpFactoryProductionVersion::getIsFinal, "1")
                .eq(MpFactoryProductionVersion::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                .orderByDesc(MpFactoryProductionVersion::getId)
                .last("LIMIT 1");
        return mpFactoryProductionVersionMapper.selectOne(wrapper);
    }
}

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
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.mapper.LhScheFinishQtyMapper;
import com.zlt.aps.lh.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.lh.mapper.MdmSkuConstructionRefMapper;
import com.zlt.aps.lh.mapper.MdmSkuLhCapacityMapper;
import com.zlt.aps.lh.mapper.MdmSkuMouldRelMapper;
import com.zlt.aps.lh.mapper.LhMachineInfoMapper;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.lh.service.ILhScheduleResultService;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import cn.hutool.core.date.DateUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    @Resource
    private MdmSkuConstructionRefMapper mdmSkuConstructionRefMapper;

    @Resource
    private LhMachineInfoMapper lhMachineInfoMapper;

    @Resource
    private MdmMaterialInfoMapper mdmMaterialInfoMapper;

    @Resource
    private MdmSkuLhCapacityMapper mdmSkuLhCapacityMapper;

    @Resource
    private LhDayFinishQtyMapper lhDayFinishQtyMapper;

    @Resource
    private LhScheFinishQtyMapper lhScheFinishQtyMapper;

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
                result.setProductStatus(validateResult.getTrialStatus());
                result.setChangedTrialStatus(validateResult.getTrialStatus());
            } else if (StringUtils.isNotBlank(dto.getOriginalTrialStatus())) {
                result.setProductStatus(dto.getOriginalTrialStatus());
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

    @Override
    public void fillScheduleResultFields(List<LhScheduleResult> lhScheduleResultList, Date scheduleDate) {
        if (CollectionUtils.isEmpty(lhScheduleResultList) || Objects.isNull(scheduleDate)) {
            log.warn("fillScheduleResultFields: 传入参数为空, listSize={}, scheduleDate={}", 
                    lhScheduleResultList == null ? 0 : lhScheduleResultList.size(), scheduleDate);
            return;
        }

        // 提取排程日期年月
        cn.hutool.core.date.DateTime dateTime = DateUtil.date(scheduleDate);
        int year = DateUtil.year(dateTime);
        int month = DateUtil.month(dateTime) + 1;

        log.info("fillScheduleResultFields: 开始填充排程结果字段, 排程日期={}, 结果数量={}, year={}, month={}",
                DateUtil.formatDate(scheduleDate), lhScheduleResultList.size(), year, month);

        // scheduleDate 业务上为 T+2，计算 T 日用于完成量相关查询
        Date tDay = DateUtil.offsetDay(scheduleDate, -2);
        log.info("fillScheduleResultFields: scheduleDate={}, T日={}",
                DateUtil.formatDate(scheduleDate), DateUtil.formatDate(tDay));

        // 收集所有去重key
        Set<String> factoryCodes = new HashSet<>();
        Set<String> materialCodes = new HashSet<>();
        Set<String> machineCodes = new HashSet<>();
        for (LhScheduleResult r : lhScheduleResultList) {
            if (StringUtils.isNotEmpty(r.getFactoryCode())) {
                factoryCodes.add(r.getFactoryCode());
            }
            if (StringUtils.isNotEmpty(r.getMaterialCode())) {
                materialCodes.add(r.getMaterialCode());
            }
            if (StringUtils.isNotEmpty(r.getLhMachineCode())) {
                machineCodes.add(r.getLhMachineCode());
            }
        }

        // ======== 1. 加载定稿排产版本 ========
        // key: factoryCode, value: MpFactoryProductionVersion
        Map<String, MpFactoryProductionVersion> productionVersionMap = new HashMap<>(factoryCodes.size());
        for (String fc : factoryCodes) {
            MpFactoryProductionVersion version = getFinalProductionVersion(fc, year, month);
            if (Objects.nonNull(version)) {
                productionVersionMap.put(fc, version);
            }
        }
        log.info("fillScheduleResultFields: 排产版本加载完成, 工厂数={}, 匹配数={}", factoryCodes.size(), productionVersionMap.size());

        // ======== 2. 加载月计划定稿数据 ========
        // key: factoryCode + "|" + materialCode, value: FactoryMonthPlanProductionFinalResult
        Map<String, FactoryMonthPlanProductionFinalResult> monthPlanMap = new HashMap<>(lhScheduleResultList.size());
        for (String fc : factoryCodes) {
            MpFactoryProductionVersion version = productionVersionMap.get(fc);
            String productionVersion = Objects.nonNull(version) ? version.getProductionVersion() : null;
            if (StringUtils.isBlank(productionVersion)) {
                continue;
            }
            // 按物料编码分批查询
            LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, fc)
                    .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                    .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                    .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, productionVersion)
                    .in(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCodes)
                    .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
            List<FactoryMonthPlanProductionFinalResult> monthPlanList = monthPlanMapper.selectList(wrapper);
            for (FactoryMonthPlanProductionFinalResult mp : monthPlanList) {
                if (StringUtils.isNotEmpty(mp.getMaterialCode())) {
                    monthPlanMap.put(fc + "|" + mp.getMaterialCode(), mp);
                }
            }
        }
        log.info("fillScheduleResultFields: 月计划定稿加载完成, 月计划匹配数={}", monthPlanMap.size());

        // ======== 3. 加载机台信息（使用模数） ========
        // key: machineCode, value: LhMachineInfo
        Map<String, LhMachineInfo> machineInfoMap = new HashMap<>(machineCodes.size());
        if (!machineCodes.isEmpty()) {
            LambdaQueryWrapper<LhMachineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(LhMachineInfo::getMachineCode, machineCodes);
            List<LhMachineInfo> machineList = lhMachineInfoMapper.selectList(wrapper);
            for (LhMachineInfo m : machineList) {
                if (StringUtils.isNotEmpty(m.getMachineCode())) {
                    machineInfoMap.put(m.getMachineCode(), m);
                }
            }
        }
        log.info("fillScheduleResultFields: 机台信息加载完成, 机台数={}, 匹配数={}", machineCodes.size(), machineInfoMap.size());

        // ======== 4. 加载物料信息（规格编码、规格描述） ========
        // key: materialCode, value: MdmMaterialInfo
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(materialCodes.size());
        if (!materialCodes.isEmpty()) {
            LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MdmMaterialInfo::getMaterialCode, materialCodes);
            List<MdmMaterialInfo> materialList = mdmMaterialInfoMapper.selectList(wrapper);
            for (MdmMaterialInfo m : materialList) {
                if (StringUtils.isNotEmpty(m.getMaterialCode())) {
                    materialInfoMap.put(m.getMaterialCode(), m);
                }
            }
        }
        log.info("fillScheduleResultFields: 物料信息加载完成, 物料数={}, 匹配数={}", materialCodes.size(), materialInfoMap.size());

        // ======== 5. 加载SKU硫化产能（硫化时间） ========
        // key: materialCode, value: MdmSkuLhCapacity
        Map<String, MdmSkuLhCapacity> skuLhCapacityMap = new HashMap<>(materialCodes.size());
        if (!materialCodes.isEmpty()) {
            LambdaQueryWrapper<MdmSkuLhCapacity> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MdmSkuLhCapacity::getMaterialCode, materialCodes);
            List<MdmSkuLhCapacity> capacityList = mdmSkuLhCapacityMapper.selectList(wrapper);
            for (MdmSkuLhCapacity c : capacityList) {
                if (StringUtils.isNotEmpty(c.getMaterialCode())) {
                    skuLhCapacityMap.put(c.getMaterialCode(), c);
                }
            }
        }
        log.info("fillScheduleResultFields: SKU硫化产能加载完成, 物料数={}, 匹配数={}", materialCodes.size(), skuLhCapacityMap.size());

        // ======== 6. 加载SKU示方书关系 ========
        // key: materialCode + "|" + trialStatus, value: MdmSkuConstructionRef
        Map<String, MdmSkuConstructionRef> constructionRefMap = new HashMap<>(materialCodes.size());
        if (!materialCodes.isEmpty()) {
            LambdaQueryWrapper<MdmSkuConstructionRef> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MdmSkuConstructionRef::getMaterialCode, materialCodes);
            List<MdmSkuConstructionRef> refList = mdmSkuConstructionRefMapper.selectList(wrapper);
            for (MdmSkuConstructionRef ref : refList) {
                if (StringUtils.isNotEmpty(ref.getMaterialCode())) {
                    String key = ref.getMaterialCode() + "|" + StringUtils.defaultString(ref.getTrialStatus());
                    // 同物料同产品状态只保留一条
                    constructionRefMap.putIfAbsent(key, ref);
                }
            }
        }
        log.info("fillScheduleResultFields: 示方书关系加载完成, 物料数={}, 匹配数={}", materialCodes.size(), constructionRefMap.size());

        // ======== 7. 加载日完成量（本月1日至T-1日累计） ========
        // T-1 日 = tDay - 1天
        // key: factoryCode + "|" + materialCode, value: 累计完成量
        Map<String, BigDecimal> dayFinishSumMap = new HashMap<>(materialCodes.size());
        Date monthStart = DateUtil.beginOfMonth(scheduleDate);
        Date dayBeforeTDay = DateUtil.offsetDay(tDay, -1);
        if (!factoryCodes.isEmpty() && !materialCodes.isEmpty()) {
            LambdaQueryWrapper<LhDayFinishQty> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(LhDayFinishQty::getFactoryCode, factoryCodes)
                    .in(LhDayFinishQty::getMaterialCode, materialCodes)
                    .ge(LhDayFinishQty::getFinishDate, monthStart)
                    .le(LhDayFinishQty::getFinishDate, dayBeforeTDay);
            List<LhDayFinishQty> dayFinishList = lhDayFinishQtyMapper.selectList(wrapper);
            for (LhDayFinishQty qty : dayFinishList) {
                if (StringUtils.isNotEmpty(qty.getFactoryCode()) && StringUtils.isNotEmpty(qty.getMaterialCode())
                        && Objects.nonNull(qty.getDayFinishQty())) {
                    String key = qty.getFactoryCode() + "|" + qty.getMaterialCode();
                    dayFinishSumMap.merge(key, qty.getDayFinishQty(), BigDecimal::add);
                }
            }
        }
        log.info("fillScheduleResultFields: 日完成量加载完成, 匹配数={}", dayFinishSumMap.size());

        // ======== 8. 加载班完成量（T日夜班完成量） ========
        // key: factoryCode + "|" + materialCode, value: class1FinishQty
        Map<String, BigDecimal> scheNightFinishMap = new HashMap<>(materialCodes.size());
        if (!factoryCodes.isEmpty() && !materialCodes.isEmpty()) {
            LambdaQueryWrapper<LhScheFinishQty> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(LhScheFinishQty::getFactoryCode, factoryCodes)
                    .in(LhScheFinishQty::getMaterialCode, materialCodes)
                    .eq(LhScheFinishQty::getScheduleDate, tDay);
            List<LhScheFinishQty> scheFinishList = lhScheFinishQtyMapper.selectList(wrapper);
            for (LhScheFinishQty qty : scheFinishList) {
                if (StringUtils.isNotEmpty(qty.getFactoryCode()) && StringUtils.isNotEmpty(qty.getMaterialCode())
                        && Objects.nonNull(qty.getClass1FinishQty())) {
                    String key = qty.getFactoryCode() + "|" + qty.getMaterialCode();
                    scheNightFinishMap.merge(key, qty.getClass1FinishQty(), BigDecimal::add);
                }
            }
        }
        log.info("fillScheduleResultFields: 班完成量加载完成, 匹配数={}", scheNightFinishMap.size());

        // ======== 9. 填充每条排程结果 ========
        for (LhScheduleResult result : lhScheduleResultList) {
            String fc = result.getFactoryCode();
            String matCode = result.getMaterialCode();
            String machineCode = result.getLhMachineCode();
            String fcMatKey = fc + "|" + matCode;

            // 月计划对象（用于获取 productStatus、totalQty、constructionStage、monthPlanVersion）
            FactoryMonthPlanProductionFinalResult monthPlan = monthPlanMap.get(fcMatKey);

            // ---------- TOTAL_DAILY_PLAN_QTY：月计划总量 ----------
            if (Objects.nonNull(monthPlan) && Objects.nonNull(monthPlan.getTotalQty())) {
                result.setTotalDailyPlanQty(monthPlan.getTotalQty());
            }

            // ---------- PRODUCTION_VERSION：排产版本 ----------
            MpFactoryProductionVersion finalVersion = productionVersionMap.get(fc);
            if (Objects.nonNull(finalVersion) && StringUtils.isNotEmpty(finalVersion.getProductionVersion())) {
                result.setProductionVersion(finalVersion.getProductionVersion());
            }

            // ---------- MONTH_PLAN_VERSION：需求计划版本 ----------
            if (Objects.nonNull(finalVersion) && StringUtils.isNotEmpty(finalVersion.getMonthPlanVersion())) {
                result.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
            }
            // 如果排产版本表没有，再尝试从月计划定稿取
            if (StringUtils.isEmpty(result.getMonthPlanVersion()) && Objects.nonNull(monthPlan)
                    && StringUtils.isNotEmpty(monthPlan.getMonthPlanVersion())) {
                result.setMonthPlanVersion(monthPlan.getMonthPlanVersion());
            }

            // ---------- CONSTRUCTION_STAGE：施工阶段 ----------
            if (Objects.nonNull(monthPlan) && StringUtils.isNotEmpty(monthPlan.getConstructionStage())) {
                result.setConstructionStage(monthPlan.getConstructionStage());
            }

            // ---------- MOULD_QTY：使用模数 ----------
            LhMachineInfo machineInfo = machineInfoMap.get(machineCode);
            if (Objects.nonNull(machineInfo) && Objects.nonNull(machineInfo.getMaxMoldNum())) {
                result.setMouldQty(machineInfo.getMaxMoldNum());
            }

            // ---------- LH_TIME：硫化时间 ----------
            Integer lhTime = null;
            MdmSkuLhCapacity capacity = skuLhCapacityMap.get(matCode);
            if (Objects.nonNull(capacity) && Objects.nonNull(capacity.getVulcanizationTime())
                    && capacity.getVulcanizationTime() > 0) {
                lhTime = capacity.getVulcanizationTime();
            }
            // 兜底：根据单班硫化量和使用模数反推
            if (lhTime == null || lhTime <= 0) {
                Integer singleMouldShiftQty = result.getSingleMouldShiftQty();
                Integer mouldQty = result.getMouldQty();
                // 如果result的singleMouldShiftQty为空，尝试从产能主数据取班产
                if (singleMouldShiftQty == null || singleMouldShiftQty <= 0) {
                    if (Objects.nonNull(capacity) && Objects.nonNull(capacity.getClassCapacity())
                            && capacity.getClassCapacity() > 0) {
                        singleMouldShiftQty = capacity.getClassCapacity();
                    }
                }
                if (singleMouldShiftQty != null && singleMouldShiftQty > 0
                        && mouldQty != null && mouldQty > 0) {
                    // 单班硫化量 = floor(28800 / 硫化时间) * 使用模数
                    // 反推：硫化时间 ≈ 28800 * 使用模数 / 单班硫化量
                    lhTime = (int) (28800.0 * mouldQty / singleMouldShiftQty);
                }
            }
            if (lhTime != null && lhTime > 0) {
                result.setLhTime(lhTime);
            }

            // ---------- DAILY_PLAN_QTY：日计划量 ----------
            int dailyPlanQty = 0;
            for (int shift = 1; shift <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shift++) {
                Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shift);
                if (shiftQty != null) {
                    dailyPlanQty += shiftQty;
                }
            }
            result.setDailyPlanQty(dailyPlanQty);

            // ---------- MOULD_SURPLUS_QTY：硫化余量 ----------
            if (Objects.nonNull(monthPlan) && Objects.nonNull(monthPlan.getTotalQty())) {
                int finishedQty = 0;
                // 本月1日至T-1日累计完成量
                BigDecimal dayFinishSum = dayFinishSumMap.get(fcMatKey);
                if (dayFinishSum != null) {
                    finishedQty += dayFinishSum.intValue();
                }
                // T日夜班完成量（class1代表夜班）
                BigDecimal nightFinish = scheNightFinishMap.get(fcMatKey);
                if (nightFinish != null) {
                    finishedQty += nightFinish.intValue();
                }
                int surplus = monthPlan.getTotalQty() - finishedQty;
                result.setMouldSurplusQty(Math.max(surplus, 0));
            }

            // ---------- SPEC_CODE：规格编码 ----------
            MdmMaterialInfo materialInfo = materialInfoMap.get(matCode);
            if (Objects.nonNull(materialInfo) && StringUtils.isNotEmpty(materialInfo.getSpecifications())) {
                result.setSpecCode(materialInfo.getSpecifications());
            }

            // ---------- SPEC_DESC：规格描述 ----------
            if (Objects.nonNull(materialInfo) && StringUtils.isNotEmpty(materialInfo.getMaterialDesc())) {
                result.setSpecDesc(materialInfo.getMaterialDesc());
            }

            // ---------- DATA_SOURCE：数据来源（固定值2-导入） ----------
            result.setDataSource("2");

            // ---------- EMBRYO_NO / TEXT_NO / LH_NO：示方书号 ----------
            fillConstructionRefFields(result, monthPlan, constructionRefMap);
        }

        log.info("fillScheduleResultFields: 排程结果字段填充完成, 共处理{}条记录", lhScheduleResultList.size());
    }

    /**
     * 填充制造示方书号、文字示方号、硫化示方号
     * <p>优先根据物料编码+月计划产品状态匹配示方书关系；若月计划缺失则仅按物料编码匹配。</p>
     *
     * @param result             排程结果
     * @param monthPlan          月计划对象（可能为null）
     * @param constructionRefMap 示方书关系Map，key为 materialCode + "|" + trialStatus
     */
    private void fillConstructionRefFields(LhScheduleResult result,
                                           FactoryMonthPlanProductionFinalResult monthPlan,
                                           Map<String, MdmSkuConstructionRef> constructionRefMap) {
        String matCode = result.getMaterialCode();
        if (StringUtils.isBlank(matCode)) {
            return;
        }

        MdmSkuConstructionRef ref = null;
        // 优先按物料编码+产品状态匹配
        if (Objects.nonNull(monthPlan) && StringUtils.isNotEmpty(monthPlan.getProductStatus())) {
            String key = matCode + "|" + monthPlan.getProductStatus();
            ref = constructionRefMap.get(key);
        }
        // 兜底：仅按物料编码匹配（取第一条）
        if (ref == null) {
            String keyPrefix = matCode + "|";
            ref = constructionRefMap.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(keyPrefix))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
        }

        if (ref != null) {
            if (StringUtils.isNotEmpty(ref.getEmbryoNo())) {
                result.setEmbryoNo(ref.getEmbryoNo());
            }
            if (StringUtils.isNotEmpty(ref.getTextNo())) {
                result.setTextNo(ref.getTextNo());
            }
            if (StringUtils.isNotEmpty(ref.getLhNo())) {
                result.setLhNo(ref.getLhNo());
            }
        }

        // 如果月计划中有更准确的示方书号，优先覆盖
        if (Objects.nonNull(monthPlan)) {
            if (StringUtils.isNotEmpty(monthPlan.getEmbryoNo())) {
                result.setEmbryoNo(monthPlan.getEmbryoNo());
            }
            if (StringUtils.isNotEmpty(monthPlan.getTextNo())) {
                result.setTextNo(monthPlan.getTextNo());
            }
            if (StringUtils.isNotEmpty(monthPlan.getLhNo())) {
                result.setLhNo(monthPlan.getLhNo());
            }
        }
    }
}

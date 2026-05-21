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
import com.zlt.aps.lh.service.ILhScheduleResultService;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
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

        String batchNo = generateNextBatchNo(dto.getScheduleDate(), dto.getFactoryCode());
        String orderNo = generateInsertOrderNo(dto.getScheduleDate());

        LhScheduleResult result = buildInsertOrderResult(dto, batchNo, orderNo, validateResult);
        mapper.insert(result);

        generateInsertMouldChangePlan(dto, batchNo);

        // TODO 同步触发成型机台均衡
        log.info("插单操作完成, 工单号: {}, 批次号: {}", orderNo, batchNo);
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
            if (validateResult.getMachineShiftCapacity() != null) {
                result.setSingleMouldShiftQty(validateResult.getSingleMouldShiftQty());
            }
            if (StringUtils.isNotBlank(validateResult.getTrialStatus())) {
                result.setTrialStatus(validateResult.getTrialStatus());
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

        return result;
    }

    /**
     * 填充胎胚相关字段（胎胚代码、胎胚描述）
     * <p>从月计划定稿表中根据工厂+物料编码查询胎胚代码和胎胚描述</p>
     *
     * @param result 排程结果实体
     * @param dto    插单请求数据
     */
    private void fillEmbryoRelatedFields(LhScheduleResult result, LhOrderInsertDTO dto) {
        String materialCode = StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();
        if (StringUtils.isBlank(materialCode) || StringUtils.isBlank(dto.getFactoryCode())) {
            return;
        }
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, dto.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1");
        FactoryMonthPlanProductionFinalResult monthPlan = monthPlanMapper.selectOne(wrapper);
        if (monthPlan != null) {
            if (StringUtils.isNotBlank(monthPlan.getEmbryoCode())) {
                result.setEmbryoCode(monthPlan.getEmbryoCode());
            }
            if (StringUtils.isNotBlank(monthPlan.getMainMaterialDesc())) {
                result.setMainMaterialDesc(monthPlan.getMainMaterialDesc());
            }
        }
    }

    /**
     * 生成插单工单号：LHGD+yyyyMMdd+3位流水号
     *
     * @param scheduleDate 排程日期
     * @return 工单号
     */
    private String generateInsertOrderNo(Date scheduleDate) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(scheduleDate);
        int seq = INSERT_ORDER_SEQ.incrementAndGet() % 1000;
        return String.format("LHGD%s%03d", dateStr, seq);
    }

    /**
     * 生成插单对应的模具交替计划
     *
     * @param dto     插单请求数据
     * @param batchNo 批次号
     */
    private void generateInsertMouldChangePlan(LhOrderInsertDTO dto, String batchNo) {
        LambdaQueryWrapper<LhScheduleResult> prevWrapper = new LambdaQueryWrapper<>();
        prevWrapper.eq(LhScheduleResult::getLhMachineCode, dto.getLhMachineCode())
                .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .eq(LhScheduleResult::getScheduleDate, dto.getScheduleDate());
        if (StringUtils.isNotBlank(dto.getFactoryCode())) {
            prevWrapper.eq(LhScheduleResult::getFactoryCode, dto.getFactoryCode());
        }
        prevWrapper.orderByDesc(LhScheduleResult::getCreateTime);
        prevWrapper.last("LIMIT 1");
        LhScheduleResult prevResult = mapper.selectOne(prevWrapper);

        String beforeMaterialCode = null;
        String beforeMaterialDesc = null;
        if (prevResult != null) {
            beforeMaterialCode = prevResult.getMaterialCode();
            beforeMaterialDesc = prevResult.getMaterialDesc();
        }

        String mouldCode = resolveMouldCode(dto);
        String afterMaterialCode = StringUtils.isNotBlank(dto.getProductCode()) ? dto.getProductCode() : dto.getMaterialCode();

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
}

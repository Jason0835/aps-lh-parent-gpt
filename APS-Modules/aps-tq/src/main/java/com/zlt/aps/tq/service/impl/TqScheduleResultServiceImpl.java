package com.zlt.aps.tq.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.tq.api.domain.dto.TqChangeMachineDTO;
import com.zlt.aps.tq.api.domain.dto.TqInsertOrderDTO;
import com.zlt.aps.tq.api.domain.entity.*;
import com.zlt.aps.tq.api.domain.vo.TqInsertTaskRequestVo;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;
import com.zlt.aps.tq.engine.vo.TqScheduleBaseInfoVo;
import com.zlt.aps.tq.mapper.TqMachineChuckMapper;
import com.zlt.aps.tq.mapper.TqMachineMaintenancePlanMapper;
import com.zlt.aps.tq.mapper.TqMouthPlateMapper;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.mapper.TqSpecifyMachineMapper;
import com.zlt.aps.tq.service.ITqScheduleResultService;
import com.zlt.aps.tq.service.ITqRollingUpdateService;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.TqDispatcherLogService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 胎圈排程结果Service实现类
 *
 * @author APS
 */
@Slf4j
@Service
public class TqScheduleResultServiceImpl extends AbstractDocService<TqScheduleResult> implements ITqScheduleResultService {

    /**
     * 班次字段名模板常量（遵循动态字段访问规范，配合 String.format 使用）。
     * 用于动态访问 class1~6PlanQty/FinishQty/Sequence/Analysis 等批量字段。
     */
    private static final String CLASS_PLAN_QTY_FIELD_TEMPLATE = "class%dPlanQty";
    private static final String CLASS_FINISH_QTY_FIELD_TEMPLATE = "class%dFinishQty";
    private static final String CLASS_SEQUENCE_FIELD_TEMPLATE = "class%dSequence";
    private static final String CLASS_ANALYSIS_FIELD_TEMPLATE = "class%dAnalysis";

    @Autowired
    private TqScheduleResultMapper tqScheduleResultMapper;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private FactoryService factoryService;

    /**
     * 胎圈排程滚动更新服务（用于插单/调量/转机台/删除后触发同班次内时间重算）
     */
    @Resource
    private ITqRollingUpdateService tqRollingUpdateService;

    /**
     * 胎圈调度员排程操作日志服务（用于记录6班次制操作日志）
     */
    @Autowired
    private TqDispatcherLogService tqDispatcherLogService;

    /**
     * 胎圈机台信息Feign服务（用于校验新机台是否存在且启用）
     */
    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    /**
     * 胎圈排程引擎服务（用于生成插单的批次号、工单号）
     */
    @Autowired
    private TqEngineService tqEngineService;

    /**
     * Redisson分布式锁客户端（用于发布排程时加锁，避免并发下发MES）
     */
    @Autowired
    private RedissonClient redissonClient;

    /** 定点机台Mapper（用于转机台校验：限制作业/不可作业） */
    @Resource
    private TqSpecifyMachineMapper tqSpecifyMachineMapper;

    /** 口型板Mapper（用于转机台校验：口型板→机台映射） */
    @Resource
    private TqMouthPlateMapper tqMouthPlateMapper;

    /** 机台寸口Mapper（用于转机台校验：机台-寸口绑定） */
    @Resource
    private TqMachineChuckMapper tqMachineChuckMapper;

    /** 维修计划Mapper（用于转机台校验：维修中机台排除） */
    @Resource
    private TqMachineMaintenancePlanMapper tqMachineMaintenancePlanMapper;

    /**
     * 胎圈人工排程操作统一门面（对齐胎面 TmManualOperationFacade）
     *
     * <p>统一插单/调量/转机台/删除四类业务触发的分布式锁、短事务、行锁、
     * 释放状态校验和调度日志，避免不同入口绕过同一组安全约束。</p>
     */
    @Autowired
    private TqManualOperationFacade tqManualOperationFacade;

    @Override
    public String getDocTypeCode() {
        return "TQ_SCHEDULE_RESULT";
    }

    /**
     * 插单前校验
     * 校验规则：
     * 1. 排程日期不能为空，且需在生产周期内
     * 2. 胎圈代码不能为空，施工必须存在
     * 3. 机台编号不能为空
     * 4. 6个班次中至少有一个班次的计划量有值
     * 5. 有计划量的班次，顺序也必须有值；反之亦然
     * 6. 只能往当前班次或后续班次插单
     * 7. 插单只能加到第二个在产规格之后
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateInsertOrder(TqInsertOrderDTO dto) {
        // 1. 排程日期校验
        if (dto.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }

        // 2. 胎圈代码校验
        if (ObjectUtils.isEmpty(dto.getBeadCode())) {
            return AjaxResult.error("胎圈代码不能为空");
        }
        // 校验施工是否存在（查询施工表 T_MDM_CONSTRUCTION_INFO）
        List<TqScheduleBaseInfoVo> baseInfoList = tqEngineService.listTqScheduleBaseInfo(
                Collections.singletonList(dto.getBeadCode()));
        if (CollectionUtils.isEmpty(baseInfoList)) { 
            return AjaxResult.error("胎圈规格有误，施工不存在");
        }

        // 3. 机台编号校验
        if (ObjectUtils.isEmpty(dto.getMachineCode())) {
            return AjaxResult.error("机台编号不能为空");
        }

        // 4. 至少一个班次有计划量
        boolean hasAnyPlanQty = false;
        for (int i = 1; i <= 6; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                hasAnyPlanQty = true;
                break;
            }
        }
        if (!hasAnyPlanQty) {
            return AjaxResult.error("至少一个班次的计划量必须有值");
        }

        // 5. 有计划量的班次顺序必须有值，反之亦然
        for (int i = 1; i <= 6; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            Integer sequence = getSequenceByClassIndex(dto, i);
            boolean hasPlanQty = planQty != null && planQty > 0;
            boolean hasSequence = sequence != null && sequence > 0;
            if (hasPlanQty && !hasSequence) {
                return AjaxResult.error("第" + i + "班有计划量，顺序也必须有值");
            }
            if (hasSequence && !hasPlanQty) {
                return AjaxResult.error("第" + i + "班有顺序，计划量也必须有值");
            }
        }

        // 6. 只能往当前班次或后续班次插单（根据排程日期精确判断当前班次）
        int currentShiftIndex = resolveCurrentShiftIndex(dto.getScheduleDate());
        for (int i = 1; i < currentShiftIndex; i++) {
            Integer planQty = getPlanQtyByClassIndex(dto, i);
            if (planQty != null && planQty > 0) {
                return AjaxResult.error("不能往历史班次插单，当前班次为第" + currentShiftIndex + "班");
            }
        }

        // 7. 插单只能加到第二个在产规格之后
        // 查询该机台该日期已有排程记录
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getScheduleDate, dto.getScheduleDate());
        wrapper.eq(TqScheduleResult::getMachineCode, dto.getMachineCode());
        wrapper.eq(TqScheduleResult::getIsDelete, 0);
        List<TqScheduleResult> existingList = tqScheduleResultMapper.selectList(wrapper);
        // 按"任一班次有顺序号"的最小顺序号作为排序依据（自定义比较）
        existingList.sort(Comparator.comparingInt(this::getMinSequenceOfRecord));
        if (existingList.size() >= 2) {
            // 第二个在产规格的最小顺序号
            int secondSpecMinSeq = getMinSequenceFromSecondSpec(existingList);
            for (int i = 1; i <= 6; i++) {
                Integer sequence = getSequenceByClassIndex(dto, i);
                if (sequence != null && sequence < secondSpecMinSeq) {
                    return AjaxResult.error("插单只能加到第二个在产规格之后，顺序号不能小于" + secondSpecMinSeq);
                }
            }
        }

        return AjaxResult.success("校验通过");
    }

    /**
     * 插单
     *
     * @param dto 插单数据
     * @return 结果
     */
    @Deprecated
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult insertOrder(TqInsertOrderDTO dto) {
        // 先执行校验
        AjaxResult validateResult = validateInsertOrder(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 构建排程记录实体
        TqScheduleResult entity = new TqScheduleResult();
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setBeadCode(dto.getBeadCode());
        entity.setMachineCode(dto.getMachineCode());
        entity.setDataSource("1"); // 插单
        entity.setReleaseStatus("0"); // 未发布

        // 填充6个班次字段
        entity.setClass1PlanQty(dto.getClass1PlanQty());
        entity.setClass1Sequence(dto.getClass1Sequence());
        entity.setClass1Analysis(dto.getClass1Analysis());
        entity.setClass2PlanQty(dto.getClass2PlanQty());
        entity.setClass2Sequence(dto.getClass2Sequence());
        entity.setClass2Analysis(dto.getClass2Analysis());
        entity.setClass3PlanQty(dto.getClass3PlanQty());
        entity.setClass3Sequence(dto.getClass3Sequence());
        entity.setClass3Analysis(dto.getClass3Analysis());
        entity.setClass4PlanQty(dto.getClass4PlanQty());
        entity.setClass4Sequence(dto.getClass4Sequence());
        entity.setClass4Analysis(dto.getClass4Analysis());
        entity.setClass5PlanQty(dto.getClass5PlanQty());
        entity.setClass5Sequence(dto.getClass5Sequence());
        entity.setClass5Analysis(dto.getClass5Analysis());
        entity.setClass6PlanQty(dto.getClass6PlanQty());
        entity.setClass6Sequence(dto.getClass6Sequence());
        entity.setClass6Analysis(dto.getClass6Analysis());

        entity.setRemark(dto.getRemark());

        // 生成批次号、工单号（复用当前排程日期已有批次号，不影响其他记录）
        String scheduleDateStr = DateUtil.formatDate(dto.getScheduleDate());
        String[] batchAndOrder = tqEngineService.generateBatchNoAndOrderNo(scheduleDateStr);
        entity.setBatchNo(batchAndOrder[0]);
        entity.setOrderNo(batchAndOrder[1]);

        // 回显施工字段（钢丝圈、三角胶、尺寸），从施工表获取
        List<TqScheduleBaseInfoVo> baseInfoList = tqEngineService.listTqScheduleBaseInfo(
                Collections.singletonList(dto.getBeadCode()));
        if (CollectionUtils.isNotEmpty(baseInfoList)) {
            TqScheduleBaseInfoVo baseInfo = baseInfoList.get(0);
            entity.setSteelRingCode(baseInfo.getSteelRingCode());
            entity.setTriangleGlueCode(baseInfo.getTriangleGlueCode());
            entity.setProSize(baseInfo.getSpecSize());
        }

        // 插入数据库
        tqScheduleResultMapper.insert(entity);

        // 滚动更新：对每个有计划量的班次执行同班次内时间重算
        this.triggerRollingUpdateForAllShifts("1", entity.getId(), entity);

        // 记录调度日志（6班次制，操作类型：2-插单，无操作前数据）
        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, entity, null, entity);

        log.info("胎圈排程插单成功，排程日期：{}，胎圈代码：{}，机台：{}",
                dto.getScheduleDate(), dto.getBeadCode(), dto.getMachineCode());

        return AjaxResult.success(I18nUtil.getMessage("ui.tq.scheduleResult.insertOrder.success"));
    }

    /**
     * 转机台前校验
     * 校验规则（与自动排程策略链口径一致）：
     * 1. 基础校验：ID、新机台编号、新旧机台不重复、排程记录存在
     * 2. 机台启用校验：新机台必须在胎圈机台管理中存在且启用
     * 3. 寸口校验：新机台的寸口列表必须包含该胎圈的寸口值（proSize→inchSize）
     * 4. 口型板校验：胎圈的三角胶口型板必须在新机台可用的口型板列表中
     * 5. 定点机台校验：限制作业→新机台必须在限制列表；不可作业→新机台不能在排除列表
     * 6. 维修校验：新机台在排程日期的所有班次不能处于维修状态
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateChangeMachine(TqChangeMachineDTO dto) {
        // ========== 1. 基础校验 ==========
        if (dto.getId() == null) {
            return AjaxResult.error("请选择需要转机台的记录");
        }
        if (ObjectUtils.isEmpty(dto.getNewMachineCode())) {
            return AjaxResult.error("新机台编号不能为空");
        }
        if (dto.getNewMachineCode().equals(dto.getOldMachineCode())) {
            return AjaxResult.error("新机台与原机台不能相同");
        }

        // 校验排程记录是否存在
        TqScheduleResult record = tqScheduleResultMapper.selectById(dto.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error("排程记录不存在或已删除");
        }

        String newMachineCode = dto.getNewMachineCode();
        String beadCode = record.getBeadCode();
        String triangleGlueCode = record.getTriangleGlueCode();
        String proSize = record.getProSize();

        // ========== 2. 机台启用校验 ==========
        TqMachineInfo queryMachine = new TqMachineInfo();
        queryMachine.setMachineCode(newMachineCode);
        List<TqMachineInfo> machineList = tqMachineInfoService.listMachineInfo(queryMachine);
        if (CollectionUtils.isEmpty(machineList)) {
            return AjaxResult.error("新机台不存在或已停用：" + newMachineCode);
        }

        // ========== 3. 寸口校验 ==========
        // 查询新机台的寸口列表（来自T_TQ_MACHINE_CHUCK表）
        LambdaQueryWrapper<TqMachineChuck> chuckWrapper = new LambdaQueryWrapper<>();
        chuckWrapper.eq(TqMachineChuck::getMachineCode, newMachineCode);
        chuckWrapper.eq(TqMachineChuck::getIsDelete, 0);
        List<TqMachineChuck> chuckList = tqMachineChuckMapper.selectList(chuckWrapper);
        if (CollectionUtils.isNotEmpty(chuckList) && StringUtils.isNotEmpty(proSize)) {
            // proSize是英寸尺寸字符串，转换为BigDecimal与inchSize比较
            java.math.BigDecimal dimension;
            try {
                dimension = new java.math.BigDecimal(proSize);
            } catch (NumberFormatException e) {
                log.warn("[转机台校验] 胎圈{}的英寸尺寸{}无法转换为数字，跳过寸口校验", beadCode, proSize);
                dimension = null;
            }
            if (dimension != null) {
                java.math.BigDecimal finalDimension = dimension;
                boolean inchMatch = chuckList.stream()
                        .anyMatch(c -> c.getInchSize() != null && c.getInchSize().compareTo(finalDimension) == 0);
                if (!inchMatch) {
                    return AjaxResult.error("新机台" + newMachineCode + "的寸口范围不包含胎圈英寸尺寸" + proSize + "，无法转机台");
                }
            }
        }

        // ========== 4. 口型板校验 ==========
        // 查询口型板→机台映射：三角胶口型板对应的机台列表
        if (StringUtils.isNotEmpty(triangleGlueCode)) {
            LambdaQueryWrapper<TqMouthPlate> mpWrapper = new LambdaQueryWrapper<>();
            mpWrapper.eq(TqMouthPlate::getMouthPlateCode, triangleGlueCode);
            mpWrapper.eq(TqMouthPlate::getIsDelete, 0);
            List<TqMouthPlate> mouthPlateList = tqMouthPlateMapper.selectList(mpWrapper);
            // 口型板绑定了机台时，校验新机台是否在口型板的机台列表中
            if (CollectionUtils.isNotEmpty(mouthPlateList)) {
                boolean mouthPlateMatch = mouthPlateList.stream()
                        .anyMatch(mp -> newMachineCode.equals(mp.getMachineCode()));
                if (!mouthPlateMatch) {
                    return AjaxResult.error("口型板" + triangleGlueCode + "不在新机台" + newMachineCode + "的可用口型板中，无法转机台");
                }
            }
        }

        // ========== 5. 定点机台校验 ==========
        LambdaQueryWrapper<TqSpecifyMachine> specifyWrapper = new LambdaQueryWrapper<>();
        specifyWrapper.eq(TqSpecifyMachine::getBeadCode, beadCode);
        specifyWrapper.eq(TqSpecifyMachine::getIsDelete, 0);
        List<TqSpecifyMachine> specifyList = tqSpecifyMachineMapper.selectList(specifyWrapper);

        // 5.1 限制作业（jobType=0）：新机台必须在限制列表中
        List<TqSpecifyMachine> canList = specifyList.stream()
                .filter(s -> "0".equals(s.getJobType()) && "0".equals(s.getLineType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(canList)) {
            boolean canMatch = canList.stream()
                    .anyMatch(s -> newMachineCode.equals(s.getMachineCode()));
            if (!canMatch) {
                return AjaxResult.error("胎圈" + beadCode + "有限制作业机台约束，新机台" + newMachineCode + "不在限制作业列表中，无法转机台");
            }
        }

        // 5.2 不可作业（jobType=1）：新机台不能在排除列表中
        List<TqSpecifyMachine> notList = specifyList.stream()
                .filter(s -> "1".equals(s.getJobType()) && "1".equals(s.getLineType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notList)) {
            boolean notMatch = notList.stream()
                    .anyMatch(s -> newMachineCode.equals(s.getMachineCode()));
            if (notMatch) {
                return AjaxResult.error("胎圈" + beadCode + "有不可作业机台约束，新机台" + newMachineCode + "在不可作业列表中，无法转机台");
            }
        }

        // ========== 6. 维修校验 ==========
        // 校验排程日期的所有班次中，新机台是否处于维修状态
        if (record.getScheduleDate() != null) {
            String scheduleDateStr = cn.hutool.core.date.DateUtil.formatDate(record.getScheduleDate());
            LambdaQueryWrapper<TqMachineMaintenancePlan> maintWrapper = new LambdaQueryWrapper<>();
            maintWrapper.eq(TqMachineMaintenancePlan::getMachineCode, newMachineCode);
            maintWrapper.eq(TqMachineMaintenancePlan::getIsDelete, 0);
            // 查询该机台在排程日期当天的维修计划
            maintWrapper.ge(TqMachineMaintenancePlan::getDowntimeDate, record.getScheduleDate());
            maintWrapper.apply("DATE(DOWNTIME_DATE) = DATE({0})", record.getScheduleDate());
            List<TqMachineMaintenancePlan> maintList = tqMachineMaintenancePlanMapper.selectList(maintWrapper);
            if (CollectionUtils.isNotEmpty(maintList)) {
                String maintShifts = maintList.stream()
                        .map(TqMachineMaintenancePlan::getDowntimeShift)
                        .collect(Collectors.joining(","));
                return AjaxResult.error("新机台" + newMachineCode + "在排程日期" + scheduleDateStr + "有维修计划（班次：" + maintShifts + "），无法转机台");
            }
        }

        log.info("[转机台校验] 胎圈{}转机台{}校验通过，寸口={}, 口型板={}, 定点={}, 维修=无",
                beadCode, newMachineCode, proSize, triangleGlueCode,
                CollectionUtils.isEmpty(canList) ? "无限制" : "已校验");

        return AjaxResult.success("校验通过");
    }

    /**
     * 转机台
     *
     * @param dto 转机台数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult changeMachine(TqChangeMachineDTO dto) {
        // 先执行校验
        AjaxResult validateResult = validateChangeMachine(dto);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 查询原记录
        TqScheduleResult record = tqScheduleResultMapper.selectById(dto.getId());
        String oldMachineCode = record.getMachineCode();

        // 更新机台编号
        LambdaUpdateWrapper<TqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TqScheduleResult::getId, dto.getId())
                .set(TqScheduleResult::getMachineCode, dto.getNewMachineCode());

        // 如果原排程已发布成功，更新发布状态为待发布
        if (ApsConstant.IS_RELEASE.equals(record.getReleaseStatus())) {
            updateWrapper.set(TqScheduleResult::getReleaseStatus, ApsConstant.WAIT_RELEASING);
        }

        tqScheduleResultMapper.update(null, updateWrapper);

        // 滚动更新：原机台和新机台都需要重算（每个有计划量的班次）
        // 原机台：删除任务后重算
        this.triggerRollingUpdateForAllShifts("2", dto.getId(), record);
        // 新机台：从原 record 复制后修改机台号，避免手工 new 遗漏字段
        TqScheduleResult newMachineRecord = new TqScheduleResult();
        BeanUtil.copyProperties(record, newMachineRecord);
        newMachineRecord.setMachineCode(dto.getNewMachineCode());
        this.triggerRollingUpdateForAllShifts("2", dto.getId(), newMachineRecord);

        // 记录调度日志（6班次制，操作类型：0-转机台，操作前=原机台记录，操作后=新机台记录）
        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, record, record, newMachineRecord);

        log.info("胎圈排程转机台成功，id：{}，原机台：{}，新机台：{}",
                dto.getId(), oldMachineCode, dto.getNewMachineCode());

        return AjaxResult.success("转机台成功");
    }

    /**
     * 调量前校验
     * 校验规则：
     * 1. 排程记录必须存在且未删除
     * 2. 至少有一个班次的计划量被修改
     * 3. 计划量不能小于0
     * 4. 历史班次不允许修改计划量（根据当前时间和排程日期判断）
     * 5. 非历史班次的计划量不能小于完成量
     *
     * @param entity 调量数据
     * @return 校验结果
     */
    @Override
    public AjaxResult validateChangeQty(TqScheduleResult entity) {
        if (entity == null || entity.getId() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.selectRecord"));
        }

        TqScheduleResult record = tqScheduleResultMapper.selectById(entity.getId());
        if (record == null || Objects.equals(record.getIsDelete(), 1)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.recordNotExist"));
        }

        Date now = new Date();
        boolean hasAdjustField = false;
        List<String> errorMessages = new ArrayList<>();

        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer newPlanQty = this.getPlanQtyByShiftIndex(entity, shiftIndex);
            Integer oldPlanQty = this.getPlanQtyByShiftIndex(record, shiftIndex);

            // 只检查被修改的班次
            if (newPlanQty == null || Objects.equals(newPlanQty, oldPlanQty)) {
                continue;
            }

            hasAdjustField = true;

            // 规则3：计划量不能小于0
            if (newPlanQty < 0) {
                errorMessages.add(String.format(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.planQtyLessThanZero"), shiftIndex));
                continue;
            }

            // 判断是否为历史班次
            boolean historyShift = this.isHistoryShift(record, shiftIndex, now);

            if (historyShift) {
                // 规则4：历史班次不允许修改
                errorMessages.add(String.format(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.historyShiftForbidden"), shiftIndex));
            } else {
                // 规则5：非历史班次计划量不能小于完成量
                Integer finishQty = this.getFinishQtyByShiftIndex(record, shiftIndex);
                if (finishQty != null && finishQty > 0 && newPlanQty < finishQty) {
                    errorMessages.add(String.format(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.planQtyLessThanFinish"), shiftIndex, finishQty));
                }
            }
        }

        if (!hasAdjustField) {
            errorMessages.add(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.noAdjustField"));
        }

        if (!errorMessages.isEmpty()) {
            return AjaxResult.error(String.join(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.errorSeparator"), errorMessages));
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.validatePass"));
    }

    /**
     * 调量
     * 业务逻辑：
     * 1. 前置校验
     * 2. 更新各班次计划量和原因分析
     * 3. 如果原排程已发布成功，更新发布状态为待发布
     * 4. 记录操作日志
     *
     * @param entity 调量数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult changeQty(TqScheduleResult entity) {
        // 1. 前置校验
        AjaxResult validateResult = this.validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }

        // 2. 查询原记录
        TqScheduleResult record = tqScheduleResultMapper.selectById(entity.getId());
        if (record == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.recordNotExist"));
        }

        // 3. 构建更新实体（携带需要 set 的非 null 字段）和更新条件 wrapper
        // 遵循动态字段访问规范：班次字段通过 setFieldValueByFieldName 设置到 updateEntity，
        // 由 MyBatis-Plus 自动生成 set 子句；公共字段（releaseStatus/remark）仍用 LambdaUpdateWrapper.set。
        TqScheduleResult updateEntity = new TqScheduleResult();
        LambdaUpdateWrapper<TqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TqScheduleResult::getId, entity.getId());

        boolean hasChange = false;

        // 4. 更新各班次计划量和原因分析
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer newPlanQty = this.getPlanQtyByShiftIndex(entity, shiftIndex);
            Integer oldPlanQty = this.getPlanQtyByShiftIndex(record, shiftIndex);
            String newAnalysis = this.getAnalysisByShiftIndex(entity, shiftIndex);
            String oldAnalysis = this.getAnalysisByShiftIndex(record, shiftIndex);

            // 更新被修改的计划量
            if (newPlanQty != null && !Objects.equals(newPlanQty, oldPlanQty)) {
                this.setPlanQtyToUpdateEntity(updateEntity, shiftIndex, newPlanQty);
                hasChange = true;
            }

            // 更新原因分析（非空且与原值不同时更新）
            if (newAnalysis != null && !newAnalysis.equals(oldAnalysis)) {
                this.setAnalysisToUpdateEntity(updateEntity, shiftIndex, newAnalysis);
                hasChange = true;
            }
        }

        if (!hasChange) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.noChange"));
        }

        // 5. 更新备注
        if (entity.getRemark() != null && !entity.getRemark().equals(record.getRemark())) {
            updateWrapper.set(TqScheduleResult::getRemark, entity.getRemark());
        }

        // 6. 状态更新：如果原排程已发布成功，更新为待发布（需重新下发MES）
        if (ApsConstant.IS_RELEASE.equals(record.getReleaseStatus())) {
            updateWrapper.set(TqScheduleResult::getReleaseStatus, ApsConstant.WAIT_RELEASING);
        }

        // 7. 执行更新（updateEntity 携带班次字段的 set 子句，wrapper 携带公共字段和 where 条件）
        tqScheduleResultMapper.update(updateEntity, updateWrapper);

        log.info("胎圈排程调量成功，id：{}，胎圈代码：{}，机台：{}",
                entity.getId(), record.getBeadCode(), record.getMachineCode());

        // 8. 滚动更新：对每个被修改的班次执行同班次内时间重算（triggerType="3" 表示调量触发）
        this.triggerRollingUpdateForAllShifts("3", entity.getId(), record);

        // 9. 记录调度日志（6班次制，操作类型：1-调量，操作前=原记录，操作后=更新后记录）
        TqScheduleResult afterRecord = tqScheduleResultMapper.selectById(entity.getId());
        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, record, record, afterRecord);

        return AjaxResult.success(I18nUtil.getMessage("ui.tq.scheduleResult.changeQty.success"));
    }

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0的计划
     *
     * @param ids 需要删除的记录ID列表
     * @return 结果
     */
    @Deprecated
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult logicDeleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.scheduleResult.delete.selectRecord"));
        }

        for (Long id : ids) {
            TqScheduleResult record = tqScheduleResultMapper.selectById(id);
            if (record == null) {
                continue;
            }
            // 校验：已发布成功的计划不允许删除
            if (ApsConstant.IS_RELEASE.equals(record.getReleaseStatus())) {
                return AjaxResult.error(String.format(I18nUtil.getMessage("ui.tq.scheduleResult.delete.publishedForbidden"), record.getBeadCode()));
            }

            // 逻辑删除：更新 is_delete = 1
            LambdaUpdateWrapper<TqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TqScheduleResult::getId, id)
                    .set(TqScheduleResult::getIsDelete, 1);
            tqScheduleResultMapper.update(null, updateWrapper);

            // 滚动更新：删除后对每个有计划量的班次执行同班次内时间重算
            this.triggerRollingUpdateForAllShifts("4", id, record);

            // 记录调度日志（6班次制，操作类型：3-删除，操作前=原记录，操作后=null）
            this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_DELETE, record, record, null);

            log.info("胎圈排程逻辑删除成功，id：{}，胎圈代码：{}", id, record.getBeadCode());
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.tq.scheduleResult.delete.success"));
    }

    /**
     * 发布排程到MES
     * 业务流程：
     * 1. 查询排程日期下所有未发布/发布失败/待发布的胎圈排程记录
     * 2. 校验机台是否已分配
     * 3. 将6班数据拆分为3天的TqScheduleResultIssue列表（D日/D+1日/D+2日）
     * 4. 通过Feign调用itf服务的issueTqScheduleResult接口下发到MES
     * 5. 根据返回结果更新发布状态
     *
     * 6班→3天拆分映射：
     * Day1(D日)：MID=胎圈1班, NIGHT=null, DAY=null
     * Day2(D+1日)：NIGHT=胎圈2班, DAY=胎圈3班, MID=胎圈4班
     * Day3(D+2日)：NIGHT=胎圈5班, DAY=胎圈6班, MID=null
     * CX_CLASS3~8_PLAN全量传递到每条记录
     *
     * @param queryVO 查询条件（含排程日期等）
     * @return 结果
     */
    @Override
    public AjaxResult publish(TqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        if (scheduleDate == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        log.info("胎圈排程发布，排程日期：{}", scheduleDate);

        // 加分布式锁，避免并发下发MES造成数据不一致
        String lockKey = "tq:schedule:publish:" + DateUtil.formatDate(scheduleDate);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 最多等待5秒获取锁，获取到锁后不自动释放（在finally中手动释放）
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                return AjaxResult.error("排程正在发布中，请稍后再试");
            }
            return doPublish(scheduleDate);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("胎圈排程发布获取锁被中断", e);
            return AjaxResult.error("发布排程被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行发布排程到MES（实际业务逻辑，由publish加锁后调用）
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    private AjaxResult doPublish(Date scheduleDate) {

        // 1. 查询该排程日期下所有未发布/发布失败/待发布的记录
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getIsDelete, 0);
        wrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.in(TqScheduleResult::getReleaseStatus,
                ApsConstant.NO_RELEASE, ApsConstant.FAILURE_RELEASE, ApsConstant.WAIT_RELEASING);
        List<TqScheduleResult> scheduleList = tqScheduleResultMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(scheduleList)) {
            return AjaxResult.error("没有需要发布的排程数据");
        }

        // 2. 校验机台是否已分配
        List<TqScheduleResult> noMachineList = scheduleList.stream()
                .filter(s -> StringUtils.isBlank(s.getMachineCode()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(noMachineList)) {
            return AjaxResult.error("存在未分配机台的排程记录，请先分配机台");
        }

        // 3. 将6班数据拆分为3天的下发列表
        List<TqScheduleResultIssue> issueList = new ArrayList<>();
        for (TqScheduleResult source : scheduleList) {
            // D日 = 排程日期 - 2
            Date dDay = DateUtil.offsetDay(scheduleDate, -2);
            Date dPlus1Day = DateUtil.offsetDay(scheduleDate, -1);
            Date dPlus2Day = scheduleDate;

            // Day1(D日)：胎圈1班→MES中班
            TqScheduleResultIssue day1Issue = buildDay1Issue(source, dDay);
            issueList.add(day1Issue);

            // Day2(D+1日)：胎圈2班→MES夜班, 胎圈3班→MES早班, 胎圈4班→MES中班
            TqScheduleResultIssue day2Issue = buildDay2Issue(source, dPlus1Day);
            issueList.add(day2Issue);

            // Day3(D+2日)：胎圈5班→MES夜班, 胎圈6班→MES早班
            TqScheduleResultIssue day3Issue = buildDay3Issue(source, dPlus2Day);
            issueList.add(day3Issue);
        }

        // 4. 更新发布状态为"发布中"
        List<Long> ids = scheduleList.stream().map(TqScheduleResult::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<TqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(TqScheduleResult::getId, ids);
        updateWrapper.set(TqScheduleResult::getReleaseStatus, ApsConstant.RELEASING);
        tqScheduleResultMapper.update(null, updateWrapper);

        // 5. 通过Feign调用itf服务下发到MES
        AjaxResult ajaxResult;
        try {
            ajaxResult = mesItfService.issueTqScheduleResult(issueList);
            // 根据返回结果更新发布状态
            String status = ajaxResult.get(AjaxResult.CODE_TAG).equals(200)
                    ? ApsConstant.IS_RELEASE
                    : ApsConstant.FAILURE_RELEASE;
            LambdaUpdateWrapper<TqScheduleResult> resultWrapper = new LambdaUpdateWrapper<>();
            resultWrapper.in(TqScheduleResult::getId, ids);
            resultWrapper.set(TqScheduleResult::getReleaseStatus, status);
            tqScheduleResultMapper.update(null, resultWrapper);
        } catch (Exception e) {
            log.error("胎圈排程发布失败", e);
            // 发布失败，更新状态
            LambdaUpdateWrapper<TqScheduleResult> errorWrapper = new LambdaUpdateWrapper<>();
            errorWrapper.in(TqScheduleResult::getId, ids);
            errorWrapper.set(TqScheduleResult::getReleaseStatus, ApsConstant.FAILURE_RELEASE);
            tqScheduleResultMapper.update(null, errorWrapper);
            return AjaxResult.error("胎圈排程发布失败：" + e.getMessage());
        }

        return ajaxResult;
    }

    /**
     * 构建D日（今天）下发数据
     * 胎圈1班(D日中班) → MES中班(MID_PLAN_QTY)
     * 夜班、早班已过不下发，NEXT_MID不下发
     *
     * @param source 胎圈排程结果
     * @param dDay   D日日期
     * @return D日下发对象
     */
    private TqScheduleResultIssue buildDay1Issue(TqScheduleResult source, Date dDay) {
        TqScheduleResultIssue issue = buildBaseIssue(source, dDay);
        // 胎圈1班→MES中班
        issue.setMidPlanQty(source.getClass1PlanQty() != null ? source.getClass1PlanQty().doubleValue() : null);
        issue.setMidProduceOrder(source.getClass1Sequence());
        // 夜班、早班已过不下发
        issue.setNightPlanQty(null);
        issue.setNightProduceOrder(null);
        issue.setDayPlanQty(null);
        issue.setDayProduceOrder(null);
        return issue;
    }

    /**
     * 构建D+1日（明天）下发数据
     * 胎圈2班(D+1日夜班) → MES夜班(NIGHT_PLAN_QTY)
     * 胎圈3班(D+1日早班) → MES早班(DAY_PLAN_QTY)
     * 胎圈4班(D+1日中班) → MES中班(MID_PLAN_QTY)
     *
     * @param source    胎圈排程结果
     * @param dPlus1Day D+1日日期
     * @return D+1日下发对象
     */
    private TqScheduleResultIssue buildDay2Issue(TqScheduleResult source, Date dPlus1Day) {
        TqScheduleResultIssue issue = buildBaseIssue(source, dPlus1Day);
        // 胎圈2班→MES夜班
        issue.setNightPlanQty(source.getClass2PlanQty() != null ? source.getClass2PlanQty().doubleValue() : null);
        issue.setNightProduceOrder(source.getClass2Sequence());
        // 胎圈3班→MES早班
        issue.setDayPlanQty(source.getClass3PlanQty() != null ? source.getClass3PlanQty().doubleValue() : null);
        issue.setDayProduceOrder(source.getClass3Sequence());
        // 胎圈4班→MES中班
        issue.setMidPlanQty(source.getClass4PlanQty() != null ? source.getClass4PlanQty().doubleValue() : null);
        issue.setMidProduceOrder(source.getClass4Sequence());
        return issue;
    }

    /**
     * 构建D+2日（后天）下发数据
     * 胎圈5班(D+2日夜班) → MES夜班(NIGHT_PLAN_QTY)
     * 胎圈6班(D+2日早班) → MES早班(DAY_PLAN_QTY)
     * 中班尚未排产不下发
     *
     * @param source    胎圈排程结果
     * @param dPlus2Day D+2日日期
     * @return D+2日下发对象
     */
    private TqScheduleResultIssue buildDay3Issue(TqScheduleResult source, Date dPlus2Day) {
        TqScheduleResultIssue issue = buildBaseIssue(source, dPlus2Day);
        // 胎圈5班→MES夜班
        issue.setNightPlanQty(source.getClass5PlanQty() != null ? source.getClass5PlanQty().doubleValue() : null);
        issue.setNightProduceOrder(source.getClass5Sequence());
        // 胎圈6班→MES早班
        issue.setDayPlanQty(source.getClass6PlanQty() != null ? source.getClass6PlanQty().doubleValue() : null);
        issue.setDayProduceOrder(source.getClass6Sequence());
        // 中班尚未排产不下发
        issue.setMidPlanQty(null);
        issue.setMidProduceOrder(null);
        return issue;
    }

    /**
     * 构建基础下发对象（公共字段+成型1~8班计划量全量传递）
     *
     * @param source       胎圈排程结果
     * @param scheduleDate MES目标日期
     * @return 基础下发对象
     */
    private TqScheduleResultIssue buildBaseIssue(TqScheduleResult source, Date scheduleDate) {
        TqScheduleResultIssue issue = new TqScheduleResultIssue();
        // 日期转换：Date → LocalDate
        issue.setScheduleDate(scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        // 基础信息
        issue.setBatchNo(source.getBatchNo());
        issue.setOrderNo(source.getOrderNo());
        // 物料信息
        issue.setBeadCode(source.getBeadCode());
        issue.setSteelRingCode(source.getSteelRingCode());
        issue.setSpecSize(source.getProSize());
        issue.setMachineCode(source.getMachineCode());
        // 库存信息
        issue.setStockQty(source.getStockQty() != null ? source.getStockQty().doubleValue() : null);
        // 成型1~8班计划量全量传递（MES通过这些字段理解胎圈与成型的供应关系）
        // 1~2班为库存供应关系，3~8班为胎圈6班次产出供应关系，均从排程记录取值
        issue.setCxClass1Plan(source.getCxClass1Plan());
        issue.setCxClass2Plan(source.getCxClass2Plan());
        issue.setCxClass3Plan(source.getCxClass3Plan());
        issue.setCxClass4Plan(source.getCxClass4Plan());
        issue.setCxClass5Plan(source.getCxClass5Plan());
        issue.setCxClass6Plan(source.getCxClass6Plan());
        issue.setCxClass7Plan(source.getCxClass7Plan());
        issue.setCxClass8Plan(source.getCxClass8Plan());
        return issue;
    }

    /**
     * 查询排程日期是否已发布
     * 判断依据：该排程日期下所有未删除记录的发布状态均为"已发布"
     *
     * @param scheduleDate 排程日期
     * @return 是否已发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        if (scheduleDate == null) {
            return false;
        }
        // 查询该排程日期下所有未删除的记录
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.eq(TqScheduleResult::getIsDelete, 0);
        List<TqScheduleResult> list = tqScheduleResultMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            // 无记录视为未发布
            return false;
        }
        // 所有记录都已发布（RELEASE_STATUS=1）才返回 true
        return list.stream().allMatch(r -> ApsConstant.IS_RELEASE.equals(r.getReleaseStatus()));
    }

    // ==================== 新人工操作入口（走任务链路径） ====================

    /**
     * 人工插单（走任务链路径，支持锚点插入、resequence 重排）。
     *
     * <p>统一走 {@link TqManualOperationFacade#insertTask}，由门面负责多机台分布式锁、
     * 短事务、行锁、释放状态校验和调度日志，避免绕过安全约束。</p>
     *
     * <p>与旧 insertOrder 的差异：</p>
     * <ul>
     *   <li>支持锚点插入：anchorTaskId 不为空时在锚点之后插入，锚点之后任务 sequence +1</li>
     *   <li>自动重排顺位：resequence 保证同机台同班次 sequence 从1连续递增</li>
     *   <li>滚动重装箱：超额任务自动顺延到下一班次</li>
     * </ul>
     *
     * @param vo 插单请求
     * @return 结果
     */
    @Override
    public AjaxResult insertTask(TqInsertTaskRequestVo vo) {
        if (vo == null) {
            return AjaxResult.error("插单请求不能为空");
        }
        if (vo.getScheduleDate() == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (StringUtils.isBlank(vo.getBeadCode())) {
            return AjaxResult.error("胎圈代码不能为空");
        }
        if (StringUtils.isBlank(vo.getMachineCode())) {
            return AjaxResult.error("机台编号不能为空");
        }
        // 校验施工是否存在
        List<TqScheduleBaseInfoVo> baseInfoList = tqEngineService.listTqScheduleBaseInfo(
                Collections.singletonList(vo.getBeadCode()));
        if (CollectionUtils.isEmpty(baseInfoList)) {
            return AjaxResult.error("胎圈规格有误，施工不存在");
        }
        // 构建 TqScheduleResult 模板
        TqScheduleResult template = new TqScheduleResult();
        template.setFactoryCode(vo.getFactoryCode());
        template.setScheduleDate(vo.getScheduleDate());
        template.setBeadCode(vo.getBeadCode());
        template.setTriangleGlueCode(vo.getTriangleGlueCode());
        template.setProSize(vo.getProSize());
        template.setMachineCode(vo.getMachineCode());
        template.setClass1PlanQty(vo.getClass1PlanQty());
        template.setClass1Sequence(vo.getClass1Sequence());
        template.setClass1Analysis(vo.getClass1Analysis());
        template.setClass2PlanQty(vo.getClass2PlanQty());
        template.setClass2Sequence(vo.getClass2Sequence());
        template.setClass2Analysis(vo.getClass2Analysis());
        template.setClass3PlanQty(vo.getClass3PlanQty());
        template.setClass3Sequence(vo.getClass3Sequence());
        template.setClass3Analysis(vo.getClass3Analysis());
        template.setClass4PlanQty(vo.getClass4PlanQty());
        template.setClass4Sequence(vo.getClass4Sequence());
        template.setClass4Analysis(vo.getClass4Analysis());
        template.setClass5PlanQty(vo.getClass5PlanQty());
        template.setClass5Sequence(vo.getClass5Sequence());
        template.setClass5Analysis(vo.getClass5Analysis());
        template.setClass6PlanQty(vo.getClass6PlanQty());
        template.setClass6Sequence(vo.getClass6Sequence());
        template.setClass6Analysis(vo.getClass6Analysis());
        template.setRemark(vo.getRemark());
        template.setDataSource("1");
        template.setReleaseStatus(ApsConstant.NO_RELEASE);
        // 生成批次号、工单号（复用当前排程日期已有批次号，不影响其他记录）
        String scheduleDateStr = DateUtil.formatDate(vo.getScheduleDate());
        String[] batchAndOrder = tqEngineService.generateBatchNoAndOrderNo(scheduleDateStr);
        template.setBatchNo(batchAndOrder[0]);
        template.setOrderNo(batchAndOrder[1]);
        // 回显施工字段（钢丝圈、三角胶、尺寸），从施工表获取
        TqScheduleBaseInfoVo baseInfo = baseInfoList.get(0);
        template.setSteelRingCode(baseInfo.getSteelRingCode());
        if (StringUtils.isBlank(template.getTriangleGlueCode())) {
            template.setTriangleGlueCode(baseInfo.getTriangleGlueCode());
        }
        if (StringUtils.isBlank(template.getProSize())) {
            template.setProSize(baseInfo.getSpecSize());
        }
        // 走门面统一入口：门面负责分布式锁、短事务、行锁、释放状态校验和调度日志
        try {
            tqManualOperationFacade.insertTask(template);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("胎圈人工插单成功");
    }

    /**
     * 批量转机台（走门面统一入口，支持锚点、目标班次）。
     *
     * <p>对齐胎面 TmScheduleResultServiceImpl.batchChangeMachine：
     * 批量转机台仅支持同一目标机台。每条请求的 machineCode 即目标机台编码，
     * 源机台由门面按 id 从数据库读取并加行锁，避免请求携带的机台被篡改。</p>
     *
     * <p>门面负责：多机台分布式锁、短事务、行锁、释放状态校验、调度日志。</p>
     *
     * @param list 转机台请求列表（每条携带 id 与同一目标 machineCode）
     * @return 结果
     */
    @Override
    public AjaxResult batchChangeMachine(List<TqScheduleResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.schedule.changeMachine.batchEmpty"));
        }
        // 对齐胎面：批量转机台仅支持同一目标机台，避免门面将异构目标静默覆盖为单一目标
        String targetMachineCode = StringUtils.trimToEmpty(list.get(0).getMachineCode());
        if (StringUtils.isBlank(targetMachineCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.schedule.machineCode.empty"));
        }
        for (TqScheduleResult request : list) {
            if (!targetMachineCode.equals(StringUtils.trimToEmpty(request.getMachineCode()))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.tq.schedule.changeMachine.batchTargetNotSame"));
            }
        }
        try {
            tqManualOperationFacade.batchChangeMachine(targetMachineCode, list);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("胎圈批量转机台成功");
    }

    /**
     * 批量调量（走门面统一入口）。
     *
     * <p>门面负责：多机台分布式锁、短事务、行锁、释放状态校验、调度日志。</p>
     *
     * @param list 调量请求列表
     * @return 结果
     */
    @Override
    public AjaxResult batchChangeQty(List<TqScheduleResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.schedule.changeQty.batchEmpty"));
        }
        try {
            tqManualOperationFacade.batchChangeQty(list);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("胎圈批量调量成功");
    }

    /**
     * 批量删除（走门面统一入口，删除后 resequence 重排）。
     *
     * <p>门面负责：加载待删除记录、多机台分布式锁、短事务、行锁、释放状态校验、
     * 局部滚动、逻辑删除和调度日志，任一步失败整批回滚。</p>
     *
     * @param ids 排程记录ID列表
     * @return 结果
     */
    @Override
    public AjaxResult batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tq.schedule.delete.idsEmpty"));
        }
        try {
            tqManualOperationFacade.deleteTasks(ids);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("胎圈批量删除成功");
    }

    // ==================== 私有方法 ====================

    /**
     * 记录调度员排程操作日志（6班次制）
     *
     * <p>统一封装调度日志写入逻辑，覆盖4种操作类型：
     * <ul>
     *   <li>0-转机台：beforeRecord为原机台记录，afterRecord为新机台记录</li>
     *   <li>1-调量：beforeRecord为原记录，afterRecord为更新后记录</li>
     *   <li>2-插单：beforeRecord为null，afterRecord为新增记录</li>
     *   <li>3-删除：beforeRecord为原记录，afterRecord为null</li>
     * </ul>
     * 日志异常不影响主操作，仅记录错误日志。
     *
     * @param operType     操作类型：0-转机台、1-调量、2-插单、3-删除
     * @param baseRecord   基础记录（用于获取排程日期、胎圈代码、排程记录ID等公共字段）
     * @param beforeRecord 操作前记录（用于填充before*字段，插单时为null）
     * @param afterRecord  操作后记录（用于填充after*字段，删除时为null）
     */
    private void recordDispatcherLog(String operType, TqScheduleResult baseRecord,
                                     TqScheduleResult beforeRecord, TqScheduleResult afterRecord) {
        try {
            if (baseRecord == null) {
                log.warn("记录调度日志跳过：baseRecord为空");
                return;
            }
            TqDispatcherLog dispatcherLog = new TqDispatcherLog();
            dispatcherLog.setOperType(operType);
            dispatcherLog.setScheduleId(baseRecord.getId());
            dispatcherLog.setScheduleDate(baseRecord.getScheduleDate());
            dispatcherLog.setBeadCode(baseRecord.getBeadCode());
            // 操作前机台编码和6班次计划量
            if (beforeRecord != null) {
                dispatcherLog.setBeforeMachineCode(beforeRecord.getMachineCode());
                dispatcherLog.setBeforeClass1Plan(beforeRecord.getClass1PlanQty());
                dispatcherLog.setBeforeClass2Plan(beforeRecord.getClass2PlanQty());
                dispatcherLog.setBeforeClass3Plan(beforeRecord.getClass3PlanQty());
                dispatcherLog.setBeforeClass4Plan(beforeRecord.getClass4PlanQty());
                dispatcherLog.setBeforeClass5Plan(beforeRecord.getClass5PlanQty());
                dispatcherLog.setBeforeClass6Plan(beforeRecord.getClass6PlanQty());
            }
            // 操作后机台编码和6班次计划量
            if (afterRecord != null) {
                dispatcherLog.setAfterMachineCode(afterRecord.getMachineCode());
                dispatcherLog.setAfterClass1Plan(afterRecord.getClass1PlanQty());
                dispatcherLog.setAfterClass2Plan(afterRecord.getClass2PlanQty());
                dispatcherLog.setAfterClass3Plan(afterRecord.getClass3PlanQty());
                dispatcherLog.setAfterClass4Plan(afterRecord.getClass4PlanQty());
                dispatcherLog.setAfterClass5Plan(afterRecord.getClass5PlanQty());
                dispatcherLog.setAfterClass6Plan(afterRecord.getClass6PlanQty());
            }
            tqDispatcherLogService.insertTqDispatcherLog(dispatcherLog);
        } catch (Exception e) {
            // 日志记录失败不影响主操作
            log.error("记录胎圈调度日志失败，operType：{}，scheduleId：{}", operType, baseRecord.getId(), e);
        }
    }

    /**
     * 根据班次索引获取DTO中的计划量
     * 遵循动态字段访问规范：禁止使用 switch/case 硬编码访问班次字段。
     */
    private Integer getPlanQtyByClassIndex(TqInsertOrderDTO dto, int classIndex) {
        String fieldName = String.format(CLASS_PLAN_QTY_FIELD_TEMPLATE, classIndex);
        return (Integer) dto.getFieldValueByFieldName(fieldName);
    }

    /**
     * 根据班次索引获取DTO中的顺序
     * 遵循动态字段访问规范：禁止使用 switch/case 硬编码访问班次字段。
     */
    private Integer getSequenceByClassIndex(TqInsertOrderDTO dto, int classIndex) {
        String fieldName = String.format(CLASS_SEQUENCE_FIELD_TEMPLATE, classIndex);
        return (Integer) dto.getFieldValueByFieldName(fieldName);
    }

    /**
     * 解析当前班次索引（1~6）
     * 胎圈排程6班次时间窗口：
     * 1班：D日中班(16:00-24:00)
     * 2班：D+1日夜班(00:00-08:00)
     * 3班：D+1日早班(08:00-16:00)
     * 4班：D+1日中班(16:00-24:00)
     * 5班：D+2日夜班(00:00-08:00)
     * 6班：D+2日早班(08:00-16:00)
     * D = 排程日期 - 2
     *
     * 根据排程日期精确判断当前时间落在哪个班次窗口内，
     * 无排程日期时按当前小时回退到简化判断。
     *
     * @param scheduleDate 排程日期
     * @return 当前班次索引
     */
    private int resolveCurrentShiftIndex(Date scheduleDate) {
        if (scheduleDate == null) {
            // 无排程日期时按小时回退判断
            int hour = DateUtil.hour(new Date(), true);
            if (hour >= 16) {
                return 1;
            } else if (hour >= 8) {
                return 3;
            } else {
                return 2;
            }
        }
        Date now = new Date();
        // D日 = 排程日期 - 2
        Date dDay = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, -2));
        Date dPlus1Day = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, -1));
        Date dPlus2Day = DateUtil.beginOfDay(scheduleDate);
        // 各班次时间窗口
        // 1班：D日 16:00 - 24:00
        Date shift1Start = DateUtil.offsetHour(dDay, 16);
        Date shift1End = DateUtil.offsetHour(dDay, 24);
        // 2班：D+1日 00:00 - 08:00
        Date shift2Start = dPlus1Day;
        Date shift2End = DateUtil.offsetHour(dPlus1Day, 8);
        // 3班：D+1日 08:00 - 16:00
        Date shift3Start = DateUtil.offsetHour(dPlus1Day, 8);
        Date shift3End = DateUtil.offsetHour(dPlus1Day, 16);
        // 4班：D+1日 16:00 - 24:00
        Date shift4Start = DateUtil.offsetHour(dPlus1Day, 16);
        Date shift4End = DateUtil.offsetHour(dPlus1Day, 24);
        // 5班：D+2日 00:00 - 08:00
        Date shift5Start = dPlus2Day;
        Date shift5End = DateUtil.offsetHour(dPlus2Day, 8);
        // 6班：D+2日 08:00 - 16:00
        Date shift6Start = DateUtil.offsetHour(dPlus2Day, 8);
        Date shift6End = DateUtil.offsetHour(dPlus2Day, 16);
        if (!now.before(shift1Start) && now.before(shift1End)) {
            return 1;
        }
        if (!now.before(shift2Start) && now.before(shift2End)) {
            return 2;
        }
        if (!now.before(shift3Start) && now.before(shift3End)) {
            return 3;
        }
        if (!now.before(shift4Start) && now.before(shift4End)) {
            return 4;
        }
        if (!now.before(shift5Start) && now.before(shift5End)) {
            return 5;
        }
        if (!now.before(shift6Start) && now.before(shift6End)) {
            return 6;
        }
        // 不在任何班次窗口内（排程尚未开始或已结束），默认返回1
        return 1;
    }

    /**
     * 获取一条排程记录6个班次中最小的非空顺序号
     * 用于"按任一班次有顺序号"排序
     *
     * @param record 排程记录
     * @return 最小顺序号，无顺序号时返回 Integer.MAX_VALUE
     */
    private int getMinSequenceOfRecord(TqScheduleResult record) {
        int minSeq = Integer.MAX_VALUE;
        if (record.getClass1Sequence() != null && record.getClass1Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass1Sequence());
        }
        if (record.getClass2Sequence() != null && record.getClass2Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass2Sequence());
        }
        if (record.getClass3Sequence() != null && record.getClass3Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass3Sequence());
        }
        if (record.getClass4Sequence() != null && record.getClass4Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass4Sequence());
        }
        if (record.getClass5Sequence() != null && record.getClass5Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass5Sequence());
        }
        if (record.getClass6Sequence() != null && record.getClass6Sequence() > 0) {
            minSeq = Math.min(minSeq, record.getClass6Sequence());
        }
        return minSeq;
    }

    /**
     * 获取第二个在产规格的最小顺序号
     * 用于校验插单只能加到第二个在产规格之后
     *
     * @param existingList 已有排程记录列表（按顺序排序）
     * @return 第二个在产规格的最小顺序号
     */
    private int getMinSequenceFromSecondSpec(List<TqScheduleResult> existingList) {
        if (existingList.size() < 2) {
            return 1;
        }
        // 取第二条记录的顺序号（取所有班次顺序中最小的非空值）
        TqScheduleResult secondRecord = existingList.get(1);
        int minSeq = Integer.MAX_VALUE;
        if (secondRecord.getClass1Sequence() != null && secondRecord.getClass1Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass1Sequence());
        }
        if (secondRecord.getClass2Sequence() != null && secondRecord.getClass2Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass2Sequence());
        }
        if (secondRecord.getClass3Sequence() != null && secondRecord.getClass3Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass3Sequence());
        }
        if (secondRecord.getClass4Sequence() != null && secondRecord.getClass4Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass4Sequence());
        }
        if (secondRecord.getClass5Sequence() != null && secondRecord.getClass5Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass5Sequence());
        }
        if (secondRecord.getClass6Sequence() != null && secondRecord.getClass6Sequence() > 0) {
            minSeq = Math.min(minSeq, secondRecord.getClass6Sequence());
        }
        return minSeq == Integer.MAX_VALUE ? 1 : minSeq;
    }

    /**
     * 触发滚动更新（遍历6个班次，对有计划量的班次执行同班次内时间重算）
     *
     * <p>MVP阶段：仅同班次内时间重算，不跨班次推迟。</p>
     * <p>异常处理：滚动更新失败不影响主操作（已插入/修改的数据保留），
     * 仅记录日志，提示用户重试。</p>
     *
     * @param triggerType 触发类型：1-插单，2-转机台，3-调量，4-删除
     * @param sourceId    触发源排程记录ID
     * @param record      排程记录（含机台、胎圈、6班计划量）
     */
    private void triggerRollingUpdateForAllShifts(String triggerType, Long sourceId, TqScheduleResult record) {
        if (record == null || record.getScheduleDate() == null || StringUtils.isBlank(record.getMachineCode())) {
            log.warn("触发滚动更新跳过：排程记录信息不完整，sourceId={}", sourceId);
            return;
        }

        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer planQty = getPlanQtyByShiftIndex(record, shiftIndex);
            // 仅对有计划量的班次触发滚动更新
            if (planQty == null || planQty <= 0) {
                continue;
            }
            try {
                RollingUpdateResult result = tqRollingUpdateService.manualRollingUpdate(
                        triggerType, sourceId, record.getScheduleDate(),
                        shiftIndex, record.getMachineCode(), record.getBeadCode());
                if (result.isSuccess()) {
                    log.info("滚动更新成功，班次：{}，影响记录数：{}", shiftIndex, result.getAffectedCount());
                } else {
                    log.warn("滚动更新失败，班次：{}，原因：{}", shiftIndex, result.getErrorMsg());
                }
            } catch (Exception e) {
                // 滚动更新失败不影响主操作，仅记录日志
                log.error("滚动更新异常，班次：{}，sourceId：{}，原因：{}", shiftIndex, sourceId, e.getMessage(), e);
            }
        }
    }

    /**
     * 根据班次索引获取实体中的计划量
     * 遵循动态字段访问规范：禁止使用 switch/case 硬编码访问班次字段。
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 计划量
     */
    private Integer getPlanQtyByShiftIndex(TqScheduleResult entity, int shiftIndex) {
        String fieldName = String.format(CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftIndex);
        return (Integer) entity.getFieldValueByFieldName(fieldName);
    }

    /**
     * 根据班次索引获取实体中的完成量
     * 遵循动态字段访问规范：禁止使用 switch/case 硬编码访问班次字段。
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 完成量
     */
    private Integer getFinishQtyByShiftIndex(TqScheduleResult entity, int shiftIndex) {
        String fieldName = String.format(CLASS_FINISH_QTY_FIELD_TEMPLATE, shiftIndex);
        return (Integer) entity.getFieldValueByFieldName(fieldName);
    }

    /**
     * 根据班次索引获取实体中的原因分析
     * 遵循动态字段访问规范：禁止使用 switch/case 硬编码访问班次字段。
     *
     * @param entity     排程结果实体
     * @param shiftIndex 班次索引（1~6）
     * @return 原因分析
     */
    private String getAnalysisByShiftIndex(TqScheduleResult entity, int shiftIndex) {
        String fieldName = String.format(CLASS_ANALYSIS_FIELD_TEMPLATE, shiftIndex);
        return (String) entity.getFieldValueByFieldName(fieldName);
    }

    /**
     * 设置指定班次计划量到UpdateWrapper
     * 遵循动态字段访问规范：通过 setFieldValueByFieldName 设置到更新实体，
     * 配合 mapper.update(updateEntity, wrapper) 实现动态 set 子句。
     *
     * @param updateEntity 更新实体（用于携带 set 子句的非 null 字段）
     * @param shiftIndex   班次索引（1~6）
     * @param planQty      计划量
     */
    private void setPlanQtyToUpdateEntity(TqScheduleResult updateEntity,
                                          int shiftIndex, Integer planQty) {
        String fieldName = String.format(CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftIndex);
        updateEntity.setFieldValueByFieldName(fieldName, planQty);
    }

    /**
     * 设置指定班次原因分析到UpdateWrapper
     * 遵循动态字段访问规范：通过 setFieldValueByFieldName 设置到更新实体。
     *
     * @param updateEntity 更新实体
     * @param shiftIndex   班次索引（1~6）
     * @param analysis     原因分析
     */
    private void setAnalysisToUpdateEntity(TqScheduleResult updateEntity,
                                           int shiftIndex, String analysis) {
        String fieldName = String.format(CLASS_ANALYSIS_FIELD_TEMPLATE, shiftIndex);
        updateEntity.setFieldValueByFieldName(fieldName, analysis);
    }

    /**
     * 判断指定班次是否已成为历史班次
     * 胎圈排程6班次时间窗口：
     * 1班：D日中班(16:00-24:00)
     * 2班：D+1日夜班(00:00-08:00)
     * 3班：D+1日早班(08:00-16:00)
     * 4班：D+1日中班(16:00-24:00)
     * 5班：D+2日夜班(00:00-08:00)
     * 6班：D+2日早班(08:00-16:00)
     * D = 排程日期 - 2（即今天）
     *
     * @param record     排程结果记录
     * @param shiftIndex 班次索引（1~6）
     * @param now        当前时间
     * @return true表示班次已结束，属于历史班次
     */
    private boolean isHistoryShift(TqScheduleResult record, int shiftIndex, Date now) {
        if (record.getScheduleDate() == null) {
            return false;
        }
        // D = 排程日期 - 2
        Date dDay = DateUtil.beginOfDay(DateUtil.offsetDay(record.getScheduleDate(), -2));
        Date shiftEndTime = resolveShiftEndTime(dDay, shiftIndex);
        if (shiftEndTime == null) {
            return false;
        }
        return !now.before(shiftEndTime);
    }

    /**
     * 根据D日和班次索引推导班次结束时间
     *
     * @param dDay       D日（排程日期-2）
     * @param shiftIndex 班次索引（1~6）
     * @return 班次结束时间
     */
    private Date resolveShiftEndTime(Date dDay, int shiftIndex) {
        switch (shiftIndex) {
            case 1:
                // 1班：D日中班(16:00-24:00)，结束时间=D日24:00=D+1日00:00
                return DateUtil.endOfDay(dDay);
            case 2:
                // 2班：D+1日夜班(00:00-08:00)，结束时间=D+1日08:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 1)), 8);
            case 3:
                // 3班：D+1日早班(08:00-16:00)，结束时间=D+1日16:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 1)), 16);
            case 4:
                // 4班：D+1日中班(16:00-24:00)，结束时间=D+1日24:00=D+2日00:00
                return DateUtil.endOfDay(DateUtil.offsetDay(dDay, 1));
            case 5:
                // 5班：D+2日夜班(00:00-08:00)，结束时间=D+2日08:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 2)), 8);
            case 6:
                // 6班：D+2日早班(08:00-16:00)，结束时间=D+2日16:00
                return DateUtil.offsetHour(DateUtil.beginOfDay(DateUtil.offsetDay(dDay, 2)), 16);
            default:
                return null;
        }
    }
}

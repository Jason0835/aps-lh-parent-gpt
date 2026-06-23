package com.zlt.aps.tq.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.aps.tq.api.domain.entity.TqRollingLogDetail;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;
import com.zlt.aps.tq.engine.vo.TqRollingContext;
import com.zlt.aps.tq.engine.vo.TqRollingTaskNode;
import com.zlt.aps.tq.mapper.TqMachineSpecSpeedMapper;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.mapper.TqRollingLogDetailMapper;
import com.zlt.aps.tq.mapper.TqRollingLogMapper;
import com.zlt.aps.tq.mapper.TqStockMapper;
import com.zlt.aps.tq.service.ITqRollingLogDetailService;
import com.zlt.aps.tq.service.ITqRollingLogService;
import com.zlt.aps.tq.service.ITqRollingUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 胎圈排程滚动更新Service实现类
 *
 * <p>MVP阶段实现范围：</p>
 * <ul>
 *   <li>仅手动触发（插单/调量/转机台/删除后）</li>
 *   <li>仅同班次内的时间重算和顺序调整</li>
 *   <li>不实现跨班次推迟（场景较复杂，完整版补充）</li>
 *   <li>日志仅记录主表，明细表后续补充</li>
 * </ul>
 *
 * <p>风险点防护：</p>
 * <ul>
 *   <li>风险点1（性能）：懒加载+批量SQL+请求内缓存</li>
 *   <li>风险点2（死循环）：MVP不涉及跨班次推迟，无需防护</li>
 *   <li>风险点3（并发）：@DistributedLock 按排程日期加锁</li>
 *   <li>风险点4（生产速度）：从 TqMachineSpecSpeed.standardSpeed 获取，缺失抛异常</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
public class TqRollingUpdateServiceImpl implements ITqRollingUpdateService {

    @Resource
    private TqNewScheduleResultMapper tqNewScheduleResultMapper;

    @Resource
    private TqMachineSpecSpeedMapper tqMachineSpecSpeedMapper;

    @Resource
    private ITqRollingLogService tqRollingLogService;

    @Resource
    private TqRollingLogMapper tqRollingLogMapper;

    @Resource
    private ITqRollingLogDetailService tqRollingLogDetailService;

    @Resource
    private TqRollingLogDetailMapper tqRollingLogDetailMapper;

    @Resource
    private TqStockMapper tqStockMapper;

    /** 任务状态：正常 */
    private static final String TASK_STATUS_NORMAL = "0";
    /** 任务状态：已取消 */
    private static final String TASK_STATUS_CANCELLED = "1";
    /** 任务状态：已推迟 */
    private static final String TASK_STATUS_POSTPONED = "2";

    /** 变更类型：1-时间 */
    private static final String CHANGE_TYPE_TIME = "1";
    /** 变更类型：2-顺序 */
    private static final String CHANGE_TYPE_SEQUENCE = "2";
    /** 变更类型：3-状态 */
    private static final String CHANGE_TYPE_STATUS = "3";
    /** 变更类型：4-计划量 */
    private static final String CHANGE_TYPE_PLAN_QTY = "4";

    /** 日期格式化（用于明细记录前后值） */
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 默认单班时长（小时） */
    private static final double DEFAULT_SHIFT_HOURS = 8.0;

    /** 默认规格切换时长（小时） */
    private static final double DEFAULT_SWITCH_TIME = 0.5;

    /**
     * 手动触发滚动更新
     *
     * <p>分布式锁策略：</p>
     * <ul>
     *   <li>锁Key：TQ:ROLLING:{排程日期}</li>
     *   <li>waitTime=3s：手动操作允许短暂等待</li>
     *   <li>leaseTime=60s：足够完成单次滚动</li>
     * </ul>
     *
     * @param triggerType     触发类型：1-插单，2-转机台，3-调量，4-删除
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param shiftIndex      触发班次索引（1~6）
     * @param machineCode     触发机台编号
     * @param beadCode        触发胎圈代码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
            key = "'TQ:ROLLING:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd')",
            waitTime = 3,
            leaseTime = 60,
            failMsg = "ui.tq.rolling.manual.conflict"
    )
    public RollingUpdateResult manualRollingUpdate(String triggerType, Long triggerSourceId,
                                                   Date scheduleDate, int shiftIndex,
                                                   String machineCode, String beadCode) {
        // 生成滚动批次号
        String batchNo = generateBatchNo();
        log.info("胎圈排程滚动更新开始，批次号：{}，触发类型：{}，触发源ID：{}，排程日期：{}，班次：{}，机台：{}",
                batchNo, triggerType, triggerSourceId, scheduleDate, shiftIndex, machineCode);

        // 1. 创建日志主表记录（状态：进行中）
        TqRollingLog rollingLog = createRollingLog(batchNo, triggerType, triggerSourceId,
                scheduleDate, shiftIndex, machineCode, beadCode);
        tqRollingLogMapper.insert(rollingLog);

        try {
            // 2. 构建滚动上下文
            TqRollingContext context = buildContext(triggerType, triggerSourceId, scheduleDate,
                    shiftIndex, machineCode, beadCode, batchNo);
            // 设置日志主表ID（用于明细记录关联）
            context.setRollingLogId(rollingLog.getId());

            // 3. 加载触发机台的任务链（懒加载：仅加载触发机台）
            LinkedList<TqRollingTaskNode> taskChain = loadTaskChainFromDb(machineCode, scheduleDate, shiftIndex);
            if (taskChain == null || taskChain.isEmpty()) {
                log.warn("胎圈排程滚动更新：机台{}在班次{}无任务链，无需滚动", machineCode, shiftIndex);
                updateRollingLogSuccess(rollingLog, 0, 0, 0, "无任务链，无需滚动");
                return RollingUpdateResult.success(batchNo, 0, 0, 0);
            }
            context.getTaskChainMap().put(machineCode, taskChain);

            // 4. 计算滚动前预计库存（基于当前库存和当天排程计划）
            double beforeStock = calculateExpectedStock(scheduleDate, beadCode);
            context.setBeforeStockQty(beforeStock);
            log.info("胎圈排程滚动更新：滚动前预计库存={}", beforeStock);

            // 5. 执行同班次内时间重算（MVP核心逻辑）
            recalculateShiftTimes(context, machineCode, shiftIndex);

            // 6. 持久化变更（批量更新）
            int affectedCount = persistChanges(context, machineCode, shiftIndex);

            // 7. 计算滚动后预计库存（基于变更后的排程计划）
            double afterStock = calculateExpectedStock(scheduleDate, beadCode);
            context.setAfterStockQty(afterStock);
            log.info("胎圈排程滚动更新：滚动后预计库存={}", afterStock);

            // 8. 更新日志为成功
            updateRollingLogSuccess(rollingLog, affectedCount, context.getBeforeStockQty(),
                    context.getAfterStockQty(), context.getAdjustReason());

            log.info("胎圈排程滚动更新成功，批次号：{}，影响记录数：{}", batchNo, affectedCount);
            return RollingUpdateResult.success(batchNo, affectedCount,
                    context.getBeforeStockQty(), context.getAfterStockQty());

        } catch (Exception e) {
            log.error("胎圈排程滚动更新失败，批次号：{}", batchNo, e);
            // 更新日志为失败
            updateRollingLogFailure(rollingLog, e.getMessage());
            // 抛出异常，事务回滚
            throw new RuntimeException("胎圈排程滚动更新失败：" + e.getMessage(), e);
        }
    }

    // ==================== 核心逻辑：同班次内时间重算 ====================

    /**
     * 同班次内时间重算（MVP核心）
     *
     * <p>算法：</p>
     * <ol>
     *   <li>按生产顺序排序任务链</li>
     *   <li>第一个任务的预计开始时间 = 班次开始时间</li>
     *   <li>后续任务的预计开始时间 = 上一个任务预计结束时间 + 规格切换时长</li>
     *   <li>预计结束时间 = 预计开始时间 + (计划量 / 生产速度)</li>
     *   <li>标记有变更的节点</li>
     * </ol>
     *
     * @param context    滚动上下文
     * @param machineCode 机台编号
     * @param shiftIndex 班次索引
     */
    private void recalculateShiftTimes(TqRollingContext context, String machineCode, int shiftIndex) {
        LinkedList<TqRollingTaskNode> taskChain = context.getTaskChainMap().get(machineCode);
        if (taskChain == null || taskChain.isEmpty()) {
            return;
        }

        // 1. 按生产顺序排序
        taskChain.sort(Comparator.comparingInt(TqRollingTaskNode::getProduceOrder));

        // 2. 班次开始时间
        Date shiftStartTime = context.getShiftStartTime();
        if (shiftStartTime == null) {
            // 兜底：使用排程日期 + (班次-1) * 8小时
            shiftStartTime = calculateShiftStartTime(context.getScheduleDate(), shiftIndex);
            context.setShiftStartTime(shiftStartTime);
        }

        // 3. 遍历任务链，重算时间
        Date prevEndTime = null;
        String prevBeadCode = null;
        for (int i = 0; i < taskChain.size(); i++) {
            TqRollingTaskNode node = taskChain.get(i);
            // 跳过已取消的任务
            if (TASK_STATUS_CANCELLED.equals(node.getTaskStatus())) {
                continue;
            }

            // 计算预计开始时间
            Date newStartTime;
            if (i == 0 || prevEndTime == null) {
                // 第一个任务：班次开始时间
                newStartTime = shiftStartTime;
                node.setFirstInShift(true);
            } else {
                // 后续任务：上一个任务结束时间 + 规格切换时长
                double switchTime = calculateSwitchTime(prevBeadCode, node.getBeadCode());
                newStartTime = DateUtil.offsetHour(prevEndTime, (int) Math.ceil(switchTime));
            }

            // 计算预计结束时间 = 开始时间 + (计划量 / 生产速度) 小时
            double speed = getProductionSpeed(context, node.getMachineId(), node.getBeadCode());
            double planQty = node.getPlanQty();
            if (speed <= 0) {
                throw new RuntimeException("机台[" + node.getMachineId() + "]胎圈[" + node.getBeadCode()
                        + "]生产速度配置异常：" + speed);
            }
            double hours = planQty / speed;
            Date newEndTime = DateUtil.offsetHour(newStartTime, (int) Math.ceil(hours));

            // 检测是否有变更
            boolean changed = !equalsDate(node.getStartTime(), newStartTime)
                    || !equalsDate(node.getEndTime(), newEndTime);
            if (changed) {
                node.setStartTime(newStartTime);
                node.setEndTime(newEndTime);
                context.setHasChange(true);
            }

            prevEndTime = newEndTime;
            prevBeadCode = node.getBeadCode();
        }

        // 4. 计算班次结束时间，标记超时任务（MVP不推迟，仅记录日志）
        Date shiftEndTime = DateUtil.offsetHour(shiftStartTime, (int) context.getShiftHours());
        context.setShiftEndTime(shiftEndTime);
        List<TqRollingTaskNode> overTimeNodes = taskChain.stream()
                .filter(n -> !TASK_STATUS_CANCELLED.equals(n.getTaskStatus()))
                .filter(n -> n.getEndTime() != null && n.getEndTime().after(shiftEndTime))
                .collect(Collectors.toList());
        if (!overTimeNodes.isEmpty()) {
            log.warn("胎圈排程滚动更新：机台{}班次{}有{}个任务超过班次结束时间，MVP阶段不推迟，需人工处理",
                    machineCode, shiftIndex, overTimeNodes.size());
            // MVP阶段：仅记录告警，不推迟
            // 完整版：将超时任务推迟到下个班次
        }
    }

    // ==================== 任务链加载 ====================

    /**
     * 从数据库加载任务链
     *
     * <p>性能优化：仅加载触发机台在指定班次的任务链，不加载全部机台。</p>
     *
     * @param machineCode  机台编号
     * @param scheduleDate 排程日期
     * @param shiftIndex   班次索引
     * @return 任务链（按生产顺序排序）
     */
    private LinkedList<TqRollingTaskNode> loadTaskChainFromDb(String machineCode, Date scheduleDate, int shiftIndex) {
        // 查询该机台在该排程日期的所有排程记录
        LambdaQueryWrapper<TqNewScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqNewScheduleResult::getMachineCode, machineCode)
               .eq(TqNewScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(TqNewScheduleResult::getIsDelete, 0)
               .orderByAsc(TqNewScheduleResult::getId);

        List<TqNewScheduleResult> scheduleList = tqNewScheduleResultMapper.selectList(wrapper);
        if (scheduleList == null || scheduleList.isEmpty()) {
            return new LinkedList<>();
        }

        // 转换为任务节点（仅提取指定班次的数据）
        LinkedList<TqRollingTaskNode> taskChain = new LinkedList<>();
        for (TqNewScheduleResult schedule : scheduleList) {
            TqRollingTaskNode node = convertToTaskNode(schedule, shiftIndex);
            if (node != null) {
                taskChain.add(node);
            }
        }

        // 按生产顺序排序
        taskChain.sort(Comparator.comparingInt(TqRollingTaskNode::getProduceOrder));
        return taskChain;
    }

    /**
     * 将排程记录转换为指定班次的任务节点
     *
     * @param schedule   排程记录
     * @param shiftIndex 班次索引（1~6）
     * @return 任务节点（若该班次无计划则返回null）
     */
    private TqRollingTaskNode convertToTaskNode(TqNewScheduleResult schedule, int shiftIndex) {
        Integer planQty = getPlanQtyByShiftIndex(schedule, shiftIndex);
        Integer sequence = getSequenceByShiftIndex(schedule, shiftIndex);
        Integer finishQty = getFinishQtyByShiftIndex(schedule, shiftIndex);
        Date startTime = getStartTimeByShiftIndex(schedule, shiftIndex);
        Date endTime = getEndTimeByShiftIndex(schedule, shiftIndex);
        String taskStatus = getTaskStatusByShiftIndex(schedule, shiftIndex);

        // 该班次无计划，跳过
        if (planQty == null && sequence == null) {
            return null;
        }

        TqRollingTaskNode node = new TqRollingTaskNode();
        node.setScheduleId(schedule.getId());
        node.setClassIndex(shiftIndex);
        node.setMachineCode(schedule.getMachineCode());
        node.setBeadCode(schedule.getBeadCode());
        node.setPlanQty(planQty == null ? 0 : planQty);
        node.setFinishQty(finishQty == null ? 0 : finishQty);
        node.setProduceOrder(sequence == null ? 0 : sequence);
        node.setStartTime(startTime);
        node.setEndTime(endTime);
        node.setTaskStatus(StringUtils.isBlank(taskStatus) ? TASK_STATUS_NORMAL : taskStatus);
        // machineId 需通过机台编号查询，此处暂用null，速度查询时按 machineCode 兜底
        node.setMachineId(null);
        return node;
    }

    // ==================== 持久化 ====================

    /**
     * 持久化变更（批量更新）
     *
     * <p>性能优化：使用 LambdaUpdateWrapper 单条更新，避免全字段更新。</p>
     * <p>明细记录：更新前查询原值，对比变更字段，批量插入日志明细。</p>
     *
     * @param context     滚动上下文
     * @param machineCode 机台编号
     * @param shiftIndex  班次索引
     * @return 影响的记录数
     */
    private int persistChanges(TqRollingContext context, String machineCode, int shiftIndex) {
        if (!context.isHasChange()) {
            log.info("胎圈排程滚动更新：无变更，跳过持久化");
            return 0;
        }

        LinkedList<TqRollingTaskNode> taskChain = context.getTaskChainMap().get(machineCode);
        if (taskChain == null || taskChain.isEmpty()) {
            return 0;
        }

        // 查询主表日志ID（用于关联明细）
        Long logId = context.getRollingLogId();
        List<TqRollingLogDetail> detailList = new ArrayList<>();

        int affectedCount = 0;
        for (TqRollingTaskNode node : taskChain) {
            if (node.getScheduleId() == null) {
                continue;
            }

            // 查询变更前的原值（用于对比记录明细）
            TqNewScheduleResult original = tqNewScheduleResultMapper.selectById(node.getScheduleId());

            // 构建更新条件
            LambdaUpdateWrapper<TqNewScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TqNewScheduleResult::getId, node.getScheduleId());

            // 按班次设置开始时间、结束时间、任务状态
            setUpdateTimeFields(updateWrapper, shiftIndex, node.getStartTime(), node.getEndTime(),
                    node.getTaskStatus(), node.getProduceOrder());

            int rows = tqNewScheduleResultMapper.update(null, updateWrapper);
            if (rows > 0) {
                affectedCount += rows;
                // 记录变更明细（仅当有变更时）
                if (original != null && logId != null) {
                    detailList.addAll(buildChangeDetails(logId, original, node, shiftIndex, context));
                }
            }
        }

        // 批量保存日志明细
        if (!detailList.isEmpty()) {
            for (TqRollingLogDetail detail : detailList) {
                tqRollingLogDetailMapper.insert(detail);
            }
            log.info("胎圈排程滚动更新：记录日志明细{}条", detailList.size());
        }

        return affectedCount;
    }

    /**
     * 构建变更明细列表
     *
     * <p>对比原值和新值，仅记录发生变更的字段。</p>
     *
     * @param logId     主表日志ID
     * @param original  变更前排程记录
     * @param newNode   变更后任务节点
     * @param shiftIndex 班次索引
     * @param context   滚动上下文
     * @return 变更明细列表
     */
    private List<TqRollingLogDetail> buildChangeDetails(Long logId, TqNewScheduleResult original,
                                                         TqRollingTaskNode newNode, int shiftIndex,
                                                         TqRollingContext context) {
        List<TqRollingLogDetail> detailList = new ArrayList<>();
        String changeReason = buildChangeReason(context);

        // 获取原值
        Date oldStartTime = getStartTimeByShiftIndex(original, shiftIndex);
        Date oldEndTime = getEndTimeByShiftIndex(original, shiftIndex);
        Integer oldSequence = getSequenceByShiftIndex(original, shiftIndex);
        String oldTaskStatus = getTaskStatusByShiftIndex(original, shiftIndex);

        // 1. 对比开始时间
        if (!equalsDate(oldStartTime, newNode.getStartTime())) {
            detailList.add(buildDetail(logId, original, newNode, shiftIndex,
                    "START_TIME", formatDate(oldStartTime), formatDate(newNode.getStartTime()),
                    CHANGE_TYPE_TIME, changeReason));
        }

        // 2. 对比结束时间
        if (!equalsDate(oldEndTime, newNode.getEndTime())) {
            detailList.add(buildDetail(logId, original, newNode, shiftIndex,
                    "END_TIME", formatDate(oldEndTime), formatDate(newNode.getEndTime()),
                    CHANGE_TYPE_TIME, changeReason));
        }

        // 3. 对比顺序
        if (oldSequence == null || oldSequence != newNode.getProduceOrder()) {
            detailList.add(buildDetail(logId, original, newNode, shiftIndex,
                    "SEQUENCE", String.valueOf(oldSequence), String.valueOf(newNode.getProduceOrder()),
                    CHANGE_TYPE_SEQUENCE, changeReason));
        }

        // 4. 对比任务状态
        String newNodeStatus = StringUtils.isBlank(newNode.getTaskStatus()) ? TASK_STATUS_NORMAL : newNode.getTaskStatus();
        String oldStatus = StringUtils.isBlank(oldTaskStatus) ? TASK_STATUS_NORMAL : oldTaskStatus;
        if (!oldStatus.equals(newNodeStatus)) {
            detailList.add(buildDetail(logId, original, newNode, shiftIndex,
                    "TASK_STATUS", oldStatus, newNodeStatus,
                    CHANGE_TYPE_STATUS, changeReason));
        }

        return detailList;
    }

    /**
     * 构建单条变更明细
     */
    private TqRollingLogDetail buildDetail(Long logId, TqNewScheduleResult original, TqRollingTaskNode newNode,
                                            int shiftIndex, String fieldName, String beforeValue,
                                            String afterValue, String changeType, String changeReason) {
        TqRollingLogDetail detail = new TqRollingLogDetail();
        detail.setLogId(logId);
        detail.setScheduleId(original.getId());
        detail.setMachineCode(original.getMachineCode());
        detail.setBeadCode(original.getBeadCode());
        detail.setShiftIndex(shiftIndex);
        detail.setFieldName(fieldName);
        detail.setBeforeValue(beforeValue);
        detail.setAfterValue(afterValue);
        detail.setChangeType(changeType);
        detail.setChangeReason(changeReason);
        detail.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        detail.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        detail.setCreateTime(new Date());
        try {
            detail.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            // 定时任务场景无登录用户，忽略
        }
        return detail;
    }

    /**
     * 构建变更原因
     */
    private String buildChangeReason(TqRollingContext context) {
        String triggerType = context.getTriggerType();
        if (StringUtils.isBlank(triggerType)) {
            return "滚动更新";
        }
        switch (triggerType) {
            case "0": return "自动定时滚动";
            case "1": return "插单触发";
            case "2": return "转机台触发";
            case "3": return "调量触发";
            case "4": return "删除触发";
            default: return "滚动更新";
        }
    }

    /**
     * 格式化日期（用于明细记录）
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_FORMAT_PATTERN).format(date);
    }

    /**
     * 按班次设置时间字段到 UpdateWrapper
     */
    private void setUpdateTimeFields(LambdaUpdateWrapper<TqNewScheduleResult> wrapper, int shiftIndex,
                                      Date startTime, Date endTime, String taskStatus, int produceOrder) {
        switch (shiftIndex) {
            case 1:
                wrapper.set(TqNewScheduleResult::getClass1StartTime, startTime)
                       .set(TqNewScheduleResult::getClass1EndTime, endTime)
                       .set(TqNewScheduleResult::getClass1TaskStatus, taskStatus)
                       .set(TqNewScheduleResult::getClass1Sequence, produceOrder);
                break;
            case 2:
                wrapper.set(TqNewScheduleResult::getClass2StartTime, startTime)
                       .set(TqNewScheduleResult::getClass2EndTime, endTime)
                       .set(TqNewScheduleResult::getClass2TaskStatus, taskStatus)
                       .set(TqNewScheduleResult::getClass2Sequence, produceOrder);
                break;
            case 3:
                wrapper.set(TqNewScheduleResult::getClass3StartTime, startTime)
                       .set(TqNewScheduleResult::getClass3EndTime, endTime)
                       .set(TqNewScheduleResult::getClass3TaskStatus, taskStatus)
                       .set(TqNewScheduleResult::getClass3Sequence, produceOrder);
                break;
            case 4:
                wrapper.set(TqNewScheduleResult::getClass4StartTime, startTime)
                       .set(TqNewScheduleResult::getClass4EndTime, endTime)
                       .set(TqNewScheduleResult::getClass4TaskStatus, taskStatus)
                       .set(TqNewScheduleResult::getClass4Sequence, produceOrder);
                break;
            case 5:
                wrapper.set(TqNewScheduleResult::getClass5StartTime, startTime)
                       .set(TqNewScheduleResult::getClass5EndTime, endTime)
                       .set(TqNewScheduleResult::getClass5TaskStatus, taskStatus)
                       .set(TqNewScheduleResult::getClass5Sequence, produceOrder);
                break;
            case 6:
                wrapper.set(TqNewScheduleResult::getClass6StartTime, startTime)
                       .set(TqNewScheduleResult::getClass6EndTime, endTime)
                       .set(TqNewScheduleResult::getClass6TaskStatus, taskStatus)
                       .set(TqNewScheduleResult::getClass6Sequence, produceOrder);
                break;
            default:
                throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    // ==================== 生产速度查询（风险点4） ====================

    /**
     * 获取机台+胎圈的生产速度（个/小时）
     *
     * <p>风险点4防护：</p>
     * <ul>
     *   <li>优先从 TqMachineSpecSpeed.standardSpeed 获取</li>
     *   <li>缺失时抛异常，强制配置</li>
     *   <li>单次滚动更新内缓存查询结果</li>
     * </ul>
     *
     * @param context  滚动上下文
     * @param machineId 机台ID（可能为null）
     * @param beadCode 胎圈代码
     * @return 生产速度（个/小时）
     */
    private double getProductionSpeed(TqRollingContext context, Long machineId, String beadCode) {
        // 优先读缓存
        String cacheKey = (machineId == null ? "null" : machineId.toString()) + ":" + beadCode;
        Double cached = context.getSpeedCache().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查数据库
        LambdaQueryWrapper<TqMachineSpecSpeed> wrapper = new LambdaQueryWrapper<>();
        if (machineId != null) {
            wrapper.eq(TqMachineSpecSpeed::getMachineId, machineId);
        }
        wrapper.eq(TqMachineSpecSpeed::getMaterialCode, beadCode);
        TqMachineSpecSpeed specSpeed = tqMachineSpecSpeedMapper.selectOne(wrapper);

        if (specSpeed == null || specSpeed.getStandardSpeed() == null
                || specSpeed.getStandardSpeed().doubleValue() <= 0) {
            throw new RuntimeException("机台[" + (machineId == null ? "未知" : machineId)
                    + "]胎圈[" + beadCode + "]未配置生产速度，请先维护机台规格速度");
        }

        double speed = specSpeed.getStandardSpeed().doubleValue();
        // 写入缓存
        context.getSpeedCache().put(cacheKey, speed);
        return speed;
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算规格切换时长
     *
     * <p>规则：</p>
     * <ul>
     *   <li>第一个任务：0小时</li>
     *   <li>与上一个任务胎圈相同：0小时</li>
     *   <li>与上一个任务胎圈不同：默认0.5小时（完整版可按英寸/规格细分）</li>
     * </ul>
     */
    private double calculateSwitchTime(String prevBeadCode, String currentBeadCode) {
        if (StringUtils.isBlank(prevBeadCode)) {
            return 0;
        }
        if (prevBeadCode.equals(currentBeadCode)) {
            return 0;
        }
        // MVP阶段：固定0.5小时；完整版可按英寸切换/规格切换细分
        return DEFAULT_SWITCH_TIME;
    }

    /**
     * 计算班次开始时间
     *
     * <p>6班次映射规则（参考 20260618_mes_tq_schedule_result.sql）：</p>
     * <ul>
     *   <li>1班：D日中班（D日 14:00）</li>
     *   <li>2班：D+1日夜班（D+1日 22:00）</li>
     *   <li>3班：D+1日早班（D+1日 06:00）</li>
     *   <li>4班：D+1日中班（D+1日 14:00）</li>
     *   <li>5班：D+2日夜班（D+2日 22:00）</li>
     *   <li>6班：D+2日早班（D+2日 06:00）</li>
     * </ul>
     *
     * @param scheduleDate 排程日期（D日）
     * @param shiftIndex   班次索引（1~6）
     * @return 班次开始时间
     */
    private Date calculateShiftStartTime(Date scheduleDate, int shiftIndex) {
        Date dDay = DateUtil.beginOfDay(scheduleDate);
        switch (shiftIndex) {
            case 1:
                // D日中班 14:00
                return DateUtil.offsetHour(dDay, 14);
            case 2:
                // D+1日夜班 22:00
                return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 1), 22);
            case 3:
                // D+1日早班 06:00
                return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 1), 6);
            case 4:
                // D+1日中班 14:00
                return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 1), 14);
            case 5:
                // D+2日夜班 22:00
                return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 2), 22);
            case 6:
                // D+2日早班 06:00
                return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 2), 6);
            default:
                throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    /**
     * 构建滚动上下文
     */
    private TqRollingContext buildContext(String triggerType, Long triggerSourceId, Date scheduleDate,
                                          int shiftIndex, String machineCode, String beadCode, String batchNo) {
        TqRollingContext context = new TqRollingContext();
        context.setTriggerType(triggerType);
        context.setTriggerSourceId(triggerSourceId);
        context.setScheduleDate(scheduleDate);
        context.setShiftIndex(shiftIndex);
        context.setMachineCode(machineCode);
        context.setBeadCode(beadCode);
        context.setBatchNo(batchNo);
        context.setShiftHours(DEFAULT_SHIFT_HOURS);
        context.setShiftStartTime(calculateShiftStartTime(scheduleDate, shiftIndex));
        return context;
    }

    /**
     * 生成滚动批次号
     */
    private String generateBatchNo() {
        return "ROLL" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 计算预计库存
     *
     * <p>公式：预计库存 = 当前实际库存 - 当天所有班次计划量之和</p>
     * <p>说明：当前实际库存从 T_TQ_STOCK 获取最新数据，计划量从排程结果汇总。</p>
     *
     * @param scheduleDate 排程日期
     * @param beadCode     胎圈代码
     * @return 预计库存量
     */
    private double calculateExpectedStock(Date scheduleDate, String beadCode) {
        try {
            // 1. 查询当前库存（取最新库存日期的数据）
            LambdaQueryWrapper<TqStock> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.eq(TqStock::getMaterialCode, beadCode)
                        .eq(TqStock::getIsDelete, 0)
                        .orderByDesc(TqStock::getStockDate)
                        .last("FETCH FIRST 1 ROWS ONLY");
            TqStock stock = tqStockMapper.selectOne(stockWrapper);
            double currentStock = (stock != null && stock.getStockNum() != null)
                    ? stock.getStockNum().doubleValue() : 0;

            // 2. 查询当天所有班次的计划量合计
            LambdaQueryWrapper<TqNewScheduleResult> scheduleWrapper = new LambdaQueryWrapper<>();
            scheduleWrapper.eq(TqNewScheduleResult::getScheduleDate, scheduleDate)
                           .eq(TqNewScheduleResult::getBeadCode, beadCode)
                           .eq(TqNewScheduleResult::getIsDelete, 0);
            List<TqNewScheduleResult> scheduleList = tqNewScheduleResultMapper.selectList(scheduleWrapper);

            double totalPlanQty = 0;
            for (TqNewScheduleResult schedule : scheduleList) {
                totalPlanQty += sumAllShiftPlanQty(schedule);
            }

            // 3. 预计库存 = 当前库存 - 当天计划量
            return currentStock - totalPlanQty;
        } catch (Exception e) {
            log.warn("计算预计库存失败：胎圈={}，返回0", beadCode, e);
            return 0;
        }
    }

    /**
     * 汇总排程记录所有班次的计划量
     */
    private double sumAllShiftPlanQty(TqNewScheduleResult schedule) {
        double total = 0;
        if (schedule.getClass1PlanQty() != null) total += schedule.getClass1PlanQty();
        if (schedule.getClass2PlanQty() != null) total += schedule.getClass2PlanQty();
        if (schedule.getClass3PlanQty() != null) total += schedule.getClass3PlanQty();
        if (schedule.getClass4PlanQty() != null) total += schedule.getClass4PlanQty();
        if (schedule.getClass5PlanQty() != null) total += schedule.getClass5PlanQty();
        if (schedule.getClass6PlanQty() != null) total += schedule.getClass6PlanQty();
        return total;
    }

    /**
     * 创建日志主表记录（状态：进行中）
     */
    private TqRollingLog createRollingLog(String batchNo, String triggerType, Long triggerSourceId,
                                          Date scheduleDate, int shiftIndex, String machineCode, String beadCode) {
        TqRollingLog rollingLog = new TqRollingLog();
        rollingLog.setBatchNo(batchNo);
        rollingLog.setTriggerType(triggerType);
        rollingLog.setTriggerSourceId(triggerSourceId);
        rollingLog.setScheduleDate(scheduleDate);
        rollingLog.setShiftIndex(shiftIndex);
        rollingLog.setMachineCode(machineCode);
        rollingLog.setBeadCode(beadCode);
        rollingLog.setStatus("0"); // 进行中
        rollingLog.setAffectedCount(0);
        try {
            rollingLog.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            // 定时任务场景无登录用户，忽略
        }
        rollingLog.setCreateTime(new Date());
        return rollingLog;
    }

    /**
     * 更新日志为成功
     */
    private void updateRollingLogSuccess(TqRollingLog rollingLog, int affectedCount,
                                          double beforeStock, double afterStock, String adjustReason) {
        LambdaUpdateWrapper<TqRollingLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TqRollingLog::getId, rollingLog.getId())
               .set(TqRollingLog::getStatus, "1") // 成功
               .set(TqRollingLog::getAffectedCount, affectedCount)
               .set(TqRollingLog::getBeforeStockQty, BigDecimal.valueOf(beforeStock))
               .set(TqRollingLog::getAfterStockQty, BigDecimal.valueOf(afterStock))
               .set(TqRollingLog::getAdjustReason, adjustReason)
               .set(TqRollingLog::getUpdateTime, new Date());
        try {
            wrapper.set(TqRollingLog::getUpdateBy, SecurityUtils.getUsername());
        } catch (Exception e) {
            // 忽略
        }
        tqRollingLogMapper.update(null, wrapper);
    }

    /**
     * 更新日志为失败
     */
    private void updateRollingLogFailure(TqRollingLog rollingLog, String errorMsg) {
        LambdaUpdateWrapper<TqRollingLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TqRollingLog::getId, rollingLog.getId())
               .set(TqRollingLog::getStatus, "2") // 失败
               .set(TqRollingLog::getErrorMsg, StringUtils.left(errorMsg, 2000))
               .set(TqRollingLog::getUpdateTime, new Date());
        try {
            wrapper.set(TqRollingLog::getUpdateBy, SecurityUtils.getUsername());
        } catch (Exception e) {
            // 忽略
        }
        tqRollingLogMapper.update(null, wrapper);
    }

    /**
     * 判断两个日期是否相等（null安全）
     */
    private boolean equalsDate(Date d1, Date d2) {
        if (d1 == null && d2 == null) {
            return true;
        }
        if (d1 == null || d2 == null) {
            return false;
        }
        return d1.equals(d2);
    }

    // ==================== 反射式字段访问（按班次索引） ====================

    private Integer getPlanQtyByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1PlanQty();
            case 2: return entity.getClass2PlanQty();
            case 3: return entity.getClass3PlanQty();
            case 4: return entity.getClass4PlanQty();
            case 5: return entity.getClass5PlanQty();
            case 6: return entity.getClass6PlanQty();
            default: return null;
        }
    }

    private Integer getSequenceByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1Sequence();
            case 2: return entity.getClass2Sequence();
            case 3: return entity.getClass3Sequence();
            case 4: return entity.getClass4Sequence();
            case 5: return entity.getClass5Sequence();
            case 6: return entity.getClass6Sequence();
            default: return null;
        }
    }

    private Integer getFinishQtyByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1FinishQty();
            case 2: return entity.getClass2FinishQty();
            case 3: return entity.getClass3FinishQty();
            case 4: return entity.getClass4FinishQty();
            case 5: return entity.getClass5FinishQty();
            case 6: return entity.getClass6FinishQty();
            default: return null;
        }
    }

    private Date getStartTimeByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1StartTime();
            case 2: return entity.getClass2StartTime();
            case 3: return entity.getClass3StartTime();
            case 4: return entity.getClass4StartTime();
            case 5: return entity.getClass5StartTime();
            case 6: return entity.getClass6StartTime();
            default: return null;
        }
    }

    private Date getEndTimeByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1EndTime();
            case 2: return entity.getClass2EndTime();
            case 3: return entity.getClass3EndTime();
            case 4: return entity.getClass4EndTime();
            case 5: return entity.getClass5EndTime();
            case 6: return entity.getClass6EndTime();
            default: return null;
        }
    }

    private String getTaskStatusByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1TaskStatus();
            case 2: return entity.getClass2TaskStatus();
            case 3: return entity.getClass3TaskStatus();
            case 4: return entity.getClass4TaskStatus();
            case 5: return entity.getClass5TaskStatus();
            case 6: return entity.getClass6TaskStatus();
            default: return null;
        }
    }
}

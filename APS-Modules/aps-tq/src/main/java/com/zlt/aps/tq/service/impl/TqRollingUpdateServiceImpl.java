package com.zlt.aps.tq.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.tq.api.constant.TqScheduleConstants;
import com.zlt.aps.tq.api.domain.entity.TqRollingAdjustment;
import com.zlt.aps.tq.api.domain.dto.TqRollingRecalcRequestDTO;
import com.zlt.aps.tq.api.domain.entity.TqDispatcherLog;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.aps.tq.api.domain.entity.TqRollingLogDetail;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.entity.TqShiftStock;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.api.domain.entity.TqParams;
import com.zlt.aps.tq.api.domain.vo.TqRollingRecalcResponseVO;
import com.zlt.aps.tq.engine.event.TqScheduleEventPublisher;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;
import com.zlt.aps.tq.engine.vo.TqRollingContext;
import com.zlt.aps.tq.engine.vo.TqRollingTaskNode;
import com.zlt.aps.tq.mapper.*;
import com.zlt.aps.tq.service.ITqRollingLogDetailService;
import com.zlt.aps.tq.service.ITqRollingLogService;
import com.zlt.aps.tq.service.ITqRollingUpdateService;
import com.zlt.aps.tq.service.TqRollingWindowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
    private TqScheduleResultMapper tqScheduleResultMapper;

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

    // ==================== 自动滚动相关依赖（对齐胎面 TmRollingUpdateServiceImpl） ====================

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private TqParamsMapper tqParamsMapper;

    @Resource
    private TqShiftStockMapper tqShiftStockMapper;

    @Resource
    private TqDispatcherLogMapper tqDispatcherLogMapper;

    @Resource
    private TqManualInsertRollingService tqManualInsertRollingService;

    @Resource
    private TqScheduleEventPublisher tqScheduleEventPublisher;

    @Resource
    private TqRollingWindowService tqRollingWindowService;

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

    // ==================== 自动滚动状态常量（对齐胎面 TmRollingUpdateServiceImpl） ====================

    /** 自动滚动执行状态：成功 */
    private static final String STATUS_SUCCESS = "SUCCESS";
    /** 自动滚动执行状态：跳过 */
    private static final String STATUS_SKIPPED = "SKIPPED";
    /** 审计日志摘要前缀 */
    private static final String SUMMARY_PREFIX = "ROLLING_SUMMARY=";
    /** 跳过原因：库存缺失 */
    private static final String SKIP_STOCK_MISSING = "STOCK_MISSING";
    /** 跳过原因：目标结果缺失 */
    private static final String SKIP_TARGET_RESULT_MISSING = "TARGET_RESULT_MISSING";
    /** 跳过原因：需求缺失 */
    private static final String SKIP_DEMAND_MISSING = "DEMAND_MISSING";
    /** 跳过原因：需求窗口不足 */
    private static final String SKIP_WINDOW_INSUFFICIENT = "DEMAND_WINDOW_INSUFFICIENT";
    /** 跳过原因：阈值未达到 */
    private static final String SKIP_THRESHOLD_NOT_REACHED = "THRESHOLD_NOT_REACHED";

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
    // 使用REQUIRES_NEW独立事务：滚动更新作为辅助操作，失败时仅回滚自身事务，
    // 不应污染调用方（如删除/插单等）的主事务，与triggerRollingUpdateForAllShifts的try-catch容错意图保持一致
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
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
            // 滚动更新作为辅助操作，失败时返回失败结果而非抛异常，
            // 由调用方走 WARN 分支处理（调用方均有 isSuccess() 判断），
            // 避免双重 ERROR 日志污染；REQUIRES_NEW 事务提交日志记录，
            // persistChanges 未执行，无业务数据被修改或残留
            log.warn("胎圈排程滚动更新失败，批次号：{}，原因：{}", batchNo, e.getMessage());
            // 更新日志为失败
            updateRollingLogFailure(rollingLog, e.getMessage());
            return RollingUpdateResult.fail(batchNo, e.getMessage());
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
            double speed = getProductionSpeed(context, node.getMachineCode(), node.getBeadCode());
            double planQty = node.getPlanQty();
            if (speed <= 0) {
                throw new RuntimeException("机台[" + node.getMachineCode() + "]胎圈[" + node.getBeadCode()
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
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getMachineCode, machineCode)
               .eq(TqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(TqScheduleResult::getIsDelete, 0)
               .orderByAsc(TqScheduleResult::getId);

        List<TqScheduleResult> scheduleList = tqScheduleResultMapper.selectList(wrapper);
        if (scheduleList == null || scheduleList.isEmpty()) {
            return new LinkedList<>();
        }

        // 转换为任务节点（仅提取指定班次的数据）
        LinkedList<TqRollingTaskNode> taskChain = new LinkedList<>();
        for (TqScheduleResult schedule : scheduleList) {
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
    private TqRollingTaskNode convertToTaskNode(TqScheduleResult schedule, int shiftIndex) {
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
            TqScheduleResult original = tqScheduleResultMapper.selectById(node.getScheduleId());

            // 构建更新条件
            LambdaUpdateWrapper<TqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TqScheduleResult::getId, node.getScheduleId());

            // 按班次设置开始时间、结束时间、任务状态
            setUpdateTimeFields(updateWrapper, shiftIndex, node.getStartTime(), node.getEndTime(),
                    node.getTaskStatus(), node.getProduceOrder());

            int rows = tqScheduleResultMapper.update(null, updateWrapper);
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
    private List<TqRollingLogDetail> buildChangeDetails(Long logId, TqScheduleResult original,
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
    private TqRollingLogDetail buildDetail(Long logId, TqScheduleResult original, TqRollingTaskNode newNode,
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
    private void setUpdateTimeFields(LambdaUpdateWrapper<TqScheduleResult> wrapper, int shiftIndex,
                                      Date startTime, Date endTime, String taskStatus, int produceOrder) {
        switch (shiftIndex) {
            case 1:
                wrapper.set(TqScheduleResult::getClass1StartTime, startTime)
                       .set(TqScheduleResult::getClass1EndTime, endTime)
                       .set(TqScheduleResult::getClass1TaskStatus, taskStatus)
                       .set(TqScheduleResult::getClass1Sequence, produceOrder);
                break;
            case 2:
                wrapper.set(TqScheduleResult::getClass2StartTime, startTime)
                       .set(TqScheduleResult::getClass2EndTime, endTime)
                       .set(TqScheduleResult::getClass2TaskStatus, taskStatus)
                       .set(TqScheduleResult::getClass2Sequence, produceOrder);
                break;
            case 3:
                wrapper.set(TqScheduleResult::getClass3StartTime, startTime)
                       .set(TqScheduleResult::getClass3EndTime, endTime)
                       .set(TqScheduleResult::getClass3TaskStatus, taskStatus)
                       .set(TqScheduleResult::getClass3Sequence, produceOrder);
                break;
            case 4:
                wrapper.set(TqScheduleResult::getClass4StartTime, startTime)
                       .set(TqScheduleResult::getClass4EndTime, endTime)
                       .set(TqScheduleResult::getClass4TaskStatus, taskStatus)
                       .set(TqScheduleResult::getClass4Sequence, produceOrder);
                break;
            case 5:
                wrapper.set(TqScheduleResult::getClass5StartTime, startTime)
                       .set(TqScheduleResult::getClass5EndTime, endTime)
                       .set(TqScheduleResult::getClass5TaskStatus, taskStatus)
                       .set(TqScheduleResult::getClass5Sequence, produceOrder);
                break;
            case 6:
                wrapper.set(TqScheduleResult::getClass6StartTime, startTime)
                       .set(TqScheduleResult::getClass6EndTime, endTime)
                       .set(TqScheduleResult::getClass6TaskStatus, taskStatus)
                       .set(TqScheduleResult::getClass6Sequence, produceOrder);
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
     * @param machineCode 机台编号
     * @param beadCode 胎圈代码
     * @return 生产速度（个/小时）
     */
    private double getProductionSpeed(TqRollingContext context, String machineCode, String beadCode) {
        // 优先读缓存
        String cacheKey = machineCode + ":" + beadCode;
        Double cached = context.getSpeedCache().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查数据库
        LambdaQueryWrapper<TqMachineSpecSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqMachineSpecSpeed::getMachineCode, machineCode);
        wrapper.eq(TqMachineSpecSpeed::getBeadCode, beadCode);
        TqMachineSpecSpeed specSpeed = tqMachineSpecSpeedMapper.selectOne(wrapper);

        if (specSpeed == null || specSpeed.getStandardSpeed() == null
                || specSpeed.getStandardSpeed().doubleValue() <= 0) {
            throw new RuntimeException("机台[" + machineCode
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
            stockWrapper.eq(TqStock::getBeadCode, beadCode)
                        .eq(TqStock::getIsDelete, 0)
                        .orderByDesc(TqStock::getStockDate)
                        .last("LIMIT 1");
            TqStock stock = tqStockMapper.selectOne(stockWrapper);
            double currentStock = (stock != null && stock.getStockNum() != null)
                    ? stock.getStockNum().doubleValue() : 0;

            // 2. 查询当天所有班次的计划量合计
            LambdaQueryWrapper<TqScheduleResult> scheduleWrapper = new LambdaQueryWrapper<>();
            scheduleWrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate)
                           .eq(TqScheduleResult::getBeadCode, beadCode)
                           .eq(TqScheduleResult::getIsDelete, 0);
            List<TqScheduleResult> scheduleList = tqScheduleResultMapper.selectList(scheduleWrapper);

            double totalPlanQty = 0;
            for (TqScheduleResult schedule : scheduleList) {
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
    private double sumAllShiftPlanQty(TqScheduleResult schedule) {
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

    private Integer getPlanQtyByShiftIndex(TqScheduleResult entity, int shiftIndex) {
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

    private Integer getSequenceByShiftIndex(TqScheduleResult entity, int shiftIndex) {
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

    private Integer getFinishQtyByShiftIndex(TqScheduleResult entity, int shiftIndex) {
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

    private Date getStartTimeByShiftIndex(TqScheduleResult entity, int shiftIndex) {
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

    private Date getEndTimeByShiftIndex(TqScheduleResult entity, int shiftIndex) {
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

    private String getTaskStatusByShiftIndex(TqScheduleResult entity, int shiftIndex) {
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

    // ==================== 自动滚动重算（对齐胎面 TmRollingUpdateServiceImpl） ====================

    /**
     * 自动滚动重算入口（对齐胎面 TmRollingUpdateServiceImpl.rollingRecalcAutomatically）。
     *
     * <p>由 TqAutoRollingApplicationService 在窗口锁内调用，
     * 执行库存上下界调量算法、行锁、释放状态校验、审计日志和事件发布。</p>
     *
     * @param request 重算请求（工厂、排程日期、库存日期、目标班次、操作人）
     * @return 滚动重算响应（含幂等键、调整统计、跳过摘要）
     */
    @Override
    public TqRollingRecalcResponseVO rollingRecalcAutomatically(TqRollingRecalcRequestDTO request) {
        return this.executeAutoRolling(request, true);
    }

    /**
     * 在分布式锁范围内执行幂等检查、数据加载和事务写入。
     *
     * @param request   滚动请求
     * @param automatic  true 表示定时触发
     * @return 滚动结果
     */
    private TqRollingRecalcResponseVO executeAutoRolling(TqRollingRecalcRequestDTO request, boolean automatic) {
        this.validateAutoRequest(request);
        request.setFactoryCode(StrUtil.trim(request.getFactoryCode()));
        request.setScheduleDate(DateUtil.beginOfDay(request.getScheduleDate()));
        this.resolveStockDate(request);
        request.setOperator(StrUtil.blankToDefault(StrUtil.trim(request.getOperator()),
                automatic ? TqScheduleConstants.ROLLING_OPERATOR_AUTO : "TQ_ROLLING_API"));
        if (automatic && !this.isRollingEnabled(request.getFactoryCode())) {
            return this.buildDisabledResponse(request);
        }
        this.ensureShiftStockExists(request);

        String runKey = this.buildRunKey(request);
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String lockKey = TqScheduleConstants.ROLLING_LOCK_KEY_PREFIX + request.getFactoryCode() + ":"
                + DateUtil.formatDate(request.getScheduleDate()) + ":" + request.getTargetShiftOrder();
        RLock rollingLock = redissonClient.getLock(lockKey);
        if (!rollingLock.tryLock()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.locked"));
        }
        try {
            TqRollingRecalcResponseVO existingResponse = this.loadExistingResponse(request, runKey, traceId);
            if (existingResponse != null) {
                return existingResponse;
            }
            Map<String, Object> skipEvidenceMap = new LinkedHashMap<>();
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            TqRollingRecalcResponseVO response = transactionTemplate.execute(status ->
                    this.executeInsideTransaction(request, runKey, traceId, skipEvidenceMap));
            if (response == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.failed"));
            }
            this.publishRollingEvent(request, response);
            return response;
        } finally {
            if (rollingLock.isHeldByCurrentThread()) {
                rollingLock.unlock();
            }
        }
    }

    /**
     * 执行数据库事务内的行锁、计算、滚动写入和审计。
     *
     * @param request       滚动请求
     * @param runKey        运行键
     * @param traceId       追踪号
     * @param skipEvidenceMap 跳过证据收集器
     * @return 滚动响应
     */
    private TqRollingRecalcResponseVO executeInsideTransaction(TqRollingRecalcRequestDTO request,
                                                                 String runKey, String traceId,
                                                                 Map<String, Object> skipEvidenceMap) {
        TqRollingRecalcResponseVO existingResponse = this.loadExistingResponse(request, runKey, traceId);
        if (existingResponse != null) {
            return existingResponse;
        }
        List<TqScheduleResult> initialResultList = this.loadAutoScheduleResults(request);
        List<Long> resultIds = initialResultList.stream().map(TqScheduleResult::getId)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (!resultIds.isEmpty()) {
            List<TqScheduleResult> lockedResultList = tqScheduleResultMapper.selectBatchIdsForUpdate(resultIds);
            if (lockedResultList == null || lockedResultList.size() != resultIds.size()) {
                throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.concurrentChanged"));
            }
        }
        List<TqScheduleResult> beforeList = this.loadAutoScheduleResults(request);
        List<TqRollingAdjustment> adjustmentList = this.calculateAdjustments(request, beforeList, skipEvidenceMap);
        this.validateAffectedReleaseStatuses(beforeList, adjustmentList, request.getTargetShiftOrder());

        List<TqScheduleResult> changeRequestList = new ArrayList<>();
        for (TqRollingAdjustment adjustment : adjustmentList) {
            this.appendAdjustmentRequests(request, adjustment, changeRequestList);
        }
        int updateCount = changeRequestList.isEmpty() ? 0
                : tqManualInsertRollingService.changeQtyAndRollBatch(changeRequestList);
        List<TqScheduleResult> afterList = this.loadAutoScheduleResults(request);
        TqRollingRecalcResponseVO response = this.buildAutoResponse(request, runKey, traceId,
                beforeList, afterList, adjustmentList, updateCount, skipEvidenceMap);
        this.recordRollingLog(request, runKey, beforeList, afterList, response);
        return response;
    }

    /**
     * 按胎圈计算上修或下修目标。
     *
     * <p>需求口径：胎圈无独立的班次需求草稿，使用月计划剩余量（monthSurplusQty）
     * 按剩余班次数均摊为每班需求，再乘以阈值班数得到窗口需求。</p>
     *
     * @param request       滚动请求
     * @param resultList    当前排程结果
     * @param skipEvidenceMap 跳过证据收集器
     * @return 需要实际修改的胎圈调整指令
     */
    private List<TqRollingAdjustment> calculateAdjustments(TqRollingRecalcRequestDTO request,
                                                             List<TqScheduleResult> resultList,
                                                             Map<String, Object> skipEvidenceMap) {
        Map<String, List<TqScheduleResult>> resultMap = resultList.stream()
                .filter(result -> StrUtil.isNotBlank(result.getBeadCode()))
                .collect(Collectors.groupingBy(TqScheduleResult::getBeadCode,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, TqShiftStock> stockMap = this.loadStockMap(request);

        BigDecimal upThreshold = this.readPositiveDecimalParam(request.getFactoryCode(),
                TqScheduleConstants.PARAM_ROLLING_UP_THRESHOLD,
                new BigDecimal(TqScheduleConstants.DEFAULT_ROLLING_UP_THRESHOLD));
        BigDecimal downThreshold = this.readPositiveDecimalParam(request.getFactoryCode(),
                TqScheduleConstants.PARAM_ROLLING_DOWN_THRESHOLD,
                new BigDecimal(TqScheduleConstants.DEFAULT_ROLLING_DOWN_THRESHOLD));
        int rollingShiftCount = this.readPositiveIntegerParam(request.getFactoryCode(),
                TqScheduleConstants.PARAM_ROLLING_SHIFT_COUNT,
                TqScheduleConstants.DEFAULT_ROLLING_SHIFT_COUNT);
        BigDecimal downTarget = this.readPositiveDecimalParam(request.getFactoryCode(),
                TqScheduleConstants.PARAM_ROLLING_DOWN_TARGET,
                new BigDecimal(TqScheduleConstants.DEFAULT_ROLLING_DOWN_TARGET));

        int targetShiftOrder = request.getTargetShiftOrder();
        int remainingShifts = TqScheduleConstants.TQ_MAX_SHIFT_ORDER - targetShiftOrder + 1;
        List<TqRollingAdjustment> adjustmentList = new ArrayList<>();
        for (Map.Entry<String, List<TqScheduleResult>> entry : resultMap.entrySet()) {
            String beadCode = entry.getKey();
            List<TqScheduleResult> beadResultList = entry.getValue();

            TqShiftStock stock = stockMap.get(beadCode);
            if (stock == null) {
                this.incrementSkip(skipEvidenceMap, request, beadCode, SKIP_STOCK_MISSING);
                continue;
            }
            BigDecimal beforePlanQty = this.sumResultQty(beadResultList, targetShiftOrder, false);
            if (beadResultList.isEmpty() || beforePlanQty.compareTo(BigDecimal.ZERO) <= 0) {
                this.incrementSkip(skipEvidenceMap, request, beadCode, SKIP_TARGET_RESULT_MISSING);
                continue;
            }
            BigDecimal monthSurplus = beadResultList.stream()
                    .map(TqScheduleResult::getMonthSurplusQty)
                    .filter(Objects::nonNull)
                    .map(BigDecimalUtils::valueOf)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            if (monthSurplus.compareTo(BigDecimal.ZERO) <= 0) {
                this.incrementSkip(skipEvidenceMap, request, beadCode, SKIP_DEMAND_MISSING);
                continue;
            }
            BigDecimal perShiftDemand = monthSurplus.divide(BigDecimal.valueOf(remainingShifts),
                    TqScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
            BigDecimal upDemand = perShiftDemand.multiply(upThreshold);
            BigDecimal downDemand = perShiftDemand.multiply(downThreshold);
            BigDecimal downTargetDemand = perShiftDemand.multiply(downTarget);

            BigDecimal expectedStock = this.calculateExpectedStock(stock);
            BigDecimal availableQty = expectedStock.add(beforePlanQty);
            BigDecimal targetPlanQty = null;
            String direction = null;
            boolean downWindowEnough = this.hasDemandWindow(targetShiftOrder, downThreshold);
            if (availableQty.compareTo(upDemand) < 0) {
                targetPlanQty = upDemand.subtract(expectedStock).max(BigDecimal.ZERO);
                direction = "UP";
            } else if (downWindowEnough) {
                if (availableQty.compareTo(downDemand) > 0) {
                    targetPlanQty = downTargetDemand.subtract(expectedStock).max(BigDecimal.ZERO);
                    direction = "DOWN";
                }
            } else {
                this.incrementSkip(skipEvidenceMap, request, beadCode, SKIP_WINDOW_INSUFFICIENT);
            }
            if (targetPlanQty == null || targetPlanQty.compareTo(beforePlanQty) == 0) {
                if (targetPlanQty == null && downWindowEnough) {
                    this.incrementSkip(skipEvidenceMap, request, beadCode, SKIP_THRESHOLD_NOT_REACHED);
                }
                continue;
            }
            BigDecimal targetFinishQty = this.sumResultQty(beadResultList, targetShiftOrder, true);
            targetPlanQty = targetPlanQty.max(targetFinishQty);
            if (targetPlanQty.compareTo(beforePlanQty) == 0) {
                this.incrementSkip(skipEvidenceMap, request, beadCode, SKIP_THRESHOLD_NOT_REACHED);
                continue;
            }
            TqRollingAdjustment adjustment = new TqRollingAdjustment();
            adjustment.setBeadCode(beadCode);
            adjustment.setBeforePlanQty(beforePlanQty);
            adjustment.setTargetPlanQty(targetPlanQty);
            adjustment.setDirection(direction);
            adjustment.getEvidence().put("expectedStock", expectedStock);
            adjustment.getEvidence().put("beforePlanQty", beforePlanQty);
            adjustment.getEvidence().put("availableQty", availableQty);
            adjustment.getEvidence().put("upDemand", upDemand);
            adjustment.getEvidence().put("downDemand", downDemand);
            adjustment.getEvidence().put("downWindowEnough", downWindowEnough);
            adjustment.getEvidence().put("rollingShiftCount", rollingShiftCount);
            adjustment.getEvidence().put("monthSurplus", monthSurplus);
            adjustment.getEvidence().put("targetPlanQty", targetPlanQty);
            adjustmentList.add(adjustment);
        }
        return adjustmentList;
    }

    /**
     * 按目标胎圈和班次生成调量命令，全部调整在同一运行态上下文计算。
     *
     * @param request           滚动请求
     * @param adjustment         调整指令
     * @param changeRequestList  调量请求收集器
     */
    private void appendAdjustmentRequests(TqRollingRecalcRequestDTO request,
                                            TqRollingAdjustment adjustment,
                                            List<TqScheduleResult> changeRequestList) {
        List<TqScheduleResult> currentList = this.loadBeadResults(request, adjustment.getBeadCode());
        Comparator<TqScheduleResult> comparator = Comparator
                .comparing((TqScheduleResult result) -> this.readShiftSequence(result, request.getTargetShiftOrder()),
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TqScheduleResult::getId, Comparator.nullsLast(Long::compareTo));
        currentList.sort(comparator);
        BigDecimal currentTotal = this.sumResultQty(currentList, request.getTargetShiftOrder(), false);
        BigDecimal delta = adjustment.getTargetPlanQty().subtract(currentTotal);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            TqScheduleResult target = currentList.get(currentList.size() - 1);
            BigDecimal currentQty = this.readShiftQty(target, request.getTargetShiftOrder(), false);
            changeRequestList.add(this.buildChangeRequest(target, request.getTargetShiftOrder(),
                    currentQty.add(delta)));
            return;
        }
        BigDecimal remainingReduceQty = delta.abs();
        ListIterator<TqScheduleResult> iterator = currentList.listIterator(currentList.size());
        while (iterator.hasPrevious() && remainingReduceQty.compareTo(BigDecimal.ZERO) > 0) {
            TqScheduleResult target = iterator.previous();
            BigDecimal currentQty = this.readShiftQty(target, request.getTargetShiftOrder(), false);
            BigDecimal finishQty = this.readShiftQty(target, request.getTargetShiftOrder(), true);
            BigDecimal reducibleQty = currentQty.subtract(finishQty).max(BigDecimal.ZERO);
            BigDecimal reduceQty = reducibleQty.min(remainingReduceQty);
            if (reduceQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            changeRequestList.add(this.buildChangeRequest(target, request.getTargetShiftOrder(),
                    currentQty.subtract(reduceQty)));
            remainingReduceQty = remainingReduceQty.subtract(reduceQty);
        }
        if (remainingReduceQty.compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.finishLimit"));
        }
    }

    /**
     * 构建自动滚动调量请求。
     *
     * @param current   当前结果
     * @param shiftOrder 目标班次
     * @param planQty   新计划量
     * @return 调量请求
     */
    private TqScheduleResult buildChangeRequest(TqScheduleResult current, int shiftOrder, BigDecimal planQty) {
        TqScheduleResult changeRequest = new TqScheduleResult();
        changeRequest.setId(current.getId());
        changeRequest.setFieldValueByFieldName(
                String.format(TqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty.intValue());
        changeRequest.setFieldValueByFieldName(
                String.format(TqScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder), "ROLLING_RECALC");
        return changeRequest;
    }

    /**
     * 校验所有可能被滚动影响的机台结果均处于可编辑状态。
     *
     * @param resultList     当前日期结果
     * @param adjustmentList 调整指令
     * @param shiftOrder     目标班次
     */
    private void validateAffectedReleaseStatuses(List<TqScheduleResult> resultList,
                                                   List<TqRollingAdjustment> adjustmentList,
                                                   int shiftOrder) {
        Set<String> beadCodes = adjustmentList.stream().map(TqRollingAdjustment::getBeadCode)
                .collect(Collectors.toSet());
        Set<String> machineCodes = resultList.stream()
                .filter(result -> beadCodes.contains(result.getBeadCode()))
                .filter(result -> this.readShiftQty(result, shiftOrder, false).compareTo(BigDecimal.ZERO) > 0)
                .map(TqScheduleResult::getMachineCode).filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        boolean containsInvalidStatus = resultList.stream()
                .filter(result -> machineCodes.contains(result.getMachineCode()))
                .anyMatch(result -> !this.isEditableReleaseStatus(result.getReleaseStatus()));
        if (containsInvalidStatus) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.releaseStatusInvalid"));
        }
    }

    /**
     * 计算目标班前预计库存。
     *
     * @param stock 班次开始前同步的实时库存
     * @return 非负预计库存
     */
    private BigDecimal calculateExpectedStock(TqShiftStock stock) {
        return BigDecimalUtils.valueOf(stock.getStockQty())
                .subtract(BigDecimalUtils.valueOf(stock.getBadQty()))
                .add(BigDecimalUtils.valueOf(stock.getAdjustQty()))
                .max(BigDecimal.ZERO);
    }

    /**
     * 判断未来需求窗口是否完整。
     *
     * @param startShiftOrder 起始班次
     * @param shiftCount      所需班次数（可含小数）
     * @return true 表示所需最后班次仍在六班窗口内
     */
    private boolean hasDemandWindow(int startShiftOrder, BigDecimal shiftCount) {
        int requiredShiftCount = shiftCount.setScale(0, RoundingMode.CEILING).intValue();
        int lastShiftOrder = startShiftOrder + requiredShiftCount - 1;
        return lastShiftOrder <= TqScheduleConstants.TQ_MAX_SHIFT_ORDER;
    }

    /**
     * 汇总排程结果指定班次计划量或完成量。
     *
     * @param resultList 排程结果
     * @param shiftOrder  班次
     * @param finish     true 读取完成量，false 读取计划量
     * @return 汇总数量
     */
    private BigDecimal sumResultQty(List<TqScheduleResult> resultList, int shiftOrder, boolean finish) {
        return resultList.stream().map(result -> this.readShiftQty(result, shiftOrder, finish))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 动态读取班次计划量或完成量。
     *
     * @param result    排程结果
     * @param shiftOrder 班次
     * @param finish    true 读取完成量，false 读取计划量
     * @return 数量，空值按零处理
     */
    private BigDecimal readShiftQty(TqScheduleResult result, int shiftOrder, boolean finish) {
        String fieldTemplate = finish ? TqScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE
                : TqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE;
        return result == null ? BigDecimal.ZERO : BigDecimalUtils.valueOf(
                result.getFieldValueByFieldName(String.format(fieldTemplate, shiftOrder)));
    }

    /**
     * 动态读取班次顺序。
     *
     * @param result    排程结果
     * @param shiftOrder 班次
     * @return 顺序，空值返回 null
     */
    private Integer readShiftSequence(TqScheduleResult result, int shiftOrder) {
        Object value = result == null ? null : result.getFieldValueByFieldName(
                String.format(TqScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /**
     * 查询当前工厂和日期的有效结果。
     *
     * @param request 滚动请求
     * @return 排程结果
     */
    private List<TqScheduleResult> loadAutoScheduleResults(TqRollingRecalcRequestDTO request) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getFactoryCode, request.getFactoryCode());
        wrapper.eq(TqScheduleResult::getScheduleDate, request.getScheduleDate());
        wrapper.orderByAsc(TqScheduleResult::getMachineCode, TqScheduleResult::getId);
        List<TqScheduleResult> resultList = tqScheduleResultMapper.selectList(wrapper);
        return resultList == null ? Collections.emptyList() : resultList;
    }

    /**
     * 查询同一胎圈的当前结果。
     *
     * @param request  滚动请求
     * @param beadCode 胎圈编码
     * @return 同胎圈结果
     */
    private List<TqScheduleResult> loadBeadResults(TqRollingRecalcRequestDTO request, String beadCode) {
        return this.loadAutoScheduleResults(request).stream()
                .filter(result -> beadCode.equals(result.getBeadCode()))
                .collect(Collectors.toList());
    }

    /**
     * 加载库存并保留同胎圈首条记录。
     *
     * @param request 滚动请求
     * @return 胎圈库存映射
     */
    private Map<String, TqShiftStock> loadStockMap(TqRollingRecalcRequestDTO request) {
        LambdaQueryWrapper<TqShiftStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqShiftStock::getFactoryCode, request.getFactoryCode());
        wrapper.eq(TqShiftStock::getStockDate, request.getStockDate());
        wrapper.eq(TqShiftStock::getShiftOrder, request.getTargetShiftOrder());
        wrapper.orderByAsc(TqShiftStock::getId);
        List<TqShiftStock> stockList = tqShiftStockMapper.selectList(wrapper);
        return (stockList == null ? Collections.<TqShiftStock>emptyList() : stockList).stream()
                .filter(stock -> StrUtil.isNotBlank(stock.getBeadCode()))
                .collect(Collectors.toMap(TqShiftStock::getBeadCode, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    /**
     * 补齐并规范化MES库存物理日期。
     *
     * @param request 滚动请求
     */
    private void resolveStockDate(TqRollingRecalcRequestDTO request) {
        if (request.getStockDate() != null) {
            request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
            return;
        }
        com.zlt.aps.tq.domain.vo.TqRollingWindow window = this.tqRollingWindowService.resolveWindow(
                request.getFactoryCode(), request.getScheduleDate(), request.getTargetShiftOrder());
        if (window == null || window.getStockDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.requestInvalid"));
        }
        request.setStockDate(DateUtil.beginOfDay(window.getStockDate()));
    }

    /**
     * 在幂等记录查询前校验整个班次库存快照存在。
     *
     * @param request 滚动请求
     */
    private void ensureShiftStockExists(TqRollingRecalcRequestDTO request) {
        Long stockCount = this.tqShiftStockMapper.selectCount(new LambdaQueryWrapper<TqShiftStock>()
                .eq(TqShiftStock::getFactoryCode, request.getFactoryCode())
                .eq(TqShiftStock::getStockDate, request.getStockDate())
                .eq(TqShiftStock::getShiftOrder, request.getTargetShiftOrder()));
        if (stockCount == null || stockCount <= 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.shiftStockMissing"));
        }
    }

    /**
     * 查询滚动参数，空值或非法值使用默认值。
     *
     * @param factoryCode  工厂编号
     * @param paramCode    参数编码
     * @param defaultValue 默认值
     * @return 正数参数值
     */
    private BigDecimal readPositiveDecimalParam(String factoryCode, String paramCode, BigDecimal defaultValue) {
        String value = this.readParamValue(factoryCode, paramCode, defaultValue.toPlainString());
        try {
            BigDecimal parsedValue = new BigDecimal(value);
            return parsedValue.compareTo(BigDecimal.ZERO) > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 查询正整数滚动参数，空值、零值或非法值使用默认值。
     *
     * @param factoryCode  工厂编号
     * @param paramCode    参数编码
     * @param defaultValue 默认值
     * @return 正整数参数值
     */
    private int readPositiveIntegerParam(String factoryCode, String paramCode, int defaultValue) {
        String value = this.readParamValue(factoryCode, paramCode, String.valueOf(defaultValue));
        try {
            int parsedValue = Integer.parseInt(value);
            return parsedValue > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 查询单个工厂参数。
     *
     * @param factoryCode  工厂编号
     * @param paramCode    参数编码
     * @param defaultValue 默认值
     * @return 生效参数值
     */
    private String readParamValue(String factoryCode, String paramCode, String defaultValue) {
        LambdaQueryWrapper<TqParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqParams::getFactoryCode, factoryCode);
        wrapper.eq(TqParams::getParamCode, paramCode);
        wrapper.eq(TqParams::getEnableStatus, "1");
        wrapper.orderByDesc(TqParams::getId);
        List<TqParams> paramsList = tqParamsMapper.selectList(wrapper);
        if (paramsList == null || paramsList.isEmpty()) {
            return defaultValue;
        }
        TqParams params = paramsList.get(0);
        return StrUtil.blankToDefault(StrUtil.trim(params.getParamValue()),
                StrUtil.blankToDefault(StrUtil.trim(params.getDefaultValue()), defaultValue));
    }

    /**
     * 判断工厂自动滚动开关是否开启。
     *
     * @param factoryCode 工厂编号
     * @return true 表示开启
     */
    private boolean isRollingEnabled(String factoryCode) {
        return "1".equals(this.readParamValue(factoryCode, TqScheduleConstants.PARAM_ROLLING_ENABLED,
                TqScheduleConstants.DEFAULT_ROLLING_ENABLED));
    }

    /**
     * 记录单胎圈跳过证据。
     *
     * @param skipEvidenceMap 跳过证据收集器
     * @param request         滚动请求
     * @param beadCode        胎圈编码
     * @param reasonCode      跳过原因
     */
    private void incrementSkip(Map<String, Object> skipEvidenceMap, TqRollingRecalcRequestDTO request,
                                String beadCode, String reasonCode) {
        skipEvidenceMap.put(beadCode, reasonCode);
        log.warn("[TQ_ROLLING_SKIP] factoryCode={}, scheduleDate={}, beadCode={}, reasonCode={}",
                request.getFactoryCode(), DateUtil.formatDate(request.getScheduleDate()), beadCode, reasonCode);
    }

    /**
     * 构建响应并统计真正变化的结果行。
     *
     * @param request       滚动请求
     * @param runKey        运行键
     * @param traceId       追踪号
     * @param beforeList    操作前结果
     * @param afterList     操作后结果
     * @param adjustmentList 调整指令
     * @param updateCount   滚动更新次数
     * @param skipEvidenceMap 跳过证据
     * @return 滚动响应
     */
    private TqRollingRecalcResponseVO buildAutoResponse(TqRollingRecalcRequestDTO request, String runKey,
                                                           String traceId, List<TqScheduleResult> beforeList,
                                                           List<TqScheduleResult> afterList,
                                                           List<TqRollingAdjustment> adjustmentList,
                                                           int updateCount,
                                                           Map<String, Object> skipEvidenceMap) {
        TqRollingRecalcResponseVO response = new TqRollingRecalcResponseVO();
        response.setRunKey(runKey);
        response.setStatus(adjustmentList.isEmpty() ? STATUS_SKIPPED : STATUS_SUCCESS);
        response.setScheduleDate(request.getScheduleDate());
        response.setTargetShiftOrder(request.getTargetShiftOrder());
        response.setAdjustedBeadCount(adjustmentList.size());
        response.setAffectedResultCount(this.countChangedRows(beforeList, afterList));
        response.setBeforePlanQty(this.sumResultQty(beforeList, request.getTargetShiftOrder(), false));
        response.setAfterPlanQty(this.sumResultQty(afterList, request.getTargetShiftOrder(), false));
        response.setTraceId(traceId);
        Map<String, Integer> skipSummary = skipEvidenceMap.values().stream().map(String::valueOf)
                .collect(Collectors.toMap(Function.identity(), ignored -> 1, Integer::sum, LinkedHashMap::new));
        response.setSkippedBeadCount(skipEvidenceMap.size());
        response.setSkippedReasonSummary(skipSummary);
        log.info("[TQ_ROLLING] runKey={}, adjustedBeadCount={}, updateCount={}, affectedResultCount={}, skippedBeadCount={}",
                runKey, adjustmentList.size(), updateCount, response.getAffectedResultCount(),
                response.getSkippedBeadCount());
        return response;
    }

    /**
     * 统计操作前后任一班次计划量发生变化的结果行。
     *
     * @param beforeList 操作前结果
     * @param afterList  操作后结果
     * @return 变化结果行数量
     */
    private int countChangedRows(List<TqScheduleResult> beforeList, List<TqScheduleResult> afterList) {
        Map<Long, TqScheduleResult> beforeMap = beforeList.stream()
                .filter(result -> result.getId() != null)
                .collect(Collectors.toMap(TqScheduleResult::getId, Function.identity(),
                        (first, ignored) -> first));
        Map<Long, TqScheduleResult> afterMap = afterList.stream()
                .filter(result -> result.getId() != null)
                .collect(Collectors.toMap(TqScheduleResult::getId, Function.identity(),
                        (first, ignored) -> first));
        Set<Long> allIds = new HashSet<>(beforeMap.keySet());
        allIds.addAll(afterMap.keySet());
        int changedCount = 0;
        for (Long id : allIds) {
            TqScheduleResult before = beforeMap.get(id);
            TqScheduleResult after = afterMap.get(id);
            if (before == null || after == null || !this.hasSamePlan(before, after)) {
                changedCount++;
            }
        }
        return changedCount;
    }

    /**
     * 比较六班计划量是否一致。
     *
     * @param before 操作前结果
     * @param after  操作后结果
     * @return true 表示一致
     */
    private boolean hasSamePlan(TqScheduleResult before, TqScheduleResult after) {
        for (int shiftOrder = 1; shiftOrder <= TqScheduleConstants.TQ_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.readShiftQty(before, shiftOrder, false)
                    .compareTo(this.readShiftQty(after, shiftOrder, false)) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 写入不可撤销的自动滚动审计日志。
     *
     * @param request  滚动请求
     * @param runKey   运行键
     * @param beforeList 操作前结果
     * @param afterList  操作后结果
     * @param response 滚动响应
     */
    private void recordRollingLog(TqRollingRecalcRequestDTO request, String runKey,
                                   List<TqScheduleResult> beforeList, List<TqScheduleResult> afterList,
                                   TqRollingRecalcResponseVO response) {
        TqDispatcherLog dispatcherLog = new TqDispatcherLog();
        dispatcherLog.setScheduleDate(request.getScheduleDate());
        dispatcherLog.setOperType(TqScheduleConstants.DISPATCHER_OPER_ROLLING);
        dispatcherLog.setCreateBy(request.getOperator());
        dispatcherLog.setRemark(SUMMARY_PREFIX + JSON.toJSONString(response));
        if (tqDispatcherLogMapper.insertTqDispatcherLog(dispatcherLog) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.auditFailed"));
        }
    }

    /**
     * 读取相同运行键的成功或跳过摘要，实现定时与手动入口幂等。
     *
     * @param request 滚动请求
     * @param runKey  运行键
     * @param traceId 本次追踪号
     * @return 已执行响应；不存在返回 null
     */
    private TqRollingRecalcResponseVO loadExistingResponse(TqRollingRecalcRequestDTO request,
                                                             String runKey, String traceId) {
        TqDispatcherLog queryLog = new TqDispatcherLog();
        queryLog.setScheduleDate(request.getScheduleDate());
        queryLog.setOperType(TqScheduleConstants.DISPATCHER_OPER_ROLLING);
        List<TqDispatcherLog> logList = tqDispatcherLogMapper.selectTqDispatcherLogList(queryLog);
        if (logList == null || logList.isEmpty()) {
            return null;
        }
        for (TqDispatcherLog logEntry : logList) {
            String remark = logEntry.getRemark();
            if (StrUtil.isNotBlank(remark) && remark.startsWith(SUMMARY_PREFIX)) {
                try {
                    TqRollingRecalcResponseVO existing = JSON.parseObject(
                            remark.substring(SUMMARY_PREFIX.length()), TqRollingRecalcResponseVO.class);
                    if (existing != null && runKey.equals(existing.getRunKey())) {
                        existing.setTraceId(traceId);
                        return existing;
                    }
                } catch (RuntimeException ex) {
                    log.warn("[TQ_ROLLING] 解析历史运行摘要失败，runKey={}，原因={}", runKey, ex.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 发布低敏滚动事件摘要。
     *
     * @param request  滚动请求
     * @param response 滚动响应
     */
    private void publishRollingEvent(TqRollingRecalcRequestDTO request, TqRollingRecalcResponseVO response) {
        tqScheduleEventPublisher.publishRollingEvent(request, response);
    }

    /**
     * 生成固定运行键。
     *
     * @param request 滚动请求
     * @return 运行键
     */
    private String buildRunKey(TqRollingRecalcRequestDTO request) {
        return TqScheduleConstants.ROLLING_RUN_KEY_PREFIX + request.getFactoryCode() + ":"
                + DateUtil.format(request.getScheduleDate(), "yyyyMMdd") + ":" + request.getTargetShiftOrder();
    }

    /**
     * 构建自动开关关闭时的跳过响应，不写数据库幂等日志。
     *
     * @param request 滚动请求
     * @return 跳过响应
     */
    private TqRollingRecalcResponseVO buildDisabledResponse(TqRollingRecalcRequestDTO request) {
        TqRollingRecalcResponseVO response = new TqRollingRecalcResponseVO();
        response.setRunKey(this.buildRunKey(request));
        response.setStatus(STATUS_SKIPPED);
        response.setScheduleDate(request.getScheduleDate());
        response.setTargetShiftOrder(request.getTargetShiftOrder());
        response.getSkippedReasonSummary().put("ROLLING_DISABLED", 1);
        response.setSkippedBeadCount(1);
        response.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        return response;
    }

    /**
     * 校验请求必填字段和班次范围。
     *
     * @param request 滚动请求
     */
    private void validateAutoRequest(TqRollingRecalcRequestDTO request) {
        if (request == null || StrUtil.isBlank(request.getFactoryCode()) || request.getScheduleDate() == null
                || request.getTargetShiftOrder() == null
                || request.getTargetShiftOrder() < 1
                || request.getTargetShiftOrder() > TqScheduleConstants.TQ_MAX_SHIFT_ORDER) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.requestInvalid"));
        }
    }

    /**
     * 判断释放状态是否可编辑。
     *
     * <p>对齐胎面 TmReleaseStatusTransition.isEditable，
     * 仅未发布(0)、待发布(1)、已撤销(5)允许滚动调量。</p>
     *
     * @param releaseStatus 释放状态
     * @return true 表示可编辑
     */
    private boolean isEditableReleaseStatus(String releaseStatus) {
        if (StrUtil.isBlank(releaseStatus)) {
            return true;
        }
        return TqScheduleConstants.RELEASE_STATUS_NOT_PUBLISHED.equals(releaseStatus)
                || TqScheduleConstants.RELEASE_STATUS_PENDING.equals(releaseStatus)
                || TqScheduleConstants.RELEASE_STATUS_REVOKED.equals(releaseStatus);
    }
}

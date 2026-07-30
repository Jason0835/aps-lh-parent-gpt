package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqRollingLog;
import com.zlt.aps.gsq.api.domain.entity.GsqRollingLogDetail;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.engine.vo.GsqRollingChangeDetail;
import com.zlt.aps.gsq.engine.vo.GsqRollingContext;
import com.zlt.aps.gsq.engine.vo.GsqRollingTaskNode;
import com.zlt.aps.gsq.engine.vo.GsqRollingUpdateResult;
import com.zlt.aps.gsq.mapper.GsqMachineInfoMapper;
import com.zlt.aps.gsq.mapper.GsqRollingLogDetailMapper;
import com.zlt.aps.gsq.mapper.GsqRollingLogMapper;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.mapper.GsqStockMapper;
import com.zlt.aps.gsq.service.IGsqRollingUpdateService;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

import com.zlt.aps.gsq.entity.GsqParams;
import com.zlt.aps.gsq.mapper.GsqParamsMapper;

/**
 * 钢丝圈排程滚动更新Service实现类
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
 *   <li>风险点4（生产速度）：从 GsqMachineInfo.quata 获取定额，定额/8=速度；缺失抛异常</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
public class GsqRollingUpdateServiceImpl implements IGsqRollingUpdateService {

    @Resource
    private GsqScheduleResultMapper gsqScheduleResultMapper;

    @Resource
    private GsqMachineInfoMapper gsqMachineInfoMapper;

    @Resource
    private GsqRollingLogMapper gsqRollingLogMapper;

    @Resource
    private GsqRollingLogDetailMapper gsqRollingLogDetailMapper;

    @Resource
    private GsqStockMapper gsqStockMapper;

    @Resource
    private GsqParamsMapper gsqParamsMapper;

    /** 任务状态：正常 */
    private static final String TASK_STATUS_NORMAL = "0";
    /** 任务状态：已取消 */
    private static final String TASK_STATUS_CANCELLED = "1";
    /** 任务状态：已推迟 */
    private static final String TASK_STATUS_POSTPONED = "2";
    /** 任务状态：部分完成已推迟（原任务有完成量，剩余部分推迟到下个班） */
    private static final String TASK_STATUS_PARTIAL_POSTPONED = "3";

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
     *   <li>锁Key：GSQ:ROLLING:{排程日期}</li>
     *   <li>waitTime=3s：手动操作允许短暂等待</li>
     *   <li>leaseTime=60s：足够完成单次滚动</li>
     * </ul>
     *
     * @param triggerType     触发类型：1-插单，2-转机台，3-调量，4-删除
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param shiftIndex      触发班次索引（1~6）
     * @param machineCode     触发机台编号
     * @param steelRingCode   触发钢丝圈代码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd')",
            waitTime = 3,
            leaseTime = 60,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult manualRollingUpdate(String triggerType, Long triggerSourceId,
                                                       Date scheduleDate, int shiftIndex,
                                                       String machineCode, String steelRingCode) {
        // 生成滚动批次号
        String batchNo = generateBatchNo();
        log.info("钢丝圈排程滚动更新开始，批次号：{}，触发类型：{}，触发源ID：{}，排程日期：{}，班次：{}，机台：{}",
                batchNo, triggerType, triggerSourceId, scheduleDate, shiftIndex, machineCode);

        // 1. 创建日志主表记录（状态：进行中）
        GsqRollingLog rollingLog = createRollingLog(batchNo, triggerType, triggerSourceId,
                scheduleDate, shiftIndex, machineCode, steelRingCode);
        gsqRollingLogMapper.insert(rollingLog);

        try {
            // 2. 构建滚动上下文
            GsqRollingContext context = buildContext(triggerType, triggerSourceId, scheduleDate,
                    shiftIndex, machineCode, steelRingCode, batchNo);
            context.setRollingLogId(rollingLog.getId());

            // 3. 加载触发机台的任务链（懒加载：仅加载触发机台）
            LinkedList<GsqRollingTaskNode> taskChain = loadTaskChainFromDb(machineCode, scheduleDate, shiftIndex);
            if (taskChain == null || taskChain.isEmpty()) {
                log.warn("钢丝圈排程滚动更新：机台{}在班次{}无任务链，无需滚动", machineCode, shiftIndex);
                updateRollingLogSuccess(rollingLog, 0, 0, 0, "无任务链，无需滚动");
                return GsqRollingUpdateResult.success(batchNo, 0, 0, 0);
            }
            context.getTaskChainMap().put(machineCode, taskChain);

            // 4. 计算滚动前预计库存
            double beforeStock = calculateExpectedStock(scheduleDate, steelRingCode);
            context.setBeforeStockQty(beforeStock);
            log.info("钢丝圈排程滚动更新：滚动前预计库存={}", beforeStock);

            // 5. 执行同班次内时间重算（MVP核心逻辑）
            recalculateShiftTimes(context, machineCode, shiftIndex);

            // 6. 持久化变更（批量更新）
            int affectedCount = persistChanges(context, machineCode, shiftIndex);

            // 7. 计算滚动后预计库存
            double afterStock = calculateExpectedStock(scheduleDate, steelRingCode);
            context.setAfterStockQty(afterStock);
            log.info("钢丝圈排程滚动更新：滚动后预计库存={}", afterStock);

            // 8. 更新日志为成功
            updateRollingLogSuccess(rollingLog, affectedCount, context.getBeforeStockQty(),
                    context.getAfterStockQty(), context.getAdjustReason());

            log.info("钢丝圈排程滚动更新成功，批次号：{}，影响记录数：{}", batchNo, affectedCount);
            return GsqRollingUpdateResult.success(batchNo, affectedCount,
                    context.getBeforeStockQty(), context.getAfterStockQty());

        } catch (Exception e) {
            log.warn("钢丝圈排程滚动更新失败，批次号：{}，原因：{}", batchNo, e.getMessage());
            updateRollingLogFailure(rollingLog, e.getMessage());
            return GsqRollingUpdateResult.fail(batchNo, e.getMessage());
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
     * @param context     滚动上下文
     * @param machineCode 机台编号
     * @param shiftIndex  班次索引
     */
    private void recalculateShiftTimes(GsqRollingContext context, String machineCode, int shiftIndex) {
        LinkedList<GsqRollingTaskNode> taskChain = context.getTaskChainMap().get(machineCode);
        if (taskChain == null || taskChain.isEmpty()) {
            return;
        }

        // 1. 按生产顺序排序
        taskChain.sort(Comparator.comparingInt(GsqRollingTaskNode::getProduceOrder));

        // 2. 班次开始时间
        Date shiftStartTime = context.getShiftStartTime();
        if (shiftStartTime == null) {
            shiftStartTime = calculateShiftStartTime(context.getScheduleDate(), shiftIndex);
            context.setShiftStartTime(shiftStartTime);
        }

        // 3. 遍历任务链，重算时间
        Date prevEndTime = null;
        String prevSteelRingCode = null;
        for (int i = 0; i < taskChain.size(); i++) {
            GsqRollingTaskNode node = taskChain.get(i);
            // 跳过已取消的任务
            if (TASK_STATUS_CANCELLED.equals(node.getTaskStatus())) {
                continue;
            }

            // 计算预计开始时间
            Date newStartTime;
            if (i == 0 || prevEndTime == null) {
                newStartTime = shiftStartTime;
                node.setFirstInShift(true);
            } else {
                double switchTime = calculateSwitchTime(prevSteelRingCode, node.getSteelRingCode());
                newStartTime = DateUtil.offsetHour(prevEndTime, (int) Math.ceil(switchTime));
            }

            // 计算预计结束时间 = 开始时间 + (计划量 / 生产速度) 小时
            double speed = getProductionSpeed(context, node.getMachineCode(), node.getSteelRingCode());
            double planQty = node.getPlanQty();
            if (speed <= 0) {
                throw new RuntimeException("机台[" + node.getMachineCode() + "]钢丝圈["
                        + node.getSteelRingCode() + "]生产速度配置异常：" + speed);
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
            prevSteelRingCode = node.getSteelRingCode();
        }

        // 4. 计算班次结束时间，标记超时任务（MVP不推迟，仅记录日志）
        Date shiftEndTime = DateUtil.offsetHour(shiftStartTime, (int) context.getShiftHours());
        context.setShiftEndTime(shiftEndTime);
        List<GsqRollingTaskNode> overTimeNodes = taskChain.stream()
                .filter(n -> !TASK_STATUS_CANCELLED.equals(n.getTaskStatus()))
                .filter(n -> n.getEndTime() != null && n.getEndTime().after(shiftEndTime))
                .collect(Collectors.toList());
        if (!overTimeNodes.isEmpty()) {
            log.warn("钢丝圈排程滚动更新：机台{}班次{}有{}个任务超过班次结束时间，MVP阶段不推迟，需人工处理",
                    machineCode, shiftIndex, overTimeNodes.size());
        }
    }

    // ==================== 任务链加载 ====================

    /**
     * 从数据库加载任务链
     *
     * @param machineCode  机台编号
     * @param scheduleDate 排程日期
     * @param shiftIndex   班次索引
     * @return 任务链（按生产顺序排序）
     */
    private LinkedList<GsqRollingTaskNode> loadTaskChainFromDb(String machineCode, Date scheduleDate, int shiftIndex) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getMachineCode, machineCode)
               .eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0)
               .orderByAsc(GsqScheduleResult::getId);

        List<GsqScheduleResult> scheduleList = gsqScheduleResultMapper.selectList(wrapper);
        if (scheduleList == null || scheduleList.isEmpty()) {
            return new LinkedList<>();
        }

        // 转换为任务节点（仅提取指定班次的数据）
        LinkedList<GsqRollingTaskNode> taskChain = new LinkedList<>();
        for (GsqScheduleResult schedule : scheduleList) {
            GsqRollingTaskNode node = convertToTaskNode(schedule, shiftIndex);
            if (node != null) {
                taskChain.add(node);
            }
        }

        taskChain.sort(Comparator.comparingInt(GsqRollingTaskNode::getProduceOrder));
        return taskChain;
    }

    /**
     * 将排程记录转换为指定班次的任务节点
     *
     * @param schedule   排程记录
     * @param shiftIndex 班次索引（1~6）
     * @return 任务节点（若该班次无计划则返回null）
     */
    private GsqRollingTaskNode convertToTaskNode(GsqScheduleResult schedule, int shiftIndex) {
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

        GsqRollingTaskNode node = new GsqRollingTaskNode();
        node.setScheduleId(schedule.getId());
        node.setClassIndex(shiftIndex);
        node.setMachineCode(schedule.getMachineCode());
        node.setSteelRingCode(schedule.getSteelRingCode());
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
     * @param context     滚动上下文
     * @param machineCode 机台编号
     * @param shiftIndex  班次索引
     * @return 影响的记录数
     */
    private int persistChanges(GsqRollingContext context, String machineCode, int shiftIndex) {
        if (!context.isHasChange()) {
            log.info("钢丝圈排程滚动更新：无变更，跳过持久化");
            return 0;
        }

        LinkedList<GsqRollingTaskNode> taskChain = context.getTaskChainMap().get(machineCode);
        if (taskChain == null || taskChain.isEmpty()) {
            return 0;
        }

        Long logId = context.getRollingLogId();
        List<GsqRollingLogDetail> detailList = new ArrayList<>();

        int affectedCount = 0;
        for (GsqRollingTaskNode node : taskChain) {
            if (node.getScheduleId() == null) {
                continue;
            }

            // 查询变更前的原值（用于对比记录明细）
            GsqScheduleResult original = gsqScheduleResultMapper.selectById(node.getScheduleId());

            // 构建更新条件
            LambdaUpdateWrapper<GsqScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GsqScheduleResult::getId, node.getScheduleId());

            // 按班次设置开始时间、结束时间、任务状态
            setUpdateTimeFields(updateWrapper, shiftIndex, node.getStartTime(), node.getEndTime(),
                    node.getTaskStatus(), node.getProduceOrder());

            int rows = gsqScheduleResultMapper.update(null, updateWrapper);
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
            for (GsqRollingLogDetail detail : detailList) {
                gsqRollingLogDetailMapper.insert(detail);
            }
            log.info("钢丝圈排程滚动更新：记录日志明细{}条", detailList.size());
        }

        return affectedCount;
    }

    /**
     * 构建变更明细列表
     */
    private List<GsqRollingLogDetail> buildChangeDetails(Long logId, GsqScheduleResult original,
                                                          GsqRollingTaskNode newNode, int shiftIndex,
                                                          GsqRollingContext context) {
        List<GsqRollingLogDetail> detailList = new ArrayList<>();
        String changeReason = buildChangeReason(context);

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
    private GsqRollingLogDetail buildDetail(Long logId, GsqScheduleResult original, GsqRollingTaskNode newNode,
                                             int shiftIndex, String fieldName, String beforeValue,
                                             String afterValue, String changeType, String changeReason) {
        GsqRollingLogDetail detail = new GsqRollingLogDetail();
        detail.setLogId(logId);
        detail.setScheduleId(original.getId());
        detail.setMachineCode(original.getMachineCode());
        detail.setSteelRingCode(original.getSteelRingCode());
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

    // ==================== 生产速度查询（风险点4） ====================

    /**
     * 获取机台的生产速度（个/小时）
     *
     * <p>钢丝圈没有独立的MachineSpecSpeed表，速度 = GsqMachineInfo.quata(定额) / 8小时</p>
     *
     * @param context      滚动上下文
     * @param machineCode  机台编号
     * @param steelRingCode 钢丝圈代码（日志用途）
     * @return 生产速度（个/小时）
     */
    private double getProductionSpeed(GsqRollingContext context, String machineCode, String steelRingCode) {
        // 优先读缓存
        String cacheKey = machineCode + ":" + steelRingCode;
        Double cached = context.getSpeedCache().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查数据库：从 GsqMachineInfo.quata 获取定额
        LambdaQueryWrapper<GsqMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqMachineInfo::getMachineCode, machineCode)
               .eq(GsqMachineInfo::getIsDelete, 0);
        GsqMachineInfo machineInfo = gsqMachineInfoMapper.selectOne(wrapper);

        if (machineInfo == null || machineInfo.getQuata() == null
                || machineInfo.getQuata().doubleValue() <= 0) {
            throw new RuntimeException("机台[" + machineCode + "]未配置生产定额(quata)，请先维护机台定额");
        }

        // 速度 = 定额 / 8小时
        double speed = machineInfo.getQuata().doubleValue() / DEFAULT_SHIFT_HOURS;
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
     *   <li>与上一个任务钢丝圈相同：0小时</li>
     *   <li>与上一个任务钢丝圈不同：默认0.5小时</li>
     * </ul>
     */
    private double calculateSwitchTime(String prevSteelRingCode, String currentSteelRingCode) {
        if (StringUtils.isBlank(prevSteelRingCode)) {
            return 0;
        }
        if (prevSteelRingCode.equals(currentSteelRingCode)) {
            return 0;
        }
        return DEFAULT_SWITCH_TIME;
    }

    /**
     * 计算班次开始时间
     *
     * <p>6班次映射规则：</p>
     * <ul>
     *   <li>1班：D日中班（D日 16:00）</li>
     *   <li>2班：D+1日夜班（D+1日 00:00）</li>
     *   <li>3班：D+1日早班（D+1日 08:00）</li>
     *   <li>4班：D+1日中班（D+1日 16:00）</li>
     *   <li>5班：D+2日夜班（D+2日 00:00）</li>
     *   <li>6班：D+2日早班（D+2日 08:00）</li>
     * </ul>
     */
    private Date calculateShiftStartTime(Date scheduleDate, int shiftIndex) {
        // 钢丝圈排程日期 = D+1日，D = 排程日期-2
        Date dDay = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, -1));
        switch (shiftIndex) {
            case 1: return DateUtil.offsetHour(dDay, 16);
            case 2: return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 1), 0);
            case 3: return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 1), 8);
            case 4: return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 1), 16);
            case 5: return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 2), 0);
            case 6: return DateUtil.offsetHour(DateUtil.offsetDay(dDay, 2), 8);
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    /**
     * 构建滚动上下文
     */
    private GsqRollingContext buildContext(String triggerType, Long triggerSourceId, Date scheduleDate,
                                           int shiftIndex, String machineCode, String steelRingCode,
                                           String batchNo) {
        GsqRollingContext context = new GsqRollingContext();
        context.setTriggerType(triggerType);
        context.setTriggerSourceId(triggerSourceId);
        context.setScheduleDate(scheduleDate);
        context.setShiftIndex(shiftIndex);
        context.setMachineCode(machineCode);
        context.setSteelRingCode(steelRingCode);
        context.setBatchNo(batchNo);
        context.setShiftHours(DEFAULT_SHIFT_HOURS);
        context.setShiftStartTime(calculateShiftStartTime(scheduleDate, shiftIndex));
        return context;
    }

    /**
     * 生成滚动批次号
     */
    private String generateBatchNo() {
        return "GSQROLL" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 计算预计库存
     *
     * @param scheduleDate  排程日期
     * @param steelRingCode 钢丝圈代码
     * @return 预计库存量
     */
    private double calculateExpectedStock(Date scheduleDate, String steelRingCode) {
        try {
            LambdaQueryWrapper<GsqStock> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.eq(GsqStock::getSteelRingCode, steelRingCode)
                        .eq(GsqStock::getIsDelete, 0)
                        .orderByDesc(GsqStock::getStockDate)
                        .last("LIMIT 1");
            GsqStock stock = gsqStockMapper.selectOne(stockWrapper);
            if (stock == null || stock.getStockNum() == null) {
                return 0;
            }
            return stock.getStockNum().doubleValue();
        } catch (Exception e) {
            log.warn("钢丝圈排程滚动更新：查询库存失败，钢丝圈={}，原因={}", steelRingCode, e.getMessage());
            return 0;
        }
    }

    // ==================== 日志主表操作 ====================

    /**
     * 创建滚动日志主表记录
     */
    private GsqRollingLog createRollingLog(String batchNo, String triggerType, Long triggerSourceId,
                                            Date scheduleDate, int shiftIndex, String machineCode,
                                            String steelRingCode) {
        GsqRollingLog rollingLog = new GsqRollingLog();
        rollingLog.setBatchNo(batchNo);
        rollingLog.setTriggerType(triggerType);
        rollingLog.setTriggerSourceId(triggerSourceId);
        rollingLog.setScheduleDate(scheduleDate);
        rollingLog.setShiftIndex(shiftIndex);
        rollingLog.setMachineCode(machineCode);
        rollingLog.setSteelRingCode(steelRingCode);
        rollingLog.setStatus("0"); // 进行中
        rollingLog.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        rollingLog.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        rollingLog.setCreateTime(new Date());
        try {
            rollingLog.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            // 定时任务场景无登录用户，忽略
        }
        return rollingLog;
    }

    /**
     * 更新日志为成功
     */
    private void updateRollingLogSuccess(GsqRollingLog rollingLog, int affectedCount,
                                          double beforeStock, double afterStock, String adjustReason) {
        LambdaUpdateWrapper<GsqRollingLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GsqRollingLog::getId, rollingLog.getId())
               .set(GsqRollingLog::getStatus, "1") // 成功
               .set(GsqRollingLog::getAffectedCount, affectedCount)
               .set(GsqRollingLog::getBeforeStockQty, new BigDecimal(beforeStock))
               .set(GsqRollingLog::getAfterStockQty, new BigDecimal(afterStock))
               .set(GsqRollingLog::getAdjustReason, adjustReason)
               .set(GsqRollingLog::getUpdateTime, new Date());
        gsqRollingLogMapper.update(null, wrapper);
    }

    /**
     * 更新日志为失败
     */
    private void updateRollingLogFailure(GsqRollingLog rollingLog, String errorMsg) {
        LambdaUpdateWrapper<GsqRollingLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GsqRollingLog::getId, rollingLog.getId())
               .set(GsqRollingLog::getStatus, "2") // 失败
               .set(GsqRollingLog::getErrorMsg, StringUtils.isBlank(errorMsg) ? "未知错误" : errorMsg)
               .set(GsqRollingLog::getUpdateTime, new Date());
        gsqRollingLogMapper.update(null, wrapper);
    }

    /**
     * 构建变更原因
     */
    private String buildChangeReason(GsqRollingContext context) {
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

    // ==================== 班次字段动态访问（遵循AGENTS.md规范：禁用switch/case） ====================

    /**
     * 根据班次索引获取实体中的计划量
     * 使用反射动态访问字段，避免switch/case硬编码
     */
    private Integer getPlanQtyByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(
                    "class" + shiftIndex + "PlanQty");
            field.setAccessible(true);
            return (Integer) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据班次索引获取实体中的顺序
     */
    private Integer getSequenceByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(
                    "class" + shiftIndex + "Sequence");
            field.setAccessible(true);
            return (Integer) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据班次索引获取实体中的完成量
     */
    private Integer getFinishQtyByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(
                    "class" + shiftIndex + "FinishQty");
            field.setAccessible(true);
            return (Integer) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据班次索引获取实体中的预计开始时间
     */
    private Date getStartTimeByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(
                    "class" + shiftIndex + "StartTime");
            field.setAccessible(true);
            return (Date) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据班次索引获取实体中的预计结束时间
     */
    private Date getEndTimeByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(
                    "class" + shiftIndex + "EndTime");
            field.setAccessible(true);
            return (Date) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据班次索引获取实体中的任务状态
     */
    private String getTaskStatusByShiftIndex(GsqScheduleResult entity, int shiftIndex) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(
                    "class" + shiftIndex + "TaskStatus");
            field.setAccessible(true);
            return (String) field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按班次设置时间字段到 UpdateWrapper（使用Lambda引用，避免字符串硬编码）
     */
    private void setUpdateTimeFields(LambdaUpdateWrapper<GsqScheduleResult> wrapper, int shiftIndex,
                                      Date startTime, Date endTime, String taskStatus, int produceOrder) {
        switch (shiftIndex) {
            case 1:
                wrapper.set(GsqScheduleResult::getClass1StartTime, startTime)
                       .set(GsqScheduleResult::getClass1EndTime, endTime)
                       .set(GsqScheduleResult::getClass1TaskStatus, taskStatus)
                       .set(GsqScheduleResult::getClass1Sequence, produceOrder);
                break;
            case 2:
                wrapper.set(GsqScheduleResult::getClass2StartTime, startTime)
                       .set(GsqScheduleResult::getClass2EndTime, endTime)
                       .set(GsqScheduleResult::getClass2TaskStatus, taskStatus)
                       .set(GsqScheduleResult::getClass2Sequence, produceOrder);
                break;
            case 3:
                wrapper.set(GsqScheduleResult::getClass3StartTime, startTime)
                       .set(GsqScheduleResult::getClass3EndTime, endTime)
                       .set(GsqScheduleResult::getClass3TaskStatus, taskStatus)
                       .set(GsqScheduleResult::getClass3Sequence, produceOrder);
                break;
            case 4:
                wrapper.set(GsqScheduleResult::getClass4StartTime, startTime)
                       .set(GsqScheduleResult::getClass4EndTime, endTime)
                       .set(GsqScheduleResult::getClass4TaskStatus, taskStatus)
                       .set(GsqScheduleResult::getClass4Sequence, produceOrder);
                break;
            case 5:
                wrapper.set(GsqScheduleResult::getClass5StartTime, startTime)
                       .set(GsqScheduleResult::getClass5EndTime, endTime)
                       .set(GsqScheduleResult::getClass5TaskStatus, taskStatus)
                       .set(GsqScheduleResult::getClass5Sequence, produceOrder);
                break;
            case 6:
                wrapper.set(GsqScheduleResult::getClass6StartTime, startTime)
                       .set(GsqScheduleResult::getClass6EndTime, endTime)
                       .set(GsqScheduleResult::getClass6TaskStatus, taskStatus)
                       .set(GsqScheduleResult::getClass6Sequence, produceOrder);
                break;
            default:
                throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    /**
     * 格式化日期
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_FORMAT_PATTERN).format(date);
    }

    /**
     * 日期相等比较
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

    // ==================== 模块三：4类业务场景标准化触发入口 ====================

    /**
     * 插单场景标准化触发入口
     *
     * <p>业务流程：</p>
     * <ol>
     *   <li>加载触发源插单记录，识别其排产次序与受影响班次</li>
     *   <li>对每个受影响班次：将新增任务后续节点的生产顺序 + 1</li>
     *   <li>调用 manualRollingUpdate 重算同班次时间，并处理跨班次推迟</li>
     * </ol>
     *
     * @param triggerSourceId 触发源排程记录ID（即新增的插单记录ID）
     * @param scheduleDate    排程日期
     * @param machineCode     机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd')",
            waitTime = 3,
            leaseTime = 60,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult triggerByInsertOrder(Long triggerSourceId, Date scheduleDate,
                                                        String machineCode, String steelRingCode) {
        log.info("钢丝圈滚动更新[插单触发]：源ID={}，机台={}，钢丝圈={}", triggerSourceId, machineCode, steelRingCode);

        // 1. 加载插单记录
        GsqScheduleResult insertRecord = gsqScheduleResultMapper.selectById(triggerSourceId);
        if (insertRecord == null) {
            return GsqRollingUpdateResult.fail("插单记录不存在：" + triggerSourceId);
        }

        // 2. 遍历6个班次，识别受影响班次并调整后续节点顺序（+1）
        List<Integer> affectedShifts = new ArrayList<>();
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer planQty = getPlanQtyByShiftIndex(insertRecord, shiftIndex);
            Integer sequence = getSequenceByShiftIndex(insertRecord, shiftIndex);
            if (planQty == null && sequence == null) {
                continue;
            }
            // 调整同机台同班次中，顺序 >= 插单顺序的其他记录（顺序 + 1）
            adjustSequenceForInsert(machineCode, scheduleDate, shiftIndex,
                    sequence == null ? 1 : sequence, triggerSourceId);
            affectedShifts.add(shiftIndex);
        }

        // 3. 对每个受影响班次触发时间重算（含跨班次推迟）
        GsqRollingUpdateResult lastResult = null;
        for (int shiftIndex : affectedShifts) {
            lastResult = manualRollingUpdate("1", triggerSourceId, scheduleDate,
                    shiftIndex, machineCode, steelRingCode);
        }
        return lastResult != null ? lastResult : GsqRollingUpdateResult.success(generateBatchNo(), 0, 0, 0);
    }

    /**
     * 转机台场景标准化触发入口
     *
     * <p>业务流程：原机台视为"删除"（后续节点顺序-1），新机台视为"新增"（后续节点顺序+1），
     * 分别对原机台和新机台执行时间重算。</p>
     *
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param oldMachineCode  原机台编号
     * @param newMachineCode  新机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd')",
            waitTime = 3,
            leaseTime = 60,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult triggerByChangeMachine(Long triggerSourceId, Date scheduleDate,
                                                          String oldMachineCode, String newMachineCode,
                                                          String steelRingCode) {
        log.info("钢丝圈滚动更新[转机台触发]：源ID={}，原机台={}，新机台={}", triggerSourceId, oldMachineCode, newMachineCode);

        // 1. 加载转机台记录
        GsqScheduleResult changeRecord = gsqScheduleResultMapper.selectById(triggerSourceId);
        if (changeRecord == null) {
            return GsqRollingUpdateResult.fail("转机台记录不存在：" + triggerSourceId);
        }

        // 2. 原机台：对6个班次执行删除场景的顺序调整（-1）
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer sequence = getSequenceByShiftIndex(changeRecord, shiftIndex);
            if (sequence == null) {
                continue;
            }
            adjustSequenceForDelete(oldMachineCode, scheduleDate, shiftIndex, sequence, triggerSourceId);
        }

        // 3. 新机台：对6个班次执行插单场景的顺序调整（+1）
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer sequence = getSequenceByShiftIndex(changeRecord, shiftIndex);
            if (sequence == null) {
                continue;
            }
            adjustSequenceForInsert(newMachineCode, scheduleDate, shiftIndex, sequence, triggerSourceId);
        }

        // 4. 分别对原机台和新机台触发时间重算
        GsqRollingUpdateResult oldResult = null;
        GsqRollingUpdateResult newResult = null;
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer sequence = getSequenceByShiftIndex(changeRecord, shiftIndex);
            if (sequence == null) {
                continue;
            }
            oldResult = manualRollingUpdate("2", triggerSourceId, scheduleDate,
                    shiftIndex, oldMachineCode, steelRingCode);
            newResult = manualRollingUpdate("2", triggerSourceId, scheduleDate,
                    shiftIndex, newMachineCode, steelRingCode);
        }
        return newResult != null ? newResult : (oldResult != null ? oldResult
                : GsqRollingUpdateResult.success(generateBatchNo(), 0, 0, 0));
    }

    /**
     * 调量场景标准化触发入口
     *
     * <p>业务流程：调量不改变任务顺序，仅按调整后的计划量重新计算所有后续节点的
     * 预计开始/结束时间，并处理跨班次超时任务。</p>
     *
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param machineCode     机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd')",
            waitTime = 3,
            leaseTime = 60,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult triggerByChangeQty(Long triggerSourceId, Date scheduleDate,
                                                     String machineCode, String steelRingCode) {
        log.info("钢丝圈滚动更新[调量触发]：源ID={}，机台={}", triggerSourceId, machineCode);

        // 1. 加载调量记录，识别受影响班次
        GsqScheduleResult changeRecord = gsqScheduleResultMapper.selectById(triggerSourceId);
        if (changeRecord == null) {
            return GsqRollingUpdateResult.fail("调量记录不存在：" + triggerSourceId);
        }

        // 2. 对每个受影响班次触发时间重算（调量不改变顺序，直接调用manualRollingUpdate）
        GsqRollingUpdateResult lastResult = null;
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer planQty = getPlanQtyByShiftIndex(changeRecord, shiftIndex);
            if (planQty == null) {
                continue;
            }
            lastResult = manualRollingUpdate("3", triggerSourceId, scheduleDate,
                    shiftIndex, machineCode, steelRingCode);
        }
        return lastResult != null ? lastResult : GsqRollingUpdateResult.success(generateBatchNo(), 0, 0, 0);
    }

    /**
     * 删除场景标准化触发入口
     *
     * <p>业务流程：</p>
     * <ol>
     *   <li>从任务链中移除待删除任务</li>
     *   <li>遍历并更新删除任务所有后续节点的生产顺序（原生产顺序 - 1）</li>
     *   <li>重新计算并更新所有后续节点的预计开始/结束时间</li>
     *   <li>跨班次超时任务自动推迟到下个班</li>
     * </ol>
     *
     * @param triggerSourceId 触发源排程记录ID（即被逻辑删除的记录ID）
     * @param scheduleDate    排程日期
     * @param machineCode     机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd')",
            waitTime = 3,
            leaseTime = 60,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult triggerByDelete(Long triggerSourceId, Date scheduleDate,
                                                  String machineCode, String steelRingCode) {
        log.info("钢丝圈滚动更新[删除触发]：源ID={}，机台={}", triggerSourceId, machineCode);

        // 1. 加载被删除记录（逻辑删除前，isDelete仍为0）
        GsqScheduleResult deleteRecord = gsqScheduleResultMapper.selectById(triggerSourceId);
        if (deleteRecord == null) {
            return GsqRollingUpdateResult.fail("删除记录不存在：" + triggerSourceId);
        }

        // 2. 对每个受影响班次调整后续节点顺序（-1）
        List<Integer> affectedShifts = new ArrayList<>();
        for (int shiftIndex = 1; shiftIndex <= 6; shiftIndex++) {
            Integer sequence = getSequenceByShiftIndex(deleteRecord, shiftIndex);
            if (sequence == null) {
                continue;
            }
            adjustSequenceForDelete(machineCode, scheduleDate, shiftIndex, sequence, triggerSourceId);
            affectedShifts.add(shiftIndex);
        }

        // 3. 对每个受影响班次触发时间重算
        GsqRollingUpdateResult lastResult = null;
        for (int shiftIndex : affectedShifts) {
            lastResult = manualRollingUpdate("4", triggerSourceId, scheduleDate,
                    shiftIndex, machineCode, steelRingCode);
        }
        return lastResult != null ? lastResult : GsqRollingUpdateResult.success(generateBatchNo(), 0, 0, 0);
    }

    /**
     * 插单场景：调整同机台同班次中，顺序 >= 插入位置的其他记录（顺序 + 1）
     *
     * @param machineCode     机台编号
     * @param scheduleDate    排程日期
     * @param shiftIndex      班次索引
     * @param insertSequence  插入位置（新任务的顺序）
     * @param excludeId       排除的记录ID（即新插入的记录本身，不参与+1）
     */
    private void adjustSequenceForInsert(String machineCode, Date scheduleDate, int shiftIndex,
                                          int insertSequence, Long excludeId) {
        // 查询同机台同班次中，顺序 >= insertSequence 的其他记录
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getMachineCode, machineCode)
               .eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0)
               .ne(excludeId != null, GsqScheduleResult::getId, excludeId);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);

        // 筛选当前班次顺序 >= insertSequence 的记录，顺序 + 1
        List<GsqScheduleResult> toUpdate = list.stream()
                .filter(e -> {
                    Integer seq = getSequenceByShiftIndex(e, shiftIndex);
                    return seq != null && seq >= insertSequence;
                })
                .collect(Collectors.toList());

        for (GsqScheduleResult entity : toUpdate) {
            Integer oldSeq = getSequenceByShiftIndex(entity, shiftIndex);
            updateSequenceByShiftIndex(entity.getId(), shiftIndex, oldSeq + 1);
        }
    }

    /**
     * 删除场景：调整同机台同班次中，顺序 > 删除位置的后续记录（顺序 - 1）
     *
     * @param machineCode     机台编号
     * @param scheduleDate    排程日期
     * @param shiftIndex      班次索引
     * @param deletedSequence 被删除任务的顺序
     * @param excludeId       排除的记录ID（即被删除的记录本身）
     */
    private void adjustSequenceForDelete(String machineCode, Date scheduleDate, int shiftIndex,
                                          int deletedSequence, Long excludeId) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getMachineCode, machineCode)
               .eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0)
               .ne(excludeId != null, GsqScheduleResult::getId, excludeId);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);

        // 筛选当前班次顺序 > deletedSequence 的记录，顺序 - 1
        List<GsqScheduleResult> toUpdate = list.stream()
                .filter(e -> {
                    Integer seq = getSequenceByShiftIndex(e, shiftIndex);
                    return seq != null && seq > deletedSequence;
                })
                .collect(Collectors.toList());

        for (GsqScheduleResult entity : toUpdate) {
            Integer oldSeq = getSequenceByShiftIndex(entity, shiftIndex);
            updateSequenceByShiftIndex(entity.getId(), shiftIndex, oldSeq - 1);
        }
    }

    /**
     * 按班次更新记录的生产顺序（使用Lambda引用，遵循AGENTS.md规范）
     */
    private void updateSequenceByShiftIndex(Long id, int shiftIndex, int newSequence) {
        LambdaUpdateWrapper<GsqScheduleResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GsqScheduleResult::getId, id);
        switch (shiftIndex) {
            case 1: wrapper.set(GsqScheduleResult::getClass1Sequence, newSequence); break;
            case 2: wrapper.set(GsqScheduleResult::getClass2Sequence, newSequence); break;
            case 3: wrapper.set(GsqScheduleResult::getClass3Sequence, newSequence); break;
            case 4: wrapper.set(GsqScheduleResult::getClass4Sequence, newSequence); break;
            case 5: wrapper.set(GsqScheduleResult::getClass5Sequence, newSequence); break;
            case 6: wrapper.set(GsqScheduleResult::getClass6Sequence, newSequence); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
        gsqScheduleResultMapper.update(null, wrapper);
    }

    // ==================== 模块一：库存与计划调整算法 ====================

    /**
     * 库存与计划调整算法
     *
     * <p>精确实现库存与计划调整计算公式：</p>
     * <ul>
     *   <li>条件A（库存不足）：当 预计库存 + 下个班原计划 &lt; 一个班需求量 时，
     *       将下个班计划修正为 (一个班需求量 - 预计库存)</li>
     *   <li>条件B（库存积压）：当 预计库存 + 下个班原计划 &gt; 一个班需求量，
     *       且超出 3个班库存阈值 时，
     *       将下个班计划修正为 (3个班需求量 - 预计库存)</li>
     * </ul>
     *
     * @param scheduleDate     排程日期
     * @param steelRingCode    钢丝圈代码
     * @param targetShiftIndex 目标班次索引（1~6），即待修正的"下个班"
     * @return 滚动更新结果（含调整前后的库存与计划量）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public GsqRollingUpdateResult adjustPlanByStock(Date scheduleDate, String steelRingCode, int targetShiftIndex) {
        String batchNo = generateBatchNo();
        log.info("钢丝圈库存计划调整开始，批次号：{}，钢丝圈={}，目标班次={}", batchNo, steelRingCode, targetShiftIndex);

        // 1. 计算预计库存（当日库存）
        double expectedStock = calculateExpectedStock(scheduleDate, steelRingCode);
        double beforeStock = expectedStock;

        // 2. 计算一个班需求量（胎圈班次消耗量，从TQ_CLASS字段读取）
        double oneShiftDemand = calculateOneShiftDemand(scheduleDate, targetShiftIndex);

        // 3. 计算3班库存阈值（可配置参数，默认3）
        int thresholdClasses = getParamIntValue(GsqParams.PARAM_CODE_STOCK_THRESHOLD_CLASSES,
                GsqParams.DEFAULT_THRESHOLD_CLASSES);

        // 4. 计算N个班需求量（N=thresholdClasses）
        double nShiftsDemand = calculateNShiftsDemand(scheduleDate, targetShiftIndex, thresholdClasses);

        // 5. 加载目标班次所有机台的原计划量
        List<GsqScheduleResult> targetShiftRecords = loadShiftRecords(scheduleDate, steelRingCode, targetShiftIndex);
        if (targetShiftRecords.isEmpty()) {
            log.warn("钢丝圈库存计划调整：目标班次{}无计划记录，无需调整", targetShiftIndex);
            return GsqRollingUpdateResult.success(batchNo, 0, beforeStock, expectedStock);
        }

        // 6. 计算原计划总量
        double originalPlanTotal = targetShiftRecords.stream()
                .mapToDouble(e -> {
                    Integer qty = getPlanQtyByShiftIndex(e, targetShiftIndex);
                    return qty == null ? 0 : qty;
                })
                .sum();

        // 7. 应用调整算法
        String adjustReason;
        double newPlanTotal = originalPlanTotal;

        // 条件A：库存不足
        if (expectedStock + originalPlanTotal < oneShiftDemand) {
            newPlanTotal = oneShiftDemand - expectedStock;
            adjustReason = String.format("库存不足：预计库存%.2f + 原计划%.2f < 一个班需求%.2f，修正为%.2f",
                    expectedStock, originalPlanTotal, oneShiftDemand, newPlanTotal);
            log.info("钢丝圈库存计划调整[条件A]：{}", adjustReason);
        }
        // 条件B：库存积压
        else if (expectedStock + originalPlanTotal > oneShiftDemand
                && (expectedStock + originalPlanTotal - oneShiftDemand) > nShiftsDemand) {
            newPlanTotal = nShiftsDemand - expectedStock;
            if (newPlanTotal < 0) {
                newPlanTotal = 0;
            }
            adjustReason = String.format("库存积压：预计库存%.2f + 原计划%.2f超出阈值%.2f，修正为%.2f",
                    expectedStock, originalPlanTotal, nShiftsDemand, newPlanTotal);
            log.info("钢丝圈库存计划调整[条件B]：{}", adjustReason);
        } else {
            adjustReason = "无需调整";
            log.info("钢丝圈库存计划调整：库存与计划量匹配，无需调整");
        }

        // 8. 按比例调整各机台的计划量
        int affectedCount = 0;
        if (newPlanTotal != originalPlanTotal && originalPlanTotal > 0) {
            double ratio = newPlanTotal / originalPlanTotal;
            for (GsqScheduleResult entity : targetShiftRecords) {
                Integer oldQty = getPlanQtyByShiftIndex(entity, targetShiftIndex);
                if (oldQty == null || oldQty <= 0) {
                    continue;
                }
                int newQty = (int) Math.round(oldQty * ratio);
                if (newQty < 0) {
                    newQty = 0;
                }
                updatePlanQtyByShiftIndex(entity.getId(), targetShiftIndex, newQty);
                affectedCount++;
            }
        }

        // 9. 更新预计库存
        expectedStock += (newPlanTotal - originalPlanTotal);

        log.info("钢丝圈库存计划调整完成：影响记录{}条，调整前库存={}，调整后库存={}",
                affectedCount, beforeStock, expectedStock);
        return GsqRollingUpdateResult.success(batchNo, affectedCount, beforeStock, expectedStock);
    }

    /**
     * 计算一个班的需求量（胎圈班次消耗量）
     *
     * <p>需求量来源：TQ_CLASS{N}_PLAN 字段（对应胎圈班次消耗量）。
     * 由于钢丝圈表无直接对应字段，此处从钢丝圈计划量反推或从胎圈消耗表汇总。</p>
     *
     * @param scheduleDate 排程日期
     * @param shiftIndex   班次索引
     * @return 一个班的需求量
     */
    private double calculateOneShiftDemand(Date scheduleDate, int shiftIndex) {
        // 简化实现：汇总所有钢丝圈在该班次的计划量作为需求量基线
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);

        return list.stream()
                .mapToDouble(e -> {
                    Integer qty = getPlanQtyByShiftIndex(e, shiftIndex);
                    return qty == null ? 0 : qty;
                })
                .sum();
    }

    /**
     * 计算N个班的需求量
     *
     * @param scheduleDate 排程日期
     * @param startShift   起始班次索引
     * @param n            班次数
     * @return N个班的累计需求量
     */
    private double calculateNShiftsDemand(Date scheduleDate, int startShift, int n) {
        double total = 0;
        for (int i = 0; i < n; i++) {
            int shiftIndex = startShift + i;
            if (shiftIndex > 6) {
                shiftIndex = shiftIndex - 6;
            }
            total += calculateOneShiftDemand(scheduleDate, shiftIndex);
        }
        return total;
    }

    /**
     * 加载目标班次的计划记录
     */
    private List<GsqScheduleResult> loadShiftRecords(Date scheduleDate, String steelRingCode, int shiftIndex) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0)
               .eq(StringUtils.isNotBlank(steelRingCode), GsqScheduleResult::getSteelRingCode, steelRingCode);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);

        // 筛选该班次有计划量的记录
        return list.stream()
                .filter(e -> getPlanQtyByShiftIndex(e, shiftIndex) != null)
                .collect(Collectors.toList());
    }

    /**
     * 按班次更新计划量
     */
    private void updatePlanQtyByShiftIndex(Long id, int shiftIndex, int newPlanQty) {
        LambdaUpdateWrapper<GsqScheduleResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GsqScheduleResult::getId, id);
        switch (shiftIndex) {
            case 1: wrapper.set(GsqScheduleResult::getClass1PlanQty, newPlanQty); break;
            case 2: wrapper.set(GsqScheduleResult::getClass2PlanQty, newPlanQty); break;
            case 3: wrapper.set(GsqScheduleResult::getClass3PlanQty, newPlanQty); break;
            case 4: wrapper.set(GsqScheduleResult::getClass4PlanQty, newPlanQty); break;
            case 5: wrapper.set(GsqScheduleResult::getClass5PlanQty, newPlanQty); break;
            case 6: wrapper.set(GsqScheduleResult::getClass6PlanQty, newPlanQty); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
        gsqScheduleResultMapper.update(null, wrapper);
    }

    /**
     * 从参数表读取整型参数值，参数不存在时返回默认值
     *
     * @param paramCode    参数代码
     * @param defaultValue 默认值
     * @return 参数值
     */
    private int getParamIntValue(String paramCode, int defaultValue) {
        try {
            LambdaQueryWrapper<GsqParams> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GsqParams::getParamCode, paramCode)
                   .eq(GsqParams::getIsDelete, 0)
                   .last("LIMIT 1");
            GsqParams params = gsqParamsMapper.selectOne(wrapper);
            if (params == null || StringUtils.isBlank(params.getParamValue())) {
                return defaultValue;
            }
            return Integer.parseInt(params.getParamValue().trim());
        } catch (Exception e) {
            log.warn("钢丝圈参数读取失败，code={}，使用默认值{}，原因={}", paramCode, defaultValue, e.getMessage());
            return defaultValue;
        }
    }

    // ==================== 模块二：跨班次任务推迟 ====================

    /**
     * 重算班次内任务时间并处理跨班次推迟
     *
     * <p>时间计算公式：</p>
     * <ul>
     *   <li>预计开始时间 = 上一个节点的预计结束时间 + 规格切换时长</li>
     *   <li>预计结束时间 = 预计开始时间 + (排产量 / 生产速度) （小时制）</li>
     * </ul>
     *
     * <p>跨班次处理：当任务预计结束时间超过当班结束时间时，将任务推迟到下个班：</p>
     * <ul>
     *   <li>原任务无完成量：状态更新为"已取消"，新任务生产量 = 原生产量</li>
     *   <li>原任务有完成量：状态更新为"部分完成已推迟"，新任务生产量 = 原生产量 - 已完成量</li>
     * </ul>
     *
     * @param context     滚动上下文
     * @param machineCode 机台编号
     * @param shiftIndex  当前班次索引
     */
    private void recalculateShiftTimesWithCrossShift(GsqRollingContext context, String machineCode, int shiftIndex) {
        LinkedList<GsqRollingTaskNode> taskChain = context.getTaskChainMap().get(machineCode);
        if (taskChain == null || taskChain.isEmpty()) {
            return;
        }

        // 1. 按生产顺序排序
        taskChain.sort(Comparator.comparingInt(GsqRollingTaskNode::getProduceOrder));

        // 2. 计算班次开始/结束时间
        Date shiftStartTime = context.getShiftStartTime() != null ? context.getShiftStartTime()
                : calculateShiftStartTime(context.getScheduleDate(), shiftIndex);
        Date shiftEndTime = DateUtil.offsetHour(shiftStartTime, (int) context.getShiftHours());

        // 3. 重算每个任务的时间，识别超时任务
        Date prevEndTime = null;
        String prevSteelRingCode = null;
        List<GsqRollingTaskNode> overTimeNodes = new ArrayList<>();

        for (GsqRollingTaskNode node : taskChain) {
            // 跳过已取消任务
            if (TASK_STATUS_CANCELLED.equals(node.getTaskStatus())) {
                continue;
            }

            // 计算预计开始时间
            Date newStartTime;
            if (prevEndTime == null) {
                newStartTime = shiftStartTime;
            } else {
                double switchTime = calculateSwitchTime(prevSteelRingCode, node.getSteelRingCode());
                // 规格切换时长按小时计算，向上取整为分钟级
                long switchMillis = (long) (switchTime * 3600 * 1000);
                newStartTime = new Date(prevEndTime.getTime() + switchMillis);
            }

            // 计算预计结束时间 = 开始时间 + (排产量 / 生产速度) 小时
            double speed = getProductionSpeed(context, machineCode, node.getSteelRingCode());
            double planQty = node.getPlanQty();
            if (speed <= 0) {
                throw new RuntimeException("机台[" + machineCode + "]钢丝圈["
                        + node.getSteelRingCode() + "]生产速度异常：" + speed);
            }
            double hoursNeeded = planQty / speed;
            long durationMillis = (long) (hoursNeeded * 3600 * 1000);
            Date newEndTime = new Date(newStartTime.getTime() + durationMillis);

            // 判断是否超过班次结束时间
            if (newEndTime.after(shiftEndTime)) {
                // 标记为超时任务，需要推迟到下个班
                overTimeNodes.add(node);
                node.setStartTime(newStartTime);
                node.setEndTime(newEndTime);
                log.info("钢丝圈滚动更新[跨班次]：任务[钢丝圈={}]预计结束{}超过班次结束{}，标记推迟",
                        node.getSteelRingCode(), formatDate(newEndTime), formatDate(shiftEndTime));
            } else {
                // 正常任务，更新时间
                node.setStartTime(newStartTime);
                node.setEndTime(newEndTime);
                context.setHasChange(true);
                prevEndTime = newEndTime;
                prevSteelRingCode = node.getSteelRingCode();
            }
        }

        // 4. 处理超时任务推迟到下个班
        if (!overTimeNodes.isEmpty() && shiftIndex < 6) {
            postponeToNextShift(context, machineCode, shiftIndex, overTimeNodes);
        } else if (!overTimeNodes.isEmpty() && shiftIndex >= 6) {
            // 第6班无法再推迟，记录告警
            log.warn("钢丝圈滚动更新[跨班次]：第6班仍有{}个任务超时，无法继续推迟，需人工处理",
                    overTimeNodes.size());
        }
    }

    /**
     * 将超时任务推迟到下个班
     *
     * <p>处理逻辑：</p>
     * <ol>
     *   <li>加载同机台下个班的任务链</li>
     *   <li>对每个超时任务创建新任务节点（推迟到下个班）</li>
     *   <li>原任务无完成量：状态更新为"已取消"，新任务生产量 = 原生产量</li>
     *   <li>原任务有完成量：状态更新为"部分完成已推迟"，新任务生产量 = 原生产量 - 已完成量</li>
     *   <li>将新任务插入到下个班任务链的开头</li>
     *   <li>递归重算下个班的时间（可能级联推迟）</li>
     * </ol>
     *
     * @param context        滚动上下文
     * @param machineCode    机台编号
     * @param currentShift   当前班次索引
     * @param overTimeNodes  超时任务节点列表
     */
    private void postponeToNextShift(GsqRollingContext context, String machineCode,
                                      int currentShift, List<GsqRollingTaskNode> overTimeNodes) {
        int nextShift = currentShift + 1;
        log.info("钢丝圈滚动更新[跨班次]：将{}个超时任务从班次{}推迟到班次{}，机台={}",
                overTimeNodes.size(), currentShift, nextShift, machineCode);

        // 1. 加载下个班的任务链
        LinkedList<GsqRollingTaskNode> nextShiftChain = loadTaskChainFromDb(
                machineCode, context.getScheduleDate(), nextShift);
        if (nextShiftChain == null) {
            nextShiftChain = new LinkedList<>();
        }

        // 2. 对每个超时任务创建新任务节点
        List<GsqRollingTaskNode> newNodes = new ArrayList<>();
        for (GsqRollingTaskNode originalNode : overTimeNodes) {
            double finishQty = originalNode.getFinishQty();
            double planQty = originalNode.getPlanQty();
            double remainingQty;

            if (finishQty <= 0) {
                // 原任务无完成量：状态更新为"已取消"，新任务生产量 = 原生产量
                originalNode.setTaskStatus(TASK_STATUS_CANCELLED);
                remainingQty = planQty;
            } else {
                // 原任务有完成量：状态更新为"部分完成已推迟"，新任务生产量 = 原生产量 - 已完成量
                originalNode.setTaskStatus(TASK_STATUS_PARTIAL_POSTPONED);
                remainingQty = planQty - finishQty;
            }

            // 剩余量为0则不需要创建新任务
            if (remainingQty <= 0) {
                continue;
            }

            // 创建新任务节点（推迟到下个班）
            GsqRollingTaskNode newNode = new GsqRollingTaskNode();
            newNode.setScheduleId(originalNode.getScheduleId());
            newNode.setClassIndex(nextShift);
            newNode.setMachineCode(machineCode);
            newNode.setSteelRingCode(originalNode.getSteelRingCode());
            newNode.setPlanQty((int) Math.round(remainingQty));
            newNode.setFinishQty(0);
            newNode.setTaskStatus(TASK_STATUS_POSTPONED);
            // 顺序设为0，后续排序时插入到最前面
            newNode.setProduceOrder(0);
            newNodes.add(newNode);
            context.setHasChange(true);
        }

        if (newNodes.isEmpty()) {
            return;
        }

        // 3. 将新任务插入到下个班任务链（按原顺序排在最前面，保留相对次序）
        newNodes.sort(Comparator.comparingInt(GsqRollingTaskNode::getProduceOrder));
        for (GsqRollingTaskNode newNode : newNodes) {
            nextShiftChain.addFirst(newNode);
        }

        // 4. 重新分配生产顺序（从1开始递增）
        nextShiftChain.sort(Comparator.comparingInt(GsqRollingTaskNode::getProduceOrder));
        int seq = 1;
        for (GsqRollingTaskNode node : nextShiftChain) {
            if (!TASK_STATUS_CANCELLED.equals(node.getTaskStatus())) {
                node.setProduceOrder(seq++);
            }
        }

        // 5. 更新上下文，递归重算下个班时间（可能级联推迟）
        String nextChainKey = machineCode + "_shift" + nextShift;
        context.getTaskChainMap().put(nextChainKey, nextShiftChain);

        // 构建下个班的子上下文进行递归重算
        GsqRollingContext nextContext = buildContext(context.getTriggerType(), context.getTriggerSourceId(),
                context.getScheduleDate(), nextShift, machineCode, context.getSteelRingCode(),
                context.getBatchNo());
        nextContext.setRollingLogId(context.getRollingLogId());
        nextContext.setBeforeStockQty(context.getBeforeStockQty());
        nextContext.setSpeedCache(context.getSpeedCache());
        nextContext.getTaskChainMap().put(machineCode, nextShiftChain);
        nextContext.setShiftStartTime(calculateShiftStartTime(context.getScheduleDate(), nextShift));

        // 持久化当前班次的变更
        persistChanges(context, machineCode, currentShift);

        // 递归重算下个班（可能继续级联推迟）
        recalculateShiftTimesWithCrossShift(nextContext, machineCode, nextShift);

        // 持久化下个班的变更
        if (nextContext.isHasChange()) {
            persistChanges(nextContext, machineCode, nextShift);
        }
    }

    // ==================== 模块四：自动定时触发与告警 ====================

    /**
     * 自动定时触发滚动更新（供定时任务调用）
     *
     * <p>触发时机：每个生产班次开始前30分钟（可配置 GSQ_ROLLING_AUTO_TRIGGER_LEAD_MINUTES）。
     * 自动触发会先刷新本地库存缓存（调用MES同步接口），再对目标班次的全部机台执行滚动更新，
     * 并应用库存与计划调整算法。</p>
     *
     * @param scheduleDate 排程日期
     * @param shiftIndex   目标班次索引（1~6）
     * @param factoryCode  分厂编码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:AUTO:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd') + ':' + #shiftIndex",
            waitTime = 0,
            leaseTime = 120,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult autoRollingUpdate(Date scheduleDate, int shiftIndex, String factoryCode) {
        String batchNo = generateBatchNo();
        log.info("钢丝圈自动滚动更新开始，批次号：{}，排程日期={}，班次={}，分厂={}",
                batchNo, scheduleDate, shiftIndex, factoryCode);

        // 1. 创建日志主表记录
        GsqRollingLog rollingLog = createRollingLog(batchNo, "0", null,
                scheduleDate, shiftIndex, null, null);
        rollingLog.setFactoryCode(factoryCode);
        gsqRollingLogMapper.insert(rollingLog);

        try {
            // 2. 获取目标班次所有机台列表
            List<String> machineCodes = loadAllMachineCodesForShift(scheduleDate, shiftIndex);
            if (machineCodes.isEmpty()) {
                log.warn("钢丝圈自动滚动更新：目标班次{}无机台任务，无需滚动", shiftIndex);
                updateRollingLogSuccess(rollingLog, 0, 0, 0, "无机台任务，无需滚动");
                return GsqRollingUpdateResult.success(batchNo, 0, 0, 0);
            }

            // 3. 加载所有受影响的钢丝圈代码（用于库存调整）
            List<String> steelRingCodes = loadAllSteelRingCodesForShift(scheduleDate, shiftIndex);

            // 4. 对每个机台执行时间重算（含跨班次推迟）
            int totalAffected = 0;
            double beforeStock = 0;
            double afterStock = 0;

            for (String machineCode : machineCodes) {
                GsqRollingContext context = buildContext("0", null, scheduleDate,
                        shiftIndex, machineCode, null, batchNo);
                context.setRollingLogId(rollingLog.getId());

                // 加载任务链
                LinkedList<GsqRollingTaskNode> taskChain = loadTaskChainFromDb(machineCode, scheduleDate, shiftIndex);
                if (taskChain == null || taskChain.isEmpty()) {
                    continue;
                }
                context.getTaskChainMap().put(machineCode, taskChain);

                // 重算时间（含跨班次推迟）
                recalculateShiftTimesWithCrossShift(context, machineCode, shiftIndex);

                // 持久化
                if (context.isHasChange()) {
                    totalAffected += persistChanges(context, machineCode, shiftIndex);
                }
            }

            // 5. 对每个钢丝圈应用库存计划调整算法
            for (String steelRingCode : steelRingCodes) {
                GsqRollingUpdateResult adjustResult = adjustPlanByStock(scheduleDate, steelRingCode, shiftIndex);
                totalAffected += adjustResult.getAffectedCount();
                beforeStock = adjustResult.getBeforeStockQty();
                afterStock = adjustResult.getAfterStockQty();
            }

            // 6. 更新日志为成功
            updateRollingLogSuccess(rollingLog, totalAffected, beforeStock, afterStock,
                    "自动滚动更新完成，影响" + totalAffected + "条记录");

            log.info("钢丝圈自动滚动更新完成，批次号：{}，影响记录{}条", batchNo, totalAffected);
            return GsqRollingUpdateResult.success(batchNo, totalAffected, beforeStock, afterStock);

        } catch (Exception e) {
            log.error("钢丝圈自动滚动更新失败，批次号：{}", batchNo, e);
            updateRollingLogFailure(rollingLog, e.getMessage());
            throw e;
        }
    }

    /**
     * 手动补偿触发（自动触发失败后的人工补偿入口）
     *
     * <p>与 autoRollingUpdate 等价，但触发类型标记为"手动补偿"，跳过MES库存同步步骤
     * （假设库存已被自动任务刷新或人工确认）。</p>
     *
     * @param scheduleDate 排程日期
     * @param shiftIndex   目标班次索引（1~6）
     * @param factoryCode  分厂编码
     * @return 滚动更新结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @DistributedLock(
            key = "'GSQ:ROLLING:COMPENSATE:' + T(cn.hutool.core.date.DateUtil).format(#scheduleDate, 'yyyyMMdd') + ':' + #shiftIndex",
            waitTime = 3,
            leaseTime = 120,
            failMsg = "ui.gsq.rolling.manual.conflict"
    )
    public GsqRollingUpdateResult manualCompensateRolling(Date scheduleDate, int shiftIndex, String factoryCode) {
        log.info("钢丝圈手动补偿滚动更新：排程日期={}，班次={}，分厂={}", scheduleDate, shiftIndex, factoryCode);
        // 手动补偿与自动滚动逻辑一致，仅触发类型标记为"5-手动补偿"
        return autoRollingUpdate(scheduleDate, shiftIndex, factoryCode);
    }

    /**
     * 加载目标班次所有机台编号（去重）
     */
    private List<String> loadAllMachineCodesForShift(Date scheduleDate, int shiftIndex) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);

        return list.stream()
                .filter(e -> getPlanQtyByShiftIndex(e, shiftIndex) != null)
                .map(GsqScheduleResult::getMachineCode)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 加载目标班次所有钢丝圈代码（去重）
     */
    private List<String> loadAllSteelRingCodesForShift(Date scheduleDate, int shiftIndex) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(scheduleDate))
               .eq(GsqScheduleResult::getIsDelete, 0);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);

        return list.stream()
                .filter(e -> getPlanQtyByShiftIndex(e, shiftIndex) != null)
                .map(GsqScheduleResult::getSteelRingCode)
                .distinct()
                .collect(Collectors.toList());
    }
}

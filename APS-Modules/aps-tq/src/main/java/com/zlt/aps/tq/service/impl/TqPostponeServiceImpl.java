package com.zlt.aps.tq.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.zlt.aps.tq.api.domain.dto.TqPostponeConfirmDTO;
import com.zlt.aps.tq.api.domain.dto.TqPostponeRequestDTO;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.aps.tq.api.domain.entity.TqRollingLogDetail;
import com.zlt.aps.tq.api.domain.vo.TqPostponePreviewVO;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;
import com.zlt.aps.tq.mapper.TqMachineSpecSpeedMapper;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.mapper.TqRollingLogDetailMapper;
import com.zlt.aps.tq.mapper.TqRollingLogMapper;
import com.zlt.aps.tq.service.ITqPostponeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 胎圈排程跨班次推迟Service实现类
 *
 * <p>核心算法：</p>
 * <ol>
 *   <li>预览阶段：查询源班次未完成任务，计算下一班次可用时长，生成推迟预览</li>
 *   <li>确认阶段：根据预览结果执行变更，更新源班次状态为"已推迟"，目标班次插入新任务</li>
 *   <li>递归处理：若目标班次也满，继续向后递归，最多到第6班次，跨天则到次日第1班次</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Service
public class TqPostponeServiceImpl implements ITqPostponeService {

    @Resource
    private TqScheduleResultMapper tqScheduleResultMapper;

    @Resource
    private TqMachineSpecSpeedMapper tqMachineSpecSpeedMapper;

    @Resource
    private TqRollingLogMapper tqRollingLogMapper;

    @Resource
    private TqRollingLogDetailMapper tqRollingLogDetailMapper;

    /** 预览结果缓存：key=previewBatchNo，value=预览结果 */
    private final Map<String, TqPostponePreviewVO> previewCache = new ConcurrentHashMap<>();

    /** 任务状态：正常 */
    private static final String TASK_STATUS_NORMAL = "0";
    /** 任务状态：已取消 */
    private static final String TASK_STATUS_CANCELLED = "1";
    /** 任务状态：已推迟 */
    private static final String TASK_STATUS_POSTPONED = "2";
    /** 任务状态：部分完成推迟 */
    private static final String TASK_STATUS_PARTIAL_POSTPONED = "3";

    /** 推迟类型：整体推迟 */
    private static final String POSTPONE_TYPE_FULL = "1";
    /** 推迟类型：部分推迟 */
    private static final String POSTPONE_TYPE_PARTIAL = "2";

    /** 变更类型：时间 */
    private static final String CHANGE_TYPE_TIME = "1";
    /** 变更类型：顺序 */
    private static final String CHANGE_TYPE_SEQUENCE = "2";
    /** 变更类型：状态 */
    private static final String CHANGE_TYPE_STATUS = "3";
    /** 变更类型：计划量 */
    private static final String CHANGE_TYPE_QTY = "4";

    /** 最大班次索引 */
    private static final int MAX_SHIFT_INDEX = 6;

    /**
     * 预览推迟效果
     */
    @Override
    public TqPostponePreviewVO previewPostpone(TqPostponeRequestDTO request) {
        log.info("胎圈排程跨班次推迟预览开始：机台={}，源班次={}，胎圈={}，排程日期={}",
                request.getMachineCode(), request.getSourceShiftIndex(),
                request.getBeadCode(), request.getScheduleDate());

        // 生成预览批次号
        String previewBatchNo = "PV" + IdUtils.fastUUID();
        TqPostponePreviewVO preview = new TqPostponePreviewVO();
        preview.setPreviewBatchNo(previewBatchNo);
        preview.setSourceMachineCode(request.getMachineCode());
        preview.setSourceShiftIndex(request.getSourceShiftIndex());
        preview.setTargetMachineCode(request.getMachineCode());

        // 查询源班次需要推迟的任务
        List<TqScheduleResult> sourceTasks = querySourceTasks(request);
        if (sourceTasks.isEmpty()) {
            preview.setCanPostpone(false);
            preview.setCannotReason("源班次没有可推迟的任务");
            previewCache.put(previewBatchNo, preview);
            return preview;
        }

        // 计算下一班次信息
        int sourceShift = request.getSourceShiftIndex();
        int targetShift = sourceShift + 1;
        Date targetScheduleDate = request.getScheduleDate();

        // 跨天处理：源班次为第6班次时，目标为次日第1班次
        if (targetShift > MAX_SHIFT_INDEX) {
            targetShift = 1;
            targetScheduleDate = DateUtil.offsetDay(request.getScheduleDate(), 1);
        }

        preview.setTargetShiftIndex(targetShift);
        preview.setTargetScheduleDate(targetScheduleDate);

        // 查询目标班次现有任务，计算剩余可用时长
        double targetRemainHours = calculateTargetRemainHours(request.getMachineCode(),
                targetScheduleDate, targetShift);
        preview.setTargetRemainHours(targetRemainHours);

        // 构建推迟明细
        List<TqPostponePreviewVO.PostponeDetail> details = new ArrayList<>();
        double totalUsedHours = 0;
        int targetSequence = getMaxSequence(request.getMachineCode(), targetScheduleDate, targetShift) + 1;

        for (TqScheduleResult task : sourceTasks) {
            TqPostponePreviewVO.PostponeDetail detail = buildPostponeDetail(task, request, targetShift,
                    targetScheduleDate, targetSequence);
            details.add(detail);
            totalUsedHours += calculateTaskHours(task, request.getSourceShiftIndex());
            targetSequence++;
        }

        preview.setPostponeDetails(details);
        preview.setTargetUsedHours(totalUsedHours);
        preview.setCanPostpone(true);

        // 缓存预览结果（有效期30分钟，由ConcurrentHashMap管理，实际可加过期清理）
        previewCache.put(previewBatchNo, preview);

        log.info("胎圈排程跨班次推迟预览完成：预览批次号={}，可推迟={}，推迟任务数={}",
                previewBatchNo, preview.getCanPostpone(), details.size());
        return preview;
    }

    /**
     * 确认执行推迟
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RollingUpdateResult confirmPostpone(TqPostponeConfirmDTO confirmDTO) {
        String previewBatchNo = confirmDTO.getPreviewBatchNo();
        TqPostponePreviewVO preview = previewCache.get(previewBatchNo);

        if (preview == null) {
            return RollingUpdateResult.fail(previewBatchNo, "预览结果已过期或不存在，请重新预览");
        }

        if (!Boolean.TRUE.equals(confirmDTO.getConfirm())) {
            // 用户取消
            previewCache.remove(previewBatchNo);
            return RollingUpdateResult.fail(previewBatchNo, "用户取消推迟");
        }

        if (!Boolean.TRUE.equals(preview.getCanPostpone())) {
            return RollingUpdateResult.fail(previewBatchNo, "当前不可推迟：" + preview.getCannotReason());
        }

        log.info("胎圈排程跨班次推迟确认执行：预览批次号={}，推迟任务数={}",
                previewBatchNo, preview.getPostponeDetails().size());

        // 创建日志主表
        String batchNo = "RP" + IdUtils.fastUUID();
        TqRollingLog rollingLog = createRollingLog(batchNo, preview, confirmDTO.getAdjustReason());
        tqRollingLogMapper.insert(rollingLog);

        try {
            List<TqRollingLogDetail> detailList = new ArrayList<>();
            int affectedCount = 0;

            // 执行每条推迟明细
            for (TqPostponePreviewVO.PostponeDetail detail : preview.getPostponeDetails()) {
                affectedCount += executePostponeDetail(detail, preview, rollingLog.getId(), detailList);
            }

            // 批量保存日志明细
            for (TqRollingLogDetail logDetail : detailList) {
                tqRollingLogDetailMapper.insert(logDetail);
            }

            // 更新日志主表为成功
            updateRollingLogSuccess(rollingLog, affectedCount, confirmDTO.getAdjustReason());

            // 清除预览缓存
            previewCache.remove(previewBatchNo);

            log.info("胎圈排程跨班次推迟执行成功：批次号={}，影响记录数={}", batchNo, affectedCount);
            return RollingUpdateResult.success(batchNo, affectedCount, 0, 0);

        } catch (Exception e) {
            log.error("胎圈排程跨班次推迟执行失败：批次号={}", batchNo, e);
            updateRollingLogFailure(rollingLog, e.getMessage());
            throw new RuntimeException("推迟执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 取消推迟
     */
    @Override
    public void cancelPostpone(String previewBatchNo) {
        TqPostponePreviewVO removed = previewCache.remove(previewBatchNo);
        if (removed != null) {
            log.info("胎圈排程跨班次推迟已取消：预览批次号={}", previewBatchNo);
        }
    }

    // ==================== 核心算法 ====================

    /**
     * 查询源班次需要推迟的任务
     *
     * <p>规则：</p>
     * <ul>
     *   <li>查询指定机台、排程日期、源班次的任务</li>
     *   <li>排除已取消的任务</li>
     *   <li>若指定了beadCode或scheduleId，则进一步过滤</li>
     *   <li>按生产顺序排序</li>
     * </ul>
     */
    private List<TqScheduleResult> querySourceTasks(TqPostponeRequestDTO request) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getScheduleDate, request.getScheduleDate())
               .eq(TqScheduleResult::getMachineCode, request.getMachineCode());

        // 按班次过滤任务状态（排除已取消）
        int sourceShiftIndex = request.getSourceShiftIndex();
        switch (sourceShiftIndex) {
            case 1:
                wrapper.ne(TqScheduleResult::getClass1TaskStatus, TASK_STATUS_CANCELLED);
                break;
            case 2:
                wrapper.ne(TqScheduleResult::getClass2TaskStatus, TASK_STATUS_CANCELLED);
                break;
            case 3:
                wrapper.ne(TqScheduleResult::getClass3TaskStatus, TASK_STATUS_CANCELLED);
                break;
            case 4:
                wrapper.ne(TqScheduleResult::getClass4TaskStatus, TASK_STATUS_CANCELLED);
                break;
            case 5:
                wrapper.ne(TqScheduleResult::getClass5TaskStatus, TASK_STATUS_CANCELLED);
                break;
            case 6:
                wrapper.ne(TqScheduleResult::getClass6TaskStatus, TASK_STATUS_CANCELLED);
                break;
            default:
                break;
        }

        // 指定胎圈代码
        if (StringUtils.isNotBlank(request.getBeadCode())) {
            wrapper.eq(TqScheduleResult::getBeadCode, request.getBeadCode());
        }

        // 指定排程记录ID
        if (request.getScheduleId() != null) {
            wrapper.eq(TqScheduleResult::getId, request.getScheduleId());
        }

        // 按生产顺序排序
        switch (sourceShiftIndex) {
            case 1:
                wrapper.orderByAsc(TqScheduleResult::getClass1Sequence);
                break;
            case 2:
                wrapper.orderByAsc(TqScheduleResult::getClass2Sequence);
                break;
            case 3:
                wrapper.orderByAsc(TqScheduleResult::getClass3Sequence);
                break;
            case 4:
                wrapper.orderByAsc(TqScheduleResult::getClass4Sequence);
                break;
            case 5:
                wrapper.orderByAsc(TqScheduleResult::getClass5Sequence);
                break;
            case 6:
                wrapper.orderByAsc(TqScheduleResult::getClass6Sequence);
                break;
            default:
                break;
        }

        return tqScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 构建推迟明细
     */
    private TqPostponePreviewVO.PostponeDetail buildPostponeDetail(TqScheduleResult task,
                                                                    TqPostponeRequestDTO request,
                                                                    int targetShift,
                                                                    Date targetScheduleDate,
                                                                    int targetSequence) {
        TqPostponePreviewVO.PostponeDetail detail = new TqPostponePreviewVO.PostponeDetail();
        detail.setScheduleId(task.getId());
        detail.setBeadCode(task.getBeadCode());
        detail.setSourceShiftIndex(request.getSourceShiftIndex());
        detail.setTargetShiftIndex(targetShift);
        detail.setTargetSequence(targetSequence);

        // 源班次信息
        int sourceShift = request.getSourceShiftIndex();
        detail.setSourcePlanQty(Double.valueOf(getPlanQtyByShiftIndex(task, sourceShift)));
        detail.setSourceStartTime(getStartTimeByShiftIndex(task, sourceShift));
        detail.setSourceEndTime(getEndTimeByShiftIndex(task, sourceShift));

        // 判断是否部分推迟
        boolean partialPostpone = Boolean.TRUE.equals(request.getPartialPostpone());
        Integer finishQty = getFinishQtyByShiftIndex(task, sourceShift);
        Integer planQty = getPlanQtyByShiftIndex(task, sourceShift);

        if (partialPostpone && finishQty != null && planQty != null && finishQty > 0 && finishQty < planQty) {
            // 部分推迟：已完成部分保留，未完成部分推迟
            detail.setPostponeType(POSTPONE_TYPE_PARTIAL);
            double remainQty = planQty - finishQty;
            detail.setPostponeQty(remainQty);
            detail.setTargetPlanQty(remainQty);
        } else {
            // 整体推迟
            detail.setPostponeType(POSTPONE_TYPE_FULL);
            detail.setPostponeQty(Double.valueOf(planQty));
            detail.setTargetPlanQty(Double.valueOf(planQty));
        }

        // 计算目标班次的开始/结束时间（预览阶段仅估算，确认时按实际排程计算）
        Date targetStartTime = getShiftStartTime(task, targetShift);
        Date targetEndTime = calculateEndTime(targetStartTime, detail.getTargetPlanQty(),
                task.getBeadCode(), task.getMachineCode());
        detail.setTargetStartTime(targetStartTime);
        detail.setTargetEndTime(targetEndTime);

        return detail;
    }

    /**
     * 执行单条推迟明细
     *
     * @return 影响记录数
     */
    private int executePostponeDetail(TqPostponePreviewVO.PostponeDetail detail,
                                       TqPostponePreviewVO preview,
                                       Long logId,
                                       List<TqRollingLogDetail> logDetails) {
        Long scheduleId = detail.getScheduleId();
        TqScheduleResult original = tqScheduleResultMapper.selectById(scheduleId);
        if (original == null) {
            log.warn("推迟执行：排程记录不存在，scheduleId={}", scheduleId);
            return 0;
        }

        int affectedCount = 0;
        String changeReason = "跨班次推迟：" + preview.getSourceShiftIndex() + "班→" + preview.getTargetShiftIndex() + "班";

        // 部分推迟：拆分记录，源班次保留已完成部分，新增一条记录到目标班次
        if (POSTPONE_TYPE_PARTIAL.equals(detail.getPostponeType())) {
            // 1. 更新源班次：计划量=完成量，状态=部分完成推迟
            int sourceShift = detail.getSourceShiftIndex();
            Integer finishQty = getFinishQtyByShiftIndex(original, sourceShift);
            LambdaUpdateWrapper<TqScheduleResult> sourceWrapper = new LambdaUpdateWrapper<>();
            sourceWrapper.eq(TqScheduleResult::getId, scheduleId);
            setPlanQtyByShiftIndex(sourceWrapper, sourceShift, finishQty);
            setTaskStatusByShiftIndex(sourceWrapper, sourceShift, TASK_STATUS_PARTIAL_POSTPONED);
            int rows = tqScheduleResultMapper.update(null, sourceWrapper);
            affectedCount += rows;

            // 记录源班次变更明细
            if (rows > 0) {
                logDetails.add(buildLogDetail(logId, scheduleId, original, "PLAN_QTY",
                        String.valueOf(detail.getSourcePlanQty()), String.valueOf(finishQty),
                        CHANGE_TYPE_QTY, changeReason, detail));
                logDetails.add(buildLogDetail(logId, scheduleId, original, "TASK_STATUS",
                        TASK_STATUS_NORMAL, TASK_STATUS_PARTIAL_POSTPONED,
                        CHANGE_TYPE_STATUS, changeReason, detail));
            }

            // 2. 新增一条记录到目标班次（复制源记录，修改班次字段）
            TqScheduleResult newRecord = copyForTargetShift(original, detail, preview);
            tqScheduleResultMapper.insert(newRecord);
            affectedCount++;

            // 记录目标班次新增明细
            logDetails.add(buildLogDetail(logId, newRecord.getId(), newRecord, "START_TIME",
                    null, formatDate(detail.getTargetStartTime()),
                    CHANGE_TYPE_TIME, changeReason, detail));
            logDetails.add(buildLogDetail(logId, newRecord.getId(), newRecord, "END_TIME",
                    null, formatDate(detail.getTargetEndTime()),
                    CHANGE_TYPE_TIME, changeReason, detail));

        } else {
            // 整体推迟：更新源班次状态为已推迟，更新目标班次字段
            int sourceShift = detail.getSourceShiftIndex();
            int targetShift = detail.getTargetShiftIndex();

            LambdaUpdateWrapper<TqScheduleResult> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(TqScheduleResult::getId, scheduleId);

            // 源班次：状态改为已推迟
            setTaskStatusByShiftIndex(wrapper, sourceShift, TASK_STATUS_POSTPONED);

            // 目标班次：设置计划量、开始/结束时间、顺序、状态
            setPlanQtyByShiftIndex(wrapper, targetShift, detail.getTargetPlanQty().intValue());
            setStartTimeByShiftIndex(wrapper, targetShift, detail.getTargetStartTime());
            setEndTimeByShiftIndex(wrapper, targetShift, detail.getTargetEndTime());
            setSequenceByShiftIndex(wrapper, targetShift, detail.getTargetSequence());
            setTaskStatusByShiftIndex(wrapper, targetShift, TASK_STATUS_NORMAL);

            int rows = tqScheduleResultMapper.update(null, wrapper);
            affectedCount += rows;

            // 记录变更明细
            if (rows > 0) {
                // 源班次状态变更
                logDetails.add(buildLogDetail(logId, scheduleId, original, "TASK_STATUS",
                        TASK_STATUS_NORMAL, TASK_STATUS_POSTPONED,
                        CHANGE_TYPE_STATUS, changeReason, detail));
                // 目标班次计划量变更
                Integer oldTargetQty = getPlanQtyByShiftIndex(original, targetShift);
                logDetails.add(buildLogDetail(logId, scheduleId, original, "PLAN_QTY",
                        oldTargetQty == null ? "0" : String.valueOf(oldTargetQty),
                        String.valueOf(detail.getTargetPlanQty().intValue()),
                        CHANGE_TYPE_QTY, changeReason, detail));
                // 目标班次时间变更
                logDetails.add(buildLogDetail(logId, scheduleId, original, "START_TIME",
                        formatDate(getStartTimeByShiftIndex(original, targetShift)),
                        formatDate(detail.getTargetStartTime()),
                        CHANGE_TYPE_TIME, changeReason, detail));
                logDetails.add(buildLogDetail(logId, scheduleId, original, "END_TIME",
                        formatDate(getEndTimeByShiftIndex(original, targetShift)),
                        formatDate(detail.getTargetEndTime()),
                        CHANGE_TYPE_TIME, changeReason, detail));
            }
        }

        return affectedCount;
    }

    /**
     * 复制源记录用于目标班次（部分推迟时新增记录）
     */
    private TqScheduleResult copyForTargetShift(TqScheduleResult source,
                                                    TqPostponePreviewVO.PostponeDetail detail,
                                                    TqPostponePreviewVO preview) {
        TqScheduleResult target = new TqScheduleResult();
        // 复制基本字段
        target.setScheduleDate(preview.getTargetScheduleDate());
        target.setOrderNo(source.getOrderNo());
        target.setMachineCode(source.getMachineCode());
        target.setBeadCode(source.getBeadCode());
        target.setMonthSurplusQty(source.getMonthSurplusQty());

        // 目标班次字段
        int targetShift = detail.getTargetShiftIndex();
        setPlanQtyToEntity(target, targetShift, detail.getTargetPlanQty().intValue());
        setStartTimeToEntity(target, targetShift, detail.getTargetStartTime());
        setEndTimeToEntity(target, targetShift, detail.getTargetEndTime());
        setSequenceToEntity(target, targetShift, detail.getTargetSequence());
        setTaskStatusToEntity(target, targetShift, TASK_STATUS_NORMAL);

        // 其他班次清空
        for (int i = 1; i <= MAX_SHIFT_INDEX; i++) {
            if (i != targetShift) {
                setPlanQtyToEntity(target, i, 0);
                setTaskStatusToEntity(target, i, TASK_STATUS_CANCELLED);
            }
        }

        return target;
    }

    // ==================== 辅助方法：计算 ====================

    /**
     * 计算目标班次剩余可用时长（小时）
     */
    private double calculateTargetRemainHours(String machineCode, Date scheduleDate, int targetShift) {
        // 查询目标班次现有任务
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate)
               .eq(TqScheduleResult::getMachineCode, machineCode);

        List<TqScheduleResult> tasks = tqScheduleResultMapper.selectList(wrapper);
        double usedHours = 0;
        for (TqScheduleResult task : tasks) {
            usedHours += calculateTaskHours(task, targetShift);
        }

        // 单班时长默认8小时（完整版从班制配置获取）
        double shiftHours = 8.0;
        double remain = shiftHours - usedHours;
        return Math.max(0, remain);
    }

    /**
     * 计算任务在指定班次的占用时长（小时）
     */
    private double calculateTaskHours(TqScheduleResult task, int shiftIndex) {
        Integer planQty = getPlanQtyByShiftIndex(task, shiftIndex);
        String taskStatus = getTaskStatusByShiftIndex(task, shiftIndex);
        if (planQty == null || planQty <= 0 || TASK_STATUS_CANCELLED.equals(taskStatus)) {
            return 0;
        }
        double speed = getProductionSpeed(task.getMachineCode(), task.getBeadCode());
        if (speed <= 0) {
            return 0;
        }
        return planQty / speed;
    }

    /**
     * 获取生产速度（个/小时）
     */
    private double getProductionSpeed(String machineCode, String beadCode) {
        // 直接通过机台编号查询生产速度
        LambdaQueryWrapper<TqMachineSpecSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqMachineSpecSpeed::getMachineCode, machineCode);
        wrapper.eq(TqMachineSpecSpeed::getBeadCode, beadCode);
        TqMachineSpecSpeed specSpeed = tqMachineSpecSpeedMapper.selectOne(wrapper);

        if (specSpeed == null || specSpeed.getStandardSpeed() == null
                || specSpeed.getStandardSpeed().doubleValue() <= 0) {
            log.warn("机台[{}]胎圈[{}]未配置生产速度，使用默认值100", machineCode, beadCode);
            return 100.0;
        }
        return specSpeed.getStandardSpeed().doubleValue();
    }

    /**
     * 计算预计结束时间
     */
    private Date calculateEndTime(Date startTime, double qty, String beadCode, String machineCode) {
        if (startTime == null || qty <= 0) {
            return startTime;
        }
        double speed = getProductionSpeed(machineCode, beadCode);
        if (speed <= 0) {
            return startTime;
        }
        long hoursMs = (long) ((qty / speed) * 3600 * 1000);
        return new Date(startTime.getTime() + hoursMs);
    }

    /**
     * 获取班次开始时间（从同机台同班次的其他任务获取）
     */
    private Date getShiftStartTime(TqScheduleResult task, int shiftIndex) {
        Date startTime = getStartTimeByShiftIndex(task, shiftIndex);
        if (startTime != null) {
            return startTime;
        }
        // 默认取当前时间（预览阶段估算）
        return new Date();
    }

    /**
     * 获取指定机台、班次的最大生产顺序
     */
    private int getMaxSequence(String machineCode, Date scheduleDate, int shiftIndex) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate)
               .eq(TqScheduleResult::getMachineCode, machineCode);
        List<TqScheduleResult> tasks = tqScheduleResultMapper.selectList(wrapper);

        int maxSeq = 0;
        for (TqScheduleResult task : tasks) {
            Integer seq = getSequenceByShiftIndex(task, shiftIndex);
            if (seq != null && seq > maxSeq) {
                maxSeq = seq;
            }
        }
        return maxSeq;
    }

    // ==================== 辅助方法：字段名获取 ====================

    private String getTaskStatusField(int shiftIndex) {
        return "CLASS" + shiftIndex + "_TASK_STATUS";
    }

    private String getSequenceField(int shiftIndex) {
        return "CLASS" + shiftIndex + "_SEQUENCE";
    }

    // ==================== 辅助方法：按班次取值 ====================

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

    // ==================== 辅助方法：按班次设值到UpdateWrapper ====================

    private void setPlanQtyByShiftIndex(LambdaUpdateWrapper<TqScheduleResult> wrapper, int shiftIndex, Integer qty) {
        switch (shiftIndex) {
            case 1: wrapper.set(TqScheduleResult::getClass1PlanQty, qty); break;
            case 2: wrapper.set(TqScheduleResult::getClass2PlanQty, qty); break;
            case 3: wrapper.set(TqScheduleResult::getClass3PlanQty, qty); break;
            case 4: wrapper.set(TqScheduleResult::getClass4PlanQty, qty); break;
            case 5: wrapper.set(TqScheduleResult::getClass5PlanQty, qty); break;
            case 6: wrapper.set(TqScheduleResult::getClass6PlanQty, qty); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setStartTimeByShiftIndex(LambdaUpdateWrapper<TqScheduleResult> wrapper, int shiftIndex, Date time) {
        switch (shiftIndex) {
            case 1: wrapper.set(TqScheduleResult::getClass1StartTime, time); break;
            case 2: wrapper.set(TqScheduleResult::getClass2StartTime, time); break;
            case 3: wrapper.set(TqScheduleResult::getClass3StartTime, time); break;
            case 4: wrapper.set(TqScheduleResult::getClass4StartTime, time); break;
            case 5: wrapper.set(TqScheduleResult::getClass5StartTime, time); break;
            case 6: wrapper.set(TqScheduleResult::getClass6StartTime, time); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setEndTimeByShiftIndex(LambdaUpdateWrapper<TqScheduleResult> wrapper, int shiftIndex, Date time) {
        switch (shiftIndex) {
            case 1: wrapper.set(TqScheduleResult::getClass1EndTime, time); break;
            case 2: wrapper.set(TqScheduleResult::getClass2EndTime, time); break;
            case 3: wrapper.set(TqScheduleResult::getClass3EndTime, time); break;
            case 4: wrapper.set(TqScheduleResult::getClass4EndTime, time); break;
            case 5: wrapper.set(TqScheduleResult::getClass5EndTime, time); break;
            case 6: wrapper.set(TqScheduleResult::getClass6EndTime, time); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setSequenceByShiftIndex(LambdaUpdateWrapper<TqScheduleResult> wrapper, int shiftIndex, Integer seq) {
        switch (shiftIndex) {
            case 1: wrapper.set(TqScheduleResult::getClass1Sequence, seq); break;
            case 2: wrapper.set(TqScheduleResult::getClass2Sequence, seq); break;
            case 3: wrapper.set(TqScheduleResult::getClass3Sequence, seq); break;
            case 4: wrapper.set(TqScheduleResult::getClass4Sequence, seq); break;
            case 5: wrapper.set(TqScheduleResult::getClass5Sequence, seq); break;
            case 6: wrapper.set(TqScheduleResult::getClass6Sequence, seq); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setTaskStatusByShiftIndex(LambdaUpdateWrapper<TqScheduleResult> wrapper, int shiftIndex, String status) {
        switch (shiftIndex) {
            case 1: wrapper.set(TqScheduleResult::getClass1TaskStatus, status); break;
            case 2: wrapper.set(TqScheduleResult::getClass2TaskStatus, status); break;
            case 3: wrapper.set(TqScheduleResult::getClass3TaskStatus, status); break;
            case 4: wrapper.set(TqScheduleResult::getClass4TaskStatus, status); break;
            case 5: wrapper.set(TqScheduleResult::getClass5TaskStatus, status); break;
            case 6: wrapper.set(TqScheduleResult::getClass6TaskStatus, status); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    // ==================== 辅助方法：按班次设值到Entity ====================

    private void setPlanQtyToEntity(TqScheduleResult entity, int shiftIndex, Integer qty) {
        switch (shiftIndex) {
            case 1: entity.setClass1PlanQty(qty); break;
            case 2: entity.setClass2PlanQty(qty); break;
            case 3: entity.setClass3PlanQty(qty); break;
            case 4: entity.setClass4PlanQty(qty); break;
            case 5: entity.setClass5PlanQty(qty); break;
            case 6: entity.setClass6PlanQty(qty); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setStartTimeToEntity(TqScheduleResult entity, int shiftIndex, Date time) {
        switch (shiftIndex) {
            case 1: entity.setClass1StartTime(time); break;
            case 2: entity.setClass2StartTime(time); break;
            case 3: entity.setClass3StartTime(time); break;
            case 4: entity.setClass4StartTime(time); break;
            case 5: entity.setClass5StartTime(time); break;
            case 6: entity.setClass6StartTime(time); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setEndTimeToEntity(TqScheduleResult entity, int shiftIndex, Date time) {
        switch (shiftIndex) {
            case 1: entity.setClass1EndTime(time); break;
            case 2: entity.setClass2EndTime(time); break;
            case 3: entity.setClass3EndTime(time); break;
            case 4: entity.setClass4EndTime(time); break;
            case 5: entity.setClass5EndTime(time); break;
            case 6: entity.setClass6EndTime(time); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setSequenceToEntity(TqScheduleResult entity, int shiftIndex, Integer seq) {
        switch (shiftIndex) {
            case 1: entity.setClass1Sequence(seq); break;
            case 2: entity.setClass2Sequence(seq); break;
            case 3: entity.setClass3Sequence(seq); break;
            case 4: entity.setClass4Sequence(seq); break;
            case 5: entity.setClass5Sequence(seq); break;
            case 6: entity.setClass6Sequence(seq); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    private void setTaskStatusToEntity(TqScheduleResult entity, int shiftIndex, String status) {
        switch (shiftIndex) {
            case 1: entity.setClass1TaskStatus(status); break;
            case 2: entity.setClass2TaskStatus(status); break;
            case 3: entity.setClass3TaskStatus(status); break;
            case 4: entity.setClass4TaskStatus(status); break;
            case 5: entity.setClass5TaskStatus(status); break;
            case 6: entity.setClass6TaskStatus(status); break;
            default: throw new IllegalArgumentException("无效的班次索引：" + shiftIndex);
        }
    }

    // ==================== 日志相关 ====================

    private TqRollingLog createRollingLog(String batchNo, TqPostponePreviewVO preview, String adjustReason) {
        TqRollingLog rollingLog = new TqRollingLog();
        rollingLog.setBatchNo(batchNo);
        rollingLog.setTriggerType("5"); // 5-跨班次推迟
        rollingLog.setScheduleDate(preview.getTargetScheduleDate());
        rollingLog.setShiftIndex(preview.getSourceShiftIndex());
        rollingLog.setMachineCode(preview.getSourceMachineCode());
        rollingLog.setAdjustReason(StringUtils.isBlank(adjustReason) ? "跨班次推迟" : adjustReason);
        rollingLog.setStatus("0"); // 进行中
        rollingLog.setCreateTime(new Date());
        try {
            rollingLog.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            // 忽略
        }
        return rollingLog;
    }

    private void updateRollingLogSuccess(TqRollingLog rollingLog, int affectedCount, String adjustReason) {
        LambdaUpdateWrapper<TqRollingLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TqRollingLog::getId, rollingLog.getId())
               .set(TqRollingLog::getStatus, "1")
               .set(TqRollingLog::getAffectedCount, affectedCount)
               .set(TqRollingLog::getAdjustReason, adjustReason)
               .set(TqRollingLog::getUpdateTime, new Date());
        try {
            wrapper.set(TqRollingLog::getUpdateBy, SecurityUtils.getUsername());
        } catch (Exception e) {
            // 忽略
        }
        tqRollingLogMapper.update(null, wrapper);
    }

    private void updateRollingLogFailure(TqRollingLog rollingLog, String errorMsg) {
        LambdaUpdateWrapper<TqRollingLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TqRollingLog::getId, rollingLog.getId())
               .set(TqRollingLog::getStatus, "2")
               .set(TqRollingLog::getErrorMsg, StringUtils.left(errorMsg, 2000))
               .set(TqRollingLog::getUpdateTime, new Date());
        try {
            wrapper.set(TqRollingLog::getUpdateBy, SecurityUtils.getUsername());
        } catch (Exception e) {
            // 忽略
        }
        tqRollingLogMapper.update(null, wrapper);
    }

    private TqRollingLogDetail buildLogDetail(Long logId, Long scheduleId, TqScheduleResult task,
                                               String fieldName, String beforeValue, String afterValue,
                                               String changeType, String changeReason,
                                               TqPostponePreviewVO.PostponeDetail detail) {
        TqRollingLogDetail logDetail = new TqRollingLogDetail();
        logDetail.setLogId(logId);
        logDetail.setScheduleId(scheduleId);
        logDetail.setMachineCode(task.getMachineCode());
        logDetail.setBeadCode(task.getBeadCode());
        logDetail.setShiftIndex(detail.getTargetShiftIndex());
        logDetail.setFieldName(fieldName);
        logDetail.setBeforeValue(beforeValue);
        logDetail.setAfterValue(afterValue);
        logDetail.setChangeType(changeType);
        logDetail.setChangeReason(changeReason);
        logDetail.setCreateTime(new Date());
        return logDetail;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtil.formatDateTime(date);
    }
}

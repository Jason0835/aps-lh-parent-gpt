package com.zlt.aps.dj.service.impl;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.api.domain.entity.DjShiftConfig;
import com.zlt.aps.dj.api.domain.entity.DjSpecifyMachine;
import com.zlt.aps.dj.api.domain.entity.DjStock;
import com.zlt.aps.dj.engine.constant.DjEngineConstants;
import com.zlt.aps.dj.engine.mapper.DjEngineConstructionInfoMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineStockMapper;
import com.zlt.aps.dj.engine.model.CapacityValidateResult;
import com.zlt.aps.dj.engine.model.ShiftContext;
import com.zlt.aps.dj.engine.model.ShiftValidateResult;
import com.zlt.aps.dj.engine.service.IDjOrderGeneratorService;
import com.zlt.aps.dj.engine.service.IDjScheduleShiftEngineService;
import com.zlt.aps.dj.mapper.DjDayFinishQtyMapper;
import com.zlt.aps.dj.mapper.DjScheduleResultMapper;
import com.zlt.aps.dj.mapper.DjSpecifyMachineMapper;
import com.zlt.aps.dj.model.DjAdjustScheduleContext;
import com.zlt.aps.dj.model.ScheduleDateGroup;
import com.zlt.aps.dj.service.DjDispatcherLogService;
import com.zlt.aps.dj.service.DjMachineInfoService;
import com.zlt.aps.dj.service.DjScheduleResultService;
import com.zlt.aps.dj.service.IDjScheduleAdjustService;
import com.zlt.aps.dj.service.IDjShiftConfigService;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.core.dao.basedao.BaseDao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 垫胶排程调整引擎
 * <p>
 * 实现设计文档「垫胶排程调整算法设计.md」中所有调整操作的计算步骤。
 * 包括：插单(2.)、调整(3.)、删除(4.)、发布(5.)、导入(6.)。
 * </p>
 *
 * @author zlt
 */
@Component
@Slf4j
public class DjScheduleAdjustServiceImpl implements IDjScheduleAdjustService {

    @Resource
    private DjScheduleResultMapper djScheduleResultMapper;

    @Resource
    private DjScheduleResultService djScheduleResultService;

    @Resource
    private DjMachineInfoService djMachineInfoService;

    @Resource
    private DjDispatcherLogService djDispatcherLogService;

    @Resource
    private DjSpecifyMachineMapper djSpecifyMachineMapper;

    @Resource
    private DjDayFinishQtyMapper djDayFinishQtyMapper;

    @Resource
    private IDjScheduleShiftEngineService iDjScheduleShiftEngineService;

    @Resource
    private BaseDao baseDao;

    @Resource
    private DjEngineConstructionInfoMapper djEngineConstructionInfoMapper;

    @Resource
    private DjEngineStockMapper djEngineStockMapper;

    @Resource
    private IDjOrderGeneratorService iDjOrderGeneratorService;

    @Resource
    private IDjShiftConfigService djShiftConfigService;

    // ==================== 1. 公共数据预加载 ====================

    /**
     * 1.1~1.4 加载基础数据
     */
    private DjAdjustScheduleContext loadBaseData(String factoryCode, Date scheduleDate) {
        DjAdjustScheduleContext ctx = new DjAdjustScheduleContext();
        ctx.setFactoryCode(factoryCode);
        ctx.setScheduleDate(scheduleDate);

        // 1.1 加载机台数据
        DjMachineInfo machineQuery = new DjMachineInfo();
        machineQuery.setFactoryCode(factoryCode);
        List<DjMachineInfo> machineList = djMachineInfoService.selectMachineInfoList(machineQuery);
        ctx.setMachineList(machineList);
        ctx.setMachineMap(
                machineList.stream().collect(Collectors.toMap(DjMachineInfo::getMachineCode, m -> m, (a, b) -> a)));

        // 1.2 加载排程结果数据
        List<DjScheduleResult> scheduleResults = djScheduleResultMapper
                .selectList(new LambdaQueryWrapper<DjScheduleResult>().eq(DjScheduleResult::getFactoryCode, factoryCode)
                        .eq(DjScheduleResult::getScheduleDate, scheduleDate));
        ctx.setScheduleResults(scheduleResults != null ? scheduleResults : new ArrayList<>());

        // 1.3 加载定点机台数据
        List<DjSpecifyMachine> specifyMachines = djSpecifyMachineMapper.selectList(
                new LambdaQueryWrapper<DjSpecifyMachine>().eq(DjSpecifyMachine::getFactoryCode, factoryCode));
        ctx.setSpecifyMachines(specifyMachines != null ? specifyMachines : new ArrayList<>());

        // 1.4 加载发布记录（通过已有的 Service 方法校验）
        ctx.setPublishRecordCount(djScheduleResultService.isPublish(scheduleDate) ? 1 : 0);

        return ctx;
    }

    /**
     * 1.5 校验排产日是否已被锁定
     */
    private AjaxResult checkScheduleLocked(Date scheduleDate, Long[] ids) {
        int lockedCount;
        if (ids != null && ids.length > 0) {
            lockedCount = djScheduleResultService.isReleasingOrTimeoutByIds(ids);
        } else {
            lockedCount = djScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        }
        if (lockedCount > 0) {
            log.info("排产日已被锁定，lockedCount={}", lockedCount);
            return AjaxResult
                    .error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        return null;
    }

    /**
     * 2.1.2 排程计划存在性校验
     * <p>
     * 使用计算后的实际排产日期，校验目标机台在该排产日是否已有排程记录。
     * 若没有任何排程记录（排程计划尚未生成），返回 {@code AjaxResult.success("SCHEDULE_NOT_EXIST")}，
     * 前端弹窗提示用户确认后调用 {@link #confirmInsertOrder} 继续执行。
     * </p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 计算后的实际排产日期
     * @param machineCode  目标机台编码
     * @return 若排程计划不存在返回 {@code AjaxResult.success("SCHEDULE_NOT_EXIST")}，否则返回 null
     */
    private AjaxResult checkScheduleExists(String factoryCode, Date scheduleDate, String machineCode) {
        List<DjScheduleResult> exists = djScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>()
                        .eq(DjScheduleResult::getFactoryCode, factoryCode)
                        .eq(DjScheduleResult::getScheduleDate, scheduleDate)
                        .eq(DjScheduleResult::getMachineCode, machineCode));
        if (CollectionUtils.isEmpty(exists)) {
            log.info("排程计划存在性校验不通过：factoryCode={}, scheduleDate={}, machineCode={}",
                    factoryCode, scheduleDate, machineCode);
            return AjaxResult.success("SCHEDULE_NOT_EXIST");
        }
        return null;
    }

    // ==================== 2. 插单 ====================

    /**
     * 插单 — 整体入口
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult insertOrder(DjScheduleResult insertVO) {
        String factoryCode = insertVO.getFactoryCode();
        String machineCode = insertVO.getMachineCode();

        // 2.1.1 计算排产日期分组（支持跨排产日拆分多笔记录）
        List<ScheduleDateGroup> dateGroups = this.calculateInsertScheduleDateGroups(insertVO);
        if (dateGroups.isEmpty()) {
            log.warn("插单排产日期计算为空");
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.error"));
        }

        // 取第一组排产日期为主日，用于校验和顺延
        ScheduleDateGroup firstGroup = dateGroups.get(0);
        Date scheduleDate = firstGroup.getScheduleDate();
        insertVO.setScheduleDate(scheduleDate);

        // 1. 公共数据预加载
        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 1.5 校验排产日是否被锁定
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, null);
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 2.1 入参校验
        AjaxResult paramCheck = validateInsertParams(insertVO, ctx);
        if (paramCheck != null) {
            return paramCheck;
        }

        // 确定目标班次和顺位（基于第一组）
        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);

        // 2.2 约束一校验 — 生产顺位合法性
        ShiftValidateResult shiftResult = iDjScheduleShiftEngineService.validateInsertConstraint(factoryCode,
                scheduleDate, machineCode, targetClass, targetSeq);
        if (!shiftResult.isPassed()) {
            log.info("插单约束校验不通过：{}", shiftResult.getErrorMsg());
            return AjaxResult.error(shiftResult.getErrorMsg());
        }

        // 2.3 约束二校验 — 产能校验（三档判断）
        BigDecimal insertPlanQty = getPlanQtyByClass(insertVO, targetClass);
        CapacityValidateResult capacityResult = iDjScheduleShiftEngineService.validateCapacity(machineCode, targetClass,
                targetSeq, insertPlanQty, ctx.getScheduleResults(), factoryCode, scheduleDate);

        // 第一档：插单量 ≤ 剩余产能（定额 - 当班原有计划量），无产能问题直接执行插单
        if (capacityResult.isWithinQuota()) {
            return executeInsertInternalWithGroups(insertVO, targetClass, targetSeq, ctx, dateGroups);
        }

        // 第三档：插单量 > 实际剩余产能（定额 - 已生产量），超当班剩余产能，拒绝插单
        if (!capacityResult.isPassed()) {
            log.warn("插单量 {} 超出实际剩余产能 {}", insertPlanQty, capacityResult.getRemainingCapacity());
            return AjaxResult.error(capacityResult.getErrorMsg());
        }

        // 第二档在 insertOrderValidate 中已处理，用户确认后直接执行
        return executeInsertInternalWithGroups(insertVO, targetClass, targetSeq, ctx, dateGroups);
    }

    /**
     * 插单前置校验（含跨天日期计算及产能校验）
     * <p>
     * 根据 {@code scheduleShiftClass} 计算实际排产日期，然后依次执行：
     * 排产日锁定校验 → 排程计划存在性校验 → 约束一（生产顺位）校验 → 约束二（产能）校验。
     * </p>
     * <p>
     * 约束二产能校验分三档：
     * <ul>
     *   <li>第一档（withinQuota=true）：无产能问题，返回成功</li>
     *   <li>第二档（withinQuota=false, passed=true）：超出定额但 ≤ 实际剩余产能，
     *       返回 {@code AjaxResult.success("CAPACITY_OVERFLOW:" + 提示信息)}，前端弹窗让用户确认</li>
     *   <li>第三档（passed=false）：超出实际剩余产能，拒绝插单</li>
     * </ul>
     * </p>
     */
    @Override
    public AjaxResult insertOrderValidate(DjScheduleResult insertVO) {
        // 2.1.1 计算排产日期分组，取第一组排产日用于校验
        List<ScheduleDateGroup> dateGroups = this.calculateInsertScheduleDateGroups(insertVO);
        if (dateGroups.isEmpty()) {
            log.warn("插单排产日期计算为空");
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.error"));
        }

        ScheduleDateGroup firstGroup = dateGroups.get(0);
        Date scheduleDate = firstGroup.getScheduleDate();
        insertVO.setScheduleDate(scheduleDate);

        String factoryCode = insertVO.getFactoryCode();
        String machineCode = insertVO.getMachineCode();

        // 排产日锁定校验
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, null);
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 2.1.2 排程计划存在性校验：使用计算后的第一个排产日
        AjaxResult scheduleExistCheck = this.checkScheduleExists(factoryCode, scheduleDate, machineCode);
        if (scheduleExistCheck != null) {
            return scheduleExistCheck;
        }

        // 1. 加载完整上下文（用于后续约束校验）
        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 2.1 入参校验
        AjaxResult paramCheck = validateInsertParams(insertVO, ctx);
        if (paramCheck != null) {
            return paramCheck;
        }

        // 确定目标班次和顺位
        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);

        // 2.2 约束一校验 — 生产顺位合法性
        ShiftValidateResult shiftResult = iDjScheduleShiftEngineService.validateInsertConstraint(factoryCode,
                scheduleDate, machineCode, targetClass, targetSeq);
        if (!shiftResult.isPassed()) {
            return AjaxResult.error(shiftResult.getErrorMsg());
        }

        // 2.3 约束二校验 — 产能校验（三档判断）
        BigDecimal insertPlanQty = getPlanQtyByClass(insertVO, targetClass);
        CapacityValidateResult capacityResult = iDjScheduleShiftEngineService.validateCapacity(machineCode, targetClass,
                targetSeq, insertPlanQty, ctx.getScheduleResults(), factoryCode, scheduleDate);

        return this.handleCapacityResult(capacityResult, insertPlanQty);
    }

    /**
     * 确认插单（用户在前端弹窗点击"坚持执行"后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult confirmInsertOrder(DjScheduleResult insertVO) {
        String factoryCode = insertVO.getFactoryCode();
        String machineCode = insertVO.getMachineCode();

        // 计算排产日期分组
        List<ScheduleDateGroup> dateGroups = this.calculateInsertScheduleDateGroups(insertVO);
        if (dateGroups.isEmpty()) {
            log.warn("插单排产日期计算为空");
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.error"));
        }

        ScheduleDateGroup firstGroup = dateGroups.get(0);
        Date scheduleDate = firstGroup.getScheduleDate();
        insertVO.setScheduleDate(scheduleDate);

        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, null);
        if (lockedCheck != null) {
            return lockedCheck;
        }

        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);

        return executeInsertInternalWithGroups(insertVO, targetClass, targetSeq, ctx, dateGroups);
    }

    /**
     * 插单内部执行（支持多排产日分组）
     * <p>
     * 第一组执行顺延处理，后续组直接插入新记录。
     * </p>
     */
    private AjaxResult executeInsertInternalWithGroups(DjScheduleResult insertVO, int targetClass, int targetSeq,
            DjAdjustScheduleContext ctx, List<ScheduleDateGroup> dateGroups) {
        String factoryCode = ctx.getFactoryCode();
        Date scheduleDate = ctx.getScheduleDate();
        String machineCode = insertVO.getMachineCode();

        // 获取插单规格名称
        String specName = insertVO.getPaddingName();
        if (StringUtils.isBlank(specName)) {
            specName = insertVO.getPaddingCode();
        }

        // ====== 第一组：按现有逻辑执行顺延 ======
        ScheduleDateGroup firstGroup = dateGroups.get(0);

        // 2.4.1：生成工单号（基于第一组的排产日）
        String batchNoFromExisting = "";
        for (DjScheduleResult r : ctx.getScheduleResults()) {
            if (StringUtils.isNotBlank(r.getBatchNo())) {
                batchNoFromExisting = r.getBatchNo();
                break;
            }
        }
        int maxOrderSeq = 0;
        for (DjScheduleResult r : ctx.getScheduleResults()) {
            if (r.getOrderNo() != null && r.getOrderNo().contains("-")) {
                String seqPart = r.getOrderNo().substring(r.getOrderNo().lastIndexOf("-") + 1);
                try {
                    int seq = Integer.parseInt(seqPart);
                    if (seq > maxOrderSeq) {
                        maxOrderSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String orderNo = iDjOrderGeneratorService.generateOrderNo(batchNoFromExisting, maxOrderSeq);
        insertVO.setOrderNo(orderNo);
        insertVO.setBatchNo(batchNoFromExisting);
        insertVO.setDataSource(DjEngineConstants.DATA_SOURCE_INSERT);
        insertVO.setReleaseStatus(ApsConstant.NO_RELEASE);
        insertVO.setPublishSuccessCount(0);
        insertVO.setFactoryCode(factoryCode);
        insertVO.setScheduleDate(scheduleDate);

        // 加载施工表数据
        MdmConstructionInfo construction = loadConstructionByPadding(factoryCode, insertVO.getPaddingCode());
        if (construction != null) {
            if (StringUtils.isBlank(insertVO.getPaddingName())) {
                insertVO.setPaddingName(construction.getPaddingName());
            }
            if (StringUtils.isBlank(insertVO.getGlueCode())) {
                insertVO.setGlueCode(construction.getPaddingRubber());
            }
        }

        // 加载 T-1 日库存数据
        BigDecimal stockQty = loadPaddingStock(factoryCode, scheduleDate, insertVO.getPaddingCode());
        insertVO.setStockQty(BigDecimalUtils.valueOf(stockQty));

        // 开产班次取当前排产日其余记录的值
        for (DjScheduleResult r : ctx.getScheduleResults()) {
            if (StringUtils.isNotBlank(r.getScheduleShiftClass())) {
                insertVO.setScheduleShiftClass(r.getScheduleShiftClass());
                break;
            }
        }
        insertVO.setTailFlag("0");

        // 获取当前排程结果（深拷贝）
        List<DjScheduleResult> currentResults = new ArrayList<>(ctx.getScheduleResults());

        // 2.4.2+2.4.3+2.4.4：对第一组的目标班次执行顺延
        ShiftContext shiftCtx = new ShiftContext().setFactoryCode(factoryCode).setScheduleDate(scheduleDate)
                .setMachineCode(machineCode).setTargetClass(targetClass).setTargetSeq(targetSeq)
                .setInsertSpecName(specName).setInsertPlanQty(getPlanQtyByClass(insertVO, targetClass))
                .setScheduleResults(currentResults).setOperType("insert");

        List<DjScheduleResult> updatedResults = iDjScheduleShiftEngineService.processInsertAndCascade(shiftCtx);

        // 根据positionClassMap清除第一组不属本组的class位置数据（只保留该组映射到的输出class）
        Map<Integer, Integer> firstGroupClassMap = firstGroup.getPositionClassMap();
        if (firstGroupClassMap != null && !firstGroupClassMap.isEmpty()) {
            Set<Integer> mappedClasses = new HashSet<>(firstGroupClassMap.values());
            for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
                if (!mappedClasses.contains(c)) {
                    setPlanQtyByClass(insertVO, c, null);
                    setSeqByClass(insertVO, c, null);
                }
            }
        } else {
            // 没有positionClassMap时，只保留positions中对应的class位置（兼容旧逻辑）
            for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
                if (!firstGroup.getPositions().contains(c)) {
                    setPlanQtyByClass(insertVO, c, null);
                    setSeqByClass(insertVO, c, null);
                }
            }
        }

        // 保存第一组的新插单记录
        djScheduleResultMapper.insert(insertVO);

        // 更新被顺延的记录
        for (DjScheduleResult updated : updatedResults) {
            if (updated.getId() != null) {
                djScheduleResultMapper.updateById(updated);
            }
        }

        // ====== 后续组：直接插入新记录（无需顺延） ======
        if (dateGroups.size() > 1) {
            for (int i = 1; i < dateGroups.size(); i++) {
                ScheduleDateGroup group = dateGroups.get(i);
                DjScheduleResult groupRecord = new DjScheduleResult();
                BeanUtils.copyProperties(insertVO, groupRecord);
                groupRecord.setId(null); // 新记录
                groupRecord.setScheduleDate(group.getScheduleDate());
                groupRecord.setScheduleShiftClass(group.getScheduleShiftClass());

                // 为后续组单独生成工单号（基于该排产日已有记录）
                // 仅查询排产日已有排程结果获取批次号和最大工单流水号，避免完整加载机台/定点机台等数据
                List<DjScheduleResult> groupResults = djScheduleResultMapper
                        .selectList(new LambdaQueryWrapper<DjScheduleResult>()
                                .eq(DjScheduleResult::getFactoryCode, factoryCode)
                                .eq(DjScheduleResult::getScheduleDate, group.getScheduleDate()));
                String groupBatchNo = "";
                for (DjScheduleResult r : groupResults) {
                    if (StringUtils.isNotBlank(r.getBatchNo())) {
                        groupBatchNo = r.getBatchNo();
                        break;
                    }
                }
                int groupMaxOrderSeq = 0;
                for (DjScheduleResult r : groupResults) {
                    if (r.getOrderNo() != null && r.getOrderNo().contains("-")) {
                        String seqPart = r.getOrderNo().substring(r.getOrderNo().lastIndexOf("-") + 1);
                        try {
                            int seq = Integer.parseInt(seqPart);
                            if (seq > groupMaxOrderSeq) {
                                groupMaxOrderSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                groupRecord.setBatchNo(groupBatchNo);
                groupRecord.setOrderNo(iDjOrderGeneratorService.generateOrderNo(groupBatchNo, groupMaxOrderSeq));

                // 先清除所有class位置，只保留该组映射到的class
                for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
                    setPlanQtyByClass(groupRecord, c, null);
                    setSeqByClass(groupRecord, c, null);
                }

                // 根据positionClassMap填充该组班次的计划量和顺位
                Map<Integer, Integer> groupClassMap = group.getPositionClassMap();
                for (int origPos : group.getPositions()) {
                    Integer outputClass = groupClassMap != null ? groupClassMap.get(origPos) : null;
                    if (outputClass == null) {
                        outputClass = origPos; // 无映射时直接使用原位置（兼容旧逻辑）
                    }
                    BigDecimal qty = getPlanQtyByClass(insertVO, origPos);
                    Integer seq = getSeqByClass(insertVO, origPos);
                    if (qty != null) {
                        setPlanQtyByClass(groupRecord, outputClass, qty);
                    }
                    if (seq != null) {
                        setSeqByClass(groupRecord, outputClass, seq);
                    }
                }

                djScheduleResultMapper.insert(groupRecord);
            }
        }

        // 记录操作日志（基于第一组）
        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, insertVO, ctx.getScheduleResults(),
                insertVO);

        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    // ==================== 3. 调整 ====================

    /**
     * 3.3 调量（统一入口）
     * 通过比较前端传值与原数据库值的差异自动判断增量或减量，再调用对应内部方法处理。
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult changeQty(DjScheduleResult adjustVO) {
        Date scheduleDate = adjustVO.getScheduleDate();
        String factoryCode = adjustVO.getFactoryCode();
        String machineCode = adjustVO.getMachineCode();

        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 校验发布状态
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, new Long[] { adjustVO.getId() });
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 从已加载的排程结果中获取原始记录（确保与 ctx.getScheduleResults() 中为同一对象引用，
        // 避免后续 processInsertAndCascade 使用不同的实例导致数据不一致）
        DjScheduleResult original = ctx.getScheduleResults().stream()
                .filter(r -> r.getId().equals(adjustVO.getId()))
                .findFirst().orElse(null);
        if (original == null) {
            log.info("调量记录未找到（不在当前排程结果中），id={}", adjustVO.getId());
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.not.found"));
        }

        // 确定目标班次，获取新旧计划量
        int targetClass = resolveTargetClass(adjustVO);
        BigDecimal oldPlanQty = iDjScheduleShiftEngineService.getPlanQtyByIndex(original, targetClass);
        BigDecimal newPlanQty = getPlanQtyByClass(adjustVO, targetClass);

        // 新计划量为空或≤0 → 清空该班次
        if (newPlanQty == null || newPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            return this.internalChangeQtyDecrease(original, targetClass, BigDecimal.ZERO);
        }

        // 原班次无计划量 → 走插单逻辑
        if (oldPlanQty == null || oldPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            DjScheduleResult insertVO = new DjScheduleResult();
            BeanUtils.copyProperties(adjustVO, insertVO);
            insertVO.setPaddingCode(original.getPaddingCode());
            insertVO.setPaddingName(original.getPaddingName());
            insertVO.setMachineCode(original.getMachineCode());
            return this.insertOrder(insertVO);
        }

        // 比较新旧值，判断增量/减量
        int cmp = newPlanQty.compareTo(oldPlanQty);
        if (cmp > 0) {
            // 增量：计算差值后调用内部增量方法
            BigDecimal delta = newPlanQty.subtract(oldPlanQty);
            return this.internalChangeQtyIncrease(original, targetClass, delta, ctx);
        } else if (cmp < 0) {
            // 减量：直接传入新总量
            return this.internalChangeQtyDecrease(original, targetClass, newPlanQty);
        }

        // 无变化
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    /**
     * 3.4 调量前置校验（产能校验）
     * <p>
     * 仅对增量场景进行产能校验。减量或清空班次直接返回成功。
     * </p>
     */
    @Override
    public AjaxResult changeQtyValidate(DjScheduleResult adjustVO) {
        Date scheduleDate = adjustVO.getScheduleDate();
        String factoryCode = adjustVO.getFactoryCode();
        String machineCode = adjustVO.getMachineCode();

        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 校验发布状态
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, new Long[]{adjustVO.getId()});
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 获取原始记录
        DjScheduleResult original = ctx.getScheduleResults().stream()
                .filter(r -> r.getId().equals(adjustVO.getId()))
                .findFirst().orElse(null);
        if (original == null) {
            log.info("调量记录未找到（不在当前排程结果中），id={}", adjustVO.getId());
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.not.found"));
        }

        // 确定目标班次，获取新旧计划量
        int targetClass = resolveTargetClass(adjustVO);
        BigDecimal oldPlanQty = iDjScheduleShiftEngineService.getPlanQtyByIndex(original, targetClass);
        BigDecimal newPlanQty = getPlanQtyByClass(adjustVO, targetClass);

        // 新计划量为空或≤0（清空班次）或非增量场景，直接通过
        if (newPlanQty == null || newPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            return AjaxResult.success();
        }

        // 原班次无计划量（等同插单场景）或减量/无变化，直接通过
        if (oldPlanQty == null || oldPlanQty.compareTo(BigDecimal.ZERO) <= 0
                || newPlanQty.compareTo(oldPlanQty) <= 0) {
            return AjaxResult.success();
        }

        // 增量场景：执行产能校验
        BigDecimal delta = newPlanQty.subtract(oldPlanQty);
        int originSeq = iDjScheduleShiftEngineService.getSequenceByIndex(original, targetClass);

        CapacityValidateResult capacityResult = iDjScheduleShiftEngineService.validateCapacity(machineCode,
                targetClass, originSeq, delta, ctx.getScheduleResults(), factoryCode, scheduleDate);

        return this.handleCapacityResult(capacityResult, delta);
    }

    /**
     * 处理产能校验结果（三档判断），生成对应的 AjaxResult
     * <p>
     * 第一档：定额内 → {@code AjaxResult.success()}<br>
     * 第二档：超定额但未超实际剩余产能 → {@code AjaxResult.success().put("dialogType", "CAPACITY_OVERFLOW")}<br>
     * 第三档：超实际剩余产能 → {@code AjaxResult.error()}
     * </p>
     *
     * @param capacityResult 产能校验结果
     * @param checkQty       用于日志和消息中的数量（插单/调量的量值）
     * @return 对应的 AjaxResult
     */
    private AjaxResult handleCapacityResult(CapacityValidateResult capacityResult, BigDecimal checkQty) {
        // 第一档：在定额内，直接通过
        if (capacityResult.isWithinQuota()) {
            return AjaxResult.success();
        }

        // 第三档：超出实际剩余产能，拒绝
        if (!capacityResult.isPassed()) {
            log.warn("产能校验拒绝：数量 {} 超出实际剩余产能 {}", checkQty, capacityResult.getRemainingCapacity());
            return AjaxResult.error(capacityResult.getErrorMsg());
        }

        // 第二档：超出定额但未超实际剩余产能，需用户确认
        String overflowSpecsStr = "";
        if (CollectionUtils.isNotEmpty(capacityResult.getOverflowSpecs())) {
            overflowSpecsStr = String.join(",", capacityResult.getOverflowSpecs());
        }
        String overflowMsg = MessageFormat.format(
                I18nUtil.getMessage("ui.data.column.scheduleResult.validate.capacity.overflow"),
                checkQty, capacityResult.getRemainingCapacity(), overflowSpecsStr);
        log.info("产能溢出，受影响规格：{}", overflowSpecsStr);
        return AjaxResult.success().put("msg", overflowMsg).put("dialogType", "CAPACITY_OVERFLOW");
    }

    /**
     * 内部：调量增量处理
     * 已有计划量的班次在原量基础上追加，记录原因分析并进行产能校验/顺延处理。
     */
    private AjaxResult internalChangeQtyIncrease(DjScheduleResult original, int targetClass,
                                                  BigDecimal deltaQty, DjAdjustScheduleContext ctx) {
        String machineCode = original.getMachineCode();
        BigDecimal existingPlanQty = iDjScheduleShiftEngineService.getPlanQtyByIndex(original, targetClass);

        // 直接累加，不改变顺位
        BigDecimal newTotal = existingPlanQty.add(BigDecimalUtils.valueOf(deltaQty));
        iDjScheduleShiftEngineService.setPlanQtyByIndex(original, targetClass, newTotal);

        // 记录原因分析（直接覆盖，不保留之前的过程）
        String record = MessageFormat.format(I18nUtil.getMessage("ui.data.column.scheduleResult.analysis.adjust.increase"),
                original.getPaddingName(), deltaQty);
        iDjScheduleShiftEngineService.setAnalysisByIndex(original, targetClass, record);

        // 产能校验（三档判断）
        int originSeq = iDjScheduleShiftEngineService.getSequenceByIndex(original, targetClass);
        CapacityValidateResult capResult = iDjScheduleShiftEngineService.validateCapacity(machineCode, targetClass,
                originSeq, deltaQty, ctx.getScheduleResults(), ctx.getFactoryCode(), ctx.getScheduleDate());
        List<DjScheduleResult> updateList = new ArrayList<>();
        if (capResult.isWithinQuota()) {
            // 第一档：增量在定额内，直接保存
            updateReleaseStatusAfterAdjust(original);
            updateList.add(original);
        } else if (capResult.isPassed()) {
            // 第二档：增量超出定额但在实际剩余产能内，触发顺延
            // processInsertAndCascade 会处理 ctx.getScheduleResults() 中所有相关实体（含 original 对应的记录），
            // 因此不要重复添加 separate instance 的 original，避免同名记录被更新两次
            ShiftContext shiftCtx = new ShiftContext().setFactoryCode(ctx.getFactoryCode())
                    .setScheduleDate(ctx.getScheduleDate()).setMachineCode(machineCode)
                    .setTargetClass(targetClass)
                    .setTargetSeq(iDjScheduleShiftEngineService.getSequenceByIndex(original, targetClass))
                    .setInsertSpecName(original.getPaddingName()).setInsertPlanQty(newTotal)
                    .setScheduleResults(ctx.getScheduleResults()).setOperType("adjust");
            List<DjScheduleResult> updated = iDjScheduleShiftEngineService.processInsertAndCascade(shiftCtx);
            updateList.addAll(updated);
        } else {
            // 第三档：增量超出实际剩余产能，拒绝调整
            return AjaxResult.error(capResult.getErrorMsg());
        }
        baseDao.saveBatch(updateList);

        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, original, ctx.getScheduleResults(), original);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    /**
     * 内部：调量减量处理
     */
    private AjaxResult internalChangeQtyDecrease(DjScheduleResult original, int targetClass,
                                                  BigDecimal newPlanQty) {
        // 约束四：减量不能低于已生产量
        BigDecimal finishQty = getActualFinishQty(original);
        if (finishQty != null && newPlanQty.compareTo(finishQty) < 0) {
            log.info("减量不能低于已生产量，orderNo={}, 已生产={}, 目标={}", original.getOrderNo(), finishQty, newPlanQty);
            return AjaxResult.error(
                    MessageFormat.format(I18nUtil.getMessage("ui.data.column.scheduleResult.reduce.not.less.than.finish"),
                            finishQty.toString()));
        }

        if (newPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            // 清空该班次数据
            iDjScheduleShiftEngineService.setPlanQtyByIndex(original, targetClass, null);
            iDjScheduleShiftEngineService.setSequenceByIndex(original, targetClass, null);
            // 顺位空洞整理
            iDjScheduleShiftEngineService.reorganizeAfterReduce(Collections.singletonList(original), targetClass);
        } else {
            iDjScheduleShiftEngineService.setPlanQtyByIndex(original, targetClass, newPlanQty);
        }

        updateReleaseStatusAfterAdjust(original);
        djScheduleResultMapper.updateById(original);

        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, original, null, original);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    /**
     * 3.5 转机台
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult changeMachine(DjScheduleResult adjustVO) {
        DjScheduleResult original = djScheduleResultMapper.selectById(adjustVO.getId());
        if (original == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.not.found"));
        }

        String targetMachineCode = adjustVO.getMachineCode();
        String factoryCode = adjustVO.getFactoryCode() != null ? adjustVO.getFactoryCode() : original.getFactoryCode();

        // 校验目标机台是否存在且启用
        DjMachineInfo targetMachine = djMachineInfoService.selectMachineInfoList(new DjMachineInfo()).stream()
                .filter(m -> targetMachineCode.equals(m.getMachineCode()) && "0".equals(m.getStatus())).findFirst()
                .orElse(null);
        if (targetMachine == null) {
            log.info("目标机台不存在或已禁用，machineCode={}", targetMachineCode);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machine.not.exist"));
        }

        // 校验定点机台
        List<DjSpecifyMachine> specifyList = djSpecifyMachineMapper
                .selectList(new LambdaQueryWrapper<DjSpecifyMachine>().eq(DjSpecifyMachine::getFactoryCode, factoryCode)
                        .eq(DjSpecifyMachine::getMachineCode, targetMachineCode).eq(DjSpecifyMachine::getJobType, "1")); // 不可作业
        boolean isJobDenied = specifyList.stream().anyMatch(s -> original.getPaddingCode().equals(s.getPaddingCode()));
        if (isJobDenied) {
            log.info("定点机台限制：规格{}不允许在机台{}生产", original.getPaddingCode(), targetMachineCode);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machine.job.denied"));
        }

        // 记录原机台到操作日志
        String oldMachineCode = original.getMachineCode();
        original.setMachineCode(targetMachineCode);
        updateReleaseStatusAfterAdjust(original);
        djScheduleResultMapper.updateById(original);

        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, original, null, original);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    // ==================== 4. 删除 ====================

    /**
     * 4. 删除
     * <p>执行逻辑删除后，对同一机台班次中顺位大于被删除记录的所有记录进行顺位前移（减 1），填补生产顺位空缺。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:DELETE_LOCK")
    public AjaxResult deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }

        List<Long> idList = Arrays.asList(ids);
        List<DjScheduleResult> records = djScheduleResultMapper.selectBatchIds(idList);

        if (CollectionUtils.isEmpty(records)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
        }

        List<Long> canDeleteIds = new ArrayList<>();
        List<String> cannotDeleteReasons = new ArrayList<>();

        for (DjScheduleResult record : records) {
            // 校验一：publishSuccessCount > 0 不允许删除
            if (record.getPublishSuccessCount() != null && record.getPublishSuccessCount() > 0) {
                cannotDeleteReasons.add(String.format(
                        I18nUtil.getMessage("ui.data.column.scheduleResult.delete.published"), record.getOrderNo()));
                continue;
            }
            // 校验二：发布中或超时失败不允许删除
            if (ApsConstant.RELEASING.equals(record.getReleaseStatus())
                    || ApsConstant.TIMEOUT_FAILURE.equals(record.getReleaseStatus())) {
                cannotDeleteReasons.add(String.format(
                        I18nUtil.getMessage("ui.data.column.scheduleResult.delete.releasing"), record.getOrderNo()));
                continue;
            }
            canDeleteIds.add(record.getId());
        }

        if (CollectionUtils.isNotEmpty(canDeleteIds)) {
            // 获取被删除的记录详情（用于顺位补位）
            List<DjScheduleResult> deletedRecords = records.stream()
                    .filter(r -> canDeleteIds.contains(r.getId()))
                    .collect(Collectors.toList());

            // 逻辑删除
            djScheduleResultMapper.deleteBatchIds(canDeleteIds);

            // 生产顺位补位：先查询同一机台排产日的所有剩余记录（框架自动过滤已删除数据），避免循环内重复查询
            DjScheduleResult firstDeleted = deletedRecords.get(0);
            List<DjScheduleResult> allSameMachineRecords = djScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<DjScheduleResult>()
                            .eq(DjScheduleResult::getFactoryCode, firstDeleted.getFactoryCode())
                            .eq(DjScheduleResult::getScheduleDate, firstDeleted.getScheduleDate())
                            .eq(DjScheduleResult::getMachineCode, firstDeleted.getMachineCode()));

            // 遍历所有班次索引（1~6），对有顺位的班次执行顺位前移
            for (int classIdx = 1; classIdx <= DjEngineConstants.SHIFT_COUNT; classIdx++) {
                final int idx = classIdx;
                List<DjScheduleResult> recordsWithSeq = deletedRecords.stream()
                        .filter(r -> {
                            Integer seq = this.getSeqByClass(r, idx);
                            return seq != null && seq > 0;
                        })
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(recordsWithSeq)) {
                    this.fixSequenceAfterDelete(recordsWithSeq, idx, allSameMachineRecords);
                }
            }

            // 记录操作日志
            DjScheduleResult firstRecord = deletedRecords.get(0);
            String recordFactoryCode = firstRecord.getFactoryCode();
            Date recordScheduleDate = firstRecord.getScheduleDate();

            for (Long deleteId : canDeleteIds) {
                DjDispatcherLog logEntry = new DjDispatcherLog();
                logEntry.setFactoryCode(recordFactoryCode);
                logEntry.setScheduleDate(recordScheduleDate);
                logEntry.setOperType(ApsConstant.DISPATCHER_OPER_DELETE);
                logEntry.setScheduleId(deleteId);
                djDispatcherLogService.saveBill(logEntry);
            }
        }

        if (CollectionUtils.isNotEmpty(cannotDeleteReasons)) {
            log.info("删除校验不通过：{}", String.join("; ", cannotDeleteReasons));
            return AjaxResult.error(String.join("; ", cannotDeleteReasons));
        }

        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    /**
     * 删除后生产顺位补位：将同一机台排产日中指定班次索引的顺位前移
     * <p>
     * 对于被删除记录中指定 {@code classIndex} 有顺位的记录，收集所有被删除的顺位，
     * 将同一机台相同排产日的剩余记录中该班次索引的顺位减去前面被删除的个数。
     * </p>
     *
     * @param deletedGroup        被删除的记录列表（需包含在该班次索引有顺位的记录）
     * @param classIndex          班次索引（1~6）
     * @param sameMachineRecords  同一机台排产日下的所有剩余记录（预查询传入，避免循环内重复查库）
     */
    private void fixSequenceAfterDelete(List<DjScheduleResult> deletedGroup, int classIndex,
            List<DjScheduleResult> sameMachineRecords) {

        // 收集该班次索引下所有被删除的顺位
        List<Integer> deletedSequences = deletedGroup.stream()
                .map(r -> this.getSeqByClass(r, classIndex))
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (deletedSequences.isEmpty()) {
            return;
        }

        // 前移顺位：每个剩余记录在该班次索引的顺位减去其之前被删除的记录数
        for (DjScheduleResult rec : sameMachineRecords) {
            Integer seq = this.getSeqByClass(rec, classIndex);
            if (seq == null) {
                continue;
            }
            long offset = deletedSequences.stream().filter(ds -> ds < seq).count();
            if (offset > 0) {
                this.setSeqByClass(rec, classIndex, seq - (int) offset);
                djScheduleResultMapper.updateById(rec);
            }
        }
    }

    // ==================== 5. 发布 ====================

    /**
     * 5. 发布
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:AUTO_LOCK:#factoryCode:#scheduleDate")
    public AjaxResult publish(DjScheduleResult publishVO) {
        Long[] ids = publishVO.getIds();
        Date scheduleDate = publishVO.getScheduleDate();
        String factoryCode = publishVO.getFactoryCode();

        if (ids == null || ids.length == 0) {
            log.info("发布参数错误：ids为空");
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }

        // 校验：过滤出可发布的记录
        List<DjScheduleResult> records = djScheduleResultMapper.selectBatchIds(Arrays.asList(ids));
        List<DjScheduleResult> publishable = records.stream()
                .filter(r -> ApsConstant.NO_RELEASE.equals(r.getReleaseStatus())
                        || ApsConstant.FAILURE_RELEASE.equals(r.getReleaseStatus())
                        || ApsConstant.WAIT_RELEASING.equals(r.getReleaseStatus()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(publishable)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.no.publishable"));
        }

        // 更新状态为"发布中"
        for (DjScheduleResult r : publishable) {
            r.setReleaseStatus(ApsConstant.RELEASING);
            djScheduleResultMapper.updateById(r);
        }

        try {
            // 调用 MES 发布（将排程数据推到中间库）
            boolean deploySuccess = deployToMes(publishable);

            if (deploySuccess) {
                // 发布成功
                for (DjScheduleResult r : publishable) {
                    r.setReleaseStatus(ApsConstant.IS_RELEASE);
                    r.setPublishSuccessCount((r.getPublishSuccessCount() == null ? 0 : r.getPublishSuccessCount()) + 1);
                    r.setNewestPublishTime(new Date());
                    djScheduleResultMapper.updateById(r);
                }
                // 记录发布记录
                savePublishRecord(factoryCode, scheduleDate, ApsConstant.IS_RELEASE);
                return AjaxResult.success(I18nUtil.getMessage("ui.message.publish.success"));
            } else {
                // 发布失败
                for (DjScheduleResult r : publishable) {
                    r.setReleaseStatus(ApsConstant.FAILURE_RELEASE);
                    djScheduleResultMapper.updateById(r);
                }
                savePublishRecord(factoryCode, scheduleDate, ApsConstant.FAILURE_RELEASE);
                log.info("MES发布失败，factory={}, date={}", factoryCode, scheduleDate);
                return AjaxResult.error(I18nUtil.getMessage("ui.message.publish.fail"));
            }
        } catch (Exception e) {
            log.error("发布异常", e);
            for (DjScheduleResult r : publishable) {
                r.setReleaseStatus(ApsConstant.FAILURE_RELEASE);
                djScheduleResultMapper.updateById(r);
            }
            savePublishRecord(factoryCode, scheduleDate, ApsConstant.FAILURE_RELEASE);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.publish.fail"));
        }
    }

    // ==================== 6. 导入 ====================

    /**
     * 6. 导入数校验
     */
    public AjaxResult validateImportData(List<DjScheduleResult> importList, Date scheduleDate, String factoryCode) {
        // 6.1 校验：排产日期不能早于当前日期
        if (scheduleDate != null && DateUtil.compare(scheduleDate, new Date()) < 0) {
            log.info("导入日期校验失败，scheduleDate={}", DateUtil.format(scheduleDate, DatePattern.NORM_DATE_PATTERN));
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.date.before.today"));
        }

        // 校验存在发布中或超时失败记录
        int lockedCount = djScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (lockedCount > 0) {
            log.info("导入校验：存在发布中或超时失败记录，lockedCount={}", lockedCount);
            return AjaxResult
                    .error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }

        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 6.2 分离已有记录（合并班次数据后批量保存）和新增记录（走插单逻辑）
        int successCount = 0;
        int failCount = 0;
        List<String> errorMsgs = new ArrayList<>();
        List<DjScheduleResult> batchList = new ArrayList<>();
        List<DjScheduleResult> newItemList = new ArrayList<>();

        // 6.2 校验/合并导入数据：批量查询已有工单号的排程记录，避免逐条查询 N+1
        List<String> orderNos = importList.stream()
                .map(DjScheduleResult::getOrderNo)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Map<String, DjScheduleResult> existingOrderMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(orderNos)) {
            List<DjScheduleResult> existingList = djScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<DjScheduleResult>().in(DjScheduleResult::getOrderNo, orderNos));
            for (DjScheduleResult r : existingList) {
                existingOrderMap.putIfAbsent(r.getOrderNo(), r);
            }
        }

        for (DjScheduleResult item : importList) {
            String orderNo = item.getOrderNo();
            if (StringUtils.isNotBlank(orderNo) && existingOrderMap.containsKey(orderNo)) {
                // 工单号存在 → 视为调整操作，合并班次数据后加入批量保存列表
                DjScheduleResult target = existingOrderMap.get(orderNo);
                mergeClassData(target, item);
                updateReleaseStatusAfterAdjust(target);
                batchList.add(target);
                continue;
            }
            // 工单号为空或不存在 → 走插单逻辑
            newItemList.add(item);
        }

        // 6.3 批量保存已有记录的合并数据（baseDao.saveBatch 会根据 ID 自动识别为更新操作）
        if (CollectionUtils.isNotEmpty(batchList)) {
            try {
                baseDao.saveBatch(batchList);
                log.info("导入批量保存成功：{}条", batchList.size());
                successCount += batchList.size();
            } catch (Exception e) {
                log.error("导入批量保存异常", e);
                failCount += batchList.size();
                errorMsgs.add(e.getMessage());
            }
        }

        // 6.4 逐条处理新增记录（走插单逻辑）
        for (int i = 0; i < newItemList.size(); i++) {
            DjScheduleResult item = newItemList.get(i);
            try {
                AjaxResult result = this.insertOrder(item);
                if (AjaxResultUtils.checkAjaxSuccess(result)) {
                    successCount++;
                } else {
                    failCount++;
                    errorMsgs.add(MessageFormat.format(I18nUtil.getMessage("ui.message.import.row.error"), (i + 1),
                            AjaxResultUtils.getMsg(result)));
                }
            } catch (Exception e) {
                failCount++;
                errorMsgs.add(
                        MessageFormat.format(I18nUtil.getMessage("ui.message.import.row.error"), (i + 1), e.getMessage()));
            }
        }

        if (failCount > 0) {
            return AjaxResult.error(
                    MessageFormat.format(I18nUtil.getMessage("ui.message.import.result"), successCount, failCount), errorMsgs);
        }
        return AjaxResult
                .success(MessageFormat.format(I18nUtil.getMessage("ui.message.import.result"), successCount, failCount));
    }

    // ==================== 内部工具方法 ====================

    /**
     * 更新调整后的发布状态
     */
    private void updateReleaseStatusAfterAdjust(DjScheduleResult entity) {
        if (ApsConstant.IS_RELEASE.equals(entity.getReleaseStatus())) {
            entity.setReleaseStatus(ApsConstant.WAIT_RELEASING);
        }
    }



    /**
     * 记录操作日志到 T_DJ_DISPATCHER_LOG
     */
    private void recordDispatcherLog(String operType, DjScheduleResult newSchedule, List<DjScheduleResult> oldResults,
            DjScheduleResult insertVO) {
        try {
            DjDispatcherLog logEntry = new DjDispatcherLog();
            logEntry.setFactoryCode(newSchedule.getFactoryCode());
            logEntry.setScheduleDate(newSchedule.getScheduleDate());
            logEntry.setOperType(operType);
            logEntry.setScheduleId(newSchedule.getId());
            djDispatcherLogService.saveBill(logEntry);
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }
    }

    /**
     * 获取目标班次索引（从插入对象中解析）
     */
    private int resolveTargetClass(DjScheduleResult vo) {
        for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
            BigDecimal planQty = getPlanQtyByClass(vo, c);
            if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                return c;
            }
        }
        return 1;
    }

    /**
     * 获取最后一个有计划量的班次位置
     */
    private int resolveLastClass(DjScheduleResult vo) {
        for (int c = DjEngineConstants.SHIFT_COUNT; c >= 1; c--) {
            BigDecimal planQty = getPlanQtyByClass(vo, c);
            if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                return c;
            }
        }
        return 1;
    }

    /**
     * 获取目标班次的顺位
     */
    private int resolveTargetSequence(DjScheduleResult vo, int targetClass) {
        Integer seq = getSeqByClass(vo, targetClass);
        return seq != null ? seq : 1;
    }

    /**
     * 根据首班班次计算插单的实际排产日期
     * <p>
     * 逻辑说明：
     * <ol>
     *   <li>从传入的 insertVO 中获取 {@code scheduleShiftClass}（首班班次，打开插单页面时记录）</li>
     *   <li>通过 {@link #resolveTargetClass(DjScheduleResult)} 确定插单目标班次（连续3个班中的第几个班）</li>
     *   <li>查询活跃班次配置，以首班班次为起点、目标班次为终点（含两端），遍历检查是否有跨天班次（crossDayFlag="1"）：</li>
     *   <li>无跨天班次 → 排产日期 = 当前服务器时间所在排产日；有跨天班次 → 排产日期 = 当前服务器时间所在排产日 + 1</li>
     * </ol>
     * </p>
     *
     * @param insertVO 插单参数（需包含 scheduleShiftClass 及各班次计划量）
     * @return 计算后的排产日期；若缺少必要参数则返回前端传入的原始 scheduleDate
     */
    private Date calculateInsertScheduleDate(DjScheduleResult insertVO) {
        String startShiftClass = insertVO.getScheduleShiftClass();
        if (StringUtils.isBlank(startShiftClass)) {
            // 无首班班次时直接使用前端传入的排产日期（兼容旧逻辑）
            return insertVO.getScheduleDate();
        }

        // 确定目标班次是连续3个班中的第几个班（1/2/3）
        int targetClass = resolveTargetClass(insertVO);

        // 查询活动班次配置
        List<DjShiftConfig> activeShifts = djShiftConfigService.listActiveShifts();
        if (CollectionUtils.isEmpty(activeShifts)) {
            log.warn("未找到活动班次配置，无法计算插单排产日期，使用前端传入日期");
            return insertVO.getScheduleDate();
        }

        // 计算当前服务器时间所在的排产日
        LocalTime now = LocalTime.now();
        LocalDate serverDate = LocalDate.now();
        LocalDate serverProductionDate = serverDate;
        for (DjShiftConfig config : activeShifts) {
            LocalTime startTime = LocalTime.parse(config.getPlanStartTime());
            LocalTime endTime = LocalTime.parse(config.getPlanEndTime());
            boolean inRange;
            if (ApsConstant.TRUE.equals(config.getCrossDayFlag())) {
                inRange = !now.isBefore(startTime) || now.isBefore(endTime);
                if (inRange && !now.isBefore(startTime)) {
                    serverProductionDate = serverDate.plusDays(1);
                }
            } else {
                inRange = !now.isBefore(startTime) && now.isBefore(endTime);
            }
            if (inRange) {
                break;
            }
        }

        // 查找首班班次在活跃班次列表中的索引
        int startIndex = -1;
        int totalShifts = activeShifts.size();
        for (int i = 0; i < totalShifts; i++) {
            if (activeShifts.get(i).getShiftCode().equals(startShiftClass)) {
                startIndex = i;
                break;
            }
        }
        if (startIndex < 0) {
            log.warn("首班班次 {} 不在活跃班次配置中，使用前端传入日期", startShiftClass);
            return insertVO.getScheduleDate();
        }

        // 判断从首班班次到目标班次之间（含首班）是否有跨天班次
        boolean hasCrossDay = false;
        for (int j = 0; j < targetClass; j++) {
            if (ApsConstant.TRUE.equals(activeShifts.get((startIndex + j) % totalShifts).getCrossDayFlag())) {
                hasCrossDay = true;
                break;
            }
        }

        // 计算排产日期
        LocalDate localCalculatedDate = hasCrossDay ? serverProductionDate.plusDays(1) : serverProductionDate;
        Date calculatedDate = Date.from(localCalculatedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        log.info("插单排产日期计算：startShiftClass={}, targetClass={}, hasCrossDay={}, serverProductionDate={}, calculatedDate={}",
                startShiftClass, targetClass, hasCrossDay, serverProductionDate, calculatedDate);
        return calculatedDate;
    }

    /**
     * 插单排产日期组 — 班次粒度计算，支持跨排产日拆分
     * <p>
     * 按设计文档 2.1.1 精细化算法：对 position=1~lastClass 逐位计算独立排产日期，
     * 将有量班次按排产日期分组返回。
     * </p>
     * <p>
     * 排产日期变化条件（每个班次独立判断，累积偏移）：
     * <ol>
     *   <li>当遇到跨天班次（crossDayFlag=1）时，后续位置排产日+1</li>
     *   <li>当遇到开班班次（openFlag=1）且非首位置时（即生产周期切换边界），后续位置排产日+1</li>
     * </ol>
     * </p>
     *
     * @param insertVO 插单参数
     * @return 排产日期分组列表（按日期升序）
     */
    private List<ScheduleDateGroup> calculateInsertScheduleDateGroups(DjScheduleResult insertVO) {
        String startShiftClass = insertVO.getScheduleShiftClass();
        if (StringUtils.isBlank(startShiftClass)) {
            // 无首班班次时直接使用前端传入的排产日期（兼容旧逻辑如调量转插单）
            return Collections.singletonList(buildSingleDateGroup(insertVO, insertVO.getScheduleDate(), insertVO.getScheduleShiftClass()));
        }

        int lastClass = resolveLastClass(insertVO);

        // 查询活动班次配置
        List<DjShiftConfig> activeShifts = djShiftConfigService.listActiveShifts();
        if (CollectionUtils.isEmpty(activeShifts)) {
            return Collections.singletonList(buildSingleDateGroup(insertVO, insertVO.getScheduleDate(), startShiftClass));
        }

        // 计算当前服务器时间所在的排产日
        LocalDate serverProductionDate = calculateServerProductionDate(activeShifts);

        // 查找首班班次索引
        int startIndex = findShiftIndex(activeShifts, startShiftClass);
        if (startIndex < 0) {
            return Collections.singletonList(buildSingleDateGroup(insertVO, insertVO.getScheduleDate(), startShiftClass));
        }

        // 查找开班班次索引（用于生产周期边界检测）
        int openShiftIndex = findOpeningShiftIndex(activeShifts);

        // 计算各位置的排产日期偏移和位置→class映射
        int totalShifts = activeShifts.size();
        List<Integer> dateOffsetPerPosition = new ArrayList<>(lastClass);
        Map<Integer, Integer> positionClassMap = new HashMap<>();

        // 构建开班班次到class位置的映射：key=shiftIndex, value=classPosition(1~3)
        Map<Integer, Integer> shiftToClassMap = buildShiftToClassMap(activeShifts, openShiftIndex);

        int dateOffset = 0;
        for (int position = 1; position <= lastClass; position++) {
            int shiftIndex = (startIndex + position - 1) % totalShifts;
            DjShiftConfig shift = activeShifts.get(shiftIndex);

            // 日期偏移：每个班次独立判断，跨天（crossDayFlag=1）或开班边界（openFlag=1且非首位置）时排产日+1
            if (position > 1) {
                boolean isCrossDay = ApsConstant.TRUE.equals(shift.getCrossDayFlag());
                boolean isCycleBoundary = (shiftIndex == openShiftIndex);
                if (isCrossDay || isCycleBoundary) {
                    dateOffset++;
                }
            }

            dateOffsetPerPosition.add(dateOffset);

            // 计算输入position→输出class位置的映射
            Integer outputClass = shiftToClassMap.get(shiftIndex);
            if (outputClass != null) {
                positionClassMap.put(position, outputClass);
            }
        }

        // 按排产日期偏移分组
        Map<Integer, ScheduleDateGroup> dateGroupMap = new LinkedHashMap<>();
        for (int position = 1; position <= lastClass; position++) {
            if (!hasPlanQty(insertVO, position)) {
                continue;
            }
            int offset = dateOffsetPerPosition.get(position - 1);
            LocalDate posDate = serverProductionDate.plusDays(offset);
            int offsetKey = offset;

            dateGroupMap.computeIfAbsent(offsetKey, k -> {
                ScheduleDateGroup g = new ScheduleDateGroup();
                g.setScheduleDate(Date.from(serverProductionDate.plusDays(k).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                return g;
            });

            ScheduleDateGroup g = dateGroupMap.get(offsetKey);
            g.getPositions().add(position);
            g.getPositionDates().put(position, g.getScheduleDate());

            // 记录位置→class映射
            Integer outputClass = positionClassMap.get(position);
            if (outputClass != null) {
                g.getPositionClassMap().put(position, outputClass);
            }

            // 第一个遇到该组的班次设为首班班次
            if (g.getScheduleShiftClass() == null) {
                int si = (startIndex + position - 1) % totalShifts;
                g.setScheduleShiftClass(activeShifts.get(si).getShiftCode());
            }
        }

        return new ArrayList<>(dateGroupMap.values());
    }

    /**
     * 构建开班班次到输出class位置的映射
     * <p>
     * 以开班班次为起点，按班次生产周期顺序（通过 getNextClass 确定），
     * 将每个班次编码映射到输出DjScheduleResult的class位置(1~3)。
     * 例如：开班="03"，则映射为 03→1, 01→2, 02→3。
     * </p>
     */
    private Map<Integer, Integer> buildShiftToClassMap(List<DjShiftConfig> activeShifts, int openShiftIndex) {
        Map<Integer, Integer> map = new HashMap<>();
        int totalShifts = activeShifts.size();
        // 开班班次为class1（在输出记录中的位置）
        for (int classPos = 1; classPos <= totalShifts; classPos++) {
            int shiftIndex = (openShiftIndex + classPos - 1) % totalShifts;
            map.put(shiftIndex, classPos);
        }
        return map;
    }

    /**
     * 在活动班次列表中查找开班班次索引
     */
    private int findOpeningShiftIndex(List<DjShiftConfig> activeShifts) {
        for (int i = 0; i < activeShifts.size(); i++) {
            if ("1".equals(activeShifts.get(i).getOpenFlag())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在活动班次列表中查找指定班次编码的索引
     */
    private int findShiftIndex(List<DjShiftConfig> activeShifts, String shiftCode) {
        for (int i = 0; i < activeShifts.size(); i++) {
            if (activeShifts.get(i).getShiftCode().equals(shiftCode)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算服务器当前时间所在的排产日
     */
    private LocalDate calculateServerProductionDate(List<DjShiftConfig> activeShifts) {
        LocalTime now = LocalTime.now();
        LocalDate serverDate = LocalDate.now();
        LocalDate serverProductionDate = serverDate;
        for (DjShiftConfig config : activeShifts) {
            LocalTime startTime = LocalTime.parse(config.getPlanStartTime());
            LocalTime endTime = LocalTime.parse(config.getPlanEndTime());
            boolean inRange;
            if (ApsConstant.TRUE.equals(config.getCrossDayFlag())) {
                inRange = !now.isBefore(startTime) || now.isBefore(endTime);
                if (inRange && !now.isBefore(startTime)) {
                    serverProductionDate = serverDate.plusDays(1);
                }
            } else {
                inRange = !now.isBefore(startTime) && now.isBefore(endTime);
            }
            if (inRange) {
                break;
            }
        }
        return serverProductionDate;
    }

    /**
     * 构建单组排产日分组（无跨天/无班次配置时的保底逻辑）
     */
    private ScheduleDateGroup buildSingleDateGroup(DjScheduleResult insertVO, Date scheduleDate, String shiftClass) {
        ScheduleDateGroup group = new ScheduleDateGroup();
        group.setScheduleDate(scheduleDate);
        group.setScheduleShiftClass(shiftClass);
        for (int p = 1; p <= DjEngineConstants.SHIFT_COUNT; p++) {
            if (hasPlanQty(insertVO, p)) {
                group.getPositions().add(p);
                group.getPositionDates().put(p, scheduleDate);
            }
        }
        return group;
    }

    private boolean hasPlanQty(DjScheduleResult vo, int position) {
        BigDecimal qty = getPlanQtyByClass(vo, position);
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 根据垫胶编码查询施工信息
     * <p>
     * 从 T_MDM_CONSTRUCTION_INFO 表中查询对应垫胶编码的施工数据，
     * 用于填充插单记录的胶料代码、物料名称等字段。
     * </p>
     *
     * @param factoryCode 工厂编码
     * @param paddingCode 垫胶编码
     * @return 施工信息，未查到返回 null
     */
    private MdmConstructionInfo loadConstructionByPadding(String factoryCode, String paddingCode) {
        if (StringUtils.isBlank(paddingCode)) {
            return null;
        }
        List<MdmConstructionInfo> list = djEngineConstructionInfoMapper.selectList(
                new LambdaQueryWrapper<MdmConstructionInfo>()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .eq(MdmConstructionInfo::getPaddingCode, paddingCode)
                        .last("LIMIT 1"));
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * 加载垫胶 T-1 日库存
     * <p>
     * 从 T_DJ_STOCK 表中查询对应垫胶编码在排产日前一天的库存量，
     * 有效库存 = 库存量 + 修正数量 - 不良数量。
     * </p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排产日期
     * @param paddingCode  垫胶编码
     * @return 有效库存量，查不到返回 null
     */
    private BigDecimal loadPaddingStock(String factoryCode, Date scheduleDate, String paddingCode) {
        if (StringUtils.isBlank(paddingCode)) {
            return null;
        }
        List<DjStock> stockList = djEngineStockMapper.selectList(new LambdaQueryWrapper<DjStock>()
                .eq(DjStock::getFactoryCode, factoryCode)
                .eq(DjStock::getStockDate, DateUtil.offsetDay(scheduleDate, -1))
                .eq(DjStock::getMaterialCode, paddingCode)
                .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(stockList)) {
            return null;
        }
        DjStock stock = stockList.get(0);
        return BigDecimalUtils.valueOf(stock.getStockNum())
                .add(BigDecimalUtils.valueOf(stock.getModifyNum()))
                .subtract(BigDecimalUtils.valueOf(stock.getBadNum()));
    }

    /**
     * 入参校验
     */
    private AjaxResult validateInsertParams(DjScheduleResult vo, DjAdjustScheduleContext ctx) {
        if (vo.getScheduleDate() == null) {
            return AjaxResult
                    .error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "scheduleDate"));
        }
        if (StringUtils.isBlank(vo.getMachineCode())) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "machineCode"));
        }
        if (StringUtils.isBlank(vo.getPaddingCode())) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "paddingCode"));
        }
        // 校验至少一个班次有计划量
        boolean hasPlanQty = false;
        for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
            BigDecimal qty = getPlanQtyByClass(vo, c);
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                hasPlanQty = true;
                break;
            }
        }
        if (!hasPlanQty) {
            log.info("入参校验失败：未设置任何班次计划量");
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "planQty"));
        }
        return null;
    }

    /**
     * 获取排程结果中某班次的计划量
     */
    private BigDecimal getPlanQtyByClass(DjScheduleResult sr, int classIndex) {
        return iDjScheduleShiftEngineService.getPlanQtyByIndex(sr, classIndex);
    }

    /**
     * 设置排程结果中某班次的计划量
     */
    private void setPlanQtyByClass(DjScheduleResult sr, int classIndex, BigDecimal qty) {
        iDjScheduleShiftEngineService.setPlanQtyByIndex(sr, classIndex, qty);
    }

    /**
     * 获取排程结果中某班次的顺位
     */
    private Integer getSeqByClass(DjScheduleResult sr, int classIndex) {
        return iDjScheduleShiftEngineService.getSequenceByIndex(sr, classIndex);
    }

    /**
     * 设置排程结果中某班次的顺位
     */
    private void setSeqByClass(DjScheduleResult sr, int classIndex, Integer seq) {
        iDjScheduleShiftEngineService.setSequenceByIndex(sr, classIndex, seq);
    }

    /**
     * 获取实际完成量
     */
    private BigDecimal getActualFinishQty(DjScheduleResult sr) {
        List<DjDayFinishQty> finishList = djDayFinishQtyMapper
                .selectList(new LambdaQueryWrapper<DjDayFinishQty>().eq(DjDayFinishQty::getOrderNo, sr.getOrderNo()));
        if (CollectionUtils.isNotEmpty(finishList)) {
            DjDayFinishQty finish = finishList.get(0);
            return BigDecimalUtils.valueOf(finish.getNightFinishQty())
                    .add(BigDecimalUtils.valueOf(finish.getDayFinishQty()))
                    .add(BigDecimalUtils.valueOf(finish.getMidFinishQty()));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 合并导入数据到已有记录
     */
    private void mergeClassData(DjScheduleResult target, DjScheduleResult source) {
        for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
            BigDecimal srcPlanQty = getPlanQtyByClass(source, c);
            if (srcPlanQty != null) {
                setPlanQtyByClass(target, c, srcPlanQty);
            }
            Integer srcSeq = getSeqByClass(source, c);
            if (srcSeq != null) {
                setSeqByClass(target, c, srcSeq);
            }
        }
    }

    /**
     * 调用 MES 发布
     */
    private boolean deployToMes(List<DjScheduleResult> records) {
        // TODO: 调用 MES 中间库发布接口
        // deployDjScheduleToMid(records);
        log.info("发布垫胶排程到MES：{}条记录", records.size());
        return true;
    }

    /**
     * 保存发布记录
     */
    private void savePublishRecord(String factoryCode, Date scheduleDate, String status) {
        // TODO: 通过 SchedulePublishRecordService 保存
        log.info("保存发布记录：factory={}, date={}, status={}", factoryCode, scheduleDate, status);
    }
}

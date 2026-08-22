package com.zlt.aps.nc.service.impl;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.api.domain.entity.NcShiftConfig;
import com.zlt.aps.nc.api.domain.entity.NcSpecifyMachine;
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.engine.constant.NcEngineConstants;
import com.zlt.aps.nc.engine.mapper.NcEngineConstructionInfoMapper;
import com.zlt.aps.nc.engine.mapper.NcEngineStockMapper;
import com.zlt.aps.nc.engine.model.CapacityValidateResult;
import com.zlt.aps.nc.engine.model.ShiftContext;
import com.zlt.aps.nc.engine.model.ShiftValidateResult;
import com.zlt.aps.nc.engine.service.INcOrderGeneratorService;
import com.zlt.aps.nc.engine.service.INcScheduleShiftEngineService;
import com.zlt.aps.nc.mapper.NcDayFinishQtyMapper;
import com.zlt.aps.nc.mapper.NcScheduleResultMapper;
import com.zlt.aps.nc.mapper.NcSpecifyMachineMapper;
import com.zlt.aps.nc.model.NcAdjustScheduleContext;
import com.zlt.aps.nc.model.ScheduleDateGroup;
import com.zlt.aps.nc.service.INcScheduleAdjustService;
import com.zlt.aps.nc.service.INcShiftConfigService;
import com.zlt.aps.nc.service.NcDispatcherLogService;
import com.zlt.aps.nc.service.NcMachineInfoService;
import com.zlt.aps.nc.service.NcScheduleResultService;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.core.dao.basedao.BaseDao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 内衬排程调整引擎
 * <p>
 * 实现设计文档「内衬排程调整算法设计.md」中所有调整操作的计算步骤。
 * 包括：插单(2.)、调整(3.)、删除(4.)、发布(5.)、导入(6.)。
 * </p>
 *
 * @author zlt
 */
@Component
@Slf4j
public class NcScheduleAdjustServiceImpl implements INcScheduleAdjustService {

    @Resource
    private NcScheduleResultMapper NcScheduleResultMapper;

    @Resource
    private NcScheduleResultService NcScheduleResultService;

    @Resource
    private NcMachineInfoService djMachineInfoService;

    @Resource
    private NcDispatcherLogService djDispatcherLogService;

    @Resource
    private NcSpecifyMachineMapper djSpecifyMachineMapper;

    @Resource
    private NcDayFinishQtyMapper djDayFinishQtyMapper;

    @Resource
    private INcScheduleShiftEngineService iNcScheduleShiftEngineService;

    @Resource
    private BaseDao baseDao;

    @Resource
    private NcEngineConstructionInfoMapper djEngineConstructionInfoMapper;

    @Resource
    private NcEngineStockMapper djEngineStockMapper;

    @Resource
    private INcOrderGeneratorService iNcOrderGeneratorService;

    @Resource
    private INcShiftConfigService djShiftConfigService;

    // ==================== 1. 公共数据预加载 ====================

    /**
     * 1.1~1.4 加载基础数据
     */
    private NcAdjustScheduleContext loadBaseData(String factoryCode, Date scheduleDate) {
        NcAdjustScheduleContext ctx = new NcAdjustScheduleContext();
        ctx.setFactoryCode(factoryCode);
        ctx.setScheduleDate(scheduleDate);

        // 1.1 加载机台数据
        NcMachineInfo machineQuery = new NcMachineInfo();
        machineQuery.setFactoryCode(factoryCode);
        List<NcMachineInfo> machineList = djMachineInfoService.selectMachineInfoList(machineQuery);
        ctx.setMachineList(machineList);
        ctx.setMachineMap(
                machineList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineCode, m -> m, (a, b) -> a)));

        // 1.2 加载排程结果数据
        List<NcScheduleResult> scheduleResults = NcScheduleResultMapper
                .selectList(new LambdaQueryWrapper<NcScheduleResult>().eq(NcScheduleResult::getFactoryCode, factoryCode)
                        .eq(NcScheduleResult::getScheduleDate, scheduleDate));
        ctx.setScheduleResults(scheduleResults != null ? scheduleResults : new ArrayList<>());

        // 1.3 加载定点机台数据
        List<NcSpecifyMachine> specifyMachines = djSpecifyMachineMapper.selectList(
                new LambdaQueryWrapper<NcSpecifyMachine>().eq(NcSpecifyMachine::getFactoryCode, factoryCode));
        ctx.setSpecifyMachines(specifyMachines != null ? specifyMachines : new ArrayList<>());

        // 1.4 加载发布记录（通过已有的 Service 方法校验）
        ctx.setPublishRecordCount(NcScheduleResultService.isPublish(scheduleDate) ? 1 : 0);

        return ctx;
    }

    /**
     * 1.5 校验排产日是否已被锁定
     */
    private AjaxResult checkScheduleLocked(Date scheduleDate, Long[] ids) {
        int lockedCount;
        if (ids != null && ids.length > 0) {
            lockedCount = NcScheduleResultService.isReleasingOrTimeoutByIds(ids);
        } else {
            lockedCount = NcScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
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
        List<NcScheduleResult> exists = NcScheduleResultMapper.selectList(
                new LambdaQueryWrapper<NcScheduleResult>()
                        .eq(NcScheduleResult::getFactoryCode, factoryCode)
                        .eq(NcScheduleResult::getScheduleDate, scheduleDate)
                        .eq(NcScheduleResult::getMachineCode, machineCode));
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
    public AjaxResult insertOrder(NcScheduleResult insertVO) {
        String factoryCode = insertVO.getFactoryCode();
        String machineCode = insertVO.getMachineCode();

        // 2.1.1 计算排产日期分组（支持一次插入6个班，跨排产日自动拆分多笔记录）
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
        NcAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

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

        // 确定目标班次和顺位（基于第一组），并转换为第一组记录中的输出class位置
        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);
        int recordClass = resolveRecordClass(firstGroup, targetClass);

        // 2.2 约束一校验 — 生产顺位合法性
        ShiftValidateResult shiftResult = iNcScheduleShiftEngineService.validateInsertConstraint(factoryCode,
                scheduleDate, machineCode, recordClass, targetSeq);
        if (!shiftResult.isPassed()) {
            log.info("插单约束校验不通过：{}", shiftResult.getErrorMsg());
            return AjaxResult.error(shiftResult.getErrorMsg());
        }

        // 2.3 约束二校验 — 产能校验（三档判断）
        BigDecimal insertPlanQty = getPlanQtyByClass(insertVO, targetClass);
        CapacityValidateResult capacityResult = iNcScheduleShiftEngineService.validateCapacity(machineCode, recordClass,
                targetSeq, insertPlanQty, ctx.getScheduleResults(), factoryCode, scheduleDate);

        // 第一档：插单量 ≤ 剩余产能（定额 - 当班原有计划量），无产能问题直接执行插单
        if (capacityResult.isWithinQuota()) {
            return executeInsertInternalWithGroups(insertVO, recordClass, targetSeq, ctx, dateGroups);
        }

        // 第三档：插单量 > 实际剩余产能（定额 - 已生产量），超当班剩余产能，拒绝插单
        if (!capacityResult.isPassed()) {
            log.warn("插单量 {} 超出实际剩余产能 {}", insertPlanQty, capacityResult.getRemainingCapacity());
            return AjaxResult.error(capacityResult.getErrorMsg());
        }

        // 第二档在 insertOrderValidate 中已处理，用户确认后直接执行
        // 若走到这里说明未经过前置校验或前置已确认，直接执行插单
        return executeInsertInternalWithGroups(insertVO, recordClass, targetSeq, ctx, dateGroups);
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
    public AjaxResult insertOrderValidate(NcScheduleResult insertVO) {
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
        NcAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 2.1 入参校验
        AjaxResult paramCheck = validateInsertParams(insertVO, ctx);
        if (paramCheck != null) {
            return paramCheck;
        }

        // 确定目标班次和顺位，并转换为第一组记录中的输出class位置
        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);
        int recordClass = resolveRecordClass(firstGroup, targetClass);

        // 2.2 约束一校验 — 生产顺位合法性
        ShiftValidateResult shiftResult = iNcScheduleShiftEngineService.validateInsertConstraint(factoryCode,
                scheduleDate, machineCode, recordClass, targetSeq);
        if (!shiftResult.isPassed()) {
            return AjaxResult.error(shiftResult.getErrorMsg());
        }

        // 2.3 约束二校验 — 产能校验（三档判断）
        BigDecimal insertPlanQty = getPlanQtyByClass(insertVO, targetClass);
        CapacityValidateResult capacityResult = iNcScheduleShiftEngineService.validateCapacity(machineCode, recordClass,
                targetSeq, insertPlanQty, ctx.getScheduleResults(), factoryCode, scheduleDate);

        return this.handleCapacityResult(capacityResult, insertPlanQty);
    }

    /**
     * 确认插单（用户在前端弹窗点击"坚持执行"后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult confirmInsertOrder(NcScheduleResult insertVO) {
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

        NcAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, null);
        if (lockedCheck != null) {
            return lockedCheck;
        }

        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);
        int recordClass = resolveRecordClass(firstGroup, targetClass);

        return executeInsertInternalWithGroups(insertVO, recordClass, targetSeq, ctx, dateGroups);
    }

    /**
     * 插单内部执行（支持多排产日分组）
     * <p>
     * 第一组执行顺延处理，后续组直接插入新记录。
     * </p>
     *
     * @param insertVO   插单参数（含6个班次计划量/顺位/原因分析）
     * @param targetClass 目标输出class位置（第一组记录中的class索引，1~6）
     * @param targetSeq   目标生产顺位
     * @param ctx         调整上下文（基于第一组排产日加载）
     * @param dateGroups  排产日期分组列表（第一组执行顺延，后续组直接插入）
     * @return 操作结果
     */
    private AjaxResult executeInsertInternalWithGroups(NcScheduleResult insertVO, int targetClass, int targetSeq,
            NcAdjustScheduleContext ctx, List<ScheduleDateGroup> dateGroups) {
        String factoryCode = ctx.getFactoryCode();
        Date scheduleDate = ctx.getScheduleDate();
        String machineCode = insertVO.getMachineCode();

        // 获取插单规格名称
        String specName = insertVO.getLiningName();
        if (StringUtils.isBlank(specName)) {
            specName = insertVO.getLiningCode();
        }

        // ====== 第一组：按现有逻辑执行顺延 ======
        ScheduleDateGroup firstGroup = dateGroups.get(0);

        // 2.4.1：生成工单号（基于第一组的排产日）
        // 批次号取当前排产日其余记录的值（同一排产日内所有记录批次号一致）
        String batchNoFromExisting = "";
        for (NcScheduleResult r : ctx.getScheduleResults()) {
            if (StringUtils.isNotBlank(r.getBatchNo())) {
                batchNoFromExisting = r.getBatchNo();
                break;
            }
        }
        // 计算当前最大工单流水号
        int maxOrderSeq = 0;
        for (NcScheduleResult r : ctx.getScheduleResults()) {
            if (r.getOrderNo() != null && r.getOrderNo().endsWith("-")) {
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
        String orderNo = iNcOrderGeneratorService.generateOrderNo(batchNoFromExisting, maxOrderSeq);
        insertVO.setOrderNo(orderNo);
        insertVO.setBatchNo(batchNoFromExisting);
        insertVO.setDataSource(NcEngineConstants.DATA_SOURCE_INSERT); // "1"=插单
        insertVO.setReleaseStatus(ApsConstant.NO_RELEASE);
        insertVO.setPublishSuccessCount(0);
        insertVO.setFactoryCode(factoryCode);
        insertVO.setScheduleDate(scheduleDate);

        // 加载施工表数据，填充胶料等字段
        MdmConstructionInfo construction = loadConstructionByPadding(factoryCode, insertVO.getLiningCode());
        if (construction != null) {
            if (StringUtils.isBlank(insertVO.getLiningName())) {
                insertVO.setLiningName(construction.getInsideName());
            }
            if (StringUtils.isBlank(insertVO.getGlueCode())) {
                insertVO.setGlueCode(construction.getPaddingRubber());
            }
        }

        // 加载 T-1 日库存数据
        BigDecimal stockQty = loadPaddingStock(factoryCode, scheduleDate, insertVO.getLiningCode());
        insertVO.setStockQty(BigDecimalUtils.valueOf(stockQty));

        // 开产班次：优先取第一组的首班班次（保证class映射与排产日期计算一致）；
        // 无分组信息时兼容旧逻辑，取当前排产日其余记录的值
        if (StringUtils.isNotBlank(firstGroup.getScheduleShiftClass())) {
            insertVO.setScheduleShiftClass(firstGroup.getScheduleShiftClass());
        } else {
            for (NcScheduleResult r : ctx.getScheduleResults()) {
                if (StringUtils.isNotBlank(r.getScheduleShiftClass())) {
                    insertVO.setScheduleShiftClass(r.getScheduleShiftClass());
                    break;
                }
            }
        }

        // 收尾标记默认 0（否）
        insertVO.setTailFlag("0");

        // 第一组记录副本：保留 insertVO 原始输入位置数据（后续组仍按输入位置取值），
        // 副本按 positionClassMap 将输入位置数据迁移到输出 class 位置
        NcScheduleResult firstRecord = new NcScheduleResult();
        BeanUtils.copyProperties(insertVO, firstRecord);
        this.fillRecordClassData(firstRecord, firstGroup, insertVO);

        // 获取当前排程结果（深拷贝）
        List<NcScheduleResult> currentResults = new ArrayList<>(ctx.getScheduleResults());

        // 2.4.2+2.4.3+2.4.4：对第一组的目标输出class执行顺延
        // 顺延引擎按排程记录中的 class 位置处理，因此传入记录位置 recordClass（而非输入位置 targetClass）
        ShiftContext shiftCtx = new ShiftContext().setFactoryCode(factoryCode).setScheduleDate(scheduleDate)
                .setMachineCode(machineCode).setTargetClass(targetClass).setTargetSeq(targetSeq)
                .setInsertSpecName(specName).setInsertPlanQty(getPlanQtyByClass(insertVO, targetClass))
                .setScheduleResults(currentResults).setOperType("insert");

        List<NcScheduleResult> updatedResults = iNcScheduleShiftEngineService.processInsertAndCascade(shiftCtx);

        // 2.5：保存数据
        // 先保存第一组的新插单记录
        NcScheduleResultMapper.insert(firstRecord);

        // 更新被顺延的记录
        for (NcScheduleResult updated : updatedResults) {
            if (updated.getId() != null) {
                NcScheduleResultMapper.updateById(updated);
            }
        }

        // ====== 后续组：直接插入新记录（无需顺延） ======
        if (dateGroups.size() > 1) {
            for (int i = 1; i < dateGroups.size(); i++) {
                ScheduleDateGroup group = dateGroups.get(i);
                NcScheduleResult groupRecord = new NcScheduleResult();
                BeanUtils.copyProperties(insertVO, groupRecord);
                groupRecord.setId(null); // 新记录
                groupRecord.setScheduleDate(group.getScheduleDate());
                groupRecord.setScheduleShiftClass(group.getScheduleShiftClass());

                // 为后续组单独生成工单号（基于该排产日已有记录）
                List<NcScheduleResult> groupResults = NcScheduleResultMapper
                        .selectList(new LambdaQueryWrapper<NcScheduleResult>()
                                .eq(NcScheduleResult::getFactoryCode, factoryCode)
                                .eq(NcScheduleResult::getScheduleDate, group.getScheduleDate()));
                String groupBatchNo = "";
                for (NcScheduleResult r : groupResults) {
                    if (StringUtils.isNotBlank(r.getBatchNo())) {
                        groupBatchNo = r.getBatchNo();
                        break;
                    }
                }
                int groupMaxOrderSeq = 0;
                for (NcScheduleResult r : groupResults) {
                    if (r.getOrderNo() != null && r.getOrderNo().endsWith("-")) {
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
                groupRecord.setOrderNo(iNcOrderGeneratorService.generateOrderNo(groupBatchNo, groupMaxOrderSeq));

                // 按 positionClassMap 将输入位置数据迁移到该组记录的输出 class 位置
                this.fillRecordClassData(groupRecord, group, insertVO);

                NcScheduleResultMapper.insert(groupRecord);
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
    public AjaxResult changeQty(NcScheduleResult adjustVO) {
        Date scheduleDate = adjustVO.getScheduleDate();
        String factoryCode = adjustVO.getFactoryCode();
        String machineCode = adjustVO.getMachineCode();

        NcAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 校验发布状态
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, new Long[] { adjustVO.getId() });
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 从已加载的排程结果中获取原始记录（确保与 ctx.getScheduleResults() 中为同一对象引用，
        // 避免后续 processInsertAndCascade 使用不同的实例导致数据不一致）
        NcScheduleResult original = ctx.getScheduleResults().stream()
                .filter(r -> r.getId().equals(adjustVO.getId()))
                .findFirst().orElse(null);
        if (original == null) {
            log.info("调量记录未找到（不在当前排程结果中），id={}", adjustVO.getId());
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.not.found"));
        }

        // 确定目标班次，获取新旧计划量
        int targetClass = resolveTargetClass(adjustVO);
        BigDecimal oldPlanQty = iNcScheduleShiftEngineService.getPlanQtyByIndex(original, targetClass);
        BigDecimal newPlanQty = getPlanQtyByClass(adjustVO, targetClass);

        // 新计划量为空或≤0 → 清空该班次
        if (newPlanQty == null || newPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            return this.internalChangeQtyDecrease(original, targetClass, BigDecimal.ZERO);
        }

        // 原班次无计划量 → 走插单逻辑
        if (oldPlanQty == null || oldPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            NcScheduleResult insertVO = new NcScheduleResult();
            BeanUtils.copyProperties(adjustVO, insertVO);
            insertVO.setLiningCode(original.getLiningCode());
            insertVO.setLiningName(original.getLiningName());
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
    public AjaxResult changeQtyValidate(NcScheduleResult adjustVO) {
        Date scheduleDate = adjustVO.getScheduleDate();
        String factoryCode = adjustVO.getFactoryCode();
        String machineCode = adjustVO.getMachineCode();

        NcAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 校验发布状态
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, new Long[]{adjustVO.getId()});
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 获取原始记录
        NcScheduleResult original = ctx.getScheduleResults().stream()
                .filter(r -> r.getId().equals(adjustVO.getId()))
                .findFirst().orElse(null);
        if (original == null) {
            log.info("调量记录未找到（不在当前排程结果中），id={}", adjustVO.getId());
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.not.found"));
        }

        // 确定目标班次，获取新旧计划量
        int targetClass = resolveTargetClass(adjustVO);
        BigDecimal oldPlanQty = iNcScheduleShiftEngineService.getPlanQtyByIndex(original, targetClass);
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
        int originSeq = iNcScheduleShiftEngineService.getSequenceByIndex(original, targetClass);

        CapacityValidateResult capacityResult = iNcScheduleShiftEngineService.validateCapacity(machineCode,
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
    private AjaxResult internalChangeQtyIncrease(NcScheduleResult original, int targetClass,
                                                  BigDecimal deltaQty, NcAdjustScheduleContext ctx) {
        String machineCode = original.getMachineCode();
        BigDecimal existingPlanQty = iNcScheduleShiftEngineService.getPlanQtyByIndex(original, targetClass);

        // 直接累加，不改变顺位
        BigDecimal newTotal = existingPlanQty.add(BigDecimalUtils.valueOf(deltaQty));
        iNcScheduleShiftEngineService.setPlanQtyByIndex(original, targetClass, newTotal);

        // 记录原因分析（直接覆盖，不保留之前的过程）
        String record = MessageFormat.format(I18nUtil.getMessage("ui.data.column.scheduleResult.analysis.adjust.increase"),
                original.getLiningName(), deltaQty);
        iNcScheduleShiftEngineService.setAnalysisByIndex(original, targetClass, record);

        // 产能校验（三档判断）
        int originSeq = iNcScheduleShiftEngineService.getSequenceByIndex(original, targetClass);
        CapacityValidateResult capResult = iNcScheduleShiftEngineService.validateCapacity(machineCode, targetClass,
                originSeq, deltaQty, ctx.getScheduleResults(), ctx.getFactoryCode(), ctx.getScheduleDate());
        List<NcScheduleResult> updateList = new ArrayList<>();
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
                    .setTargetSeq(iNcScheduleShiftEngineService.getSequenceByIndex(original, targetClass))
                    .setInsertSpecName(original.getLiningName()).setInsertPlanQty(newTotal)
                    .setScheduleResults(ctx.getScheduleResults()).setOperType("adjust");
            List<NcScheduleResult> updated = iNcScheduleShiftEngineService.processInsertAndCascade(shiftCtx);
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
    private AjaxResult internalChangeQtyDecrease(NcScheduleResult original, int targetClass,
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
            iNcScheduleShiftEngineService.setPlanQtyByIndex(original, targetClass, null);
            iNcScheduleShiftEngineService.setSequenceByIndex(original, targetClass, null);
            // 顺位空洞整理
            iNcScheduleShiftEngineService.reorganizeAfterReduce(Collections.singletonList(original), targetClass);
        } else {
            iNcScheduleShiftEngineService.setPlanQtyByIndex(original, targetClass, newPlanQty);
        }

        updateReleaseStatusAfterAdjust(original);
        NcScheduleResultMapper.updateById(original);

        this.recordDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, original, null, original);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
    }

    /**
     * 3.5 转机台
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult changeMachine(NcScheduleResult adjustVO) {
        NcScheduleResult original = NcScheduleResultMapper.selectById(adjustVO.getId());
        if (original == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.data.not.found"));
        }

        String targetMachineCode = adjustVO.getMachineCode();
        String factoryCode = adjustVO.getFactoryCode() != null ? adjustVO.getFactoryCode() : original.getFactoryCode();

        // 校验目标机台是否存在且启用
        NcMachineInfo targetMachine = djMachineInfoService.selectMachineInfoList(new NcMachineInfo()).stream()
                .filter(m -> targetMachineCode.equals(m.getMachineCode()) && "1".equals(m.getStatus())).findFirst()
                .orElse(null);
        if (targetMachine == null) {
            log.info("目标机台不存在或已禁用，machineCode={}", targetMachineCode);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machine.not.exist"));
        }

        // 校验定点机台
        List<NcSpecifyMachine> specifyList = djSpecifyMachineMapper
                .selectList(new LambdaQueryWrapper<NcSpecifyMachine>().eq(NcSpecifyMachine::getFactoryCode, factoryCode)
                        .eq(NcSpecifyMachine::getMachineCode, targetMachineCode).eq(NcSpecifyMachine::getJobType, "1")); // 不可作业
        boolean isJobDenied = specifyList.stream().anyMatch(s -> original.getLiningCode().equals(s.getLiningCode()));
        if (isJobDenied) {
            log.info("定点机台限制：规格{}不允许在机台{}生产", original.getLiningCode(), targetMachineCode);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machine.job.denied"));
        }

        // 记录原机台到操作日志
        String oldMachineCode = original.getMachineCode();
        original.setMachineCode(targetMachineCode);
        updateReleaseStatusAfterAdjust(original);
        NcScheduleResultMapper.updateById(original);

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
        List<NcScheduleResult> records = NcScheduleResultMapper.selectBatchIds(idList);

        if (CollectionUtils.isEmpty(records)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.operation.success"));
        }

        List<Long> canDeleteIds = new ArrayList<>();
        List<String> cannotDeleteReasons = new ArrayList<>();

        for (NcScheduleResult record : records) {
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
            List<NcScheduleResult> deletedRecords = records.stream()
                    .filter(r -> canDeleteIds.contains(r.getId()))
                    .collect(Collectors.toList());

            // 逻辑删除
            NcScheduleResultMapper.deleteBatchIds(canDeleteIds);

            // 生产顺位补位：遍历所有班次索引（1~6），对有顺位的班次执行顺位前移
            for (int classIdx = 1; classIdx <= NcEngineConstants.SHIFT_COUNT; classIdx++) {
                final int idx = classIdx;
                List<NcScheduleResult> recordsWithSeq = deletedRecords.stream()
                        .filter(r -> {
                            Integer seq = this.getSeqByClass(r, idx);
                            return seq != null && seq > 0;
                        })
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(recordsWithSeq)) {
                    this.fixSequenceAfterDelete(recordsWithSeq, idx);
                }
            }

            // 记录操作日志
            NcScheduleResult firstRecord = deletedRecords.get(0);
            String recordFactoryCode = firstRecord.getFactoryCode();
            Date recordScheduleDate = firstRecord.getScheduleDate();

            for (Long deleteId : canDeleteIds) {
                NcDispatcherLog logEntry = new NcDispatcherLog();
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
     * @param deletedGroup 被删除的记录列表（需包含在该班次索引有顺位的记录）
     * @param classIndex   班次索引（1~6）
     */
    private void fixSequenceAfterDelete(List<NcScheduleResult> deletedGroup, int classIndex) {
        NcScheduleResult first = deletedGroup.get(0);

        // 收集该班次索引下所有被删除的顺位
        List<Integer> deletedSequences = deletedGroup.stream()
                .map(r -> this.getSeqByClass(r, classIndex))
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (deletedSequences.isEmpty()) {
            return;
        }

        // 查询同一机台排产日下的所有剩余记录（框架自动过滤已删除数据）
        List<NcScheduleResult> sameMachineRecords = NcScheduleResultMapper.selectList(
                new LambdaQueryWrapper<NcScheduleResult>()
                        .eq(NcScheduleResult::getFactoryCode, first.getFactoryCode())
                        .eq(NcScheduleResult::getScheduleDate, first.getScheduleDate())
                        .eq(NcScheduleResult::getMachineCode, first.getMachineCode()));

        // 前移顺位：每个剩余记录在该班次索引的顺位减去其之前被删除的记录数
        for (NcScheduleResult rec : sameMachineRecords) {
            Integer seq = this.getSeqByClass(rec, classIndex);
            if (seq == null) {
                continue;
            }
            long offset = deletedSequences.stream().filter(ds -> ds < seq).count();
            if (offset > 0) {
                this.setSeqByClass(rec, classIndex, seq - (int) offset);
                NcScheduleResultMapper.updateById(rec);
            }
        }
    }

    // ==================== 5. 发布 ====================

    /**
     * 5. 发布
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:AUTO_LOCK:#factoryCode:#scheduleDate")
    public AjaxResult publish(NcScheduleResult publishVO) {
        Long[] ids = publishVO.getIds();
        Date scheduleDate = publishVO.getScheduleDate();
        String factoryCode = publishVO.getFactoryCode();

        if (ids == null || ids.length == 0) {
            log.info("发布参数错误：ids为空");
            return AjaxResult.error(I18nUtil.getMessage("ui.message.parameter.error"));
        }

        // 校验：过滤出可发布的记录
        List<NcScheduleResult> records = NcScheduleResultMapper.selectBatchIds(Arrays.asList(ids));
        List<NcScheduleResult> publishable = records.stream()
                .filter(r -> ApsConstant.NO_RELEASE.equals(r.getReleaseStatus())
                        || ApsConstant.FAILURE_RELEASE.equals(r.getReleaseStatus())
                        || ApsConstant.WAIT_RELEASING.equals(r.getReleaseStatus()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(publishable)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.no.publishable"));
        }

        // 更新状态为"发布中"
        for (NcScheduleResult r : publishable) {
            r.setReleaseStatus(ApsConstant.RELEASING);
            NcScheduleResultMapper.updateById(r);
        }

        try {
            // 调用 MES 发布（将排程数据推到中间库）
            boolean deploySuccess = deployToMes(publishable);

            if (deploySuccess) {
                // 发布成功
                for (NcScheduleResult r : publishable) {
                    r.setReleaseStatus(ApsConstant.IS_RELEASE);
                    r.setPublishSuccessCount((r.getPublishSuccessCount() == null ? 0 : r.getPublishSuccessCount()) + 1);
                    r.setNewestPublishTime(new Date());
                    NcScheduleResultMapper.updateById(r);
                }
                // 记录发布记录
                savePublishRecord(factoryCode, scheduleDate, ApsConstant.IS_RELEASE);
                return AjaxResult.success(I18nUtil.getMessage("ui.message.publish.success"));
            } else {
                // 发布失败
                for (NcScheduleResult r : publishable) {
                    r.setReleaseStatus(ApsConstant.FAILURE_RELEASE);
                    NcScheduleResultMapper.updateById(r);
                }
                savePublishRecord(factoryCode, scheduleDate, ApsConstant.FAILURE_RELEASE);
                log.info("MES发布失败，factory={}, date={}", factoryCode, scheduleDate);
                return AjaxResult.error(I18nUtil.getMessage("ui.message.publish.fail"));
            }
        } catch (Exception e) {
            log.error("发布异常", e);
            for (NcScheduleResult r : publishable) {
                r.setReleaseStatus(ApsConstant.FAILURE_RELEASE);
                NcScheduleResultMapper.updateById(r);
            }
            savePublishRecord(factoryCode, scheduleDate, ApsConstant.FAILURE_RELEASE);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.publish.fail"));
        }
    }

    // ==================== 6. 导入 ====================

    /**
     * 6. 导入数校验
     */
    public AjaxResult validateImportData(List<NcScheduleResult> importList, Date scheduleDate, String factoryCode) {
        // 6.1 校验：排产日期不能早于当前日期
        if (scheduleDate != null && DateUtil.compare(scheduleDate, new Date()) < 0) {
            log.info("导入日期校验失败，scheduleDate={}", DateUtil.format(scheduleDate, DatePattern.NORM_DATE_PATTERN));
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.date.before.today"));
        }

        // 校验存在发布中或超时失败记录
        int lockedCount = NcScheduleResultService.isReleasingOrTimeoutByDate(scheduleDate);
        if (lockedCount > 0) {
            log.info("导入校验：存在发布中或超时失败记录，lockedCount={}", lockedCount);
            return AjaxResult
                    .error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }

        NcAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        // 6.2 分离已有记录（合并班次数据后批量保存）和新增记录（走插单逻辑）
        int successCount = 0;
        int failCount = 0;
        List<String> errorMsgs = new ArrayList<>();
        List<NcScheduleResult> batchList = new ArrayList<>();
        List<NcScheduleResult> newItemList = new ArrayList<>();

        for (NcScheduleResult item : importList) {
            String orderNo = item.getOrderNo();
            if (StringUtils.isNotBlank(orderNo)) {
                // 工单号存在 → 查系统内已有记录
                List<NcScheduleResult> existing = NcScheduleResultMapper
                        .selectList(new LambdaQueryWrapper<NcScheduleResult>().eq(NcScheduleResult::getOrderNo, orderNo));
                if (CollectionUtils.isNotEmpty(existing)) {
                    // 视为调整操作，合并班次数据后加入批量保存列表
                    NcScheduleResult target = existing.get(0);
                    mergeClassData(target, item);
                    updateReleaseStatusAfterAdjust(target);
                    batchList.add(target);
                    continue;
                }
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
            NcScheduleResult item = newItemList.get(i);
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
    private void updateReleaseStatusAfterAdjust(NcScheduleResult entity) {
        if (ApsConstant.IS_RELEASE.equals(entity.getReleaseStatus())) {
            entity.setReleaseStatus(ApsConstant.WAIT_RELEASING);
        }
    }



    /**
     * 记录操作日志到 T_DJ_DISPATCHER_LOG
     */
    private void recordDispatcherLog(String operType, NcScheduleResult newSchedule, List<NcScheduleResult> oldResults,
            NcScheduleResult insertVO) {
        try {
            NcDispatcherLog logEntry = new NcDispatcherLog();
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
    private int resolveTargetClass(NcScheduleResult vo) {
        for (int c = 1; c <= NcEngineConstants.SHIFT_COUNT; c++) {
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
    private int resolveTargetSequence(NcScheduleResult vo, int targetClass) {
        Integer seq = getSeqByClass(vo, targetClass);
        return seq != null ? seq : 1;
    }

    /**
     * 将输入班次位置转换为第一组排程记录中的输出class位置
     * <p>
     * 多班次插单时，组内第N个有量位置映射到该组记录的classN（该组记录的首班班次即第一个有量位置的班次），
     * 无映射时直接使用原位置（兼容旧逻辑）。
     * </p>
     *
     * @param firstGroup  第一组排产日期分组
     * @param targetClass 输入班次位置（1~6）
     * @return 输出class位置（1~6）
     */
    private int resolveRecordClass(ScheduleDateGroup firstGroup, int targetClass) {
        Map<Integer, Integer> classMap = firstGroup.getPositionClassMap();
        Integer outputClass = classMap != null ? classMap.get(targetClass) : null;
        return outputClass != null ? outputClass : targetClass;
    }

    /**
     * 插单排产日期组 — 班次粒度计算，支持跨排产日拆分
     * <p>
     * 对 position=1~lastClass 逐位计算独立排产日期，将有量班次按排产日期分组返回。
     * </p>
     * <p>
     * 排产日期变化条件（每个班次独立判断，累积偏移）：
     * 当遇到跨天班次（crossDayFlag=1）时，后续位置排产日+1。
     * </p>
     * <p>
     * 组内映射规则：第N个有量位置映射到该组记录的classN，
     * 该组的首班班次（scheduleShiftClass）为组内第一个有量位置的班次。
     * </p>
     *
     * @param insertVO 插单参数
     * @return 排产日期分组列表（按日期升序）
     */
    private List<ScheduleDateGroup> calculateInsertScheduleDateGroups(NcScheduleResult insertVO) {
        String startShiftClass = insertVO.getScheduleShiftClass();
        if (StringUtils.isBlank(startShiftClass)) {
            // 无首班班次时直接使用前端传入的排产日期（兼容旧逻辑如调量转插单）
            return Collections.singletonList(
                    buildSingleDateGroup(insertVO, insertVO.getScheduleDate(), insertVO.getScheduleShiftClass()));
        }

        int lastClass = resolveLastClass(insertVO);

        // 查询活动班次配置
        List<NcShiftConfig> activeShifts = djShiftConfigService.listActiveShifts();
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

        // 计算各位置的排产日期偏移
        int totalShifts = activeShifts.size();
        List<Integer> dateOffsetPerPosition = new ArrayList<>(lastClass);
        int dateOffset = 0;
        for (int position = 1; position <= lastClass; position++) {
            NcShiftConfig shift = activeShifts.get((startIndex + position - 1) % totalShifts);
            // 日期偏移：跨天班次（crossDayFlag=1）时排产日+1（首位置不判断）
            if (position > 1 && ApsConstant.TRUE.equals(shift.getCrossDayFlag())) {
                dateOffset++;
            }
            dateOffsetPerPosition.add(dateOffset);
        }

        // 按排产日期偏移分组
        Map<Integer, ScheduleDateGroup> dateGroupMap = new LinkedHashMap<>();
        for (int position = 1; position <= lastClass; position++) {
            if (!hasPlanQty(insertVO, position)) {
                continue;
            }
            int offset = dateOffsetPerPosition.get(position - 1);

            dateGroupMap.computeIfAbsent(offset, k -> {
                ScheduleDateGroup g = new ScheduleDateGroup();
                g.setScheduleDate(Date.from(
                        serverProductionDate.plusDays(k).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                return g;
            });

            ScheduleDateGroup g = dateGroupMap.get(offset);
            g.getPositions().add(position);
            g.getPositionDates().put(position, g.getScheduleDate());

            // 组内第N个有量位置 → 输出class N
            g.getPositionClassMap().put(position, g.getPositions().size());

            // 第一个遇到该组的班次设为首班班次（即组内第一个有量位置的班次）
            if (g.getScheduleShiftClass() == null) {
                g.setScheduleShiftClass(activeShifts.get((startIndex + position - 1) % totalShifts).getShiftCode());
            }
        }

        return new ArrayList<>(dateGroupMap.values());
    }

    /**
     * 计算服务器当前时间所在的排产日
     */
    private LocalDate calculateServerProductionDate(List<NcShiftConfig> activeShifts) {
        LocalTime now = LocalTime.now();
        LocalDate serverDate = LocalDate.now();
        LocalDate serverProductionDate = serverDate;
        for (NcShiftConfig config : activeShifts) {
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
     * 在活动班次列表中查找指定班次编码的索引
     */
    private int findShiftIndex(List<NcShiftConfig> activeShifts, String shiftCode) {
        for (int i = 0; i < activeShifts.size(); i++) {
            if (activeShifts.get(i).getShiftCode().equals(shiftCode)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 构建单组排产日分组（无跨天/无班次配置时的保底逻辑）
     */
    private ScheduleDateGroup buildSingleDateGroup(NcScheduleResult insertVO, Date scheduleDate, String shiftClass) {
        ScheduleDateGroup group = new ScheduleDateGroup();
        group.setScheduleDate(scheduleDate);
        group.setScheduleShiftClass(shiftClass);
        for (int p = 1; p <= NcEngineConstants.SHIFT_COUNT; p++) {
            if (hasPlanQty(insertVO, p)) {
                group.getPositions().add(p);
                group.getPositionDates().put(p, scheduleDate);
            }
        }
        return group;
    }

    /**
     * 判断指定班次位置是否有计划量
     */
    private boolean hasPlanQty(NcScheduleResult vo, int position) {
        BigDecimal qty = getPlanQtyByClass(vo, position);
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 将排产日期分组内的输入班次数据迁移到输出记录对应的 class 位置
     * <p>
     * 先清空记录的全部 class 位置，再按分组的 positionClassMap（输入位置 → 输出class位置）
     * 从源对象（保留输入位置数据的原始插单参数）取值填充，无映射时直接使用原位置（兼容旧逻辑）。
     * </p>
     *
     * @param record   目标排程记录（输出 class 数据写入该对象）
     * @param group    排产日期分组（含 positionClassMap）
     * @param sourceVO 源插单参数（保留输入位置数据，不可被清空）
     */
    private void fillRecordClassData(NcScheduleResult record, ScheduleDateGroup group, NcScheduleResult sourceVO) {
        // 先清除所有class位置，只保留该组映射到的class
        for (int c = 1; c <= NcEngineConstants.SHIFT_COUNT; c++) {
            setPlanQtyByClass(record, c, null);
            setSeqByClass(record, c, null);
        }

        // 根据positionClassMap填充该组班次的计划量和顺位
        Map<Integer, Integer> groupClassMap = group.getPositionClassMap();
        boolean hasMapping = groupClassMap != null && !groupClassMap.isEmpty();
        for (int origPos : group.getPositions()) {
            Integer outputClass = hasMapping ? groupClassMap.get(origPos) : null;
            if (outputClass == null) {
                outputClass = origPos; // 无映射时直接使用原位置（兼容旧逻辑）
            }
            BigDecimal qty = getPlanQtyByClass(sourceVO, origPos);
            Integer seq = getSeqByClass(sourceVO, origPos);
            if (qty != null) {
                setPlanQtyByClass(record, outputClass, qty);
            }
            if (seq != null) {
                setSeqByClass(record, outputClass, seq);
            }
        }
    }

    /**
     * 获取最后一个有计划量的班次位置
     */
    private int resolveLastClass(NcScheduleResult vo) {
        for (int c = NcEngineConstants.SHIFT_COUNT; c >= 1; c--) {
            BigDecimal planQty = getPlanQtyByClass(vo, c);
            if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                return c;
            }
        }
        return 1;
    }

    /**
     * 根据内衬编码查询施工信息
     * <p>
     * 从 T_MDM_CONSTRUCTION_INFO 表中查询对应内衬编码的施工数据，
     * 用于填充插单记录的胶料代码、物料名称等字段。
     * </p>
     *
     * @param factoryCode 工厂编码
     * @param liningCode 内衬编码
     * @return 施工信息，未查到返回 null
     */
    private MdmConstructionInfo loadConstructionByPadding(String factoryCode, String liningCode) {
        if (StringUtils.isBlank(liningCode)) {
            return null;
        }
        List<MdmConstructionInfo> list = djEngineConstructionInfoMapper.selectList(
                new LambdaQueryWrapper<MdmConstructionInfo>()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .eq(MdmConstructionInfo::getInsideCode, liningCode)
                        .last("LIMIT 1"));
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * 加载内衬 T-1 日库存
     * <p>
     * 从 T_DJ_STOCK 表中查询对应内衬编码在排产日前一天的库存量，
     * 有效库存 = 库存量 + 修正数量 - 不良数量。
     * </p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排产日期
     * @param liningCode  内衬编码
     * @return 有效库存量，查不到返回 null
     */
    private BigDecimal loadPaddingStock(String factoryCode, Date scheduleDate, String liningCode) {
        if (StringUtils.isBlank(liningCode)) {
            return null;
        }
        List<NcStock> stockList = djEngineStockMapper.selectList(new LambdaQueryWrapper<NcStock>()
                .eq(NcStock::getFactoryCode, factoryCode)
                .eq(NcStock::getStockDate, DateUtil.offsetDay(scheduleDate, -1))
                .eq(NcStock::getMaterialCode, liningCode)
                .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(stockList)) {
            return null;
        }
        NcStock stock = stockList.get(0);
        return BigDecimalUtils.valueOf(stock.getStockNum())
                .add(BigDecimalUtils.valueOf(stock.getModifyNum()))
                .subtract(BigDecimalUtils.valueOf(stock.getBadNum()));
    }

    /**
     * 入参校验
     */
    private AjaxResult validateInsertParams(NcScheduleResult vo, NcAdjustScheduleContext ctx) {
        if (vo.getScheduleDate() == null) {
            return AjaxResult
                    .error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "scheduleDate"));
        }
        if (StringUtils.isBlank(vo.getMachineCode())) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "machineCode"));
        }
        if (StringUtils.isBlank(vo.getLiningCode())) {
            return AjaxResult.error(MessageFormat.format(I18nUtil.getMessage("ui.message.parameter.required"), "liningCode"));
        }
        // 校验至少一个班次有计划量
        boolean hasPlanQty = false;
        for (int c = 1; c <= NcEngineConstants.SHIFT_COUNT; c++) {
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
    private BigDecimal getPlanQtyByClass(NcScheduleResult sr, int classIndex) {
        return iNcScheduleShiftEngineService.getPlanQtyByIndex(sr, classIndex);
    }

    /**
     * 设置排程结果中某班次的计划量
     */
    private void setPlanQtyByClass(NcScheduleResult sr, int classIndex, BigDecimal qty) {
        iNcScheduleShiftEngineService.setPlanQtyByIndex(sr, classIndex, qty);
    }

    /**
     * 获取排程结果中某班次的顺位
     */
    private Integer getSeqByClass(NcScheduleResult sr, int classIndex) {
        return iNcScheduleShiftEngineService.getSequenceByIndex(sr, classIndex);
    }

    /**
     * 设置排程结果中某班次的顺位
     */
    private void setSeqByClass(NcScheduleResult sr, int classIndex, Integer seq) {
        iNcScheduleShiftEngineService.setSequenceByIndex(sr, classIndex, seq);
    }

    /**
     * 获取实际完成量
     */
    private BigDecimal getActualFinishQty(NcScheduleResult sr) {
        List<NcDayFinishQty> finishList = djDayFinishQtyMapper
                .selectList(new LambdaQueryWrapper<NcDayFinishQty>().eq(NcDayFinishQty::getOrderNo, sr.getOrderNo()));
        if (CollectionUtils.isNotEmpty(finishList)) {
            NcDayFinishQty finish = finishList.get(0);
            return BigDecimalUtils.valueOf(finish.getNightFinishQty())
                    .add(BigDecimalUtils.valueOf(finish.getDayFinishQty()))
                    .add(BigDecimalUtils.valueOf(finish.getMidFinishQty()));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 合并导入数据到已有记录
     */
    private void mergeClassData(NcScheduleResult target, NcScheduleResult source) {
        for (int c = 1; c <= NcEngineConstants.SHIFT_COUNT; c++) {
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
    private boolean deployToMes(List<NcScheduleResult> records) {
        // TODO: 调用 MES 中间库发布接口
        // deployNcScheduleToMid(records);
        log.info("发布内衬排程到MES：{}条记录", records.size());
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

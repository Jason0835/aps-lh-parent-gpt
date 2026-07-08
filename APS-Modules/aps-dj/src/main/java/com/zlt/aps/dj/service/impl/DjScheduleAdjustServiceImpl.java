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
import java.util.List;
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

        // 根据首班班次计算实际排产日期（取代前端直接传入的 scheduleDate）
        Date scheduleDate = this.calculateInsertScheduleDate(insertVO);
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

        // 确定目标班次和顺位
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
            return executeInsertInternal(insertVO, targetClass, targetSeq, ctx);
        }

        // 第三档：插单量 > 实际剩余产能（定额 - 已生产量），超当班剩余产能，拒绝插单
        if (!capacityResult.isPassed()) {
            log.warn("插单量 {} 超出实际剩余产能 {}", insertPlanQty, capacityResult.getRemainingCapacity());
            return AjaxResult.error(capacityResult.getErrorMsg());
        }

        // 第二档：插单量 > 剩余产能 但 ≤ 实际剩余产能，需提示用户确认
        String overflowSpecsStr = "";
        if (CollectionUtils.isNotEmpty(capacityResult.getOverflowSpecs())) {
            overflowSpecsStr = String.join(",", capacityResult.getOverflowSpecs());
        }
        log.info("插单产能溢出，受影响规格：{}", overflowSpecsStr);
        return AjaxResult.error("CAPACITY_OVERFLOW:" + overflowSpecsStr);
    }

    /**
     * 插单前置校验（含跨天日期计算）
     * <p>
     * 根据 {@code scheduleShiftClass} 计算实际排产日期，然后执行排产日锁定校验和排程计划存在性校验。
     * </p>
     */
    @Override
    public AjaxResult insertOrderValidate(DjScheduleResult insertVO) {
        // 2.1.1 根据首班班次计算实际排产日期（取代前端直接传入的 scheduleDate）
        Date scheduleDate = this.calculateInsertScheduleDate(insertVO);
        insertVO.setScheduleDate(scheduleDate);

        String factoryCode = insertVO.getFactoryCode();
        String machineCode = insertVO.getMachineCode();

        // 排产日锁定校验
        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, null);
        if (lockedCheck != null) {
            return lockedCheck;
        }

        // 2.1.2 排程计划存在性校验：使用计算后的实际排产日期
        AjaxResult scheduleExistCheck = this.checkScheduleExists(factoryCode, scheduleDate, machineCode);
        if (scheduleExistCheck != null) {
            return scheduleExistCheck;
        }

        return AjaxResult.success();
    }

    /**
     * 确认插单（用户在前端弹窗点击"坚持执行"后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "APS:DJ:SCHEDULE:OPER_LOCK:#factoryCode:#scheduleDate:#machineCode")
    public AjaxResult confirmInsertOrder(DjScheduleResult insertVO) {
        String factoryCode = insertVO.getFactoryCode();
        String machineCode = insertVO.getMachineCode();

        // 根据首班班次计算实际排产日期
        Date scheduleDate = this.calculateInsertScheduleDate(insertVO);
        insertVO.setScheduleDate(scheduleDate);

        DjAdjustScheduleContext ctx = this.loadBaseData(factoryCode, scheduleDate);

        AjaxResult lockedCheck = this.checkScheduleLocked(scheduleDate, null);
        if (lockedCheck != null) {
            return lockedCheck;
        }

        int targetClass = resolveTargetClass(insertVO);
        int targetSeq = resolveTargetSequence(insertVO, targetClass);

        return executeInsertInternal(insertVO, targetClass, targetSeq, ctx);
    }

    /**
     * 插单内部执行
     */
    private AjaxResult executeInsertInternal(DjScheduleResult insertVO, int targetClass, int targetSeq,
            DjAdjustScheduleContext ctx) {
        String factoryCode = ctx.getFactoryCode();
        Date scheduleDate = ctx.getScheduleDate();
        String machineCode = insertVO.getMachineCode();

        // 获取插单规格名称
        String specName = insertVO.getPaddingName();
        if (StringUtils.isBlank(specName)) {
            specName = insertVO.getPaddingCode();
        }

        // 2.4.1：生成工单号
        // 批次号取当前排产日其余记录的值（同一排产日内所有记录批次号一致）
        String batchNoFromExisting = "";
        for (DjScheduleResult r : ctx.getScheduleResults()) {
            if (StringUtils.isNotBlank(r.getBatchNo())) {
                batchNoFromExisting = r.getBatchNo();
                break;
            }
        }
        // 计算当前最大工单流水号
        int maxOrderSeq = 0;
        for (DjScheduleResult r : ctx.getScheduleResults()) {
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
        String orderNo = iDjOrderGeneratorService.generateOrderNo(batchNoFromExisting, maxOrderSeq);
        insertVO.setOrderNo(orderNo);
        insertVO.setBatchNo(batchNoFromExisting);
        insertVO.setDataSource(DjEngineConstants.DATA_SOURCE_INSERT); // "2"=插单
        insertVO.setReleaseStatus(ApsConstant.NO_RELEASE);
        insertVO.setPublishSuccessCount(0);
        insertVO.setFactoryCode(factoryCode);
        insertVO.setScheduleDate(scheduleDate);

        // 加载施工表数据，填充胶料等字段
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

        // 开产班次取当前排产日其余记录的值（同一批数据值都一样）
        for (DjScheduleResult r : ctx.getScheduleResults()) {
            if (StringUtils.isNotBlank(r.getScheduleShiftClass())) {
                insertVO.setScheduleShiftClass(r.getScheduleShiftClass());
                break;
            }
        }

        // 收尾标记默认 0（否）
        insertVO.setTailFlag("0");

        // 确保只有目标班次有计划量
        for (int c = 1; c <= DjEngineConstants.SHIFT_COUNT; c++) {
            if (c == targetClass) {
                setPlanQtyByClass(insertVO, c, getPlanQtyByClass(insertVO, c)); // 需从insertVO提取实际值
                setSeqByClass(insertVO, c, targetSeq);
            } else {
                setPlanQtyByClass(insertVO, c, null);
                setSeqByClass(insertVO, c, null);
            }
        }

        // 获取当前排程结果（深拷贝）
        List<DjScheduleResult> currentResults = new ArrayList<>(ctx.getScheduleResults());

        // 2.4.2+2.4.3+2.4.4：执行顺延
        ShiftContext shiftCtx = new ShiftContext().setFactoryCode(factoryCode).setScheduleDate(scheduleDate)
                .setMachineCode(machineCode).setTargetClass(targetClass).setTargetSeq(targetSeq)
                .setInsertSpecName(specName).setInsertPlanQty(getPlanQtyByClass(insertVO, targetClass))
                .setScheduleResults(currentResults).setOperType("insert");

        List<DjScheduleResult> updatedResults = iDjScheduleShiftEngineService.processInsertAndCascade(shiftCtx);

        // 2.5：保存数据
        // 先保存新插单记录
        djScheduleResultMapper.insert(insertVO);

        // 更新被顺延的记录
        for (DjScheduleResult updated : updatedResults) {
            if (updated.getId() != null) {
                djScheduleResultMapper.updateById(updated);
            }
        }

        // 记录操作日志
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

        // 记录原因分析
        String analysis = iDjScheduleShiftEngineService.getAnalysisByIndex(original, targetClass);
        String record = MessageFormat.format(I18nUtil.getMessage("ui.data.column.scheduleResult.analysis.adjust.increase"),
                original.getPaddingName(), deltaQty);
        iDjScheduleShiftEngineService.setAnalysisByIndex(original, targetClass,
                StringUtils.isNotBlank(analysis) ? analysis + ";" + record : record);

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
            // 从已加载的记录中获取工厂编码和排产日期
            DjScheduleResult firstRecord = records.stream()
                    .filter(r -> canDeleteIds.contains(r.getId()))
                    .findFirst().orElse(null);
            String recordFactoryCode = firstRecord != null ? firstRecord.getFactoryCode() : null;
            Date recordScheduleDate = firstRecord != null ? firstRecord.getScheduleDate() : null;

            // 逻辑删除
            djScheduleResultMapper.deleteBatchIds(canDeleteIds);

            // 记录操作日志
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

        for (DjScheduleResult item : importList) {
            String orderNo = item.getOrderNo();
            if (StringUtils.isNotBlank(orderNo)) {
                // 工单号存在 → 查系统内已有记录
                List<DjScheduleResult> existing = djScheduleResultMapper
                        .selectList(new LambdaQueryWrapper<DjScheduleResult>().eq(DjScheduleResult::getOrderNo, orderNo));
                if (CollectionUtils.isNotEmpty(existing)) {
                    // 视为调整操作，合并班次数据后加入批量保存列表
                    DjScheduleResult target = existing.get(0);
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
        LocalDate calculatedDate = hasCrossDay ? serverProductionDate.plusDays(1) : serverProductionDate;
        log.info("插单排产日期计算：startShiftClass={}, targetClass={}, hasCrossDay={}, serverProductionDate={}, calculatedDate={}",
                startShiftClass, targetClass, hasCrossDay, serverProductionDate, calculatedDate);
        return Date.from(calculatedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
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

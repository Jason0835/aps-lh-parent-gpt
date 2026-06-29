package com.zlt.aps.dj.engine.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.enums.ClassNumThreePlanEnums;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.engine.mapper.DjEngineDayFinishQtyMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineMachineMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineScheduleResultMapper;
import com.zlt.aps.dj.engine.mapper.DjEngineSpecifyMachineMapper;
import com.zlt.aps.dj.engine.model.CapacityValidateResult;
import com.zlt.aps.dj.engine.model.ShiftContext;
import com.zlt.aps.dj.engine.model.ShiftValidateResult;
import com.zlt.aps.dj.engine.service.IDjScheduleShiftEngineService;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 垫胶排程顺延引擎
 * <p>
 * 处理插单/调整增量时的产能校验、同班顺位顺延、跨班顺延、末端减量等核心逻辑。
 * 对应设计文档「2. 插单」和「3.3 调量（增量）处理」。
 * </p>
 *
 * @author zlt
 */
@Component
public class DjScheduleShiftEngineServiceImpl implements IDjScheduleShiftEngineService {

    @Resource
    private DjEngineScheduleResultMapper djEngineScheduleResultMapper;

    @Resource
    private DjEngineMachineMapper djEngineMachineMapper;

    @Resource
    private DjEngineDayFinishQtyMapper djEngineDayFinishQtyMapper;

    @Resource
    private DjEngineSpecifyMachineMapper djEngineSpecifyMachineMapper;

    /**
     * 2.2 约束一校验 — 生产顺位合法性
     *
     * @param factoryCode   工厂编码
     * @param scheduleDate  排产日期
     * @param machineCode   机台编码
     * @param targetClass   目标班次索引（1~6）
     * @param targetSeq     目标顺位
     * @return 是否通过校验
     */
    private boolean validateProductionOrder(String factoryCode, Date scheduleDate,
                                           String machineCode, int targetClass, int targetSeq) {
        // 查询当前机台各班次已完成量 > 0 的规格
        List<DjDayFinishQty> finishQtyList = djEngineDayFinishQtyMapper.selectList(
                new LambdaQueryWrapper<DjDayFinishQty>()
                        .eq(DjDayFinishQty::getFactoryCode, factoryCode)
                        .eq(DjDayFinishQty::getScheduleDate, scheduleDate));

        if (CollectionUtils.isEmpty(finishQtyList)) {
            return true;
        }

        // 查询当前排产日该机台的排程结果，获取已完成规格的班次顺位信息
        List<DjScheduleResult> machineResults = djEngineScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>()
                        .eq(DjScheduleResult::getFactoryCode, factoryCode)
                        .eq(DjScheduleResult::getScheduleDate, scheduleDate)
                        .eq(DjScheduleResult::getMachineCode, machineCode));

        if (CollectionUtils.isEmpty(machineResults)) {
            return true;
        }

        Set<String> finishedOrderNos = finishQtyList.stream()
                .map(DjDayFinishQty::getOrderNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (DjScheduleResult result : machineResults) {
            if (result.getOrderNo() != null && finishedOrderNos.contains(result.getOrderNo())) {
                // 该规格有完成量，检查其生产班次和顺位
                for (int c = 1; c <= 6; c++) {
                    BigDecimal planQty = getPlanQtyByIndex(result, c);
                    if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                        // 验证：如果已完成规格的班次比目标班次早，或同班次但顺位更早
                        if (c < targetClass) {
                            return false; // 已完成规格在更早班次，不允许在更早顺位插单
                        } else if (c == targetClass) {
                            Integer seq = getSequenceByIndex(result, c);
                            if (seq != null && seq < targetSeq) {
                                return false; // 同班次但已完成规格的顺位更早
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * 2.2 约束一校验（简化版 — 通过ORDER_NO从完成量表获取）
     * 查询目标机台目标排产日下所有有完成量的规格，获取其最早生产班次和顺位
     *
     * @return 是否能插单到指定班次和顺位
     */
    @Override
    public ShiftValidateResult validateInsertConstraint(String factoryCode, Date scheduleDate,
                                                        String machineCode, int targetClass, int targetSeq) {
        ShiftValidateResult result = new ShiftValidateResult();
        result.setPassed(true);

        // 1. 查询完成量表中该排产日 + 机台的记录
        List<DjDayFinishQty> finishQtyList = djEngineDayFinishQtyMapper.selectList(
                new LambdaQueryWrapper<DjDayFinishQty>()
                        .eq(DjDayFinishQty::getFactoryCode, factoryCode)
                        .eq(DjDayFinishQty::getScheduleDate, scheduleDate));

        if (CollectionUtils.isEmpty(finishQtyList)) {
            return result; // 无完成量记录，不限制
        }

        // 查询当前排产日该机台的排程结果，获取已完成规格的班次顺位信息
        List<DjScheduleResult> machineResults = djEngineScheduleResultMapper.selectList(
                new LambdaQueryWrapper<DjScheduleResult>()
                        .eq(DjScheduleResult::getFactoryCode, factoryCode)
                        .eq(DjScheduleResult::getScheduleDate, scheduleDate)
                        .eq(DjScheduleResult::getMachineCode, machineCode));

        if (CollectionUtils.isEmpty(machineResults)) {
            return result;
        }

        Set<String> finishedOrderNos = finishQtyList.stream()
                .map(DjDayFinishQty::getOrderNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 遍历排程结果，找到最早的有完成量的规格
        int earliestClass = 7; // 超出范围
        int earliestSeq = 999;

        for (DjScheduleResult sr : machineResults) {
            if (sr.getOrderNo() == null || !finishedOrderNos.contains(sr.getOrderNo())) {
                continue;
            }
            for (int c = 1; c <= 6; c++) {
                BigDecimal planQty = getPlanQtyByIndex(sr, c);
                if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                    Integer seq = getSequenceByIndex(sr, c);
                    if (seq == null) {
                        continue;
                    }
                    if (c < earliestClass || (c == earliestClass && seq < earliestSeq)) {
                        earliestClass = c;
                        earliestSeq = seq;
                    }
                }
            }
        }

        if (earliestClass > 6) {
            return result; // 没有已完成规格
        }

        // 4. 校验：插单班次必须 >= 已完成规格的班次
        if (targetClass < earliestClass) {
            result.setPassed(false);
            result.setErrorMsg(String.format(I18nUtil.getMessage("ui.data.column.scheduleResult.validate.shift.earlier"), earliestClass));
            return result;
        }
        // 5. 同班次时，插单顺位必须 > 已完成规格的顺位
        if (targetClass == earliestClass && targetSeq <= earliestSeq) {
            result.setPassed(false);
            result.setErrorMsg(String.format(I18nUtil.getMessage("ui.data.column.scheduleResult.validate.seq.earlier"), earliestClass, earliestSeq));
            return result;
        }

        return result;
    }

    /**
     * 2.3 约束二校验 — 产能校验
     *
     * @param machineCode  机台编码
     * @param classIndex   目标班次索引（1~6）
     * @param insertPlanQty 插单计划量
     * @param currentResults 当前排程结果列表
     * @return 产能校验结果（是否通过、溢出规格列表）
     */
    @Override
    public CapacityValidateResult validateCapacity(String machineCode, int classIndex,
                                                   BigDecimal insertPlanQty,
                                                   List<DjScheduleResult> currentResults) {
        CapacityValidateResult result = new CapacityValidateResult();

        // 获取机台定额
        BigDecimal quota = getMachineQuota(machineCode);
        if (quota == null || quota.compareTo(BigDecimal.ZERO) <= 0) {
            result.setPassed(false);
            result.setErrorMsg(I18nUtil.getMessage("ui.data.column.scheduleResult.validate.quota.zero"));
            return result;
        }

        // 计算目标班次已有计划量之和
        BigDecimal currentTotal = BigDecimal.ZERO;
        List<String> overflowSpecs = new ArrayList<>();

        for (DjScheduleResult sr : currentResults) {
            BigDecimal planQty = getPlanQtyByIndex(sr, classIndex);
            if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                currentTotal = currentTotal.add(planQty);
                overflowSpecs.add(sr.getPaddingCode() + "(" + planQty + I18nUtil.getMessage("ui.data.column.scheduleResult.analysis.unit.meter") + ")");
            }
        }

        // 校验：该班次已有计划量 + 插单计划量 ≤ 机台定额
        BigDecimal newTotal = currentTotal.add(BigDecimalUtils.valueOf(insertPlanQty));
        result.setQuota(quota);
        result.setCurrentTotal(currentTotal);
        result.setNewTotal(newTotal);

        if (newTotal.compareTo(quota) <= 0) {
            result.setPassed(true);
            return result;
        }

        // 产能超出，标记受影响的规格
        result.setPassed(false);
        result.setOverflowQty(newTotal.subtract(quota));
        result.setOverflowSpecs(overflowSpecs);
        return result;
    }

    /**
     * 2.4 插入与顺延处理（核心顺延引擎）
     *
     * @param context 顺延上下文
     * @return 所有变更后的排程结果列表
     */
    @Override
    public List<DjScheduleResult> processInsertAndCascade(ShiftContext context) {
        int targetClass = context.getTargetClass();
        int targetSeq = context.getTargetSeq();
        String specName = context.getInsertSpecName();

        // 1. 按机台分组 — 只处理目标机台
        String machineCode = context.getMachineCode();
        List<DjScheduleResult> machineResults = context.getScheduleResults().stream()
                .filter(r -> machineCode.equals(r.getMachineCode()))
                .sorted(Comparator.comparingInt(r -> getSequenceByIndex(r, targetClass) != null
                        ? getSequenceByIndex(r, targetClass) : 999))
                .collect(Collectors.toList());

        // 2. 在目标班次中：原顺位 ≥ 目标顺位的规格向后顺延一位，并记录原因
        for (DjScheduleResult sr : machineResults) {
            Integer seq = getSequenceByIndex(sr, targetClass);
            if (seq != null && seq >= targetSeq) {
                // 记录原因：因XX插单推迟生产次序i->j
                String analysis = getAnalysisByIndex(sr, targetClass);
                String record = String.format(I18nUtil.getMessage("ui.data.column.scheduleResult.analysis.insert.seq.shift"), specName, seq, seq + 1);
                setAnalysisByIndex(sr, targetClass,
                        StringUtils.isNotBlank(analysis) ? analysis + record : record);
                // 顺位 +1
                setSequenceByIndex(sr, targetClass, seq + 1);
            }
        }

        // 3. 从目标班次开始，逐班检查产能溢出
        for (int i = targetClass; i <= 6; i++) {
            BigDecimal quota = getMachineQuota(machineCode);
            if (quota == null) {
                quota = BigDecimal.valueOf(999999);
            }

            // 计算当前班次总计划量
            BigDecimal total = BigDecimal.ZERO;
            // 按顺位排序
            Integer index = i;
            List<DjScheduleResult> sortedBySeq = machineResults.stream()
                    .filter(r -> getSequenceByIndex(r, index) != null)
                    .sorted(Comparator.comparingInt(r -> getSequenceByIndex(r, index)))
                    .collect(Collectors.toList());

            for (DjScheduleResult sr : sortedBySeq) {
                BigDecimal planQty = getPlanQtyByIndex(sr, i);
                if (planQty != null) {
                    total = total.add(planQty);
                }
            }

            // 未超出产能，无需顺延
            if (total.compareTo(quota) <= 0) {
                break;
            }

            // 超出产能：从末端顺位开始取出规格
            BigDecimal overflow = total.subtract(quota);
            List<DjScheduleResult> toShift = new ArrayList<>();
            List<DjScheduleResult> remainingInClass = new ArrayList<>(sortedBySeq);

            // 从末端开始移除
            while (!remainingInClass.isEmpty() && overflow.compareTo(BigDecimal.ZERO) > 0) {
                DjScheduleResult last = remainingInClass.remove(remainingInClass.size() - 1);
                BigDecimal lastPlanQty = getPlanQtyByIndex(last, i);
                if (lastPlanQty != null) {
                    toShift.add(last);
                    overflow = overflow.subtract(lastPlanQty);
                }
            }

            if (toShift.isEmpty()) {
                break;
            }

            if (i < 6) {
                // 顺延到下一个班次
                int nextClass = i + 1;
                // 反转保持相对顺序
                Collections.reverse(toShift);

                for (DjScheduleResult sr : toShift) {
                    BigDecimal planQty = getPlanQtyByIndex(sr, i);
                    Integer seq = getSequenceByIndex(sr, i);
                    String analysis = getAnalysisByIndex(sr, i);

                    // 原班次清理：计划量、顺位置NULL，原因分析保留并追加记录
                    String shiftRecord = ";因" + specName + "插单，该规格推迟生产班次class" + i + "->class" + nextClass;
                    setAnalysisByIndex(sr, i,
                            StringUtils.isNotBlank(analysis) ? analysis + shiftRecord : shiftRecord);
                    setPlanQtyByIndex(sr, i, null);
                    setSequenceByIndex(sr, i, null);

                    // 新班次：设置计划量和顺位
                    int maxSeqInNext = machineResults.stream()
                            .filter(r -> getSequenceByIndex(r, nextClass) != null)
                            .mapToInt(r -> getSequenceByIndex(r, nextClass))
                            .max().orElse(0);
                    setPlanQtyByIndex(sr, nextClass, planQty);
                    setSequenceByIndex(sr, nextClass, maxSeqInNext + 1);
                    String nextAnalysis = getAnalysisByIndex(sr, nextClass);
                    String nextShiftRecord = String.format(I18nUtil.getMessage("ui.data.column.scheduleResult.analysis.insert.to.current"), specName, i);
                    setAnalysisByIndex(sr, nextClass,
                            StringUtils.isNotBlank(nextAnalysis) ? nextAnalysis + ";" + nextShiftRecord : nextShiftRecord);
                }
                // 继续下一班次的产能检测
            } else {
                // 已到 class6（最后一个班次），无法继续顺延 → 减量处理
                for (DjScheduleResult sr : toShift) {
                    BigDecimal planQty = getPlanQtyByIndex(sr, i);
                    if (planQty == null || planQty.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    // 当前班次剩余总量
                    BigDecimal currentTotal = BigDecimal.ZERO;
                    for (DjScheduleResult r : remainingInClass) {
                        BigDecimal qty = getPlanQtyByIndex(r, i);
                        if (qty != null) {
                            currentTotal = currentTotal.add(qty);
                        }
                    }

                    BigDecimal available = quota.subtract(currentTotal);
                    if (available.compareTo(BigDecimal.ZERO) <= 0) {
                        // 整条规格移除
                        String analysis = getAnalysisByIndex(sr, i);
                        String reduceRecord = ";因" + specName + "插单减量" + planQty;
                        setAnalysisByIndex(sr, i,
                                StringUtils.isNotBlank(analysis) ? analysis + reduceRecord : reduceRecord);
                        setPlanQtyByIndex(sr, i, null);
                        setSequenceByIndex(sr, i, null);
                    } else if (planQty.compareTo(available) > 0) {
                        // 部分减量
                        BigDecimal reduceQty = planQty.subtract(available);
                        String analysis = getAnalysisByIndex(sr, i);
                        String reduceRecord = ";因" + specName + "插单减量" + reduceQty;
                        setAnalysisByIndex(sr, i,
                                StringUtils.isNotBlank(analysis) ? analysis + reduceRecord : reduceRecord);
                        setPlanQtyByIndex(sr, i, available);
                    } else {
                        // 可以容纳，无需减量
                        remainingInClass.add(sr);
                    }
                }
            }
        }

        // 4. 重新整理顺位：确保每个班次顺位从1开始连续递增
        for (int c = 1; c <= 6; c++) {
            reorganizeSequences(machineResults, c);
        }

        return machineResults;
    }

    /**
     * 顺位空洞整理：确保班次内顺位从1开始连续递增
     */
    private void reorganizeSequences(List<DjScheduleResult> results, int classIndex) {
        List<DjScheduleResult> withSeq = results.stream()
                .filter(r -> getSequenceByIndex(r, classIndex) != null)
                .sorted(Comparator.comparingInt(r -> getSequenceByIndex(r, classIndex)))
                .collect(Collectors.toList());

        int newSeq = 1;
        for (DjScheduleResult sr : withSeq) {
            setSequenceByIndex(sr, classIndex, newSeq++);
        }
    }

    /**
     * 3.4 减量后顺位空洞整理
     */
    @Override
    public void reorganizeAfterReduce(List<DjScheduleResult> machineResults, int classIndex) {
        this.reorganizeSequences(machineResults, classIndex);
    }

    // ==================== 班次字段访问工具方法 ====================

    /**
     * 根据班次索引获取顺位
     */
    @Override
    public Integer getSequenceByIndex(DjScheduleResult sr, int classIndex) {
        switch (classIndex) {
            case 1: return sr.getClass1Sequence();
            case 2: return sr.getClass2Sequence();
            case 3: return sr.getClass3Sequence();
            case 4: return sr.getClass4Sequence();
            case 5: return sr.getClass5Sequence();
            case 6: return sr.getClass6Sequence();
            default: return null;
        }
    }

    /**
     * 根据班次索引设置顺位
     */
    @Override
    public void setSequenceByIndex(DjScheduleResult sr, int classIndex, Integer seq) {
        switch (classIndex) {
            case 1: sr.setClass1Sequence(seq); break;
            case 2: sr.setClass2Sequence(seq); break;
            case 3: sr.setClass3Sequence(seq); break;
            case 4: sr.setClass4Sequence(seq); break;
            case 5: sr.setClass5Sequence(seq); break;
            case 6: sr.setClass6Sequence(seq); break;
            default: break;
        }
    }

    /**
     * 根据班次索引获取计划量
     */
    @Override
    public BigDecimal getPlanQtyByIndex(DjScheduleResult sr, int classIndex) {
        switch (classIndex) {
            case 1: return sr.getClass1PlanQty();
            case 2: return sr.getClass2PlanQty();
            case 3: return sr.getClass3PlanQty();
            case 4: return sr.getClass4PlanQty();
            case 5: return sr.getClass5PlanQty();
            case 6: return sr.getClass6PlanQty();
            default: return null;
        }
    }

    /**
     * 根据班次索引设置计划量
     */
    @Override
    public void setPlanQtyByIndex(DjScheduleResult sr, int classIndex, BigDecimal qty) {
        switch (classIndex) {
            case 1: sr.setClass1PlanQty(qty); break;
            case 2: sr.setClass2PlanQty(qty); break;
            case 3: sr.setClass3PlanQty(qty); break;
            case 4: sr.setClass4PlanQty(qty); break;
            case 5: sr.setClass5PlanQty(qty); break;
            case 6: sr.setClass6PlanQty(qty); break;
            default: break;
        }
    }

    /**
     * 根据班次索引获取原因分析
     */
    @Override
    public String getAnalysisByIndex(DjScheduleResult sr, int classIndex) {
        switch (classIndex) {
            case 1: return sr.getClass1Analysis();
            case 2: return sr.getClass2Analysis();
            case 3: return sr.getClass3Analysis();
            case 4: return sr.getClass4Analysis();
            case 5: return sr.getClass5Analysis();
            case 6: return sr.getClass6Analysis();
            default: return null;
        }
    }

    /**
     * 根据班次索引设置原因分析
     */
    @Override
    public void setAnalysisByIndex(DjScheduleResult sr, int classIndex, String analysis) {
        switch (classIndex) {
            case 1: sr.setClass1Analysis(analysis); break;
            case 2: sr.setClass2Analysis(analysis); break;
            case 3: sr.setClass3Analysis(analysis); break;
            case 4: sr.setClass4Analysis(analysis); break;
            case 5: sr.setClass5Analysis(analysis); break;
            case 6: sr.setClass6Analysis(analysis); break;
            default: break;
        }
    }

    /**
     * 获取班次映射：根据排程首班班次，返回 class1~class6 对应的真实班次
     */
    private String[] getShiftMapping(String scheduleShiftClass) {
        String[] mapping = new String[6];
        ClassNumThreePlanEnums current = ClassNumThreePlanEnums.getClassEnums(scheduleShiftClass);
        if (current == null) {
            current = ClassNumThreePlanEnums.CLASS_DAY; // 默认中班
        }
        for (int i = 0; i < 6; i++) {
            mapping[i] = current.getClassIndex();
            current = current.getNextClass();
        }
        return mapping;
    }

    /**
     * 根据映射，获取对应真实班次的完成量
     */
    private BigDecimal getFinishQtyByRealShift(DjDayFinishQty finishQty, String realShiftClass) {
        if (finishQty == null) {
            return BigDecimal.ZERO;
        }
        // "01"=夜班, "02"=早班, "03"=中班
        switch (realShiftClass) {
            case "01": return BigDecimalUtils.valueOf(finishQty.getNightFinishQty());
            case "02": return BigDecimalUtils.valueOf(finishQty.getDayFinishQty());
            case "03": return BigDecimalUtils.valueOf(finishQty.getMidFinishQty());
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 获取机台定额
     */
    private BigDecimal getMachineQuota(String machineCode) {
        List<DjMachineInfo> list = djEngineMachineMapper.selectList(
                new LambdaQueryWrapper<DjMachineInfo>()
                        .eq(DjMachineInfo::getMachineCode, machineCode));
        if (CollectionUtils.isNotEmpty(list)) {
            return list.get(0).getQuata();
        }
        return null;
    }

}

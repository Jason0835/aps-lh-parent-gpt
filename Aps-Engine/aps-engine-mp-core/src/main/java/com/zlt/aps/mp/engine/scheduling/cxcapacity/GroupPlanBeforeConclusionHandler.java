package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.GroupConclusionInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.enums.DeductionDayProductionTypeEnum;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.handler.GroupPlanConclusionHandler;
import com.zlt.aps.mp.engine.handler.GroupPlanDeductionDayHandler;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模拟排产：结构提前收尾处理
 *
 * @author ZLT
 * @date 20260115
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPlanBeforeConclusionHandler {
    /**
     * 结构收尾业务处理器
     */
    private final GroupPlanConclusionHandler groupPlanConclusionHandler;
    /**
     * 产能释放业务处理器
     */
    private final GroupPlanDeductionDayHandler groupPlanDeductionDayHandler;

    /**
     * 处理结构提前收尾
     * 实单排产量的硫化机台数低于最低硫化配比的硫化机台数，则进行提前收尾
     *
     * @param context                     排产上下文
     * @param groupPlanInfo               排产分组计划
     * @param continueCxMachineAllocation 在机结构机台分配情况
     */
    public void handlerBeforeConclusion(Context context, ProductionPlanGroupInfo groupPlanInfo, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        //20260211 需要拉量的特殊结构，不能进行提前收尾处理
        if (!groupPlanInfo.isHasBeforeConclusionHandler()) {
            return;
        }
        String groupName = groupPlanInfo.getGroupName();
        addGroupStartBeforeConclusionLog(context, groupName);
        //获取当前模拟排产的数据
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            //记录日志
            addNoAllocationInfoLog(context, groupName);
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //收尾业务判断
        GroupConclusionInfoVo groupConclusionInfo = groupPlanConclusionHandler.getConclusionInfoByProductionInfo(context, groupPlanInfo);
        if (null == groupConclusionInfo) {
            addBeforeConclusionConditionErrorInfoLog(context, groupName);
            return;
        }
        Integer minLhMachineCount = groupConclusionInfo.getMinLhMachineCount();
        if (!groupConclusionInfo.isSuccessFlag()) {
            addNoBeforeConclusionInfoLog(context, groupName, minLhMachineCount);
            return;
        }
        Integer beforeConclusionDay = groupConclusionInfo.getConclusionDay();
        Set<Integer> deductionDaySet = groupConclusionInfo.getDeductionDaySet();
        Integer deductionDay = groupConclusionInfo.getDeductionDay();
        //收尾机台选择
        CxMachineBaseInfoVo selectCxMachineInfo = groupPlanConclusionHandler.getConclusionCxMachine(context, groupPlanInfo);
        if (null == selectCxMachineInfo) {
            addNoFindBeforeConclusionCxMachineInfoLog(context, groupName);
            return;
        }
        groupConclusionInfo.addSelectedConclusionCxMachine(selectCxMachineInfo);
        String selectedCxMachineCode = selectCxMachineInfo.getCxMachineCode();

        List<CxMachineAllocationPlanHelper> allocationList = selectCxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            //记录日志
            addCxMachineNoAllocationInfoLog(context, groupName, selectedCxMachineCode);
            return;
        }
        CxMachineAllocationPlanHelper lastInfo = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        BeforeConclusionInfoHelper beforeConclusionInfo = BeforeConclusionInfoHelper.build(beforeConclusionDay, deductionDay, deductionDaySet);
        //更新数据
        addBeforeConclusionResultLog(context, groupName, minLhMachineCount, beforeConclusionDay, deductionDay);
        updateInfoByBeforeConclusion(productionContext, minLhMachineCount, beforeConclusionInfo, groupPlanInfo, selectCxMachineInfo, lastInfo, false);
    }

    /**
     * 处理结构提前收尾
     * 实单排产量的硫化机台数低于最低硫化配比的硫化机台数，则进行提前收尾
     *
     * @param context         排产上下文
     * @param groupPlanInfo   排产分组计划
     * @param cxMachineInfo   排产机台
     * @param cxLhRatio       对应的硫化配比信息
     * @param conclusionRange 当前排产分配段信息
     */
    public void handlerBeforeConclusion(Context context, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo cxLhRatio, CxMachineAllocationPlanHelper conclusionRange) {
        //20260211 需要拉量的特殊结构，不能进行提前收尾处理
        if (!groupPlanInfo.isHasBeforeConclusionHandler()) {
            return;
        }
        String groupName = groupPlanInfo.getGroupName();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        addGroupStartBeforeConclusionLog(context, groupName, cxMachineCode);

        GroupConclusionInfoVo groupConclusionInfo = groupPlanConclusionHandler.getConclusionInfoByProductionInfo(productionContext, groupPlanInfo, cxMachineInfo, cxLhRatio, conclusionRange);
        if (null == groupConclusionInfo) {
            addBeforeConclusionConditionErrorInfoLog(context, groupName);
            return;
        }
        Integer minLhMachineCount = groupConclusionInfo.getMinLhMachineCount();
        if (!groupConclusionInfo.isSuccessFlag()) {
            addNoBeforeConclusionInfoLog(context, groupName, minLhMachineCount);
            return;
        }
        //整段没有排产收尾
        if (groupConclusionInfo.isWholeRangeFlag()) {
            Set<Integer> deductionDaySet = cxMachineInfo.getLastProductionDayInfo();
            Integer beforeConclusionDay = conclusionRange.getStartDay();
            Integer deductionDay = conclusionRange.getAllocationDay();
            BeforeConclusionInfoHelper beforeConclusionInfo = BeforeConclusionInfoHelper.build(beforeConclusionDay, deductionDay, deductionDaySet);
            addBeforeConclusionResultLog(context, groupName, minLhMachineCount, beforeConclusionDay, deductionDay);
            updateInfoByBeforeConclusion(productionContext, minLhMachineCount, beforeConclusionInfo, groupPlanInfo, cxMachineInfo, conclusionRange, true);
            return;
        }
        //部分收尾
        Integer beforeConclusionDay = groupConclusionInfo.getConclusionDay();
        Integer deductionDay = groupConclusionInfo.getDeductionDay();
        Set<Integer> deductionDaySet = groupConclusionInfo.getDeductionDaySet();
        BeforeConclusionInfoHelper beforeConclusionInfo = BeforeConclusionInfoHelper.build(beforeConclusionDay, deductionDay, deductionDaySet);
        addBeforeConclusionResultLog(context, groupName, minLhMachineCount, beforeConclusionDay, deductionDay);
        //更新数据
        updateInfoByBeforeConclusion(productionContext, minLhMachineCount, beforeConclusionInfo, groupPlanInfo, cxMachineInfo, conclusionRange, true);
    }

    /**
     * 根据提前收尾日及收尾天数，更新数据
     * 1、分组计划-标记分配完成
     * 2、成型机台更新剩余天数
     * 3、分配信息更新调整分配信息
     * 4、是否需要清除排产信息？
     *
     * @param productionContext    排产上下文
     * @param minLhMachineCount    最低硫化配比
     * @param beforeConclusionInfo 提前收尾信息对象
     * @param groupPlanInfo        分组计划
     * @param cxMachineInfo        成型机台
     * @param allocationInfo       成型机台分配详情
     * @param isSingleMachine      是否单机台
     */
    private void updateInfoByBeforeConclusion(TbrProductionContext productionContext, Integer minLhMachineCount, BeforeConclusionInfoHelper beforeConclusionInfo, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper allocationInfo, boolean isSingleMachine) {
        //重新计算(分组)分配的天数: 需要排产天数 - 还需排产天数 - 收尾天数
        Integer deductionDay = beforeConclusionInfo.getDeductionDay();
        Integer beforeConclusionDay = beforeConclusionInfo.getBeforeConclusionDay();
        Set<Integer> deductionDaySet = beforeConclusionInfo.getDeductionDaySet();
        Integer realAllocationDayBeforeConclusion;
        if (!isSingleMachine) {
            Integer leftOverNeedAllocationDays = groupPlanInfo.getLeftOverNeedAllocationDays();
            Integer theoryDays = groupPlanInfo.getTheoryDays();
            realAllocationDayBeforeConclusion = theoryDays - leftOverNeedAllocationDays - deductionDay;
        } else {
            Integer allocationDay = allocationInfo.getAllocationDay();
            realAllocationDayBeforeConclusion = allocationDay - deductionDay;
        }
        Integer minAllocationDays = groupPlanInfo.getMinAllocationDays(productionContext);
        //20260119 如果提前收尾导致整个分配段不排产，则需要更新deductionDaySet的集合
        if (realAllocationDayBeforeConclusion < minAllocationDays) {
            //20260323 更新提前收尾信息 因还有可能后续持续分配到不同时间段，导致此次收尾不能直接标记不排产
            updateBeforeConclusionForAllocation(beforeConclusionInfo, cxMachineInfo, allocationInfo);
            beforeConclusionDay = beforeConclusionInfo.getBeforeConclusionDay();
            deductionDay = beforeConclusionInfo.getDeductionDay();
            deductionDaySet = beforeConclusionInfo.getDeductionDaySet();
        }
        //更新分配信息
        allocationInfo.beforeConclusion(beforeConclusionDay, deductionDay);
        if (CollectionUtils.isEmpty(deductionDaySet)) {
            return;
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionInfoMap;
        //已排产的模具信息
        if (isSingleMachine) {
            dayProductionInfoMap = cxMachineInfo.getDayProductionLimitInfo();
        } else {
            dayProductionInfoMap = groupPlanInfo.getDayProductionLimitInfo();
        }
        //资源释放(成型工装、日产能、模具、模壳、胶囊卡盘、模具分配比例、特殊原材料、换模次数)
        groupPlanDeductionDayHandler.deductionDayInfo(productionContext, DeductionDayProductionTypeEnum.FORCED_CLOSURE, cxMachineInfo, groupPlanInfo, allocationInfo, dayProductionInfoMap, deductionDaySet);
    }

    /**
     * 当结构提前收尾导致不满足最低排产天数时，收尾信息按整个分配段处理
     *
     * @param beforeConclusionInfo 提前收尾信息对象
     * @param cxMachineInfo        成型机台
     * @param allocationInfo       成型机台分配段
     */
    private void updateBeforeConclusionForAllocation(BeforeConclusionInfoHelper beforeConclusionInfo, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper allocationInfo) {
        Integer allocationStartDay = allocationInfo.getStartDay();
        Integer allocationEndDay = allocationInfo.getEndDay();
        Integer beforeConclusionDay = allocationStartDay;
        Integer deductionDay = allocationInfo.getAllocationDay();
        Set<Integer> deductionDaySet = beforeConclusionInfo.getDeductionDaySet();
        for (Integer productionDay = allocationStartDay; productionDay <= allocationEndDay; productionDay++) {
            if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                continue;
            }
            deductionDaySet.add(productionDay);
        }
        beforeConclusionInfo.updateInfo(beforeConclusionDay, deductionDay, deductionDaySet);
    }

    /**
     * 获取结构是否在其它成型机上有排产
     * true 表示有排产 false 表示没有排产
     *
     * @param productionContext 排产上下文
     * @param groupPlanInfo     排产计划
     * @param cxMachineInfo     成型机台
     * @param allocationInfo    当前排产段信息
     * @return
     */
    private boolean hasOtherProductionCxMachine(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper allocationInfo) {
        if (null == groupPlanInfo) {
            return false;
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return false;
        }
        List<CxMachineBaseInfoVo> otherProductionInfo = new ArrayList<>();
        allCxMachineInfo.forEach((cxMachineInfoCode, cxMachineProductionInfo) -> {
            if (cxMachineInfoCode.equals(cxMachineInfo.getCxMachineCode())) {
                return;
            }
            List<CxMachineAllocationPlanHelper> allocationList = cxMachineProductionInfo.getAllocationList();
            if (CollectionUtils.isEmpty(allocationList)) {
                return;
            }
            List<CxMachineAllocationPlanHelper> productionList = allocationList.stream().filter(allocationPlan -> allocationPlan.hasMatchProduction(allocationInfo)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionList)) {
                return;
            }
            otherProductionInfo.add(cxMachineProductionInfo);
        });
        if (CollectionUtils.isEmpty(otherProductionInfo)) {
            return false;
        }
        return true;
    }

    /**
     * 增加开始结构提前收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 进入提前收尾判断业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private String addGroupStartBeforeConclusionLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 进入提前收尾判断业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加开始结构提前收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 进入提前收尾判断业务 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 排产机台
     * @return
     */
    private String addGroupStartBeforeConclusionLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 进入提前收尾判断业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加没有分配信息-退出结构提前收尾业务逻辑日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有找到提前收尾机台，退出结构收尾业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private String addNoFindBeforeConclusionCxMachineInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有找到提前收尾机台，退出结构收尾业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加没有分配信息-退出结构提前收尾业务逻辑日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有分配信息，退出结构收尾业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private String addNoAllocationInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有分配信息，退出结构收尾业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加 收尾业务判断前置条件错误 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 提前收尾业务判断基础条件错误，退出提前结构收尾业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    private String addBeforeConclusionConditionErrorInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 提前收尾业务判断基础条件错误，退出提前结构收尾业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加没有低于最低硫化数，无需结构提前收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 高于最低成型硫化配比：%s，无需提前收尾 ====
     *
     * @param context    排程上下文
     * @param groupName  分组
     * @param minLhRatio 最低硫化配比
     * @return
     */
    private String addNoBeforeConclusionInfoLog(Context context, String groupName, Integer minLhRatio) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 高于最低成型硫化配比：%s，无需提前收尾 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, minLhRatio);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加成型机台没有分配信息错误，导致结构提前收尾业务错误日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 成型机台：%s 没有分配信息导致错误 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 成型机台
     * @return
     */
    private String addCxMachineNoAllocationInfoLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 成型机台：%s 没有分配信息导致错误 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加结构提前收尾结果日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 已低于最低成型硫化配比：%s，需在[%s]提前收尾，提前收尾天数[%s] ====
     *
     * @param context             排程上下文
     * @param groupName           分组
     * @param minLhRatio          最低硫化配比
     * @param beforeConclusionDay 提前收尾日
     * @param deductionDay        提前收尾天数
     * @return
     */
    private String addBeforeConclusionResultLog(Context context, String groupName, Integer minLhRatio, Integer beforeConclusionDay, Integer deductionDay) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 已低于最低成型硫化配比：%s，需在[%s]提前收尾，提前收尾天数[%s] ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, minLhRatio, beforeConclusionDay, deductionDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_BEFORE_CONCLUSION, logContent);
        return logContent;
    }
}

/**
 * 构建提前收尾信息对象
 *
 * @author ZLT
 * @date 20260115
 */
@Getter
class BeforeConclusionInfoHelper {

    private Integer beforeConclusionDay;

    private Integer deductionDay;

    private Set<Integer> deductionDaySet;

    /**
     * 构建新提前收尾信息对象
     *
     * @param beforeConclusionDay 收尾日
     * @param deductionDay        收尾天数
     * @param deductionDaySet     收尾日集合
     * @return
     */
    public static BeforeConclusionInfoHelper build(Integer beforeConclusionDay, Integer deductionDay, Set<Integer> deductionDaySet) {
        return new BeforeConclusionInfoHelper(beforeConclusionDay, deductionDay, deductionDaySet);
    }

    /**
     * 更新收尾信息
     *
     * @param beforeConclusionDay 新的收尾日
     * @param deductionDay        收尾天数
     * @param deductionDaySet     提前收尾的排产天集合
     */
    public void updateInfo(Integer beforeConclusionDay, Integer deductionDay, Set<Integer> deductionDaySet) {
        this.beforeConclusionDay = beforeConclusionDay;
        this.deductionDay = deductionDay;
        if (!CollectionUtils.isEmpty(deductionDaySet)) {
            this.deductionDaySet.addAll(deductionDaySet);
        }
    }

    /**
     * 构造函数
     *
     * @param beforeConclusionDay
     * @param deductionDay
     * @param deductionDaySet
     */
    private BeforeConclusionInfoHelper(Integer beforeConclusionDay, Integer deductionDay, Set<Integer> deductionDaySet) {
        this.beforeConclusionDay = beforeConclusionDay;
        this.deductionDay = deductionDay;
        this.deductionDaySet = deductionDaySet;
    }
}
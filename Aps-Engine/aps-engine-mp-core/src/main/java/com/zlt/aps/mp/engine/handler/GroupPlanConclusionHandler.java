package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.GroupConclusionInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.logrecorder.GroupPlanConclusionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分组收尾业务处理器
 * 判断 分组可进行收尾的业务处理
 * 1、按分组计划判断：在机结构在产机台阶段，按分组整体维度判断
 * 2、分组挑选机台、机台挑选分组时，按单机台维度判断
 * 实单排产的硫化机台数低于分组要求的最低配比Lh机台数
 * <p>
 * TBR:结构
 * PCR:英寸
 *
 * @author ZLT
 * @date 20260329
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPlanConclusionHandler {

    /**
     * 获取分组收尾信息
     *
     * @param context
     * @param groupPlanInfo
     * @return conclusionDay ：不再排产，即在此前一日为分组收尾日
     * deductionDaySet 需要清除的排产日信息
     */
    public GroupConclusionInfoVo getConclusionInfoByProductionInfo(Context context, ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return null;
        }
        String groupName = groupPlanInfo.getGroupName();
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        String cxMachineCodeInfo = "";
        if (!CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            cxMachineCodeInfo = allCxMachineCodeSet.stream().collect(Collectors.joining(StringConstant.COMMA));
        }
        GroupPlanConclusionLogRecorder.addGroupStartConclusionLog(context, groupName, cxMachineCodeInfo);
        //获取当前模拟排产的数据
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoAllocationInfoLog(context, groupName);
            return null;
        }
        Integer minLhMachineCount = groupPlanInfo.getClosureMinLhRatio();
        if (null == minLhMachineCount) {
            GroupPlanConclusionLogRecorder.addNoLhRatioInfoLog(context, groupName);
            return null;
        }
        //转化成模具数
        Integer minMouldNumber = minLhMachineCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        //获取使用模具数低于minMouldNumber的天数数据
        List<GroupPlanCxLhCapacityLimitHelper> lowMinMouldNumberList = dayLimitList.stream().filter(singleDay -> singleDay.isLowMinMouldNumber(minMouldNumber)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinMouldNumberList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return GroupConclusionInfoVo.buildNoConclusionInfo(minLhMachineCount);
        }
        //按日期排序
        lowMinMouldNumberList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        //取得最小和最大日期
        Integer conclusionDay = lowMinMouldNumberList.get(BigDecimal.ZERO.intValue()).getDay();
        Set<Integer> deductionDaySet = lowMinMouldNumberList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        return new GroupConclusionInfoVo(minLhMachineCount, conclusionDay, deductionDaySet);
    }

    /**
     * 从分组计划分配的成型机台中，获取提前收尾，配比大的机台
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组
     * @return
     */
    public CxMachineBaseInfoVo getConclusionCxMachine(Context context, ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return null;
        }
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MonthPlanStructureLhRatioVo> effectiveRatioList = new ArrayList<>();
        Map<MonthPlanStructureLhRatioVo, Set<String>> effectiveRationMap = new HashMap<>();
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        allCxMachineCodeSet.forEach(cxMachineCode -> {
            MonthPlanStructureLhRatioVo findLhRatio = groupPlanInfo.getLhRatio(cxMachineBaseInfo.get(cxMachineCode));
            if (null == findLhRatio) {
                return;
            }
            Set<String> cxMachineCodeSet = effectiveRationMap.get(findLhRatio);
            if (null == cxMachineCodeSet) {
                cxMachineCodeSet = new HashSet<>();
                effectiveRationMap.put(findLhRatio, cxMachineCodeSet);
            }
            cxMachineCodeSet.add(cxMachineCode);
            effectiveRatioList.add(findLhRatio);
        });
        if (CollectionUtils.isEmpty(effectiveRatioList)) {
            return null;
        }
        effectiveRatioList.sort(Comparator.comparing(MonthPlanStructureLhRatioVo::getLhMachineMaxQty));
        MonthPlanStructureLhRatioVo selectedLhRatio = effectiveRatioList.get(BigDecimal.ZERO.intValue());
        List<String> selectedCxMachineList = new ArrayList<>(effectiveRationMap.get(selectedLhRatio));
        Collections.sort(selectedCxMachineList);
        String selectedCxMachineCode = selectedCxMachineList.get(BigDecimal.ZERO.intValue());
        return cxMachineBaseInfo.get(selectedCxMachineCode);
    }
}

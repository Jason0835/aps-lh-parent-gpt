package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.enums.GroupCxMachineSelectedTypeEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSpecialMaterialProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成型产能优先级选择器
 *
 * @author ZLT
 * @date 20260320
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CxMachinePrioritySelector {
    /**
     * 对收尾机台，进行排序，
     * 优先对有排产含有特殊原材料分组的机台进行反选分组计划
     *
     * @param context              排产上下文
     * @param reverseCxMachineList 收尾机台
     */
    public void sortReverseCxMachineList(Context context, List<CxMachineBaseInfoVo> reverseCxMachineList) {
        if (CollectionUtils.isEmpty(reverseCxMachineList)) {
            return;
        }
        //记录在机结构是否有排产特殊结构
        if (isProductionSpecialMaterialByOnLineCxMachine(context)) {
            TbrSpecialMaterialProductionLogRecorder.addProductionSpecialMaterialInfoLog(context, "在机结构在产机台中有排产");
        }
        // 对收尾机台排序
        reverseCxMachineList.sort((before, after) -> {
            // 最先给有特殊结构在机的机台挑选
            Boolean beforeSpecial = this.hasSpecialStructure(before);
            Boolean afterSpecial = this.hasSpecialStructure(after);
            // Boolean的true比false大，倒序，优先处理true的
            int result = afterSpecial.compareTo(beforeSpecial);
            if (result != BigDecimal.ZERO.intValue()) {
                return result;
            }
            //其次最先收尾的先-剩余天数多的，倒序，越大的约优先
            Integer beforeRemainingDays = before.getRemainingDays();
            Integer afterRemainingDays = after.getRemainingDays();
            result = afterRemainingDays.compareTo(beforeRemainingDays);
            if (result != BigDecimal.ZERO.intValue()) {
                return result;
            }
            // 最后按机台编号顺序
            String beforeCode = before.getCxMachineCode();
            String afterCode = after.getCxMachineCode();
            return beforeCode.compareTo(afterCode);
        });
        String textContentFormat = "收尾机台：%s 有排产";
        reverseCxMachineList.forEach(singleMachine -> {
            if (hasSpecialStructure(singleMachine)) {
                String textContent = String.format(textContentFormat, singleMachine.getCxMachineCode());
                TbrSpecialMaterialProductionLogRecorder.addProductionSpecialMaterialInfoLog(context, textContent);
            }
        });
    }

    /**
     * 确定分组计划后，对还有多条可选机台，进行选择合适的机台
     * 1、固定优先
     * 2、与前分组同规格优先
     * 3、与前分组同英寸优先
     * 4、与前分组断面宽优先
     * 5、近1个月最近生产优先
     * 6、近n个月生产最多优先
     * 7、非零度优先
     * 8、机台编号大优先
     *
     * @param context              排产上下文
     * @param selectedCapacityList 可选择的产能机台
     * @param needProductionPlan   需排产的分组计划
     * @return
     */
    public CxMachineBaseInfoVo selectOptimalOneCxMachine(Context context, List<CxMachineBaseInfoVo> selectedCapacityList, ProductionPlanGroupInfo needProductionPlan) {
        //获取分组及零度零度供料架
        String structureName = needProductionPlan.getGroupName();
        String isZeroRack = needProductionPlan.getIsZero();
        //设置机台固定信息
        selectedCapacityList.stream().forEach(cxMachineInfo -> cxMachineInfo.setFixedPriority(cxMachineInfo.getFixedPriorityValue(needProductionPlan)));
        //1、固定优先
        Integer minFixedPriority = selectedCapacityList.stream().mapToInt(CxMachineBaseInfoVo::getFixedPriority).min().getAsInt();
        List<CxMachineBaseInfoVo> fixedPriorityList = selectedCapacityList.stream().filter(cxMachineInfo -> minFixedPriority.equals(cxMachineInfo.getFixedPriority())).collect(Collectors.toList());
        if (fixedPriorityList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo fixedSelected = fixedPriorityList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, fixedSelected.getCxMachineCode(), fixedSelected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.FIXED_PRIORITY);
            return fixedSelected;
        }
        //设置是否同规格，同英寸,断面宽
        setSameInfo(context, fixedPriorityList, needProductionPlan);
        //2、同规格优先
        List<CxMachineBaseInfoVo> sameSpecificationsList = fixedPriorityList.stream().filter(cxMachineInfo -> YesOrNoEnum.YES.getCode().equals(cxMachineInfo.getSameSpecifications())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameSpecificationsList)) {
            sameSpecificationsList = fixedPriorityList;
        }
        if (sameSpecificationsList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo selected = sameSpecificationsList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.SAME_SPECIFICATIONS_PRIORITY);
            return selected;
        }
        //3、同英寸优先
        List<CxMachineBaseInfoVo> sameProSizeList = sameSpecificationsList.stream().filter(cxMachineInfo -> YesOrNoEnum.YES.getCode().equals(cxMachineInfo.getSameProSize())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameProSizeList)) {
            sameProSizeList = sameSpecificationsList;
        }
        if (sameProSizeList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo selected = sameProSizeList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.SAME_PRO_SIZE_PRIORITY);
            return selected;
        }
        //4、断面宽优先
        List<CxMachineBaseInfoVo> sectionWidthList = sameSpecificationsList.stream().filter(cxMachineInfo -> YesOrNoEnum.YES.getCode().equals(cxMachineInfo.getSectionWidthCondition())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sectionWidthList)) {
            sectionWidthList = sameProSizeList;
        }
        if (sectionWidthList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.SECTION_WIDTH_PRIORITY);
            return selected;
        }
        //同规格优先 -> 同英寸优先 -> 断面宽优先 -> 历史最近优先 -> n个月生产最多优先 -> 非零度优先 -> 机台编号
        Comparator sortComparator = Comparator.comparing(CxMachineBaseInfoVo::getSameSpecifications, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getSameProSize, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getSectionWidthCondition, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getLastBoardingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getProductionCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CxMachineBaseInfoVo::getIsZeroRack)
                .thenComparing(CxMachineBaseInfoVo::getCxMachineCode, Comparator.reverseOrder());
        sectionWidthList.sort(sortComparator);
        CxMachineBaseInfoVo selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
        TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.HISTORY_QUALITY_PRIORITY);
        return selected;
    }

    /**
     * 在产机台是否有排产含有特殊原材料的分组计划
     * 只有有一台排过 = true，所有没排则 = false
     *
     * @param context 排产上下文
     * @return
     */
    private boolean isProductionSpecialMaterialByOnLineCxMachine(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineMap)) {
            return false;
        }
        //是否需要考虑当前空机台上个月在机结构的特殊原材料结构？
        for (Map.Entry<String, CxMachineBaseInfoVo> entry : allCxMachineMap.entrySet()) {
            CxMachineBaseInfoVo singleCxMachine = entry.getValue();
            if (null == singleCxMachine || CollectionUtils.isEmpty(singleCxMachine.getAllocationList())) {
                continue;
            }
            List<CxMachineAllocationPlanHelper> allocationList = singleCxMachine.getAllocationList();
            boolean isProduction = allocationList.stream().anyMatch(allocation -> allocation.getProductionPlanInfo().isSpecialMaterial());
            if (isProduction) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断机台是否排产含有特殊原材料的分组计划
     *
     * @param machine 机台
     * @return
     */
    private Boolean hasSpecialStructure(CxMachineBaseInfoVo machine) {
        return machine.getAllocationList().stream()
                .anyMatch(allocation -> allocation.getProductionPlanInfo().isSpecialMaterial());
    }

    /**
     * 设置同规格、同英寸，断面宽等信息
     *
     * @param context            排产上下文
     * @param fixedPriorityList  机台集合
     * @param needProductionPlan 需排产的分组计划
     */
    private void setSameInfo(Context context, List<CxMachineBaseInfoVo> fixedPriorityList, ProductionPlanGroupInfo needProductionPlan) {
        //4、断面宽差值±10 断面宽差值范围参数
        Integer diffValue = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getSectionWidthDiffValue();
        //设置是否同规格，同英寸,断面宽
        fixedPriorityList.forEach(cxMachineInfo -> cxMachineInfo.setSameInfoByCurrentGroupPlan(context, needProductionPlan, diffValue));
    }

}

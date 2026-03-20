package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.RawMaterialMonthDiff;
import com.zlt.aps.mp.api.enums.AlternativeTypeEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分组转产配置数据处理器
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260101
 */
@Slf4j
public class GroupProductionConversionHandler {
    /**
     * 获取最终的分组转产配置数据
     * 从上下文中的成型机台分配中转化数据
     * 按成型机台-分组维度进行存储
     *
     * @param productionContext 排产上下文
     * @return
     */
    public static List<MpStructureAllocation> getFinalResult(TbrProductionContext productionContext) {
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(cxMachineBaseInfo)) {
            return Collections.emptyList();
        }
        List<CxMachineAllocationPlanHelper> allAllocationList = new ArrayList<>();
        cxMachineBaseInfo.forEach((cxMachineCode, cxMachineInfo) -> {
            List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
            if (CollectionUtils.isEmpty(allocationList)) {
                return;
            }
            allAllocationList.addAll(allocationList);
        });
        if (CollectionUtils.isEmpty(allAllocationList)) {
            return Collections.emptyList();
        }
        Map<String, MpStructureAllocation> resultMap = new HashMap<>();
        allAllocationList.forEach(allocationDetail -> {
            String key = allocationDetail.getDuplicateKey();
            if (resultMap.containsKey(key)) {
                return;
            }
            MpStructureAllocation allocationInfo = conversion(allocationDetail);
            setProductionVersionInfo(allocationInfo, productionContext);
            resultMap.put(key, allocationInfo);
        });
        if (CollectionUtils.isEmpty(resultMap)) {
            return Collections.emptyList();
        }

        List<MpStructureAllocation> structureAllocationList = resultMap.values().stream().collect(Collectors.toList());
        //处理交替类型 sandy+ 2026.3.19
        setAlternatingType(structureAllocationList,productionContext.getContinueStructureMap());
        return structureAllocationList;
    }


    /**
     * 设置交替类型
     * @param structureAllocationList 结构分配列表
     * @param continueStructureMap 机台续作结构Map
     */
    private static void setAlternatingType(List<MpStructureAllocation> structureAllocationList,Map<String,String> continueStructureMap){
        List<MpStructureAllocation> machineStructureAllocationList;
        //按机台维度序列化
        Map<String, List< MpStructureAllocation>> structureAllocationMap = structureAllocationList.stream().collect(Collectors.groupingBy(MpStructureAllocation::getCxMachineCode));
        for (Map.Entry entry : structureAllocationMap.entrySet()) {
            machineStructureAllocationList = (List< MpStructureAllocation>)entry.getValue();
            machineStructureAllocationList.sort(Comparator.comparing(MpStructureAllocation::getBeginDay,
                    Comparator.nullsLast(Integer::compareTo)));
            //1. 将上个月有续作的机台,加到第1的位置
            MpStructureAllocation firstStructureAlloc = new MpStructureAllocation();
            String continueStructure = continueStructureMap.get(entry.getKey());
            firstStructureAlloc.setStructureName(StringUtil.isEmptyWithTrim(continueStructure) ? machineStructureAllocationList.get(0).getStructureName() : continueStructure);
            machineStructureAllocationList.add(0,firstStructureAlloc);

            // 2. 遍历判断相邻元素
            for (int i = 0; i < machineStructureAllocationList.size()-1; i++) {
                MpStructureAllocation current = machineStructureAllocationList.get(i);
                MpStructureAllocation next = machineStructureAllocationList.get(i + 1);
                if (!current.tbrProSize().equals(next.tbrProSize())){
                    next.setAlternatingType(AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode());
                }else{
                    next.setAlternatingType(AlternativeTypeEnum.STRUCT_ALTERNATIVE.getCode());
                }
            }
        }
    }

    /**
     * 对象转化处理
     *
     * @param allocationDetail
     * @return
     */
    private static MpStructureAllocation conversion(CxMachineAllocationPlanHelper allocationDetail) {
        MpStructureAllocation allocationInfo = new MpStructureAllocation();
        allocationInfo.setAllotDays(allocationDetail.getAllocationDay());
        allocationInfo.setBeginDay(allocationDetail.getStartDay());
        allocationInfo.setEndDay(allocationDetail.getEndDay());
        allocationInfo.setCxMachineCode(allocationDetail.getCxMachineCode());
        ProductionPlanGroupInfo productionPlanInfo = allocationDetail.getProductionPlanInfo();
        allocationInfo.setStructureName(productionPlanInfo.getGroupName());
        allocationInfo.setMaxEmbryoCodeCount(allocationDetail.getMaxEmbryoCodeCount());
        allocationInfo.setMaxLhMachineCount(allocationDetail.getMaxRatio());
        allocationInfo.setMinLhMachineCount(allocationDetail.getMinRatio());
        //20260205 特殊材料结构标记
        String isHasSpecialMaterial = productionPlanInfo.isSpecialMaterial() ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode();
        allocationInfo.setIsHasSpecialMaterial(isHasSpecialMaterial);
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return allocationInfo;
        }
        Integer sum = groupPlanData.stream().mapToInt(MonthPlanProductionRequirePlanVo::getNetQty).sum();
        Integer sumNetLossQty = groupPlanData.stream().mapToInt(MonthPlanProductionRequirePlanVo::getFactProdReqQty).sum();
        allocationInfo.setNetQty(sum);
        allocationInfo.setLossQty(sumNetLossQty);
        return allocationInfo;
    }

    /**
     * 设置结构转产表的版本信息
     *
     * @param allocationInfo 结构转产配置
     * @param context        版本信息
     */
    private static void setProductionVersionInfo(MpStructureAllocation allocationInfo, Context context) {
        if (null == allocationInfo || null == context) {
            return;
        }
        //工厂、年份、月份
        allocationInfo.setFactoryCode(context.getFactoryCode());
        allocationInfo.setYear(context.getYear());
        allocationInfo.setMonth(context.getMonth());
        //排产版本信息
        allocationInfo.setMonthPlanVersion(context.getMonthPlanVersion());
        allocationInfo.setProductionVersion(context.getProductionVersion());
        allocationInfo.setPlanType(context.getPlanType());

    }
}

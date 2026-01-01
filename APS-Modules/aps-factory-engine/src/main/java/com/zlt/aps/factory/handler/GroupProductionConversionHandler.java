package com.zlt.aps.factory.handler;

import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
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
            resultMap.put(key, allocationInfo);
        });
        if (CollectionUtils.isEmpty(resultMap)) {
            return Collections.emptyList();
        }
        return resultMap.values().stream().collect(Collectors.toList());
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
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return allocationInfo;
        }
        Long sum = groupPlanData.stream().mapToLong(MonthPlanProductionRequirePlanVo::getNetQty).sum();
        Long heightLossQty = groupPlanData.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightLossQty).sum();
        Long noHeightLossQty = groupPlanData.stream().mapToLong(MonthPlanProductionRequirePlanVo::getFactProdReqQty).sum();
        Long lossQty = heightLossQty + noHeightLossQty;
        allocationInfo.setNetQty(sum.intValue());
        allocationInfo.setLossQty(lossQty.intValue());
        return allocationInfo;
    }
}

package com.zlt.aps.mp.engine.basedata.assemble.continueinfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 续作分组信息设置
 *
 * @author ZLT
 * @date 20260418
 */
@Slf4j
@Component
public class ContinueGroupInfoHandler {

    /**
     * 构建续作分组信息
     *
     * @param context              排产上下文
     * @param allGroupPlanMap      本次所有结构需求计划
     * @param allCxContinueInfoMap 续作信息
     */
    public void buildContinueGroupInfo(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Map<String, CxContinueInfoHelper> allCxContinueInfoMap) {
        if (CollectionUtils.isEmpty(allCxContinueInfoMap)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        // 设置成型机台续作结构 sandy+ 2026.3.19
        productionContext.setContinueStructureMap(getContinueStructureMap(allCxContinueInfoMap));
        //20260418+ 设置续作结构的分组信息
        Map<String, ProductionPlanGroupInfo> continueGroupMap = new HashMap<>();
        allCxContinueInfoMap.forEach((structureName, continueSkuInfo) -> {
            ProductionPlanGroupInfo groupInfo = allGroupPlanMap.get(structureName);
            if (null != groupInfo) {
                continueGroupMap.put(structureName, groupInfo);
                return;
            }
            ProductionPlanGroupInfo virtualGroup = buildContinueGroupInfo(structureName, continueSkuInfo);
            if (null == virtualGroup) {
                return;
            }
            continueGroupMap.put(structureName, virtualGroup);
        });
        if (CollectionUtils.isEmpty(continueGroupMap)) {
            productionContext.setContinueGroupMap(Collections.emptyMap());
            return;
        }
        productionContext.setContinueGroupMap(continueGroupMap);
    }

    /**
     * 构建虚拟的续作-分组信息对象
     *
     * @param structureName   分组名
     * @param continueSkuInfo 续作Sku信息
     * @return
     */
    private ProductionPlanGroupInfo buildContinueGroupInfo(String structureName, CxContinueInfoHelper continueSkuInfo) {
        if (StringUtils.isBlank(structureName) || null == continueSkuInfo) {
            return null;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = continueSkuInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanInfo = Lists.newArrayList();
        ProductionPlanGroupInfo groupInfo = new ProductionPlanGroupInfo();
        groupInfo.setGroupName(structureName);
        groupInfo.setIsZero(YesOrNoEnum.NO.getCode());
        continueSkuInfoMap.forEach((materialDesc, singleSkuInfo) -> {
            MonthPlanProductionRequirePlanVo virtualPlan = MonthPlanProductionRequirePlanVo.buildVirtualPlanByContinue(singleSkuInfo);
            if (null == virtualPlan) {
                return;
            }
            if (YesOrNoEnum.YES.getCode().equals(virtualPlan.getIsZeroRack())) {
                groupInfo.setIsZero(YesOrNoEnum.YES.getCode());
            }
            continueSkuPlanInfo.add(virtualPlan);
        });
        if (CollectionUtils.isEmpty(continueSkuPlanInfo)) {
            return null;
        }
        groupInfo.setGroupPlanData(continueSkuPlanInfo);
        return groupInfo;
    }

    /**
     * 获取续作机台的结构信息
     *
     * @param cxContinueInfoMap
     * @return Map<成型机台 ， 续作结构>
     */
    private Map<String, String> getContinueStructureMap(Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        Map<String, String> machineStructureMap = Maps.newHashMap();
        if (PubUtil.isEmpty(cxContinueInfoMap)) {
            return machineStructureMap;
        }
        // 从续作信息中解析出成型机台对应的续作结构
        CxContinueInfoHelper cxContinueInfoHelper;
        for (Map.Entry<String, CxContinueInfoHelper> entry : cxContinueInfoMap.entrySet()) {
            cxContinueInfoHelper = entry.getValue();
            Set<String> cxMachineCodeSet = cxContinueInfoHelper.getCxMachineCodeSet();
            for (String machineCode : cxMachineCodeSet) {
                machineStructureMap.put(machineCode, entry.getKey());
            }
        }
        return machineStructureMap;
    }

}

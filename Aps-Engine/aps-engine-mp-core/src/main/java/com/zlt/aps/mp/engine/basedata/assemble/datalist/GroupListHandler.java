package com.zlt.aps.mp.engine.basedata.assemble.datalist;

import com.google.common.collect.Maps;
import com.zlt.aps.mp.api.domain.entity.MdmStructureName;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.ProductionProcessUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 结构清单信息
 *
 * @author ZLT
 * @date 20260703
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupListHandler {
    /**
     * 主数据数据提供接口
     */
    private final ProductionMdmDataService dataService;

    /**
     * 获取需求分配机台数限制最多1台的结构信息
     * key：结构名 value:1
     *
     * @return
     */
    public Map<String, Integer> getGroupMaxAllocationCxMachineInfo() {
        List<MdmStructureName> allStructureList = dataService.getAllStructureInfo();
        if (CollectionUtils.isEmpty(allStructureList)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> limitGroup = Maps.newHashMap();
        allStructureList.forEach(singleStructure -> {
            String groupName = singleStructure.getStructureName();
            if (StringUtils.isBlank(groupName)) {
                return;
            }
            if (ProductionProcessUtils.isYes(singleStructure.getIsMoreMachine())) {
                return;
            }
            limitGroup.put(groupName, BigDecimal.ONE.intValue());
        });
        return limitGroup;
    }

    /**
     * 判断分组是否达到增加机台限制
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组信息(TBR 结构)
     * @return
     */
    public static boolean isReachAddMachineLimit(Context context, ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo || null == context) {
            return true;
        }
        String groupName = groupPlanInfo.getGroupName();
        if (StringUtils.isBlank(groupName)) {
            return true;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, Integer> groupMachineLimitMap = productionContext.getBaseDataContainer().getGroupMachineLimitMap();
        if (CollectionUtils.isEmpty(groupMachineLimitMap)) {
            return false;
        }
        if (!groupMachineLimitMap.containsKey(groupName)) {
            return false;
        }
        Integer limitCount = groupMachineLimitMap.get(groupName);
        if (limitCount <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        Set<String> assignedMachineSet = groupPlanInfo.getAssignedMachineInfo(productionContext);
        if (CollectionUtils.isEmpty(assignedMachineSet)) {
            return false;
        }
        return assignedMachineSet.size() >= limitCount;
    }

}

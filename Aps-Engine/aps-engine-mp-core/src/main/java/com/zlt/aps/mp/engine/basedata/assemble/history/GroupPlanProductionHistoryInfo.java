package com.zlt.aps.mp.engine.basedata.assemble.history;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组生产历史对象
 * 以分组的视角看
 *
 * @author ZLT
 * @date 20260128
 */
@Data
public class GroupPlanProductionHistoryInfo implements Serializable {
    /**
     * 成型机台
     */
    private String groupName;
    /**
     * 近1个月最近排产日
     */
    private List<CxMachineLatestProductionInfo> latestProductionInfo;
    /**
     * 近n个月排产次数
     */
    private List<CxMachineProductionGroupInfo> productionCountInfo;


    /**
     * 近1个月有生产的机台信息，主要为最后一个排产日
     *
     * @return
     */
    public Map<String, CxMachineLatestProductionInfo> getProductionCxMachineLatestHistory() {
        if (CollectionUtils.isEmpty(latestProductionInfo)) {
            return Collections.emptyMap();
        }
        Map<String, CxMachineLatestProductionInfo> productionGroupMap = new HashMap<>();
        latestProductionInfo.forEach(singleGroupInfo -> productionGroupMap.put(singleGroupInfo.getCxMachineCode(), singleGroupInfo));
        return productionGroupMap;
    }

    /**
     * 获取近n个月有生产的机台信息，主要为生产月次数信息，每个月只要有生产则记为1次
     *
     * @return
     */
    public Map<String, CxMachineProductionGroupInfo> getProductionCxMachineHistory() {
        if (CollectionUtils.isEmpty(productionCountInfo)) {
            return Collections.emptyMap();
        }
        Map<String, CxMachineProductionGroupInfo> productionGroupInfoMap = new HashMap<>();
        productionCountInfo.forEach(singleGroupInfo -> productionGroupInfoMap.put(singleGroupInfo.getCxMachineCode(), singleGroupInfo));
        return productionGroupInfoMap;
    }
}

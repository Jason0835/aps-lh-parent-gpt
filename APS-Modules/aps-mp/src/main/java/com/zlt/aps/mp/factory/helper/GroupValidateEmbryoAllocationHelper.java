package com.zlt.aps.mp.factory.helper;

import com.zlt.aps.mp.engine.handler.embryobalance.DayEmbryoUsedInfo;
import com.zlt.aps.mp.engine.handler.embryobalance.EmbryoUsedLhMachineInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分组验证-胎胚分配信息对象-辅助类
 *
 * @author zlt
 * @date 2026-08-19
 */
@Slf4j
@Data
public class GroupValidateEmbryoAllocationHelper implements Serializable {
    /**
     * 分组名称 TBR-结构
     */
    private String groupName;
    /**
     * 排产-成型硫化产能限制
     * 包含 最大胎胚数
     * 最大硫化机台数
     * 实单最低硫化机台数
     * key=day : value=日成型硫化产能限制实例
     */
    private Map<Integer, DayEmbryoUsedInfo> dayProductionLimitInfo;
    /**
     * 日排产胎胚占用硫化机台信息
     * 中间值存储
     */
    private Map<Integer, List<EmbryoUsedLhMachineInfo>> dayEmbryoUsedLhMachineInfoMap;

    /**
     * 构建空信息
     *
     * @param groupName
     * @return
     */
    public static GroupValidateEmbryoAllocationHelper buildEmpty(String groupName) {
        GroupValidateEmbryoAllocationHelper groupInfo = new GroupValidateEmbryoAllocationHelper();
        groupInfo.setGroupName(groupName);
        groupInfo.setDayProductionLimitInfo(Collections.emptyMap());
        groupInfo.setDayEmbryoUsedLhMachineInfoMap(Collections.emptyMap());
        return groupInfo;
    }
}

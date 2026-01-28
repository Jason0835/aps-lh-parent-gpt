package com.zlt.aps.factory.basedata.history;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

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
}

package com.zlt.aps.factory.basedata.history;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 机台生产历史信息对象
 * 从机台的视角看
 *
 * @author ZLT
 * @date 20260128
 */
@Data
public class CxMachineProductionHistoryInfo implements Serializable {
    /**
     * 成型机台
     */
    private String cxMachineCode;
    /**
     * 近1个月最近排产日
     */
    private List<CxMachineLatestProductionInfo> latestProductionInfo;
    /**
     * 近n个月排产次数
     */
    private List<CxMachineProductionGroupInfo> productionCountInfo;
}

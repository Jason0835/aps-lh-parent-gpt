package com.zlt.aps.mp.engine.handler.appoint;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 在机结构续作胎胚强制下机优先级信息
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260810
 */
@Getter
public class DayReduceEmbryoPriorityInfo implements Serializable {
    /**
     * 生胎代码
     */
    private String embryoCode;

    private Integer usedLhMachine;

    private List<ContinueSkuDayUsedInfo> continueSkuInfo;

    public DayReduceEmbryoPriorityInfo(String embryoCode, Integer usedLhMachine, List<ContinueSkuDayUsedInfo> continueSkuInfo) {
        this.embryoCode = embryoCode;
        this.usedLhMachine = usedLhMachine;
        this.continueSkuInfo = continueSkuInfo;
    }
}

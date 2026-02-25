package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 日排产统计明细
 *
 * @author ZLT
 * @date 20260210
 */
@Data
public class MpDayProductionStatisticsDetailVo implements Serializable {

    /**
     * 胎胚种类数
     */
    private Integer embryoCount;
    /**
     * 硫化机台数
     */
    private Integer lhMachines;
    /**
     * 换模次数
     */
    private Integer changeMould;
}

package com.zlt.aps.xwyy.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: Chen
 * @since: 2022/5/25 17:24
 */
@Data
public class XwyyScheduleOriginalSumPlanVo implements Serializable {

    /**
     * 最大的帘布大卷编号
     */
    private String bigRollCode;

    /**
     * 原线代码
     */
    private String originalLineCode;

    /**
     * 原线品牌
     */
    private String originalBrand;

    /**
     * 原线长度
     */
    private String originalLineLength;

    /**
     * 总计划量
     */
    private Double sumPlan;
}

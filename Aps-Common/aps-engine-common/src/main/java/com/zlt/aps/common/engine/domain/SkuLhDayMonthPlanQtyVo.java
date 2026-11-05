package com.zlt.aps.common.engine.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 硫化余量计算：月计划转化成以日期为主的计划量
 *
 * @author ZLT
 * @date 20261103
 */
@Data
public class SkuLhDayMonthPlanQtyVo implements Serializable {
    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;
    /**
     * 施工阶段 00 无工艺 01 试制 02 量试 03 正式
     */
    @ApiModelProperty(value = "施工阶段 00 无工艺 01 试制 02 量试 03 正式", name = "constructionStage")
    private String constructionStage;

    /**
     * 月计划日期
     */
    @ApiModelProperty(value = "月计划日期", name = "planDate")
    private Date planDate;

    /**
     * 月计划日计划量
     */
    @ApiModelProperty(value = "月计划日计划量", name = "planQty")
    private Integer planQty;
}

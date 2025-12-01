package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanAdjustDetail;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanAdjustDetailVo.java
 * 描    述：调整通知单调整明细列表数据对象
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-0603
 */
@Data
@ApiModel(value = "调整通知单调整明细列表数据对象", description = "调整通知单调整明细列表数据对象")
public class MonthPlanAdjustDetailVo extends MonthPlanAdjustDetail {

    /**
     * 库位类别
     */
    @ApiModelProperty(value = "库位类别", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    @Override
    public Long getAdjustQty() {
        Long adjustQty = super.getAdjustQty();
        if (null == adjustQty) {
            adjustQty = BigDecimal.ZERO.longValue();
        }
        adjustQty = Math.abs(adjustQty);
        if (MonthPlanAdjustTypeEnum.SUBTRACT.getAdjustType().equals(getAdjustType())) {
            return -adjustQty;
        }
        return adjustQty;
    }
}

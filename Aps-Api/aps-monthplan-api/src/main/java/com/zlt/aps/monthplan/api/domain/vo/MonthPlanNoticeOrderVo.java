package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 月计划调整控制信息对象
 *
 * @author ZLT
 * @date 20250605
 */
@Data
@ApiModel(value = "月计划调整通知单编辑信息对象", description = "月计划调整通知单编辑信息对象")
public class MonthPlanNoticeOrderVo extends MonthPlanNoticeOrder {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "剩余库存量", name = "stockQty")
    private Long stockQty;
}
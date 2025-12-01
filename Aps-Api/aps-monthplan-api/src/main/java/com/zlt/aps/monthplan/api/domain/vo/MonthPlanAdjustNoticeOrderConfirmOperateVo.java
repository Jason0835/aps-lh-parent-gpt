package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 月计划调整通知单确认调整参数信息对象
 *
 * @author ZLT
 * @date 20250603
 */
@Data
@ApiModel(value = "月计划调整通知单确认调整参数信息对象", description = "月计划调整通知单确认调整参数信息对象")
public class MonthPlanAdjustNoticeOrderConfirmOperateVo implements Serializable {

    /**
     * 通知单号
     */
    @ApiModelProperty(value = "调整通知单号", name = "noticeNo")
    private String noticeNo;

    /**
     * 调整的计划集合
     */
    @ApiModelProperty(value = "调整的计划集合", name = "adjustPlanList")
    private List<FactoryMonthPlanProdFinal> adjustPlanList;
}

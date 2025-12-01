package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 月计划调整通知单应用调整参数对象
 *
 * @author ZLT
 * @date 20250528
 */
@Data
@ApiModel(value = "月计划调整通知单应用调整参数对象", description = "月计划调整通知单应用调整参数对象")
public class MonthPlanAdjustNoticeApplyOperateVo extends MonthPlanNoticeOrder {
    /**
     * 模具--调增时，需要界面输入
     */
    @ApiModelProperty(value = "模具--调增时，需要界面输入", name = "mouldNo")
    private String mouldNo;
    /**
     * 规格代号--调增时，需要界面输入
     */
    @ApiModelProperty(value = "规格代号--调增时，需要界面输入", name = "specCode")
    private String specCode;

    /**
     * 调整的数量 正数为调增，负数为调减
     */
    @ApiModelProperty(value = "调整的数量 正数为调增，负数为调减", name = "adjustNumber")
    private Long adjustNumber;
    /**
     * 本次调减计划信息
     */
    @ApiModelProperty(value = "本次调减计划信息", name = "applySubtract")
    private MonthPlanNeedAdjustPlanVo applySubtract;
}

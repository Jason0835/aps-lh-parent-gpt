package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 月计划调整通知单调整操作参数对象
 *
 * @author ZLT
 * @date 20250528
 */
@Data
@ApiModel(value = "月计划调整通知单调整操作参数对象", description = "月计划调整通知单调整操作参数对象")
public class MonthPlanAdjustNoticeOrderOperateVo extends MonthPlanNoticeOrder {
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

    @ApiModelProperty(value = "年-月", name = "yearMonth")
    private String yearMonth;
    /**
     * 起始的调整日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "起始的调整日期", name = "startDate")
    private Date startDate;
    /**
     * 调整的数量 正数为调增，负数为调减
     */
    @ApiModelProperty(value = "调整的数量 正数为调增，负数为调减", name = "adjustNumber")
    private Long adjustNumber;

    /**
     * 月份排产起始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产起始日", name = "productionStartDate")
    private Date productionStartDate;

    /**
     * 月份排产最大结束日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产最大结束日", name = "productionEndDate")
    private Date productionEndDate;

    /**
     * 0 不是自然月 1 是自然月
     */
    @ApiModelProperty(value = "0 不是自然月 1 是自然月", name = "isNaturalMonth")
    private Integer isNaturalMonth;
    /**
     * 是否忽略调减量与明细调减量不一致校验
     */
    @ApiModelProperty(value = "是否忽略调减量与明细调减量不一致校验", name = "isIgnoreInconsistent")
    private Integer isIgnoreInconsistent;
    /**
     * 确认需要调减的计划集合
     */
    @ApiModelProperty(value = "确认需要调减的计划集合", name = "confirmSubtractList")
    private List<MonthPlanNeedAdjustPlanVo> confirmSubtractList;
}

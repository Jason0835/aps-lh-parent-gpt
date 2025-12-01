package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 月计划调整通知单调整操作需调增的计划对象
 *
 * @author ZLT
 * @date 20250528
 */
@Data
@ApiModel(value = "月计划调整通知单调整操作需调增的计划对象", description = "月计划调整通知单调整操作需调增的计划对象")
public class MonthPlanNeedAdjustPlanVo implements Serializable {

    /**
     * 排产制造单号
     */
    @ApiModelProperty(value = "排产制造单号", name = "productionNo")
    private String productionNo;
    /**
     * 生产物料编号
     */
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;

    /**
     * 生产规格描述
     */
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    private String productDesc;
    /**
     * 起始的调整日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "起始的调整日期", name = "startAdjustDate")
    private Date startAdjustDate;
    /**
     * 需要调减的数量
     */
    @ApiModelProperty(value = "需要调整的数量", name = "needAdjustNumber")
    private Long needAdjustNumber;

    /**
     * 调整方式--后端使用
     */
    @ApiModelProperty(value = "调整方式--后端使用", name = "adjustType", hidden = true)
    private MonthPlanAdjustTypeEnum adjustType;
}

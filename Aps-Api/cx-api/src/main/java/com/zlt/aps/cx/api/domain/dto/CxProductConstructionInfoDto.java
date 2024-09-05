package com.zlt.aps.cx.api.domain.dto;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 投产施工值对象
 *
 */
@Data
public class CxProductConstructionInfoDto extends CxProductConstructionInfo {

	private static final long serialVersionUID = 1L;

	/** 外胎SAP品号 */
    @Excel(name = "ui.data.column.lhTireConstructionInfo.sapCode")
    @ApiModelProperty(value = "外胎SAP品号")
	@TableField(value = "FINISH_SAP_CODE")
    private String finishSapCode;

    /** 合模压力 */
    @Excel(name = "ui.data.column.lhTireConstructionInfo.clampingPressure")
    @ApiModelProperty(value = "合模压力")
	@TableField(value = "CLAMPING_PRESSURE")
    private BigDecimal clampingPressure;

    /** 硫化时间 */
    @Excel(name = "ui.data.column.lhTireConstructionInfo.curingTime")
    @ApiModelProperty(value = "硫化时间")
	@TableField(value = "CURING_TIME")
    private BigDecimal curingTime;
	
	/**
	 * 是否有排产标记
	 */
	private boolean isScheduleFlag;
}

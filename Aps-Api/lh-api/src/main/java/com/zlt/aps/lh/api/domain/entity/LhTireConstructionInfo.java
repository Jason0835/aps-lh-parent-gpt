package com.zlt.aps.lh.api.domain.entity;

import java.math.BigDecimal;

import com.zlt.aps.common.core.annotation.ImportValidated;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 硫化外胎施工信息对象 t_lh_tire_construction_info
 * 
 * @author zlt
 * @date 2021-11-15
 */
@ApiModel(value = "硫化外胎施工信息对象", description = "硫化外胎施工信息对象 ")
@Data
public class LhTireConstructionInfo extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 外胎SAP品号 */
    @ImportValidated(required = true,isCode = true,maxLength = 20)
    @Excel(name = "ui.data.column.lhTireConstructionInfo.sapCode")
    @ApiModelProperty(value = "外胎SAP品号")
    private String sapCode;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码")
    @Excel(name = "ui.data.column.lhTireConstructionInfo.embryoCode")
    private String embryoCode;

    @ApiModelProperty(value = "胎胚版本")
    @Excel(name = "ui.data.column.lhTireConstructionInfo.embryoVersion")
    private String embryoVersion;


    /** 合模压力 */
    @ImportValidated(number = true,min = 0,max = 9999.9)
    @Excel(name = "ui.data.column.lhTireConstructionInfo.clampingPressure")
    @ApiModelProperty(value = "合模压力")
    private BigDecimal clampingPressure;

    /** 硫化时间 */
    @ImportValidated(number = true,min = 0,max = 99999.9)
    @Excel(name = "ui.data.column.lhTireConstructionInfo.curingTime")
    @ApiModelProperty(value = "硫化时间")
    private BigDecimal curingTime;

    /**
     * 规格型号
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.cxScheduleResult.specDesc")
    @ApiModelProperty(value = "规格型号")
    private String specDesc;


    /** 删除标识 */
    private String delFlag;

    @ImportValidated(maxLength = 300)
    @Excel(name="ui.data.column.remark")
    private String remark;





}

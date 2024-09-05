package com.zlt.aps.common.engine.domain;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 硫化外胎施工信息对象 t_lh_tire_construction_info
 * 
 * @author Joran.zhang
 * @date 2021-09-04
 */
@ApiModel(value = "硫化外胎施工信息对象", description = "硫化外胎施工信息对象 ")
@Data
public class LhEngineTireConstructionInfo extends ApsBaseEntity{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 外胎SAP品号 */
    @Excel(name = "ui.data.column.info.sapCode")
    @ApiModelProperty(value = "外胎SAP品号")
    private String sapCode;

    /** 胎胚代码(代码)施工号 */
    @Excel(name = "ui.data.column.info.embryoCode")
    @ApiModelProperty(value = "胎胚代码(代码)施工号")
    private String embryoCode;

    /** 规格型号 */
    @Excel(name = "ui.data.column.info.specDesc")
    @ApiModelProperty(value = "规格型号")
    private String specDesc;

    /**
     * 胎胚施工版本
     */
    private String embryoVersion;

    /** 合模压力 */
    @Excel(name = "ui.data.column.info.clampingPressure")
    @ApiModelProperty(value = "合模压力")
    private Double clampingPressure;

    /** 硫化时间 */
    @Excel(name = "ui.data.column.info.curingTime")
    @ApiModelProperty(value = "硫化时间(分钟)")
    private Double curingTime;

    /**
     *  数据筛选条件
     */
    private List<String> sapCodeList;

}

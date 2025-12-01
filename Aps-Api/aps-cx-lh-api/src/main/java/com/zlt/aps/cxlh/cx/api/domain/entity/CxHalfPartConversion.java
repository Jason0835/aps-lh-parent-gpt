package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 半部件规格换算对象 t_cx_half_part_conversion
 * 
 * @author zlt
 * @date 2022-01-20
 */
@ApiModel(value = "半部件规格换算对象", description = "半部件规格换算对象 ")
@Data
public class CxHalfPartConversion extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 排程主键ID，可能为空 */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 排产日期 **/
    @ApiModelProperty(value = "排产日期")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date scheduleDate;

    /** 半部件类型 */
    @Excel(name = "ui.data.column.conversion.halfPartType")
    @ApiModelProperty(value = "半部件类型")
    private String halfPartType;

    /** 半部件编码 */
    @Excel(name = "ui.data.column.conversion.halfPartCode")
    @ApiModelProperty(value = "半部件编码")
    private String halfPartCode;

    /** 数量 */
    @Excel(name = "ui.data.column.conversion.plan")
    @ApiModelProperty(value = "数量")
    private Double plan;

    /** 单位 */
    @Excel(name = "ui.data.column.conversion.unit")
    @ApiModelProperty(value = "单位")
    private String unit;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.conversion.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 施工版本 */
    @Excel(name = "ui.data.column.conversion.bomDataVersion")
    @ApiModelProperty(value = "施工版本")
    private String bomDataVersion;

    /** 查询数量 */
    @Excel(name = "ui.data.column.conversion.queryPlan")
    @ApiModelProperty(value = "查询数量")
    private Long queryPlan;

    /** 机台名称 **/
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    /** 机台ID **/
    @ApiModelProperty(value = "机台ID")
    private String machineId;

    /** 中班计划量 **/
    @ApiModelProperty(value = "中班计划量")
    private Double class1Plan;

    /** 夜班计划量 **/
    @ApiModelProperty(value = "夜班计划量")
    private Double class2Plan;

    /** 白班计划量 **/
    @ApiModelProperty(value = "白班计划量")
    private Double class3Plan;

    /** 次日中班计划量 **/
    @ApiModelProperty(value = "次日中班计划量")
    private Double class4Plan;
    
    /**
     * 15度裁断2号钢带编号，其他半部件为空
     */
    @ApiModelProperty(value = "2号钢带编号")
    private String steelStripCode2;

    /** 发布状态：0、未发布，1、已发布，2、发布失败，3、发布中，4、超时失败 **/
    @ApiModelProperty(value = "发布状态")
    private String publishStatus;
}

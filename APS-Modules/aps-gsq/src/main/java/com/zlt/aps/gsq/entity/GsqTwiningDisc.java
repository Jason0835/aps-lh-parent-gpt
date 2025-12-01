package com.zlt.aps.gsq.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 钢丝圈缠绕盘信息表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GSQ_TWINING_DISC")
@ApiModel(value = "GsqTwiningDisc对象", description = "钢丝圈缠绕盘信息表")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class GsqTwiningDisc extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "编号，描述对应的缠绕盘所在信息对应内部编号信息，编号规则为唯一不可重复，每个缠绕盘都存在对应的唯一编号信息。")
    @TableField("SERIAL_NUMBER")
    private String serialNumber;

    @ApiModelProperty(value = "缠绕盘名称")
    @TableField("NAME")
    private String name;

    @ApiModelProperty(value = "序号，描述维护缠绕盘信息的顺序编号，不可重复。")
    @TableField("SEQ")
    private Long seq;

    @ApiModelProperty(value = "用途")
    @TableField("PURPOSE")
    private String purpose;

    @ApiModelProperty(value = "规格尺寸")
    @TableField("SPEC")
    private String spec;

    @ApiModelProperty(value = "排列方式")
    @TableField("ORDER_WAY")
    private String orderWay;

    @ApiModelProperty(value = "数量，描述对应的缠绕盘数量信息。")
    @TableField("TWINING_NUM")
    private Integer twiningNum;

    @ApiModelProperty(value = "入厂时间")
    @TableField("IN_TIME")
    private Date inTime;

    @ApiModelProperty(value = "报废时间")
    @TableField("SCRAP_TIME")
    private Date scrapTime;

    @ApiModelProperty(value = "报废原因")
    @TableField("SCRAP_REASON")
    private String scrapReason;

    @ApiModelProperty(value = "使用机台id（对应T_GSQ_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;
}

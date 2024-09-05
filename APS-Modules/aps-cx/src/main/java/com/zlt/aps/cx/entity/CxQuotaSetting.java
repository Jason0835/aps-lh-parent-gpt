package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;

/**
 * <p>
 * 成型定额设定表
 * </p>
 *
 * @author chen
 * @since 2021-06-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CX_QUOTA_SETTING")
@ApiModel(value = "CxQuotaSetting对象", description = "成型定额设定表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class CxQuotaSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "成型机台机型类型")
    @TableField("TYPE")
    private String machineType;

    @ApiModelProperty(value = "外胎规格尺寸信息")
    @TableField("SPEC_DIMENSION")
    private BigDecimal specDimension;

    @ApiModelProperty(value = "胎体布层数")
    @TableField("CARCASS_BOTH_LAYER")
    private Integer carcassBothLayer;

    @ApiModelProperty(value = "是否补强")
    @TableField("REINFORCE")
    private String reinforce;

    @ApiModelProperty(value = "轮胎类型")
    @TableField("TIRE_TYPE")
    private String tireType;

    @ApiModelProperty(value = "断面宽(下限)")
    @TableField("SECTION_WIDTH_MINIMUM")
    private Integer sectionWidthMinimum;

    @ApiModelProperty(value = "断面宽(上限)")
    @TableField("SECTION_WIDTH_MAXIMUM")
    private Integer sectionWidthMaximum;

    @ApiModelProperty(value = "两人定额")
    @TableField(value = "TWO_PERSON_QUOTA", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Integer twoPersonQuota;

    @ApiModelProperty(value = "单人折合定额")
    @TableField(value = "ONE_PERSON_QUOTA", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Integer onePersonQuota;
}

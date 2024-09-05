package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帘布大卷原线品牌对象 t_xwyy_big_roll_original_brand
 * 
 * @author chen
 * @date 2022-05-11
 */
@ApiModel(value = "帘布大卷原线品牌对象", description = "帘布大卷原线品牌对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_XWYY_BIG_ROLL_ORIGINAL_BRAND")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class XwyyBigRollOriginalBrand extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_PUBLIC */
    @ApiModelProperty(value = "id")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    /** 帘布大卷编号 */
    @Excel(name = "ui.data.column.bigRollOriginalBrand.bigRollCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    /** 原线品牌 */
    @Excel(name = "ui.data.column.bigRollOriginalBrand.brand")
    @ImportValidated(required = true, maxLength = 33)
    @ApiModelProperty(value = "原线品牌")
    @TableField("BRAND")
    private String brand;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}

package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SAP导入不良数对象 t_sap_import_bad_number
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
@ApiModel(value = "SAP导入不良数对象", description = "SAP导入不良数对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_SAP_IMPORT_BAD_NUMBER")
@KeySequence(value = "SEQ_SAP_IMPORT_BAD_NUMBER", clazz = Long.class)
public class SapImportBadNumber extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_SAP_IMPORT_BAD_NUMBER
     */
    @ApiModelProperty(value = "id")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    /**
     * SAP品号，外胎物料编号
     */
    @Excel(name = "ui.data.column.badNumber.sapCode")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @ApiModelProperty(value = "SAP品号，外胎物料编号")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /**
     * 不良数
     */
    @Excel(name = "ui.data.column.badNumber.badNum")
    @ImportValidated(required = true, digits = true, min = 0, maxLength = 10)
    @ApiModelProperty(value = "不良数")
    @TableField(value = "BAD_NUM")
    private Long badNum;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    private String remark;
}

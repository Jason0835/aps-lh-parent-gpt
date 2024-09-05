package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * SAP导入不良数对象 t_sap_import_bad_number
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
@ApiModel(value = "SAP导入不良数对象", description = "SAP导入不良数对象 ")
@Data
public class CxEngineSapImportBadNumber extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_NC_STOCK */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** SAP品号，外胎物料编号 */
    @ApiModelProperty(value = "SAP品号，外胎物料编号")
    private String sapCode;

    /** 不良数 */
    @ApiModelProperty(value = "不良数")
    private Integer badNum;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "不良数")
    private String delFlag;





}

package com.zlt.aps.cx.api.domain.entity;

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
 * 规格使用模数对象 t_sap_spec_mold_use
 * 
 * @author zlt
 * @date 2022-01-18
 */
@ApiModel(value = "规格使用模数对象", description = "规格使用模数对象 ")
@Data
public class SapSpecMoldUse extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** SAP品号 */
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.data.column.sapSpecMoldUse.sapCode",sort = 1)
    @ApiModelProperty(value = "物料编码")
    private String sapCode;

    /** 规格型号 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.sapSpecMoldUse.specDesc",sort = 2)
    @ApiModelProperty(value = "规格描述")
    private String specDesc;

    /** 胎胚代码 */
    @ImportValidated(maxLength = 20, isCode = true)
    @Excel(name = "ui.data.column.sapSpecMoldUse.embryoCode",sort = 3)
    @ApiModelProperty(value = "施工代码")
    private String embryoCode;

    /** 模具数量 */
    @ImportValidated(required = true, digits = true, min = 0, max = 99999999)
    @Excel(name = "ui.data.column.sapSpecMoldUse.moldNum",sort = 4)
    @ApiModelProperty(value = "模具数量")
    private Long moldNum;

    /** 删除标识 */
    @ApiModelProperty(value = "删除标识")
    private String delFlag;




}

package com.zlt.aps.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型胎胚共用模具信息对象 t_cx_share_mold_info
 *
 * @author chen
 * @date 2022-03-22
 */
@ApiModel(value = "成型胎胚共用模具信息对象", description = "成型胎胚共用模具信息对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
public class CxShareMoldInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 所属组别信息
     */
    @Excel(name = "ui.data.column.shareMoldInfo.groupName")
    @ApiModelProperty(value = "所属组别信息")
    @ImportValidated(required = true, isCode = true, maxLength = 90)
    private String groupName;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.construction.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String embryoCode;

    /**
     * SAP品号
     */
    @Excel(name = "ui.data.column.scheduleResult.sapCode")
    @ApiModelProperty(value = "SAP品号")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    private String sapCode;

    /**
     * 规格型号
     */
    @ApiModelProperty(value = "规格型号")
    private String specDesc;

    /**
     * 共用模具数量
     */
    @Excel(name = "ui.data.column.shareMoldInfo.shareMoldNum")
    @ApiModelProperty(value = "共用模具数量")
    @ImportValidated(required = true, digits = true, min = 0, max = 999999)
    private Integer shareMoldNum;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(maxLength = 300)
    private String remark;
}

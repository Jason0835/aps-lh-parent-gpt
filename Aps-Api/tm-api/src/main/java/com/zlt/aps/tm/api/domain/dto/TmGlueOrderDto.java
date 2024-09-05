package com.zlt.aps.tm.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 胎面胶料顺序维护
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
@Data
@ApiModel(value = "TmGlueOrder对象", description = "胎面胶料顺序维护")
public class TmGlueOrderDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1110056585174675869L;

    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    @ApiModelProperty(value = "胶料组别id", position = 20)
    private Long glueGroupId;

    @ApiModelProperty(value = "胶料组别代码", position = 21)
    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String glueGroupCode;

    @ApiModelProperty(value = "胶料组别名称", position = 22)
    @Excel(name = "ui.glueGroup.column.glueGroupName")
    private String glueGroupName;

    @ApiModelProperty(value = "胶料编号", position = 30)
    @Excel(name = "ui.glueOrder.column.glueCode")
    @ImportValidated(name = "ui.glueOrder.column.glueCode", required = true, isCode = true, maxLength = 30)
    private String glueCode;

    @ApiModelProperty(value = "胶料生产顺序", position = 40)
    @Excel(name = "ui.glueOrder.column.orderNum")
    @ImportValidated(name = "ui.glueOrder.column.orderNum", required = true, digits = true,min=0, max = 999)
    private Integer orderNum;

    @ApiModelProperty(value = "组别胶料生产顺序", position = 41)
    @Excel(name = "组别胶料生产顺序")
    private String glueGroupOrderNum;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}

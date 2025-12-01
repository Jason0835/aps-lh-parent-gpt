package com.zlt.aps.nc.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 内衬胶料顺序维护
 * </p>
 * @author zhangbinglin
 */
@Data
@ApiModel(value="NcGlueOrder对象", description="内衬胶料顺序维护")
public class NcGlueOrderDto extends ApsBaseDto implements Serializable {

    public static final long serialVersionUID = 1110056585174675869L;

    @ApiModelProperty(value = "主键ID", position = 10,hidden = true)
    public Long id;

    @ApiModelProperty(value = "胶料组别id", position = 20,hidden = true)
    public Long glueGroupId;

    @ApiModelProperty(value = "胶料组别代码", position = 21,hidden = true)
    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportValidated(name = "ui.glueGroup.column.glueGroupCode", required = true, isCode = true, maxLength = 30)
    public String glueGroupCode;

    @ApiModelProperty(value = "胶料组别名称", position = 22,hidden = true)
    @Excel(name = "ui.glueGroup.column.glueGroupName")
    public String glueGroupName;

    @ApiModelProperty(value = "胶料编号", position = 30,hidden = true)
    @Excel(name = "ui.glueOrder.column.glueCode")
    @ImportValidated(name = "ui.glueOrder.column.glueCode", required = true, isCode = true, maxLength = 30)
    public String glueCode;

    @ApiModelProperty(value = "胶料生产顺序", position = 40,hidden = true)
    @Excel(name = "ui.glueOrder.column.orderNum")
    @ImportValidated(name = "ui.glueOrder.column.orderNum", required = true, digits = true, min=0,max = 999)
    public Integer orderNum;

    @ApiModelProperty(value = "组别胶料生产顺序", position = 41,hidden = true)
    @Excel(name = "ui.glueOrder.column.groupGlue.orderNum")
    public String glueGroupOrderNum;

    @ApiModelProperty(value = "备注", position = 50,hidden = true)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    public String remark;
}

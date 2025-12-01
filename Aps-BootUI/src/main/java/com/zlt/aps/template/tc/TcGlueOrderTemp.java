package com.zlt.aps.template.tc;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 胎侧胶料顺序维护
 * </p>
 * @author zhangbinglin
 */
@Data
@ApiModel(value="TcGlueOrder对象", description="胎侧胶料顺序维护")
public class TcGlueOrderTemp extends ApsBaseEntity implements Serializable {

    public static final long serialVersionUID = 1110056585174675869L;


    @ApiModelProperty(value = "胶料组别代码", position = 21)
    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportValidated(name = "ui.glueGroup.column.glueGroupCode", required = true, isCode = true, maxLength = 30)
    public String glueGroupCode;


    @ApiModelProperty(value = "胶料编号", position = 30)
    @Excel(name = "ui.glueOrder.column.glueCode")
    @ImportValidated(name = "ui.glueOrder.column.glueCode", required = true, isCode = true, maxLength = 30)
    public String glueCode;

    @ApiModelProperty(value = "胶料生产顺序", position = 40)
    @Excel(name = "ui.glueOrder.column.orderNum")
    @ImportValidated(name = "ui.glueOrder.column.orderNum", required = true, digits = true, max = 999)
    public Integer orderNum;


    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    public String remark;
}

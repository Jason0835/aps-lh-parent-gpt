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
 * 胎侧胶料组别顺序维护
 * </p>
 *
 */
@Data
@ApiModel(value="TcGlueGroupOrder对象", description="胎侧胶料组别顺序维护")
public class TcGlueGroupOrderTemp extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1110056585174675869L;

    @ApiModelProperty(value = "胶料组别代码", position = 20)
    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportValidated(name = "ui.glueGroup.column.glueGroupCode", required = true, isCode = true, maxLength = 30)
    private String glueGroupCode;

    @ApiModelProperty(value = "胶料组别名称", position = 30)
    @Excel(name = "ui.glueGroup.column.glueGroupName")
    @ImportValidated(name = "ui.glueGroup.column.glueGroupName", required = true, maxLength = 15)
    private String glueGroupName;

    @ApiModelProperty(value = "生产顺序", position = 40)
    @Excel(name = "ui.glueGroup.column.orderNum")
    @ImportValidated(name = "ui.glueGroup.column.orderNum", required = true, max = 999, digits = true)
    private Integer orderNum;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}

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
 * 胎面胶料组别顺序维护
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
@Data
@ApiModel(value = "TmGlueGroupOrder对象", description = "胎面胶料组别顺序维护")
public class TmGlueGroupOrderDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1110056585174675869L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "胶料组别code", position = 20)
    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportValidated(name = "ui.glueGroup.column.glueGroupCode", required = true, isCode = true, maxLength = 30)
    private String glueGroupCode;

    @ApiModelProperty(value = "胶料组别名称", position = 30)
    @Excel(name = "ui.glueGroup.column.glueGroupName")
    @ImportValidated(name = "ui.glueGroup.column.glueGroupName", required = true, maxLength = 15)
    private String glueGroupName;

    @ApiModelProperty(value = "生产顺序", position = 40)
    @Excel(name = "ui.glueGroup.column.orderNum")
    @ImportValidated(name = "ui.glueGroup.column.orderNum", required = true, min = 0,max = 999, digits = true)
    private Integer orderNum;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}

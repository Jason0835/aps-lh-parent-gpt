package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 内衬胶料组别顺序维护
 * </p>
 *
 * @author zhangbinglin
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_GLUE_GROUP_ORDER")
@ApiModel(value = "NcGlueGroupOrder对象", description = "内衬胶料组别顺序维护")
public class NcGlueGroupOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "胶料组别code", position = 20)
    @TableField("GLUE_GROUP_CODE")
    private String glueGroupCode;

    @Excel(name = "ui.glueGroup.column.glueGroupName")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "胶料组别名称", position = 30)
    @TableField("GLUE_GROUP_NAME")
    private String glueGroupName;

    @Excel(name = "ui.glueGroup.column.orderNum")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产顺序", position = 40)
    @TableField("ORDER_NUM")
    private Integer orderNum;
}

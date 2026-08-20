package com.zlt.aps.nc.api.domain.entity;

import java.io.Serializable;

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
 * 胎侧胶料顺序维护
 * </p>
 *
 * @author zhangbinglin
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_GLUE_ORDER")
@ApiModel(value = "NcGlueOrder对象", description = "胎侧胶料顺序维护")
public class NcGlueOrder extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "胶料组别id，对应NC_GLUE_GROUP_ORDER表主键id")
    @TableField("GLUE_GROUP_ID")
    private Long glueGroupId;

    @Excel(name = "ui.glueOrder.column.glueCode")
    @ApiModelProperty(value = "胶料编号")
    @ImportExcelValidated(required = true)
    @TableField("GLUE_CODE")
    private String glueCode;

    @Excel(name = "ui.glueOrder.column.orderNum")
    @ApiModelProperty(value = "生产顺序")
    @ImportExcelValidated(required = true)
    @TableField("ORDER_NUM")
    private Integer orderNum;

    /** 反显字段：胶料组代码 */
    @Excel(name = "ui.glueGroup.column.glueGroupCode")
    @ImportExcelValidated(required = true)
    @TableField(exist = false)
    @ApiModelProperty(value = "胶料组代码")
    private String glueGroupCode;

    /** 反显字段：胶料组名称 */
    @Excel(name = "ui.glueGroup.column.glueGroupName")
    @TableField(exist = false)
    @ApiModelProperty(value = "胶料组名称")
    private String glueGroupName;

    /** 反显字段：胶料组序号 */
    @Excel(name = "ui.glueOrder.column.groupGlue.orderNum")
    @TableField(exist = false)
    @ApiModelProperty(value = "胶料组序号")
    private Integer glueGroupOrderNum;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}

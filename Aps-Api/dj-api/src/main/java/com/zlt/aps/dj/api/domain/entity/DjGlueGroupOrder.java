package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 垫胶胶料组别顺序维护
 * </p>
 *
 * @author zlt
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("T_DJ_GLUE_GROUP_ORDER")
@ApiModel(value = "DjGlueGroupOrder对象", description = "垫胶胶料组别顺序维护")
public class DjGlueGroupOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name="ui.dj.glueGroupOrder.column.glueGroupCode")
    @ApiModelProperty(value = "胶料组别code", position = 20)
    @TableField("GLUE_GROUP_CODE")
    private String glueGroupCode;

    @Excel(name="ui.dj.glueGroupOrder.column.glueGroupName")
    @ApiModelProperty(value = "胶料组别名称", position = 30)
    @TableField("GLUE_GROUP_NAME")
    private String glueGroupName;

    @Excel(name="ui.dj.glueGroupOrder.column.orderNum")
    @ApiModelProperty(value = "生产顺序", position = 40)
    @TableField("ORDER_NUM")
    private Integer orderNum;
}

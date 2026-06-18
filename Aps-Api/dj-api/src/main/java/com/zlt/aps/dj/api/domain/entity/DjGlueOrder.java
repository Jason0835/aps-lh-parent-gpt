package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 垫胶胶料顺序维护
 * </p>
 *
 * @author zlt
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_DJ_GLUE_ORDER")
@ApiModel(value = "DjGlueOrder对象", description = "垫胶胶料顺序维护")
public class DjGlueOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 胶料组别id，对应T_DJ_GLUE_GROUP_ORDER表主键id
     */
    @ApiModelProperty(value = "胶料组别id")
    @TableField("GLUE_GROUP_ID")
    private Long glueGroupId;

    @Excel(name="ui.dj.glueGroupOrder.column.glueCode")
    @ApiModelProperty(value = "胶料编号")
    @TableField("GLUE_CODE")
    private String glueCode;

    @Excel(name="ui.dj.glueGroupOrder.column.orderNum")
    @ApiModelProperty(value = "生产顺序")
    @TableField("ORDER_NUM")
    private Integer orderNum;
}

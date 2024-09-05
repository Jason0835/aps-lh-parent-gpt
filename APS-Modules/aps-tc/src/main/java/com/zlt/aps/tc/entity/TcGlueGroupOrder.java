package com.zlt.aps.tc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

/**
 * <p>
 * 胎侧胶料组别顺序维护
 * </p>
 *
 * @author zhangbinglin
 */
@Data
@Getter
@TableName("T_TC_GLUE_GROUP_ORDER")
@ApiModel(value = "TcGlueGroupOrder对象", description = "胎侧胶料组别顺序维护")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class TcGlueGroupOrder extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1110056585174675869L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "胶料组别code", position = 20)
    @TableField("GLUE_GROUP_CODE")
    private String glueGroupCode;

    @ApiModelProperty(value = "胶料组别名称", position = 30)
    @TableField("GLUE_GROUP_NAME")
    private String glueGroupName;

    @ApiModelProperty(value = "生产顺序", position = 40)
    @TableField("ORDER_NUM")
    private Integer orderNum;
}
